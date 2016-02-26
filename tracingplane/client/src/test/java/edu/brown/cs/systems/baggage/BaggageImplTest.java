package edu.brown.cs.systems.baggage;

import java.util.ArrayList;
import java.util.Set;

import junit.framework.TestCase;

import org.junit.Test;

import com.google.protobuf.ByteString;

import edu.brown.cs.systems.baggage.BaggageMessages.BaggageMessage;

public class BaggageImplTest extends TestCase {

    @Test
    public void testByteStringEquals() {
        ByteString myval = ByteString.copyFrom("v1".getBytes());
        ByteString myval2 = ByteString.copyFrom("v1".getBytes());
        // ByteString should check the values rather than references for equals
        assertNotSame(myval, myval2);
        assertEquals(myval, ByteString.copyFrom("v1".getBytes()));
        assertEquals(myval, myval2);
        assertTrue(myval.equals(myval2));
        assertEquals(myval.hashCode(), myval2.hashCode());
    }

    @Test
    public void testNamespaceAutoRemoval() {
        BaggageImpl b = new BaggageImpl();
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        ByteString mykey = ByteString.copyFrom("k1".getBytes());
        ByteString mykey2 = ByteString.copyFrom("k2".getBytes());
        ByteString myval = ByteString.copyFrom("v1".getBytes());
        b.add(myname, mykey, myval);
        b.add(myname, mykey2, myval);
        assertTrue(b.hasNamespace(myname));
        b.remove(myname, mykey);
        assertTrue(b.hasNamespace(myname));
        b.remove(myname, mykey2);
        assertFalse(b.hasNamespace(myname));
    }

    @Test
    public void testAddGetRemove() {
        BaggageImpl b = new BaggageImpl();
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        ByteString mykey = ByteString.copyFrom("k1".getBytes());
        ByteString mykey2 = ByteString.copyFrom("k2".getBytes());
        ByteString myval = ByteString.copyFrom("v1".getBytes());
        ByteString myval2 = ByteString.copyFrom("v2".getBytes());
        // should return an empty set for non-exist keys
        assertTrue(b.get(myname, mykey).isEmpty());
        // check adding the one value multiple times
        b.add(myname, mykey, myval);
        assertTrue(b.get(myname, mykey).contains(myval));
        b.add(myname, mykey, myval);
        assertTrue(b.get(myname, mykey).contains(myval));
        // check different values for the same key
        b.add(myname, mykey, myval2);
        assertTrue(b.get(myname, mykey).contains(myval));
        assertTrue(b.get(myname, mykey).contains(myval2));
        // check another key
        assertFalse(b.get(myname, mykey2).contains(myval2));
        b.add(myname, mykey2, myval2);
        assertTrue(b.get(myname, mykey).contains(myval));
        assertFalse(b.get(myname, mykey2).contains(myval));
        assertTrue(b.get(myname, mykey2).contains(myval2));
        // remove one key
        b.remove(myname, mykey);
        assertTrue(b.get(myname, mykey).isEmpty());
        assertTrue(b.get(myname, mykey2).contains(myval2));
        // remove all keys
        b.remove(myname, mykey2);
        assertTrue(b.get(myname, mykey).isEmpty());
        assertTrue(b.get(myname, mykey2).isEmpty());
        // re-add
        b.add(myname, mykey, myval);
        assertTrue(b.get(myname, mykey).contains(myval));
    }

    @Test
    public void testAddNull() {
        BaggageImpl b = new BaggageImpl();
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        ByteString mybytes = ByteString.copyFrom("k1".getBytes());
        b.add(myname, mybytes, null);
        b.add(myname, null, mybytes);
        b.add(myname, null, null);
        assertFalse(b.get(myname, mybytes).contains(null));
        assertFalse(b.get(myname, null).contains(mybytes));
        assertFalse(b.get(myname, null).contains(null));
    }

    @Test
    public void testGetNonexistAndNull() {
        BaggageImpl b = new BaggageImpl();
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        assertTrue(b.get(myname, ByteString.copyFrom("notexist".getBytes())).isEmpty());
        assertTrue(b.get(myname, null).isEmpty());
    }

    @Test
    public void testRemoveNonexistAndNull() {
        BaggageImpl b = new BaggageImpl();
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        ByteString myname2 = ByteString.copyFrom("namespace2".getBytes());
        b.remove(myname, ByteString.copyFrom("notexist".getBytes()));
        b.remove(myname, null);
        b.remove(myname2, ByteString.copyFrom("notexist".getBytes()));
    }

    @Test
    public void testReplaceNull() {
        BaggageImpl b = new BaggageImpl();
        ByteString mybytes = ByteString.copyFrom("k1".getBytes());
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        b.add(myname, mybytes, mybytes);
        assertTrue(b.contains(myname, mybytes));
        b.replace(myname, mybytes, (ByteString) null);
        assertFalse(b.contains(myname, mybytes));
    }

