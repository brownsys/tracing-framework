package edu.brown.cs.systems.pivottracing.agent.advice.baggage;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import edu.brown.cs.systems.baggage.Namespace;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.Agg;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.GroupBySpec;
import edu.brown.cs.systems.pivottracing.agent.advice.InvalidAdviceException;
import edu.brown.cs.systems.pivottracing.agent.advice.baggage.BaggageAPI.Pack;
import edu.brown.cs.systems.pivottracing.agent.advice.baggage.BaggageAPI.Unpack;
import edu.brown.cs.systems.pivottracing.baggage.BaggageProtos.Bag;
import edu.brown.cs.systems.pivottracing.baggage.BaggageProtos.Group;
import edu.brown.cs.systems.pivottracing.baggage.BaggageProtos.GroupBag;

public class BagGrouped implements Pack, Unpack {

    public final Namespace<ByteString, ByteString> ACTIVE, ARCHIVE;
    public final GroupBySpec spec;
    public final ByteString bagId;
    public final int tupleSize, groupKeyCount, aggregationCount;
    private final Agg[] aggs;

    BagGrouped(ByteString bagId, GroupBySpec spec, Namespace<ByteString, ByteString> active,
            Namespace<ByteString, ByteString> archive) throws InvalidAdviceException {
        this.bagId = bagId;
        this.spec = spec;
        this.groupKeyCount = spec.getGroupByCount();
        this.aggregationCount = spec.getAggregateCount();
        this.tupleSize = groupKeyCount + aggregationCount;
        this.ACTIVE = active;
        this.ARCHIVE = archive;
        this.aggs = new Agg[spec.getAggregateCount()];
        for (int i = 0; i < spec.getAggregateCount(); i++) {
            aggs[i] = spec.getAggregate(i).getHow();
        }
    }
    
    /** Interpret an Object as a Long, returning null if it cannot do it.  Checks if it's a Number or a String */
    public static Long interpretLong(Object v) {
        if (v == null) {
            return null;
        } else if (v instanceof Number) {
            return ((Number) v).longValue();
        } else if (v instanceof String) {
            return Longs.tryParse((String) v);
        } else {
            return null;
        }
    }
    
    class GroupedTuples {
        long versionId = BaggageAPIImpl.random(); // Generate random version ID, but this might be overwritten
        final Map<List<String>, List<Long>> groups = Maps.newHashMap();
        
        int size() {
            return groups.size();
        }
        
        void addAllTuples(List<Object[]> tuples) {
            for (Object[] tuple : tuples) {
                add(tuple);
            }
        }
        
        void add(Object[] tuple) {
            // Ditch wrong-length tuples
            if (tuple.length != tupleSize) {
                return;
            }
            
            // Pull out tuple keys
            List<String> keys = Lists.newArrayListWithExpectedSize(groupKeyCount);
            for (int i = 0; i < groupKeyCount; i++) {
                keys.add(String.valueOf(tuple[i]));
            }
            
            // Pull out tuple values
            List<Long> values = Lists.newArrayListWithExpectedSize(aggregationCount);
            for (int i = 0; i < aggregationCount; i++) {
                if (aggs[i] == Agg.COUNT) {
                    values.add(1L);
                } else {
                    Long agg = interpretLong(tuple[groupKeyCount+i]);
                    if (agg == null) {
                        return;
                    }
                    values.add(agg);
                }
            }
            
            // Add the tuple
            add(keys, values);
        }
        
        void addAllGroups(List<Group> groups) {
            for (Group group : groups) {
                add(group);
            }
        }
        
        void add(Group group) {
            add(group.getGroupByList(), Lists.newArrayList(group.getAggregationList()));
        }
        
