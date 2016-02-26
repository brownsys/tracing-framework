package edu.brown.cs.systems.pivottracing.agent.advice.baggage;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.google.protobuf.ByteString;

import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.PackSpec;
import edu.brown.cs.systems.pivottracing.advice.AdviceProtos.UnpackSpec;
import edu.brown.cs.systems.pivottracing.agent.advice.baggage.BaggageAPI.Pack;
import edu.brown.cs.systems.pivottracing.agent.advice.baggage.BaggageAPI.Unpack;
import edu.brown.cs.systems.pivottracing.agent.advice.InvalidAdviceException;
import edu.brown.cs.systems.pivottracing.agent.advice.baggage.BaggageAPIImpl;
import edu.brown.cs.systems.pivottracing.agent.advice.utils.BaggageAPIImplForTest;
import junit.framework.TestCase;

public class TestBagTuples extends TestCase {
    
    public static PackSpec pack(String outputId, String... tuples) {
        PackSpec.Builder b = PackSpec.newBuilder();
        b.setBagId(ByteString.copyFromUtf8(outputId));
        b.getTupleSpecBuilder().addAllVar(Lists.newArrayList(tuples));
        return b.build();
    }
    
    public static UnpackSpec unpack(String outputId, String... tuples) {
        UnpackSpec.Builder b = UnpackSpec.newBuilder();
        b.setBagId(ByteString.copyFromUtf8(outputId));
        b.getTupleSpecBuilder().addAllVar(Lists.newArrayList(tuples));
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
    public void testOne() throws InvalidAdviceException {
        BaggageAPIImplForTest baggage = new BaggageAPIImplForTest();

        PackSpec packSpec = pack("test", "var1", "var2", "var3");
        Pack pack = baggage.create(packSpec);
        
        UnpackSpec unpackSpec = unpack("test", "var1", "var2", "var3");
        Unpack unpack = baggage.create(unpackSpec);
        
        List<Object[]> tuples = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "dear", "reader" }
        );
        
