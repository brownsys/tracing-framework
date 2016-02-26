package edu.brown.cs.systems.baggage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import com.google.protobuf.ByteString;

import edu.brown.cs.systems.baggage.Baggage;
import edu.brown.cs.systems.baggage.BaggageContents;

public class ByteStringBaggageNamespaceTest {

    @Before
    public void setUp() {
        Baggage.current.remove();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        Baggage.current.remove();
    }

    @Test
    public void testGet() {
        ByteString mykey = ByteString.copyFrom("k1".getBytes());
        BaggageContents.getNamespace("namespace").add(mykey, mykey);
        assertTrue(BaggageContents.getNamespace("namespace").get(mykey).contains(mykey));
        assertEquals(0, BaggageContents.getNamespace("not-a-namespace").get(mykey).size());
    }

    @Test
    public void testAdd() {
        ByteString mykey = ByteString.copyFrom("k1".getBytes());
        assertFalse(BaggageContents.getNamespace("namespace").has(mykey));
        BaggageContents.getNamespace("namespace").add(mykey, mykey);
        assertTrue(BaggageContents.getNamespace("namespace").get(mykey).contains(mykey));
    }

    @Test
    public void testReplaceByteStringByteString() {
        ByteString mykey = ByteString.copyFrom("k1".getBytes());
        ByteString mykey2 = ByteString.copyFrom("k2".getBytes());
        BaggageContents.getNamespace("namespace").add(mykey, mykey);
        assertTrue(BaggageContents.getNamespace("namespace").get(mykey).contains(mykey));
        BaggageContents.getNamespace("namespace").replace(mykey, mykey2);
        assertFalse(BaggageContents.getNamespace("namespace").get(mykey).contains(mykey));
        assertTrue(BaggageContents.getNamespace("namespace").get(mykey).contains(mykey2));
        assertEquals(1, BaggageContents.getNamespace("namespace").get(mykey).size());
        // should create a new namespace
        assertFalse(BaggageContents.getNamespace("namespace2").has(mykey));
        BaggageContents.getNamespace("namespace2").replace(mykey, mykey2);
        assertTrue(BaggageContents.getNamespace("namespace2").get(mykey).contains(mykey2));
        assertEquals(1, BaggageContents.getNamespace("namespace2").get(mykey).size());
    }

    @Test
    public void testReplaceByteStringIterableOfQextendsByteString() {
        ByteString mybytes = ByteString.copyFrom("k1".getBytes());
        ByteString mybytes2 = ByteString.copyFrom("k2".getBytes());
        ArrayList<ByteString> l = new ArrayList<ByteString>();
        l.add(mybytes);
        l.add(mybytes2);
        assertFalse(BaggageContents.getNamespace("namespace").has(mybytes));
        BaggageContents.getNamespace("namespace").replace(mybytes, l);
        assertTrue(BaggageContents.getNamespace("namespace").get(mybytes).contains(mybytes));
        assertTrue(BaggageContents.getNamespace("namespace").get(mybytes).contains(mybytes2));
        assertEquals(2, BaggageContents.getNamespace("namespace").get(mybytes).size());
    }

    @Test
    public void testRemove() {
        ByteString mykey = ByteString.copyFrom("k1".getBytes());
        BaggageContents.getNamespace("namespace").add(mykey, mykey);
        assertTrue(BaggageContents.getNamespace("namespace").has(mykey));
        BaggageContents.getNamespace("namespace").remove(mykey);
        assertFalse(BaggageContents.getNamespace("namespace").has(mykey));
        BaggageContents.getNamespace("not-a-namespace").remove(mykey);
    }

    @Test
    public void testHas() {
        ByteString mykey = ByteString.copyFrom("k1".getBytes());
        assertFalse(BaggageContents.getNamespace("namespace").has(mykey));
        BaggageContents.getNamespace("namespace").add(mykey, mykey);
        assertTrue(BaggageContents.getNamespace("namespace").has(mykey));
        assertFalse(BaggageContents.getNamespace("not-namespace").has(mykey));
    }

    @Test
    public void testKeysEntries() {
        // ByteString mykey = ByteString.copyFrom("k1".getBytes());
        // ByteString mykey2 = ByteString.copyFrom("k2".getBytes());
        // Namespace<ByteString, ByteString> bn = BaggageContents
        // .getNamespace("namespace");
        // bn.add(mykey, mykey);
        // bn.add(mykey2, mykey2);
        // Set<ByteString> keys = bn.keys();
        // assertEquals(2, keys.size());
        // assertTrue(keys.contains(mykey));
        // assertTrue(keys.contains(mykey2));
        // Collection<Entry<ByteString, ByteString>> entries = bn.entries();
        // assertEquals(2, entries.size());
        // Set<ByteString> checkedKeys = new HashSet<>();
        // for (Map.Entry<ByteString, ByteString> e : entries) {
        // assertTrue(keys.contains(e.getKey()));
        // assertFalse(checkedKeys.contains(e.getKey()));
        // assertEquals(e.getKey(), e.getValue());
        // checkedKeys.add(e.getKey());
        // }
    }

}
