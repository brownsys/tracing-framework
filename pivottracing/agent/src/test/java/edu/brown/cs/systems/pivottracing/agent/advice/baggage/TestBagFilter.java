package edu.brown.cs.systems.pivottracing.agent.advice.baggage;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;

import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.Filter;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.PackSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.UnpackSpec;
import edu.brown.cs.systems.pivottracing.agent.advice.InvalidAdviceException;
import edu.brown.cs.systems.pivottracing.agent.advice.baggage.BaggageAPI.Pack;
import edu.brown.cs.systems.pivottracing.agent.advice.baggage.BaggageAPI.Unpack;
import edu.brown.cs.systems.pivottracing.agent.advice.utils.BaggageAPIImplForTest;
import junit.framework.TestCase;

public class TestBagFilter extends TestCase {
    
    public static PackSpec pack(String outputId, Filter filter, String... tuples) {
        PackSpec.Builder b = PackSpec.newBuilder();
        b.setBagId(ByteString.copyFromUtf8(outputId));
        b.getFilterSpecBuilder().setFilter(filter).addAllVar(Lists.newArrayList(tuples));
        return b.build();
    }
    
    public static UnpackSpec unpack(String outputId, Filter filter, String... tuples) {
        UnpackSpec.Builder b = UnpackSpec.newBuilder();
        b.setBagId(ByteString.copyFromUtf8(outputId));
        b.getFilterSpecBuilder().setFilter(filter).addAllVar(Lists.newArrayList(tuples));
        return b.build();
    }
    
    public static List<List<Object>> toList(Object[][] arrays) {
        return toList(Lists.<Object[]>newArrayList(arrays));
    }
    
    public static List<List<Object>> toList(List<Object[]> arrays) {
        List<List<Object>> list = Lists.newArrayList();
        for (Object[] array : arrays) {
            list.add(Lists.newArrayList(array));
        }
        return list;        
    }
    
