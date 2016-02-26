package edu.brown.cs.systems.baggage;

import org.junit.Test;

import com.google.protobuf.ByteString;

import edu.brown.cs.systems.baggage.DetachedBaggage.StringEncoding;
import junit.framework.TestCase;

public class TestNulls extends TestCase {
    
    @Test
    public void testNulls() {
        assertEquals(DetachedBaggage.EMPTY, DetachedBaggage.deserialize((byte[]) null));
        assertEquals(DetachedBaggage.EMPTY, DetachedBaggage.deserialize((ByteString) null));
        assertEquals(DetachedBaggage.EMPTY, DetachedBaggage.decode(null, null));
        assertEquals(DetachedBaggage.EMPTY, DetachedBaggage.decode(null, StringEncoding.BASE64));
        assertEquals(DetachedBaggage.EMPTY, DetachedBaggage.wrap(null));

        assertNotNull(DetachedBaggage.EMPTY.toByteArray());
        assertNotNull(DetachedBaggage.EMPTY.toByteString());
        assertNotNull(DetachedBaggage.EMPTY.toStringBase64());
        assertNotNull(DetachedBaggage.EMPTY.toString(StringEncoding.BASE64));
        assertEquals(DetachedBaggage.EMPTY, DetachedBaggage.EMPTY.split());

        assertNotNull(Baggage.stop());
        assertNotNull(Baggage.stop().toByteArray());
        assertNotNull(Baggage.stop().toByteString());
        assertNotNull(Baggage.stop().toStringBase64());
        assertNotNull(Baggage.stop().toString(StringEncoding.BASE64));
        assertEquals(DetachedBaggage.EMPTY, Baggage.fork());

        Baggage.join((byte[]) null);
        assertEquals(DetachedBaggage.EMPTY, Baggage.stop());
        
        Baggage.join(new byte[0]);
        assertEquals(DetachedBaggage.EMPTY, Baggage.stop());
        
        Baggage.join((ByteString) null);
        assertEquals(DetachedBaggage.EMPTY, Baggage.stop());
        
        Baggage.join(ByteString.EMPTY);
        assertEquals(DetachedBaggage.EMPTY, Baggage.stop());
        
        Baggage.join((DetachedBaggage) null);
        assertEquals(DetachedBaggage.EMPTY, Baggage.stop());
        
        Baggage.join(DetachedBaggage.EMPTY);
        assertEquals(DetachedBaggage.EMPTY, Baggage.stop());
        
        Baggage.join((String) null);
        assertEquals(DetachedBaggage.EMPTY, Baggage.stop());
        
        Baggage.join("");
        assertEquals(DetachedBaggage.EMPTY, Baggage.stop());
        
    }

}
