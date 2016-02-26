package edu.brown.cs.systems.baggage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import com.google.protobuf.ByteString;

import edu.brown.cs.systems.baggage.Baggage;
import edu.brown.cs.systems.baggage.BaggageContents;

public class BaggageTest {
    static final ByteString myname = ByteString.copyFrom("namespace".getBytes());

    @Before
    public void setUp() {
        Baggage.current.remove();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        Baggage.current.remove();
    }

    @Test
    public void testHas() {
        // should not be active before trace starting
        assertFalse(BaggageContents.contains(myname, ByteString.copyFrom("not-exist".getBytes())));
        Baggage.discard();
        try {
            // should not allow any null key
            assertFalse(BaggageContents.contains(myname, null));
            // try a non-exist key and an exist key
            assertFalse(BaggageContents.contains(myname, ByteString.copyFrom("not-exist".getBytes())));
            BaggageContents.add(myname, ByteString.copyFrom("exist".getBytes()), ByteString.copyFrom("val".getBytes()));
            assertTrue(BaggageContents.contains(myname, ByteString.copyFrom("exist".getBytes())));
        } finally {
            Baggage.discard();
        }
        // should not be active after trace starting
        assertFalse(BaggageContents.contains(myname, ByteString.copyFrom("exist".getBytes())));
    }

    @Test
    public void testGetAndAdd() {
        assertTrue(BaggageContents.get(myname, ByteString.copyFrom("not-exist".getBytes())).isEmpty());
        BaggageContents.add(myname, ByteString.copyFrom("exist".getBytes()), ByteString.copyFrom("val2".getBytes()));
        Baggage.discard();
        try {
            assertTrue(BaggageContents.get(myname, ByteString.copyFrom("not-exist".getBytes())).isEmpty());
            BaggageContents.add(myname, ByteString.copyFrom("exist".getBytes()), ByteString.copyFrom("val".getBytes()));
            BaggageContents
                    .add(myname, ByteString.copyFrom("exist".getBytes()), ByteString.copyFrom("val2".getBytes()));
            assertTrue(BaggageContents.get(myname, ByteString.copyFrom("exist".getBytes())).contains(
                    ByteString.copyFrom("val".getBytes())));
            assertTrue(BaggageContents.get(myname, ByteString.copyFrom("exist".getBytes())).contains(
                    ByteString.copyFrom("val2".getBytes())));
            assertEquals(2, BaggageContents.get(myname, ByteString.copyFrom("exist".getBytes())).size());
        } finally {
            Baggage.discard();
        }
        assertTrue(BaggageContents.get(myname, ByteString.copyFrom("exist".getBytes())).isEmpty());
    }

    @Test
    public void testReplace() {
        assertTrue(BaggageContents.get(myname, ByteString.copyFrom("not-exist".getBytes())).isEmpty());
        BaggageContents
                .replace(myname, ByteString.copyFrom("exist".getBytes()), ByteString.copyFrom("val2".getBytes()));
        Baggage.discard();
        try {
            assertTrue(BaggageContents.get(myname, ByteString.copyFrom("exist".getBytes())).isEmpty());
            BaggageContents.add(myname, ByteString.copyFrom("exist".getBytes()), ByteString.copyFrom("val".getBytes()));
            BaggageContents.replace(myname, ByteString.copyFrom("exist".getBytes()),
                    ByteString.copyFrom("val2".getBytes()));
            Set<ByteString> l = new HashSet<ByteString>();
            l.add(ByteString.copyFrom("t1".getBytes()));
            l.add(ByteString.copyFrom("t2".getBytes()));
            BaggageContents.replace(myname, ByteString.copyFrom("exist".getBytes()), l);
            assertTrue(BaggageContents.get(myname, ByteString.copyFrom("exist".getBytes())).contains(
                    ByteString.copyFrom("t1".getBytes())));
            assertTrue(BaggageContents.get(myname, ByteString.copyFrom("exist".getBytes())).contains(
                    ByteString.copyFrom("t2".getBytes())));
            assertEquals(2, BaggageContents.get(myname, ByteString.copyFrom("exist".getBytes())).size());
        } finally {
            Baggage.discard();
        }
    }

    @Test
    public void testRemove() {
        // should not fail even the tracing has not started
        BaggageContents.remove(myname, ByteString.copyFrom("exist".getBytes()));
        Baggage.discard();
        try {
            assertTrue(BaggageContents.get(myname, ByteString.copyFrom("exist".getBytes())).isEmpty());
            BaggageContents.add(myname, ByteString.copyFrom("exist".getBytes()), ByteString.copyFrom("val".getBytes()));
            BaggageContents
                    .add(myname, ByteString.copyFrom("exist2".getBytes()), ByteString.copyFrom("val".getBytes()));
            assertFalse(BaggageContents.get(myname, ByteString.copyFrom("exist".getBytes())).isEmpty());
            BaggageContents.remove(myname, ByteString.copyFrom("exist".getBytes()));
            assertTrue(BaggageContents.get(myname, ByteString.copyFrom("exist".getBytes())).isEmpty());
            assertFalse(BaggageContents.get(myname, ByteString.copyFrom("exist2".getBytes())).isEmpty());
        } finally {
            Baggage.discard();
        }
    }

}
