package edu.brown.cs.systems.baggage;

import org.apache.commons.lang3.ArrayUtils;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import edu.brown.cs.systems.baggage.BaggageMessages.BaggageMessage;

public class ProtobufUtils {

    /** Parse bytes to a BaggageMessage, returning null if the bytes are invalid */
    public static BaggageMessage parse(byte[] bytes) {
        if (bytes != null) {
            try {
                return BaggageMessage.parseFrom(bytes);
            } catch (InvalidProtocolBufferException e) {}
        }
        return null;
    }

    /** Parse a ByteString to a BaggageMessage, returning null if the bytes are invalid */
    public static BaggageMessage parse(ByteString bytes) {
        if (bytes != null) {
            try {
                return BaggageMessage.parseFrom(bytes);
            } catch (InvalidProtocolBufferException e) {}
        }
        return null;
    }

    /** Serialize a BaggageMessage to a byte string, returning an empty bytestring if the provided message is null or
     * invalid */
    public static ByteString toByteString(BaggageMessage message) {
        if (message != null) {
            try {
                return message.toByteString();
            } catch (Throwable t) {}
        }
        return ByteString.EMPTY;
    }

    /** Serialize a BaggageMessage to a byte string, returning an empty bytestring if the provided message is null or
     * invalid */
    public static byte[] toByteArray(BaggageMessage message) {
        if (message != null) {
            try {
                return message.toByteArray();
            } catch (Throwable t) {}
        }
        return ArrayUtils.EMPTY_BYTE_ARRAY;
    }

}
