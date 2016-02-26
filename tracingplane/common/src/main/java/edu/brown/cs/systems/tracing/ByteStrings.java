package edu.brown.cs.systems.tracing;

import com.google.common.primitives.Longs;
import com.google.common.primitives.Shorts;
import com.google.protobuf.ByteString;

public class ByteStrings {
    
    /** Create a bytestring containing the long l */
    public static ByteString copyFrom(long l) {
        return ByteString.copyFrom(Longs.toByteArray(l));
    }
    
    /** Create a bytestring containing the short s */
    public static ByteString copyFrom(short s) {
        return ByteString.copyFrom(Shorts.toByteArray(s));
    }
    
    /** Interpret the bytestring as a long */
    public static long toLong(ByteString bs) {
        return bs.asReadOnlyByteBuffer().getLong();
    }
    
    /** Interpret the bytestring as a short */
    public static short toShort(ByteString bs) {
        return bs.asReadOnlyByteBuffer().getShort();
    }

}
