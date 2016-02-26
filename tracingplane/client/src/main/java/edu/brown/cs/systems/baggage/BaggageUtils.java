package edu.brown.cs.systems.baggage;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;

/** Provides some convenience functions */
public class BaggageUtils {
    
    public static final String BAGGAGE_ENVIRONMENT_VARIABLE = "edu_brown_cs_systems_baggage";
    
    /** Assuming the specified bag contains strings, join the strings */
    public static String bagToString(String name, String key, String joinWith) {
        return StringUtils.join(BaggageContents.getStrings(name, key), joinWith);
    }
    
    /** Create a bytestring containing the long l */
    public static ByteString copyFrom(long l) {
        return ByteString.copyFrom(Longs.toByteArray(l));
    }
    
    /** Interpret the bytestring as a long */
    public static long toLong(ByteString bs) {
        return bs.asReadOnlyByteBuffer().getLong();
    }
    
    /** Check provided variables for baggage and start tracing if found */
    public static void checkEnvironment(Map<String, String> environmentVariables) {
        Baggage.start(environmentVariables.get(BAGGAGE_ENVIRONMENT_VARIABLE));
    }

}
