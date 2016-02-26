package edu.brown.cs.systems.pivottracing.agent.advice.baggage;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.protobuf.ByteString;

import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.Agg;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.GroupBySpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.PackSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.UnpackSpec;
import edu.brown.cs.systems.pivottracing.agent.advice.InvalidAdviceException;
import edu.brown.cs.systems.pivottracing.agent.advice.baggage.BagGrouped;
import edu.brown.cs.systems.pivottracing.agent.advice.baggage.BaggageAPI.Pack;
import edu.brown.cs.systems.pivottracing.agent.advice.baggage.BaggageAPI.Unpack;
import edu.brown.cs.systems.pivottracing.agent.advice.utils.BaggageAPIImplForTest;
import junit.framework.TestCase;

public class TestBagGrouped extends TestCase {
    
    public static GroupByBuilder groupby(String... groupby) {
        return new GroupByBuilder(groupby);
    }
    
    public static class GroupByBuilder {
        GroupBySpec.Builder b = GroupBySpec.newBuilder();
        public GroupByBuilder(String... groupby) {
            b.addAllGroupBy(Lists.newArrayList(groupby));
        }
        public GroupBySpec aggregate(Object... pairs) {
            for (int i = 0; i+1 < pairs.length; i+=2) {
                b.addAggregateBuilder().setName((String) pairs[i+1]).setHow((Agg) pairs[i]);
            }
            return b.build();
        }
    }
    
    public static BagBuilder bag(String bagId) {
        return new BagBuilder(bagId);
    }
    
    public static class BagBuilder {
        public final String bagId;
        public BagBuilder(String bagId) {
            this.bagId = bagId;
        }
        public PackSpec pack(GroupBySpec groupBySpec) {
            return PackSpec.newBuilder().setBagId(ByteString.copyFromUtf8(bagId)).setGroupBySpec(groupBySpec).build();
        }
        public UnpackSpec unpack(GroupBySpec groupBySpec) {
            return UnpackSpec.newBuilder().setBagId(ByteString.copyFromUtf8(bagId)).setGroupBySpec(groupBySpec).build();
        }
    }
    
    public static Set<List<Object>> toSet(Object[][] arrays) {
        return toSet(Lists.<Object[]>newArrayList(arrays));
    }
    
    public static Set<List<Object>> toSet(List<Object[]> arrays) {
        Set<List<Object>> set = Sets.newHashSet();
        for (Object[] array : arrays) {
            set.add(Lists.newArrayList(array));
        }
        return set;        
    }
    
    @Test
    public void testInterpretLong() {
        assertEquals(null, BagGrouped.interpretLong(null));
        assertEquals((Long) 1L, BagGrouped.interpretLong(1));
        assertEquals((Long) 5L, BagGrouped.interpretLong(5.7));
        assertEquals((Long) 1L, BagGrouped.interpretLong("1"));
        assertEquals(null, BagGrouped.interpretLong("5.7"));
        assertEquals(null, BagGrouped.interpretLong("hello"));
    }
    
    @Test
    public void testPackGroupedSimple() throws InvalidAdviceException {

        BaggageAPIImplForTest baggage = new BaggageAPIImplForTest();

        GroupBySpec groupSpec = groupby("g1", "g2").aggregate(
                Agg.SUM, "a1",
                Agg.COUNT, "a2",
                Agg.MIN, "a3",
                Agg.MAX, "a4"
        );
        PackSpec packSpec = bag("test1").pack(groupSpec);
        UnpackSpec unpackSpec = bag("test1").unpack(groupSpec);
        
        Pack pack = baggage.create(packSpec);
        Unpack unpack = baggage.create(unpackSpec);
        
        List<Object[]> tuples1 = Lists.<Object[]>newArrayList(
            new Object[] { "hello", "goodbye", 1L, 2L, 3L, 4L }
        );
        
        List<Object[]> expect = Lists.<Object[]>newArrayList(
            new Object[] { "hello", "goodbye", 1L, 1L, 3L, 4L }
        );
        
        pack.pack(tuples1);
        assertEquals(1, unpack.unpack().length);
        assertEquals(toSet(expect), toSet(unpack.unpack()));
    }
    