        pack.pack(tuples);
        assertEquals(1, unpack.unpack().length);
        assertEquals(toList(tuples), toList(unpack.unpack()));
    }
    
    @Test
    public void testMultiple() throws InvalidAdviceException {
        BaggageAPIImplForTest baggage = new BaggageAPIImplForTest();

        PackSpec packSpec = pack("test", "var1", "var2", "var3");
        Pack pack = baggage.create(packSpec);
        
        UnpackSpec unpackSpec = unpack("test", "var1", "var2", "var3");
        Unpack unpack = baggage.create(unpackSpec);
        
        List<Object[]> tuples = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "dear", "reader" },
                new Object[] { "hello", "dear", "reader2" },
                new Object[] { "hello", "dear", "reader3" }
        );
        
        pack.pack(tuples);
        assertEquals(3, unpack.unpack().length);
        assertEquals(toList(tuples), toList(unpack.unpack()));
    }
    
    @Test
    public void testDuplicates() throws InvalidAdviceException {
        BaggageAPIImplForTest baggage = new BaggageAPIImplForTest();

        PackSpec packSpec = pack("test", "var1", "var2", "var3");
        Pack pack = baggage.create(packSpec);
        
        UnpackSpec unpackSpec = unpack("test", "var1", "var2", "var3");
        Unpack unpack = baggage.create(unpackSpec);
        
        List<Object[]> tuples = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "dear", "reader" },
                new Object[] { "hello", "dear", "reader" },
                new Object[] { "hello", "dear", "reader" }
        );
        
        pack.pack(tuples);
        assertEquals(3, unpack.unpack().length);
        assertEquals(toList(tuples), toList(unpack.unpack()));
    }
    
    @Test
    public void testInvalidTuples() throws InvalidAdviceException {
        BaggageAPIImplForTest baggage = new BaggageAPIImplForTest();

        PackSpec packSpec = pack("test", "var1", "var2", "var3");
        Pack pack = baggage.create(packSpec);
        
        UnpackSpec unpackSpec = unpack("test", "var1", "var2", "var3");
        Unpack unpack = baggage.create(unpackSpec);
        
        List<Object[]> tuples = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "dear", },
                new Object[] { "hello", "dear", "reader", "reader" }
        );
        
        pack.pack(tuples);
        assertEquals(0, unpack.unpack().length);
        assertEquals(Lists.<Object[]>newArrayList(), toList(unpack.unpack()));
        assertEquals(0, baggage.ACTIVE.map.size());
    }
    
    @Test
    public void testMultiplePacks() throws InvalidAdviceException {
        BaggageAPIImplForTest baggage = new BaggageAPIImplForTest();

        PackSpec packSpec = pack("test", "var1", "var2", "var3");
        Pack pack = baggage.create(packSpec);
        
        UnpackSpec unpackSpec = unpack("test", "var1", "var2", "var3");
        Unpack unpack = baggage.create(unpackSpec);
        
        List<Object[]> tuples = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "dear", "reader" },
                new Object[] { "hello", "dear", "reader2" },
                new Object[] { "hello", "dear", "reader3" }
        );


        assertEquals(0, baggage.ACTIVE.map.size());
        assertEquals(0, baggage.ARCHIVE.map.size());

        pack.pack(tuples);

        assertEquals(1, baggage.ACTIVE.map.size());
        assertEquals(0, baggage.ARCHIVE.map.size());
        
        pack.pack(tuples);

        assertEquals(1, baggage.ACTIVE.map.size());
        assertEquals(0, baggage.ARCHIVE.map.size());
        
        pack.pack(tuples);

        assertEquals(1, baggage.ACTIVE.map.size());
        assertEquals(0, baggage.ARCHIVE.map.size());
        
        assertEquals(9, unpack.unpack().length);
        
        List<Object[]> expect = Lists.newArrayList(
                Iterables.concat(tuples, tuples, tuples)
        );
        assertEquals(toList(expect), toList(unpack.unpack()));        
    }
    
    @Test
    public void testArchive() throws InvalidAdviceException {
        BaggageAPIImplForTest baggage = new BaggageAPIImplForTest();

        PackSpec packSpec = pack("test", "var1", "var2", "var3");
        Pack pack = baggage.create(packSpec);
        
        UnpackSpec unpackSpec = unpack("test", "var1", "var2", "var3");
        Unpack unpack = baggage.create(unpackSpec);
        
        List<Object[]> tuples = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "dear", "reader" },
                new Object[] { "hello", "dear", "reader2" },
                new Object[] { "hello", "dear", "reader3" }
        );

        assertEquals(0, baggage.ACTIVE.map.size());
        assertEquals(0, baggage.ARCHIVE.map.size());

        pack.pack(tuples);        

        assertEquals(1, baggage.ACTIVE.map.size());
        assertEquals(0, baggage.ARCHIVE.map.size());
        
        baggage.ARCHIVE.map.putAll(baggage.ACTIVE.map);
        baggage.ACTIVE.map.clear();
        
        pack.pack(tuples);        
        
        assertEquals(1, baggage.ACTIVE.map.size());
        assertEquals(1, baggage.ARCHIVE.map.size());

        baggage.ARCHIVE.map.putAll(baggage.ACTIVE.map);
        baggage.ACTIVE.map.clear();
        
        pack.pack(tuples);        
        
        assertEquals(1, baggage.ACTIVE.map.size());
        assertEquals(2, baggage.ARCHIVE.map.size());
        
        assertEquals(9, unpack.unpack().length);
        
        List<Object[]> expect = Lists.newArrayList(
                Iterables.concat(tuples, tuples, tuples)
        );
        assertEquals(toList(expect), toList(unpack.unpack()));        
    }
    
    @Test
    public void testActiveBaggageMergeOnUnpack() throws InvalidAdviceException {
        BaggageAPIImplForTest baggage = new BaggageAPIImplForTest();

        PackSpec packSpec = pack("test", "var1", "var2", "var3");
        Pack pack = baggage.create(packSpec);
        
        UnpackSpec unpackSpec = unpack("test", "var1", "var2", "var3");
        Unpack unpack = baggage.create(unpackSpec);
        
        List<Object[]> tuples = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "dear", "reader" },
                new Object[] { "hello", "dear", "reader2" },
                new Object[] { "hello", "dear", "reader3" }
        );

        assertEquals(0, baggage.ACTIVE.map.size());
        assertEquals(0, baggage.ARCHIVE.map.size());

        for (int i = 0; i < 5; i++) {
            assertEquals(0, baggage.ACTIVE.map.size());
            assertEquals(i, baggage.ARCHIVE.map.size());
            
            pack.pack(tuples);
            
            assertEquals(1, baggage.ACTIVE.map.size());
            assertEquals(i, baggage.ARCHIVE.map.size());
            
            baggage.ARCHIVE.map.putAll(baggage.ACTIVE.map);
            baggage.ACTIVE.map.clear();
            
            assertEquals(0, baggage.ACTIVE.map.size());
            assertEquals(i+1, baggage.ARCHIVE.map.size());
        }
        
        baggage.ACTIVE.map.putAll(baggage.ARCHIVE.map);
        baggage.ARCHIVE.map.clear();
        
        assertEquals(5, baggage.ACTIVE.map.size());
        assertEquals(0, baggage.ARCHIVE.map.size());
        
        assertEquals(15, unpack.unpack().length);
        
        assertEquals(1, baggage.ACTIVE.map.size());
        assertEquals(0, baggage.ARCHIVE.map.size());
        
        List<Object[]> expect = Lists.newArrayList(
                Iterables.concat(tuples, tuples, tuples, tuples, tuples)
        );
        assertEquals(toList(expect), toList(unpack.unpack()));  
        
    }
    
    @Test
    public void testActiveBaggageMergeOnPack() throws InvalidAdviceException {
        BaggageAPIImplForTest baggage = new BaggageAPIImplForTest();

        PackSpec packSpec = pack("test", "var1", "var2", "var3");
        Pack pack = baggage.create(packSpec);
        
        UnpackSpec unpackSpec = unpack("test", "var1", "var2", "var3");
        Unpack unpack = baggage.create(unpackSpec);
        
        List<Object[]> tuples = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "dear", "reader" },
                new Object[] { "hello", "dear", "reader2" },
                new Object[] { "hello", "dear", "reader3" }
        );

        assertEquals(0, baggage.ACTIVE.map.size());
        assertEquals(0, baggage.ARCHIVE.map.size());

        for (int i = 0; i < 4; i++) {
            assertEquals(0, baggage.ACTIVE.map.size());
            assertEquals(i, baggage.ARCHIVE.map.size());
            
            pack.pack(tuples);
            
            assertEquals(1, baggage.ACTIVE.map.size());
            assertEquals(i, baggage.ARCHIVE.map.size());
            
            baggage.ARCHIVE.map.putAll(baggage.ACTIVE.map);
            baggage.ACTIVE.map.clear();
            
            assertEquals(0, baggage.ACTIVE.map.size());
            assertEquals(i+1, baggage.ARCHIVE.map.size());
        }
        
        baggage.ACTIVE.map.putAll(baggage.ARCHIVE.map);
        baggage.ARCHIVE.map.clear();
        
        assertEquals(4, baggage.ACTIVE.map.size());
        assertEquals(0, baggage.ARCHIVE.map.size());
        
        pack.pack(tuples);
        
        assertEquals(1, baggage.ACTIVE.map.size());
        assertEquals(0, baggage.ARCHIVE.map.size());
        
        assertEquals(15, unpack.unpack().length);
        
        List<Object[]> expect = Lists.newArrayList(
                Iterables.concat(tuples, tuples, tuples, tuples, tuples)
        );
        assertEquals(toList(expect), toList(unpack.unpack()));  
    }
    
    @Test
    public void testBaggageHandlerMerge() throws InvalidAdviceException {
        BaggageAPIImplForTest baggage = new BaggageAPIImplForTest();

        PackSpec packSpec = pack("test", "var1", "var2", "var3");
        Pack pack = baggage.create(packSpec);
        
        UnpackSpec unpackSpec = unpack("test", "var1", "var2", "var3");
        Unpack unpack = baggage.create(unpackSpec);
        
        List<Object[]> tuples = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "dear", "reader" },
                new Object[] { "hello", "dear", "reader2" },
                new Object[] { "hello", "dear", "reader3" }
        );

        assertEquals(0, baggage.ACTIVE.map.size());
        assertEquals(0, baggage.ARCHIVE.map.size());

        for (int i = 0; i < 5; i++) {
            assertEquals(0, baggage.ACTIVE.map.size());
            assertEquals(i, baggage.ARCHIVE.map.size());
            
            pack.pack(tuples);
            
            assertEquals(1, baggage.ACTIVE.map.size());
            assertEquals(i, baggage.ARCHIVE.map.size());
            
            baggage.ARCHIVE.map.putAll(baggage.ACTIVE.map);
            baggage.ACTIVE.map.clear();
            
            assertEquals(0, baggage.ACTIVE.map.size());
            assertEquals(i+1, baggage.ARCHIVE.map.size());
        }
        
        baggage.ACTIVE.map.putAll(baggage.ARCHIVE.map);
        baggage.ARCHIVE.map.clear();
        
        assertEquals(5, baggage.ACTIVE.map.size());
        assertEquals(0, baggage.ARCHIVE.map.size());
        
        SetMultimap<ByteString, ByteString> newactive = HashMultimap.create();
        for (ByteString key : baggage.ACTIVE.map.keySet()) {
            Set<ByteString> contents = baggage.ACTIVE.map.get(key);
            ByteString merged = BaggageAPIImpl.PTBaggageHandler.merge(contents);
            newactive.put(key, merged);
        }
        baggage.ACTIVE.map.clear();
        baggage.ACTIVE.map.putAll(newactive);
        
        assertEquals(1, baggage.ACTIVE.map.size());
        assertEquals(0, baggage.ARCHIVE.map.size());
        
        assertEquals(15, unpack.unpack().length);
        
        List<Object[]> expect = Lists.newArrayList(
                Iterables.concat(tuples, tuples, tuples, tuples, tuples)
        );
        assertEquals(toList(expect), toList(unpack.unpack()));  
    }
    
    @Test
    public void testPackUnpackMismatch() throws InvalidAdviceException {
        BaggageAPIImplForTest baggage = new BaggageAPIImplForTest();

        PackSpec packSpec = pack("test", "var1", "var2", "var3");
        Pack pack = baggage.create(packSpec);
        
        UnpackSpec unpackSpec1 = unpack("test", "var1", "var2", "var3");
        Unpack unpack1 = baggage.create(unpackSpec1);
        
        UnpackSpec unpackSpec2 = unpack("test", "var1", "var2");
        Unpack unpack2 = baggage.create(unpackSpec2);
        
        List<Object[]> tuples = Lists.<Object[]>newArrayList(
                new Object[] { "hello", "dear", "reader" }
        );
        
        pack.pack(tuples);
        assertEquals(1, unpack1.unpack().length);
        assertEquals(toList(tuples), toList(unpack1.unpack()));
        assertEquals(0, unpack2.unpack().length);
        assertEquals(Lists.<Object[]>newArrayList(), toList(unpack2.unpack()));
    }

}