    @Test
    public void testReplaceNullIterable() {
        BaggageImpl b = new BaggageImpl();
        ByteString mybytes = ByteString.copyFrom("k1".getBytes());
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        b.add(myname, mybytes, mybytes);
        assertTrue(b.contains(myname, mybytes));
        b.replace(myname, mybytes, (ArrayList<ByteString>) null);
        assertFalse(b.contains(myname, mybytes));
    }

    @Test
    public void testReplaceIterableWithNullValue() {
        BaggageImpl b = new BaggageImpl();
        ByteString mybytes = ByteString.copyFrom("k1".getBytes());
        ByteString mybytes2 = ByteString.copyFrom("k2".getBytes());
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        b.add(myname, mybytes, mybytes);
        assertTrue(b.contains(myname, mybytes));
        // should ignore the null values
        ArrayList<ByteString> l = new ArrayList<ByteString>();
        l.add(mybytes2);
        l.add(null);
        l.add(mybytes2);
        l.add(null);
        b.replace(myname, mybytes, l);
        assertFalse(b.get(myname, mybytes).contains(null));
        assertFalse(b.get(myname, mybytes).contains(mybytes));
        assertTrue(b.get(myname, mybytes).contains(mybytes2));
        // should be equivalent to remove the key if only null in the iterable
        ArrayList<ByteString> l2 = new ArrayList<ByteString>();
        l2.add(null);
        b.replace(myname, mybytes, l2);
        assertFalse(b.contains(myname, mybytes));
    }

    @Test
    public void testReplace() {
        BaggageImpl b = new BaggageImpl();
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        ByteString myname2 = ByteString.copyFrom("namespace2".getBytes());
        ByteString mybytes = ByteString.copyFrom("k1".getBytes());
        ByteString mybytes2 = ByteString.copyFrom("k2".getBytes());
        // edge cases
        b.replace(myname, mybytes, mybytes);
        assertTrue(b.get(myname, mybytes).contains(mybytes));
        // single replacement
        b.replace(myname, mybytes, mybytes2);
        assertFalse(b.get(myname, mybytes).contains(mybytes));
        assertTrue(b.get(myname, mybytes).contains(mybytes2));
        // replace with an empty list
        b.replace(myname, mybytes, new ArrayList<ByteString>());
        assertTrue(b.get(myname, mybytes).isEmpty());
        // replace with a list with duplicated items
        ArrayList<ByteString> l = new ArrayList<ByteString>();
        l.add(mybytes);
        l.add(mybytes2);
        l.add(mybytes);
        b.replace(myname, mybytes, l);
        assertTrue(b.get(myname, mybytes).contains(mybytes));
        assertTrue(b.get(myname, mybytes).contains(mybytes2));
        assertEquals(2, b.get(myname, mybytes).size());
        // replace with null key
        assertTrue(b.hasNamespace(myname));
        b.replace(myname, null, l);
        assertFalse(b.contains(myname, null));
        b.replace(myname, null, mybytes);
        assertFalse(b.contains(myname, null));
        // replace a non-exist namespace
        b.replace(myname2, mybytes, (ByteString) null);
        assertFalse(b.contains(myname2, mybytes));
        assertFalse(b.hasNamespace(myname2));
    }

    @Test
    public void testSplit() {
        BaggageImpl b = new BaggageImpl();
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        ByteString myname2 = ByteString.copyFrom("namespace2".getBytes());
        ByteString mybytes = ByteString.copyFrom("k1".getBytes());
        ByteString mybytes2 = ByteString.copyFrom("k2".getBytes());
        b.add(myname, mybytes, mybytes);
        b.add(myname, mybytes, mybytes2);
        b.add(myname2, mybytes2, mybytes);
        BaggageImpl b2 = b.split();
        assertTrue(b.get(myname, mybytes).contains(mybytes));
        assertTrue(b.get(myname, mybytes).contains(mybytes2));
        assertTrue(b.get(myname2, mybytes2).contains(mybytes));
        assertTrue(b2.get(myname, mybytes).contains(mybytes));
        assertTrue(b2.get(myname, mybytes).contains(mybytes2));
        assertTrue(b2.get(myname2, mybytes2).contains(mybytes));
        assertEquals(b.get(myname, mybytes).size(), b2.get(myname, mybytes).size());
        assertEquals(b.get(myname2, mybytes2).size(), b2.get(myname2, mybytes2).size());
    }

