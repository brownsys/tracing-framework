package edu.brown.cs.systems.pivottracing.agent.advice.baggage;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import edu.brown.cs.systems.baggage.Namespace;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.TupleSpec;
import edu.brown.cs.systems.pivottracing.agent.advice.InvalidAdviceException;
import edu.brown.cs.systems.pivottracing.agent.advice.baggage.BaggageAPI.Pack;
import edu.brown.cs.systems.pivottracing.agent.advice.baggage.BaggageAPI.Unpack;
import edu.brown.cs.systems.pivottracing.baggage.BaggageProtos.Bag;
import edu.brown.cs.systems.pivottracing.baggage.BaggageProtos.SimpleTuple;
import edu.brown.cs.systems.pivottracing.baggage.BaggageProtos.TupleBag;

public class BagTuples implements Pack, Unpack {

    public final Namespace<ByteString, ByteString> ACTIVE, ARCHIVE;
    public final TupleSpec spec;
    public final ByteString bagId;
    public final int tupleSize;

    BagTuples(ByteString bagId, TupleSpec spec, Namespace<ByteString, ByteString> active, Namespace<ByteString, ByteString> archive) throws InvalidAdviceException {
        this.bagId = bagId;
        this.spec = spec;
        this.tupleSize = spec.getVarCount();
        this.ACTIVE = active;
        this.ARCHIVE = archive;
    }

    @Override
    public void pack(List<Object[]> tuples) {          
        // Pull out the current active bag(s)
        Set<ByteString> serializedActive = ACTIVE.get(bagId);
        Bag.Builder active = Bag.newBuilder();
        TupleBag.Builder tupleBag = active.getTupleBagBuilder();
        for (ByteString serialized : serializedActive) {
            try {
                Bag bag = Bag.parseFrom(serialized);
                if (bag.hasTupleBag()) {
                    tupleBag.mergeFrom(bag.getTupleBag());
                    active.setVersionId(bag.getVersionId());
                }
            } catch (InvalidProtocolBufferException e) {
            }
        }

        // Serialize tuples and add to the bag
        for (Object[] tuple : tuples) {
            if (tuple.length != tupleSize) continue;
            SimpleTuple.Builder tupleProto = tupleBag.addTupleBuilder();
            for (Object value : tuple) {
                tupleProto.addValue(String.valueOf(value));
            }
        }
        
        // If it turns out there are no tuples in the bag, remove it from the baggage
        if (tupleBag.getTupleCount() == 0) {
            ACTIVE.remove(bagId);
            return;
        }
        
        // We try to pick up a version ID of a previous active bag, but if there is non, generate one
        if (!active.hasVersionId()) {
            active.setVersionId(BaggageAPIImpl.random());
        }
        ACTIVE.replace(bagId, active.build().toByteString());
    }

    @Override
    public Object[][] unpack() {
        // Grab the active bags. If there are more than one active bag, merge and re-pack.  Rarely should this happen
        Set<ByteString> activeBags = ACTIVE.get(bagId); 
        if (activeBags.size() > 1) {
            pack(Lists.<Object[]>newArrayList());
            activeBags = ACTIVE.get(bagId);
        }
        
        // Grab the archive bags. Return immediately if no tuples.
        Set<ByteString> archiveBags = ARCHIVE.get(bagId);
        if (activeBags.size() == 0 && archiveBags.size() == 0) {
            return new Object[0][];
        }
        
        // Deserialize the tuples
        List<Object[]> tuples = Lists.newArrayList();
        for (ByteString serialized : Iterables.concat(activeBags, archiveBags)) {
            try {
                Bag bag = Bag.parseFrom(serialized);
                if (bag.hasTupleBag()) {
                    for (SimpleTuple tuple : bag.getTupleBag().getTupleList()) {
                        if (tuple.getValueCount() == tupleSize) {
                            tuples.add(tuple.getValueList().toArray());
                        }
                    }
                }
            } catch (InvalidProtocolBufferException e) {
            }
        }        
        return tuples.toArray(new Object[tuples.size()][]);
    }
}