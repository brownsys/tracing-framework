package edu.brown.cs.systems.pivottracing.agent.advice.baggage;

import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import edu.brown.cs.systems.baggage.Namespace;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.Filter;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.FilterSpec;
import edu.brown.cs.systems.pivottracing.agent.advice.baggage.BaggageAPI.Pack;
import edu.brown.cs.systems.pivottracing.agent.advice.baggage.BaggageAPI.Unpack;
import edu.brown.cs.systems.pivottracing.baggage.BaggageProtos.Bag;
import edu.brown.cs.systems.pivottracing.baggage.BaggageProtos.FilterBag;
import edu.brown.cs.systems.pivottracing.baggage.BaggageProtos.SimpleTuple;

public class BagFiltered implements Pack, Unpack {

    public final Namespace<ByteString, ByteString> ACTIVE, ARCHIVE;
    public final ByteString bagId;
    public final FilterSpec spec;
    public final Filter filter;
    public final int tupleSize;

    public BagFiltered(ByteString bagId, FilterSpec spec, Namespace<ByteString, ByteString> active, Namespace<ByteString, ByteString> archive) {
        this.bagId = bagId;
        this.spec = spec;
        this.filter = spec.getFilter();
        this.tupleSize = spec.getVarCount();
        this.ACTIVE = active;
        this.ARCHIVE = archive;
    }
    
    @Override
    public void pack(List<Object[]> tuples) {
        // FIRST means we ignore tuples if they already exist
        if (filter == Filter.FIRST && (ACTIVE.has(bagId) || ARCHIVE.has(bagId))) {
            return;
        }
        
        // MOSTRECENT means we just clear out all previous
        if (filter == Filter.MOSTRECENT) {
            ACTIVE.remove(bagId);
            ARCHIVE.remove(bagId);
        }
        
        // If we make it here, we should pack a new bag and replace any previous bag
        Bag.Builder newBag = Bag.newBuilder().setVersionId(BaggageAPIImpl.random());
        FilterBag.Builder filterBag = newBag.getFilterBagBuilder();
        for (Object[] tuple : tuples) {
            SimpleTuple.Builder tupleProto = filterBag.addTupleBuilder();
            for (Object value : tuple) {
                tupleProto.addValue(String.valueOf(value));
            }
        }
        
        // If it turns out there are no tuples in the bag, remove it from the baggage
        if (filterBag.getTupleCount() == 0) {
            ACTIVE.remove(bagId);
            return;
        }
        ACTIVE.replace(bagId, newBag.build().toByteString());
    }

    @Override
    public Object[][] unpack() {
        // Most of the filter logic is done in PACK.  We could do some checking here, but for now we won't bother
        List<Object[]> tuples = Lists.newArrayList();
        for (ByteString serialized : Iterables.concat(ACTIVE.get(bagId), ARCHIVE.get(bagId))) {
            try {
                Bag bag = Bag.parseFrom(serialized);
                if (bag.hasFilterBag()) {
                    for (SimpleTuple tuple : bag.getFilterBag().getTupleList()) {
                        tuples.add(tuple.getValueList().toArray());
                    }
                }
            } catch (InvalidProtocolBufferException e) {
            }
        }        
        return tuples.toArray(new Object[tuples.size()][]);
    }

}