    @Test
    public void testJoin() {
        BaggageImpl b = new BaggageImpl();
        BaggageImpl b2 = new BaggageImpl();
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        ByteString myname2 = ByteString.copyFrom("namespace2".getBytes());
        ByteString v1 = ByteString.copyFrom("k1".getBytes());
        ByteString v2 = ByteString.copyFrom("k2".getBytes());
        ByteString v3 = ByteString.copyFrom("k3".getBytes());
        b.add(myname, v1, v2);
        b.add(myname, v2, v3);
        b2.add(myname, v3, v1);
        b2.add(myname, v2, v1);
        b2.add(myname2, v1, v1);
        b.merge(b2);
        b.merge(null);
        // the original baggage is not affected
        assertTrue(b2.get(myname, v1).isEmpty());
        // new key created
        assertTrue(b.get(myname, v3).contains(v1));
        assertEquals(1, b.get(myname, v3).size());
        // duplicated key merged
        assertTrue(b.get(myname, v2).contains(v1));
        assertTrue(b.get(myname, v2).contains(v3));
        assertEquals(2, b.get(myname, v2).size());
        // new namespace created
        assertTrue(b.get(myname2, v1).contains(v1));
        assertEquals(1, b.get(myname2, v1).size());
    }

    @Test
    public void testCopy() {
        BaggageImpl b = new BaggageImpl();
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        ByteString v1 = ByteString.copyFrom("k1".getBytes());
        ByteString v2 = ByteString.copyFrom("k2".getBytes());
        ByteString v3 = ByteString.copyFrom("k3".getBytes());
        b.add(myname, v1, v2);
        b.add(myname, v2, v3);
        BaggageImpl b2 = b.split();
        b.add(myname, v3, v1);
        b2.add(myname, v1, v1);
        // the original baggage is not affected
        assertTrue(b2.get(myname, v3).isEmpty());
        assertFalse(b.get(myname, v1).contains(v1));
        // new key created
        assertFalse(b.get(myname, v3).isEmpty());
        assertTrue(b2.get(myname, v1).contains(v1));
        // old values copied
        assertTrue(b.get(myname, v1).contains(v2));
        assertTrue(b.get(myname, v2).contains(v3));
        assertTrue(b2.get(myname, v1).contains(v2));
        assertTrue(b2.get(myname, v2).contains(v3));
    }

    @Test
    public void testSerialization() {
        BaggageImpl b = new BaggageImpl();
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        ByteString myname2 = ByteString.copyFrom("namespace2".getBytes());
        ByteString v1 = ByteString.copyFrom("k1".getBytes());
        ByteString v2 = ByteString.copyFrom("k2".getBytes());
        ByteString v3 = ByteString.copyFrom("k3".getBytes());
        ByteString v4 = ByteString.copyFrom("k4".getBytes());
        b.add(myname, v1, v2);
        b.add(myname, v2, null);
        b.add(myname, v3, v1);
        b.add(myname, v2, v1);
        b.add(myname2, v1, v4);
        byte[] s = b.toByteArray();
        b.add(myname, v4, v4);
        BaggageImpl b2 = BaggageImpl.deserialize(s);
        // recovery of serialization
        assertTrue(b2.get(myname, v2).contains(v1));
        assertFalse(b2.get(myname, v2).contains(null));
        assertTrue(b2.get(myname, v3).contains(v1));
        assertTrue(b2.get(myname, v1).contains(v2));
        assertTrue(b2.get(myname2, v1).contains(v4));
        // deserialize creates a new copy
        assertTrue(b2.get(myname, v4).isEmpty());
        // edge cases
        assertNull(BaggageImpl.deserialize((byte[]) null));
        assertNull(BaggageImpl.deserialize(new byte[0]));
        assertNull(BaggageImpl.deserialize(new byte[1]));
        assertNull(BaggageImpl.deserialize("not valid".getBytes()));
        assertNull(BaggageImpl.deserialize(BaggageMessage.newBuilder().build().toByteArray()));
        // null for empty baggage
        assertEquals(0, new BaggageImpl().toByteArray().length);
    }

    @Test
    public void testNamespace() {
        BaggageImpl b = new BaggageImpl();
        ByteString myoldname = ByteString.copyFrom("not-a-namespace".getBytes());
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        ByteString myname2 = ByteString.copyFrom("namespace2".getBytes());
        // should be able to run correctly even the namespace does not exist
        assertFalse(b.contains(myoldname, myname));
        assertEquals(0, b.get(myoldname, myname).size());
        b.remove(myoldname, myname);
        // namespaces should be isolated
        b.add(myname, myname, myname);
        assertFalse(b.contains(myname2, myname));
        assertTrue(b.get(myname, myname).contains(myname));
        b.add(myname2, myname, myname2);
        assertTrue(b.contains(myname2, myname));
        assertEquals(1, b.get(myname2, myname).size());
        assertTrue(b.get(myname2, myname).contains(myname2));
        assertEquals(1, b.get(myname, myname).size());
        // should be able to return all namespaces
        Set<ByteString> names = b.namespaces();
        assertTrue(names.contains(myname));
        assertTrue(names.contains(myname2));
        assertEquals(2, names.size());
        // should be able to remove a namespace
        b.removeAll(myname);
        assertFalse(b.hasNamespace(myname));
        assertTrue(b.hasNamespace(myname2));
    }
}