    @Test
    public void testMostRecentSimple() throws InvalidAdviceException {
        BaggageAPIImplForTest baggage = new BaggageAPIImplForTest();

        PackSpec packSpec = pack("test", Filter.MOSTRECENT, "var1", "var2", "var3");
        Pack pack = baggage.create(packSpec);
        
        UnpackSpec unpackSpec = unpack("test", Filter.MOSTRECENT, "var1", "var2", "var3");
        Unpack unpack = baggage.create(unpackSpec);
        
        List<Object[]> tuples1 = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "dear", "reader" }
        );
        
        List<Object[]> tuples2 = Lists.<Object[]>newArrayList(
                new Object[] { "hello2", "dear2", "reader2" }
        );
        
        pack.pack(tuples1);
        assertEquals(1, unpack.unpack().length);
        assertEquals(toList(tuples1), toList(unpack.unpack()));
        assertEquals(1, baggage.ACTIVE.map.size());
        assertEquals(0, baggage.ARCHIVE.map.size());
        
        pack.pack(tuples2);
        assertEquals(1, unpack.unpack().length);
        assertEquals(toList(tuples2), toList(unpack.unpack()));
        assertEquals(1, baggage.ACTIVE.map.size());
        assertEquals(0, baggage.ARCHIVE.map.size());
        
        pack.pack(tuples1);
        assertEquals(1, unpack.unpack().length);
        assertEquals(toList(tuples1), toList(unpack.unpack()));
        assertEquals(1, baggage.ACTIVE.map.size());
        assertEquals(0, baggage.ARCHIVE.map.size());
        
    }
    
    @Test
    public void testFirstSimple() throws InvalidAdviceException {
        BaggageAPIImplForTest baggage = new BaggageAPIImplForTest();

        PackSpec packSpec = pack("test", Filter.FIRST, "var1", "var2", "var3");
        Pack pack = baggage.create(packSpec);
        
        UnpackSpec unpackSpec = unpack("test", Filter.FIRST, "var1", "var2", "var3");
        Unpack unpack = baggage.create(unpackSpec);
        
        List<Object[]> tuples1 = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "dear", "reader" }
        );
        
        List<Object[]> tuples2 = Lists.<Object[]>newArrayList(
                new Object[] { "hello2", "dear2", "reader2" }
        );
        
        pack.pack(tuples1);
        assertEquals(1, unpack.unpack().length);
        assertEquals(toList(tuples1), toList(unpack.unpack()));
        assertEquals(1, baggage.ACTIVE.map.size());
        assertEquals(0, baggage.ARCHIVE.map.size());
        
        pack.pack(tuples2);
        assertEquals(1, unpack.unpack().length);
        assertEquals(toList(tuples1), toList(unpack.unpack()));
        assertEquals(1, baggage.ACTIVE.map.size());
        assertEquals(0, baggage.ARCHIVE.map.size());
        
        pack.pack(tuples1);
        assertEquals(1, unpack.unpack().length);
        assertEquals(toList(tuples1), toList(unpack.unpack()));
        assertEquals(1, baggage.ACTIVE.map.size());
        assertEquals(0, baggage.ARCHIVE.map.size());
    }
    
    public static void archiveAll(BaggageAPIImplForTest baggage) {
        baggage.ARCHIVE.map.putAll(baggage.ACTIVE.map);
        baggage.ACTIVE.map.clear();
    }
    
    @Test
    public void testFirstWithArchive() throws InvalidAdviceException {
        BaggageAPIImplForTest baggage = new BaggageAPIImplForTest();

        PackSpec packSpec = pack("test", Filter.FIRST, "var1", "var2", "var3");
        Pack pack = baggage.create(packSpec);
        
        UnpackSpec unpackSpec = unpack("test", Filter.FIRST, "var1", "var2", "var3");
        Unpack unpack = baggage.create(unpackSpec);
        
        List<Object[]> tuples1 = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "dear", "reader" }
        );
        
        List<Object[]> tuples2 = Lists.<Object[]>newArrayList(
                new Object[] { "hello2", "dear2", "reader2" }
        );
        
        pack.pack(tuples1);
        assertEquals(1, unpack.unpack().length);
        assertEquals(toList(tuples1), toList(unpack.unpack()));
        assertEquals(1, baggage.ACTIVE.map.size());
        assertEquals(0, baggage.ARCHIVE.map.size());
        
        archiveAll(baggage);
        
        pack.pack(tuples2);
        assertEquals(1, unpack.unpack().length);
        assertEquals(toList(tuples1), toList(unpack.unpack()));
        assertEquals(0, baggage.ACTIVE.map.size());
        assertEquals(1, baggage.ARCHIVE.map.size());
        
        pack.pack(tuples1);
        assertEquals(1, unpack.unpack().length);
        assertEquals(toList(tuples1), toList(unpack.unpack()));
        assertEquals(0, baggage.ACTIVE.map.size());
        assertEquals(1, baggage.ARCHIVE.map.size());
    }
    
    @Test
    public void testMostRecentWithArchive() throws InvalidAdviceException {
        BaggageAPIImplForTest baggage = new BaggageAPIImplForTest();

        PackSpec packSpec = pack("test", Filter.MOSTRECENT, "var1", "var2", "var3");
        Pack pack = baggage.create(packSpec);
        
        UnpackSpec unpackSpec = unpack("test", Filter.MOSTRECENT, "var1", "var2", "var3");
        Unpack unpack = baggage.create(unpackSpec);
        
        List<Object[]> tuples1 = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "dear", "reader" }
        );
        
        List<Object[]> tuples2 = Lists.<Object[]>newArrayList(
                new Object[] { "hello2", "dear2", "reader2" }
        );
        
        pack.pack(tuples1);
        assertEquals(1, unpack.unpack().length);
        assertEquals(toList(tuples1), toList(unpack.unpack()));
        assertEquals(1, baggage.ACTIVE.map.size());
        assertEquals(0, baggage.ARCHIVE.map.size());
        
        archiveAll(baggage);
        assertEquals(0, baggage.ACTIVE.map.size());
        assertEquals(1, baggage.ARCHIVE.map.size());
        
        pack.pack(tuples2);
        assertEquals(1, unpack.unpack().length);
        assertEquals(toList(tuples2), toList(unpack.unpack()));
        assertEquals(1, baggage.ACTIVE.map.size());
        assertEquals(0, baggage.ARCHIVE.map.size());
        
        archiveAll(baggage);
        assertEquals(0, baggage.ACTIVE.map.size());
        assertEquals(1, baggage.ARCHIVE.map.size());
        
        pack.pack(tuples1);
        assertEquals(1, unpack.unpack().length);
        assertEquals(toList(tuples1), toList(unpack.unpack()));
        assertEquals(1, baggage.ACTIVE.map.size());
        assertEquals(0, baggage.ARCHIVE.map.size());
    }

}