    @Test
    public void testPackGroupedAggregates() throws InvalidAdviceException {
        BaggageAPIImplForTest baggage = new BaggageAPIImplForTest();

        GroupBySpec groupSpec = groupby("g1", "g2").aggregate(
                Agg.SUM, "a1",
                Agg.COUNT, "a2",
                Agg.MIN, "a3",
                Agg.MAX, "a4"
        );
        PackSpec packSpec = bag("test1").pack(groupSpec);
        UnpackSpec unpackSpec = bag("test1").unpack(groupSpec);
        
        Pack pack = baggage.create(packSpec);
        Unpack unpack = baggage.create(unpackSpec);
        
        List<Object[]> tuples1 = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "goodbye", 1L, 2L, 1L, 1L },
                new Object[] { "hello", "friend", 8L, 2L, 5L, 2L },
                new Object[] { "hello", "goodbye", 1L, 100L, 3L, 4L },
                new Object[] { "hello", "goodbye", 7L, 2L, 3L, 4L },
                new Object[] { "hello", "goodbye", 1L, 2L, 3L, 1L },
                new Object[] { "friend", "goodbye", 8L, 2L, 5L, 2L },
                new Object[] { "friend", "goodbye", 8L, 2L, 5L, 2L },
                new Object[] { "hello", "goodbye", 1L, 2L, 5L, 4L }
        );
        
        List<Object[]> expect = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "goodbye", 11L, 5L, 1L, 4L },
                new Object[] { "hello", "friend", 8L, 1L, 5L, 2L },
                new Object[] { "friend", "goodbye", 16L, 2L, 5L, 2L }
        );
        
        pack.pack(tuples1);
        assertEquals(3, unpack.unpack().length);
        
        assertEquals(toSet(expect), toSet(unpack.unpack()));
        
    }
    
    @Test
    public void testMultiplePacks() throws InvalidAdviceException {
        BaggageAPIImplForTest baggage = new BaggageAPIImplForTest();

        GroupBySpec groupSpec = groupby("g1", "g2").aggregate(
                Agg.SUM, "a1",
                Agg.COUNT, "a2",
                Agg.MIN, "a3",
                Agg.MAX, "a4"
        );
        PackSpec packSpec = bag("test1").pack(groupSpec);
        UnpackSpec unpackSpec = bag("test1").unpack(groupSpec);
        
        Pack pack = baggage.create(packSpec);
        Unpack unpack = baggage.create(unpackSpec);
        
        List<Object[]> tuples1 = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "goodbye", 1L, 2L, 1L, 1L },
                new Object[] { "hello", "friend", 8L, 2L, 5L, 2L },
                new Object[] { "hello", "goodbye", 1L, 100L, 3L, 4L },
                new Object[] { "hello", "goodbye", 7L, 2L, 3L, 4L }
        );
        List<Object[]> tuples2 = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "goodbye", 1L, 2L, 3L, 1L },
                new Object[] { "friend", "goodbye", 8L, 2L, 5L, 2L },
                new Object[] { "friend", "goodbye", 8L, 2L, 5L, 2L },
                new Object[] { "hello", "goodbye", 1L, 2L, 5L, 4L }
        );
        
        List<Object[]> expect = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "goodbye", 11L, 5L, 1L, 4L },
                new Object[] { "hello", "friend", 8L, 1L, 5L, 2L },
                new Object[] { "friend", "goodbye", 16L, 2L, 5L, 2L }
        );

        pack.pack(tuples1);
        pack.pack(tuples2);
        assertEquals(3, unpack.unpack().length);
        
        assertEquals(toSet(expect), toSet(unpack.unpack()));
        
    }
    
    @Test
    public void testInvalid1() throws InvalidAdviceException {
        BaggageAPIImplForTest baggage = new BaggageAPIImplForTest();

        GroupBySpec groupSpec = groupby("g1", "g2").aggregate(
                Agg.SUM, "a1",
                Agg.COUNT, "a2",
                Agg.MIN, "a3",
                Agg.MAX, "a4"
        );
        PackSpec packSpec = bag("test1").pack(groupSpec);
        UnpackSpec unpackSpec = bag("test1").unpack(groupSpec);
        
        Pack pack = baggage.create(packSpec);
        Unpack unpack = baggage.create(unpackSpec);
        
        List<Object[]> tuples1 = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "goodbye", null, 2L, 1L, 1L }
        );
        
        List<Object[]> expect = Lists.<Object[]>newArrayList(
        );

        pack.pack(tuples1);
        assertEquals(0, unpack.unpack().length);
        
        assertEquals(toSet(expect), toSet(unpack.unpack()));
    }
    
    @Test
    public void testInvalid2() throws InvalidAdviceException {
        BaggageAPIImplForTest baggage = new BaggageAPIImplForTest();

        GroupBySpec groupSpec = groupby("g1", "g2").aggregate(
                Agg.SUM, "a1",
                Agg.COUNT, "a2",
                Agg.MIN, "a3",
                Agg.MAX, "a4"
        );
        PackSpec packSpec = bag("test1").pack(groupSpec);
        UnpackSpec unpackSpec = bag("test1").unpack(groupSpec);
        
        Pack pack = baggage.create(packSpec);
        Unpack unpack = baggage.create(unpackSpec);
        
        List<Object[]> tuples1 = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "goodbye", null, 2L, 1L }
        );
        
        List<Object[]> expect = Lists.<Object[]>newArrayList(
        );

        pack.pack(tuples1);
        assertEquals(0, unpack.unpack().length);
        
        assertEquals(toSet(expect), toSet(unpack.unpack()));
    }
    
    @Test
    public void testInvalid3() throws InvalidAdviceException {
        BaggageAPIImplForTest baggage = new BaggageAPIImplForTest();

        GroupBySpec groupSpec = groupby("g1", "g2").aggregate(
                Agg.SUM, "a1",
                Agg.COUNT, "a2",
                Agg.MIN, "a3",
                Agg.MAX, "a4"
        );
        PackSpec packSpec = bag("test1").pack(groupSpec);
        UnpackSpec unpackSpec = bag("test1").unpack(groupSpec);
        
        Pack pack = baggage.create(packSpec);
        Unpack unpack = baggage.create(unpackSpec);
        
        List<Object[]> tuples1 = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "goodbye", "hello", 1L, 2L, 1L }
        );
        
        List<Object[]> expect = Lists.<Object[]>newArrayList(
        );

        pack.pack(tuples1);
        assertEquals(0, unpack.unpack().length);
        
        assertEquals(toSet(expect), toSet(unpack.unpack()));
    }
    
    @Test
    public void testNoAggregates() throws InvalidAdviceException {
        BaggageAPIImplForTest baggage = new BaggageAPIImplForTest();

        GroupBySpec groupSpec = groupby("g1").aggregate();
        PackSpec packSpec = bag("test1").pack(groupSpec);
        UnpackSpec unpackSpec = bag("test1").unpack(groupSpec);
        
        Pack pack = baggage.create(packSpec);
        Unpack unpack = baggage.create(unpackSpec);
        
        List<Object[]> tuples1 = Lists.<Object[]>newArrayList(
                new Object[] { "hello"},
                new Object[] { "hello"},
                new Object[] { "hello"}
        );
        List<Object[]> tuples2 = Lists.<Object[]>newArrayList(
                new Object[] { "hello"},
                new Object[] { "hello"}
        );
        
        List<Object[]> expect = Lists.<Object[]>newArrayList(
                new Object[] { "hello"}
        );

        assertEquals(0, unpack.unpack().length);
        
        pack.pack(tuples1);
        
        assertEquals(1, unpack.unpack().length);
        assertEquals(toSet(expect), toSet(unpack.unpack()));
        
        pack.pack(tuples1);
        
        assertEquals(1, unpack.unpack().length);
        assertEquals(toSet(expect), toSet(unpack.unpack()));
        
        pack.pack(tuples1);
        
        assertEquals(1, unpack.unpack().length);
        assertEquals(toSet(expect), toSet(unpack.unpack()));
        
        pack.pack(tuples2);
        
        assertEquals(1, unpack.unpack().length);
        assertEquals(toSet(expect), toSet(unpack.unpack()));
    }
    
    public static void archiveAll(BaggageAPIImplForTest baggage) {
        baggage.ARCHIVE.map.putAll(baggage.ACTIVE.map);
        baggage.ACTIVE.map.clear();
    }
    
    @Test
    public void testArchive1() throws InvalidAdviceException {
        BaggageAPIImplForTest baggage = new BaggageAPIImplForTest();

        GroupBySpec groupSpec = groupby("g1").aggregate();
        PackSpec packSpec = bag("test1").pack(groupSpec);
        UnpackSpec unpackSpec = bag("test1").unpack(groupSpec);
        
        Pack pack = baggage.create(packSpec);
        Unpack unpack = baggage.create(unpackSpec);
        
        List<Object[]> tuples1 = Lists.<Object[]>newArrayList(
                new Object[] { "hello"},
                new Object[] { "hello"},
                new Object[] { "hello"}
        );
        List<Object[]> tuples2 = Lists.<Object[]>newArrayList(
                new Object[] { "hello"},
                new Object[] { "hello"}
        );
        
        List<Object[]> expect = Lists.<Object[]>newArrayList(
                new Object[] { "hello"}
        );

        assertEquals(0, unpack.unpack().length);
        
        pack.pack(tuples1);
        
        assertEquals(1, unpack.unpack().length);
        assertEquals(toSet(expect), toSet(unpack.unpack()));
        assertEquals(1, baggage.ACTIVE.map.size());
        assertEquals(0, baggage.ARCHIVE.map.size());
        
        archiveAll(baggage);

        assertEquals(0, baggage.ACTIVE.map.size());
        assertEquals(1, baggage.ARCHIVE.map.size());
        assertEquals(1, unpack.unpack().length);
        assertEquals(toSet(expect), toSet(unpack.unpack()));
        
        pack.pack(tuples1);
        
        assertEquals(1, unpack.unpack().length);
        assertEquals(toSet(expect), toSet(unpack.unpack()));
        assertEquals(1, baggage.ACTIVE.map.size());
        assertEquals(1, baggage.ARCHIVE.map.size());
    }
    
    @Test
    public void testArchive2() throws InvalidAdviceException {
        BaggageAPIImplForTest baggage = new BaggageAPIImplForTest();

        GroupBySpec groupSpec = groupby("g1", "g2").aggregate(
                Agg.SUM, "a1",
                Agg.COUNT, "a2",
                Agg.MIN, "a3",
                Agg.MAX, "a4"
        );
        PackSpec packSpec = bag("test1").pack(groupSpec);
        UnpackSpec unpackSpec = bag("test1").unpack(groupSpec);
        
        Pack pack = baggage.create(packSpec);
        Unpack unpack = baggage.create(unpackSpec);
        
        List<Object[]> tuples1 = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "goodbye", 1L, 2L, 1L, 1L },
                new Object[] { "hello", "friend", 8L, 2L, 5L, 2L },
                new Object[] { "hello", "goodbye", 1L, 100L, 3L, 4L },
                new Object[] { "hello", "goodbye", 1L, 2L, 3L, 1L },
                new Object[] { "hello", "goodbye", 7L, 2L, 3L, 4L },
                new Object[] { "hello", "goodbye", 1L, 2L, 5L, 4L }
        );
        List<Object[]> tuples2 = Lists.<Object[]>newArrayList(
                new Object[] { "friend", "goodbye", 8L, 2L, 5L, 2L },
                new Object[] { "friend", "goodbye", 8L, 2L, 5L, 2L }
        );
        
        List<Object[]> expect1 = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "goodbye", 11L, 5L, 1L, 4L },
                new Object[] { "hello", "friend", 8L, 1L, 5L, 2L }
        );
        
        List<Object[]> expect2 = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "goodbye", 11L, 5L, 1L, 4L },
                new Object[] { "hello", "friend", 8L, 1L, 5L, 2L },
                new Object[] { "friend", "goodbye", 16L, 2L, 5L, 2L }
        );

        assertEquals(0, unpack.unpack().length);
        
        pack.pack(tuples1);
        
        assertEquals(2, unpack.unpack().length);
        assertEquals(toSet(expect1), toSet(unpack.unpack()));
        assertEquals(1, baggage.ACTIVE.map.size());
        assertEquals(0, baggage.ARCHIVE.map.size());
        
        archiveAll(baggage);

        assertEquals(0, baggage.ACTIVE.map.size());
        assertEquals(1, baggage.ARCHIVE.map.size());
        assertEquals(2, unpack.unpack().length);
        assertEquals(toSet(expect1), toSet(unpack.unpack()));
        
        pack.pack(tuples2);
        
        assertEquals(3, unpack.unpack().length);
        assertEquals(toSet(expect2), toSet(unpack.unpack()));
        assertEquals(1, baggage.ACTIVE.map.size());
        assertEquals(1, baggage.ARCHIVE.map.size());
        
        archiveAll(baggage);
        
        assertEquals(0, baggage.ACTIVE.map.size());
        assertEquals(2, baggage.ARCHIVE.map.size());
        assertEquals(3, unpack.unpack().length);
        assertEquals(toSet(expect2), toSet(unpack.unpack()));
        assertEquals(0, baggage.ACTIVE.map.size());
        assertEquals(2, baggage.ARCHIVE.map.size());
    }

}