        void add(List<String> groupBy, List<Long> aggregates) {
            // Ignore if wrong size
            if (groupBy.size() != groupKeyCount || aggregates.size() != aggregationCount) {
                return;
            }
            
            // Merge with existing
            List<Long> existing = groups.get(groupBy);
            if (existing != null) {
                for (int i = 0; i < aggs.length; i++) {
                    switch (aggs[i]) {
                    case COUNT:
                    case SUM: existing.set(i, existing.get(i) + aggregates.get(i)); break;
                    case MAX: existing.set(i, Math.max(existing.get(i), aggregates.get(i))); break;
                    case MIN: existing.set(i, Math.min(existing.get(i), aggregates.get(i))); break;
                    }
                }
                return;
            }
            
            // No existing group, add it
            groups.put(groupBy, aggregates);
        }
        
        Object[][] getTuples() {
            Object[][] tuples = new Object[groups.size()][];
            int i = 0;
            for (Entry<List<String>, List<Long>> entry : groups.entrySet()) {
                Object[] tuple = new Object[tupleSize];
                System.arraycopy(entry.getKey().toArray(), 0, tuple, 0, groupKeyCount);
                System.arraycopy(entry.getValue().toArray(), 0, tuple, groupKeyCount, aggregationCount);
                tuples[i++] = tuple;
            }
            return tuples;
        }
        
        GroupBag.Builder getGroupBag() {
            GroupBag.Builder b = GroupBag.newBuilder();
            for (Entry<List<String>, List<Long>> entry : groups.entrySet()) {
                b.addGroupBuilder().addAllGroupBy(entry.getKey()).addAllAggregation(entry.getValue());
            }
            return b;
        }
        
        Bag.Builder getBag() {
            return Bag.newBuilder().setVersionId(versionId).setGroupBag(getGroupBag());
        }
        
    }

    GroupedTuples deserialize(Iterable<ByteString> serializedBags) {
        GroupedTuples groups = new GroupedTuples();
        for (ByteString serialized : serializedBags) {
            try {
                Bag bag = Bag.parseFrom(serialized);
                if (bag.hasGroupBag()) {
                    groups.versionId = bag.getVersionId();
                    groups.addAllGroups(bag.getGroupBag().getGroupList());
                }
            } catch (InvalidProtocolBufferException e) {}
        }
        return groups;
    }

    @Override
    public void pack(List<Object[]> tuples) {
        // Pull out the current active bag(s)
        GroupedTuples groups = new GroupedTuples();
        for (ByteString serialized : ACTIVE.get(bagId)) {
            try {
                Bag bag = Bag.parseFrom(serialized);
                if (bag.hasGroupBag()) {
                    groups.versionId = bag.getVersionId();
                    groups.addAllGroups(bag.getGroupBag().getGroupList());
                }
            } catch (InvalidProtocolBufferException e) {}
        }
        groups.addAllTuples(tuples);

        // If it turns out there are no tuples in the bag, remove it from the baggage
        if (groups.size() == 0) {
            ACTIVE.remove(bagId);
            return;
        }
        
        ACTIVE.replace(bagId, groups.getBag().build().toByteString());
    }

    @Override
    public Object[][] unpack() {
        // Grab the active bags. If there are more than one active bag, merge and re-pack. Rarely should this happen
        Set<ByteString> activeBags = ACTIVE.get(bagId);
        if (activeBags.size() > 1) {
            pack(Lists.<Object[]> newArrayList());
            activeBags = ACTIVE.get(bagId);
        }

        // Grab the archive bags. Return immediately if no tuples.
        Set<ByteString> archiveBags = ARCHIVE.get(bagId);
        if (activeBags.size() == 0 && archiveBags.size() == 0) {
            return new Object[0][];
        }

        // Deserialize the tuples
        GroupedTuples groups = new GroupedTuples();
        for (ByteString serialized : Iterables.concat(activeBags, archiveBags)) {
            try {
                Bag bag = Bag.parseFrom(serialized);
                if (bag.hasGroupBag()) {
                    groups.addAllGroups(bag.getGroupBag().getGroupList());
                }
            } catch (InvalidProtocolBufferException e) {}
        }
        return groups.getTuples();
    }
}
