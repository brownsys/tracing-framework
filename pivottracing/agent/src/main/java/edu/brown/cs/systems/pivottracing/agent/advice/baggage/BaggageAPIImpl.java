package edu.brown.cs.systems.pivottracing.agent.advice.baggage;

import java.util.List;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import edu.brown.cs.systems.baggage.BaggageContents;
import edu.brown.cs.systems.baggage.BaggageImpl;
import edu.brown.cs.systems.baggage.Handlers;
import edu.brown.cs.systems.baggage.Namespace;
import edu.brown.cs.systems.baggage.Handlers.BaggageHandler;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.PackSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.UnpackSpec;
import edu.brown.cs.systems.pivottracing.agent.advice.InvalidAdviceException;
import edu.brown.cs.systems.pivottracing.baggage.BaggageProtos.Bag;
import edu.brown.cs.systems.tracing.Utils;

/** Baggage API implementation.
 * Uses two baggage namespaces -- a namespace for active bags that may be modified, and a namespace for archived bags
 * Once an execution splits, the active bag is archived.
 * This implementation just uses randomly generated IDs for active bags, rather than ITCs, so bags are never unarchived
 */
public class BaggageAPIImpl implements BaggageAPI {
    
    // Invoked when executions branch and join
    private static final PTBaggageHandler baggageHandler = new PTBaggageHandler();
    static {
        Handlers.registerBaggageHandler(baggageHandler);
    }
    
    // Generates random bag IDs
    public static final Random r = new Random((Utils.getHost() + Utils.getProcessID() + Utils.getProcessName()).hashCode());
    public static long random() {
        return r.nextLong();
    }

    /* For now, PT bags are statically assigned the namespace for the byte '3' and '4' **/
    public static final ByteString PT_ACTIVE_NAMESPACE = ByteString.copyFrom(new byte[] { 0x03 });
    public static final ByteString PT_ARCHIVE_NAMESPACE = ByteString.copyFrom(new byte[] { 0x04 });
    
    public final Namespace<ByteString, ByteString> ACTIVE, ARCHIVE;

    /** API for test */
    public BaggageAPIImpl(Namespace<ByteString, ByteString> active, Namespace<ByteString, ByteString> archive) {
        this.ACTIVE = active;
        this.ARCHIVE = archive;
    }

    public BaggageAPIImpl() {
        this(BaggageContents.getNamespace(PT_ACTIVE_NAMESPACE), BaggageContents.getNamespace(PT_ARCHIVE_NAMESPACE));
    }

    @Override
    public Pack create(PackSpec spec) throws InvalidAdviceException {
        if (spec.hasFilterSpec()) {
            return new BagFiltered(spec.getBagId(), spec.getFilterSpec(), ACTIVE, ARCHIVE);
        } else if (spec.hasGroupBySpec()) {
            return new BagGrouped(spec.getBagId(), spec.getGroupBySpec(), ACTIVE, ARCHIVE);
        } else if (spec.hasTupleSpec()) {
            return new BagTuples(spec.getBagId(), spec.getTupleSpec(), ACTIVE, ARCHIVE);
        } else {
            throw new InvalidAdviceException(spec, "Bag type not specified");
        }
    }
    
    @Override
    public Unpack create(UnpackSpec spec) throws InvalidAdviceException {
        if (spec.hasFilterSpec()) {
            return new BagFiltered(spec.getBagId(), spec.getFilterSpec(), ACTIVE, ARCHIVE);
        } else if (spec.hasGroupBySpec()) {
            return new BagGrouped(spec.getBagId(), spec.getGroupBySpec(), ACTIVE, ARCHIVE);
        } else if (spec.hasTupleSpec()) {
            return new BagTuples(spec.getBagId(), spec.getTupleSpec(), ACTIVE, ARCHIVE);
        } else {
            throw new InvalidAdviceException(spec, "Bag type not specified");
        }     
    }

    
    /** Archives baggage when execution splits */
    static class PTBaggageHandler implements BaggageHandler {

        /** When we split, anything active is archived */
        public void preSplit(BaggageImpl current) {
            current.moveEntries(PT_ACTIVE_NAMESPACE, PT_ARCHIVE_NAMESPACE);
        }

        public void postSplit(BaggageImpl left, BaggageImpl right) {            
        }

        public void preJoin(BaggageImpl left, BaggageImpl right) {
        }

        /** If both sides had an active version of a bag, must merge its values */
        public void postJoin(BaggageImpl current) {
            for (ByteString bag : current.keys(PT_ACTIVE_NAMESPACE)) {
                Set<ByteString> entries = current.get(PT_ACTIVE_NAMESPACE, bag);
                if (entries.size() > 1) {
                    ByteString merged = merge(entries);
                    if (merged == null) {
                        current.remove(PT_ACTIVE_NAMESPACE, bag);
                    } else {
                        current.replace(PT_ACTIVE_NAMESPACE, bag, merged);
                    }
                }
            }
        }
        
        /** Blindly merges bags by merging the protobufs.  Ensures that bags don't have more than one type inside them */
        public static ByteString merge(Set<ByteString> serializedBags) {
            List<Bag> bags = Lists.newArrayList();
            for (ByteString serializedBag : serializedBags) {
                try {
                    bags.add(Bag.parseFrom(serializedBag));
                } catch (InvalidProtocolBufferException e) {
                }
            }
            if (bags.size() == 0) {
                return null;
            } else if (bags.size() == 1) {
                return bags.get(0).toByteString();
            } else {
                Bag.Builder builder = Bag.newBuilder();
                for (Bag bag : bags) {
                    builder.mergeFrom(bag);
                }
                if (builder.hasTupleBag()) {
                    builder.clearGroupBag();
                    builder.clearFilterBag();
                } else if (builder.hasGroupBag()) {
                    builder.clearTupleBag();
                    builder.clearFilterBag();
                } else if (builder.hasFilterBag()) {
                    builder.clearTupleBag();
                    builder.clearGroupBag();
                } else {
                    return null;
                }
                return builder.build().toByteString();
            }
        }

    }

}
