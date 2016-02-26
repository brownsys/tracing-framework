package edu.brown.cs.systems.baggage;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.collect.Maps;
import com.google.common.io.BaseEncoding;
import com.google.protobuf.ByteString;

/** DetachedBaggage is a baggage instance that isn't currently attached to any thread of execution. The static Baggage
 * APIs provide the ability to re-attach a DetachedBaggage to a thread, and the class provides methods to serialize the
 * detached baggage to bytes.
 * 
 * For safety reasons, DetachedBaggage does not provide an API for getting and setting values; this can only be done
 * when it is attached to a thread. */
public class DetachedBaggage {

    public static final DetachedBaggage EMPTY = new DetachedBaggage(null);

    BaggageImpl impl;

    private DetachedBaggage(BaggageImpl impl) {
        this.impl = impl;
    }

    static DetachedBaggage wrap(BaggageImpl impl) {
        if (impl == null || impl.isEmpty()) {
            return EMPTY;
        } else {
            return new DetachedBaggage(impl);
        }
    }

    /** Copy the contents of another detached baggage and create a new instance */
    public static DetachedBaggage split(DetachedBaggage other) {
        return other == null ? EMPTY : other.split();
    }

    /** Deserialize a byte array into a DetachedBaggage instance. */
    public static DetachedBaggage deserialize(byte[] bytes) {
        return wrap(BaggageImpl.deserialize(bytes));
    }

    /** Deserialize a {@link ByteString} into a DetachedBaggage instance. */
    public static DetachedBaggage deserialize(ByteString bytes) {
        return wrap(BaggageImpl.deserialize(bytes));
    }

    /** Parse the provided string that has been encoded using the specified encoding. Valid encoders are BASE16, BASE32,
     * BASE32HEX, BASE64, BASE64URL. If the encoding was invalid, the detached baggage will be empty
     * 
     * @param encodedContext The context to decode
     * @param encoding An encoding to use - one of BASE16, BASE32, BASE32HEX, BASE64, BASE64URL.
     * @return A detached baggage */
    public static DetachedBaggage decode(String encodedContext, StringEncoding encoding) {
        byte[] bytes = null;
        try {
            bytes = encoding.encoder.decode(encodedContext);
        } catch (Throwable t) {}
        return deserialize(bytes);
    }

    /** Serialize the contents of this DetachedBaggage into a byte array and clear this DetachedBaggage's contents */
    public byte[] toByteArray() {
        try {
            return impl == null ? ArrayUtils.EMPTY_BYTE_ARRAY : impl.toByteArray();
        } finally {
            impl = null;
        }
    }

    /** Serialize the contents of this DetachedBaggage into a byte string and clear this DetachedBaggage's contents */
    public ByteString toByteString() {
        try {
            return impl == null ? ByteString.EMPTY : impl.toByteString();
        } finally {
            impl = null;
        }
    }

    /** Encodes the baggage to a string using base 64 encoding. Uses Google's encoders to do the encoding. This method
     * is equivalent to calling toString(Encoding.BASE64)
     * 
     * Once the baggage has been encoded, its contents are cleared. */
    public String toStringBase64() {
        return toString(StringEncoding.BASE64);
    }

    /** Encodes the baggage to a string, using the provided encoding. Valid encoders are BASE16, BASE32, BASE32HEX,
     * BASE64, BASE64URL. Uses Google's encoders to do the encoding. Once the baggage has been encoded, its contents are
     * cleared.
     * 
     * @param encoding An encoding to use - one of BASE16, BASE32, BASE32HEX, BASE64, BASE64URL.
     * @return The context encoded as a string */
    public String toString(StringEncoding encoding) {
        return encode(toByteArray(), encoding);
    }
    
    static String encode(byte[] bytes, StringEncoding encoding) {
        return encoding.encoder.encode(bytes);
    }
    
    /** Encodes the baggage to strings to set as environment variables, which will then be picked up in a child process */
    public Map<String, String> environment() {
        String base64 = toStringBase64();
        if (base64 == null || base64.length() == 0) {
            return Collections.emptyMap();
        }
        Map<String, String> environment = Maps.newHashMap();
        environment.put(BaggageUtils.BAGGAGE_ENVIRONMENT_VARIABLE, base64);
        return environment;
    }

    /** Creates a new DetachedBaggage instance whose contents are a copy of this one. */
    public DetachedBaggage split() {
        return wrap(impl == null ? null : impl.split());
    }

    /** Merge the contents of two detached baggages into a new detached baggage.
     * The provided detached baggage instances will be empty after making this call */
    public static DetachedBaggage merge(DetachedBaggage a, DetachedBaggage b) {
        if (a == null || a.impl == null) {
            return transferContents(b);
        } else if (b == null || b.impl == null) {
            return transferContents(a);
        } else {
            BaggageImpl impl = a.impl;
            impl.merge(b.impl);
            a.impl = null;
            b.impl = null;
            return wrap(impl);
        }
    }
    
    /** Creates a new DetachedBaggage and transfer the contents from the provided DetachedBaggage.
     * After this call completes, the provided DetachedBaggage will be empty. */
    public static DetachedBaggage transferContents(DetachedBaggage from) {
        if (from == null || from.impl == null) {
            return EMPTY;
        } else {
            DetachedBaggage transferred = wrap(from.impl);
            from.impl = null;
            return transferred;
        }
    }

    /** Enum containing types of string encoding */
    public static enum StringEncoding {
        BASE16(BaseEncoding.base16()), BASE32(BaseEncoding.base32()), BASE32HEX(BaseEncoding.base32Hex()), BASE64(
                BaseEncoding.base64()), BASE64URL(BaseEncoding.base64Url());

        final BaseEncoding encoder;

        private StringEncoding(BaseEncoding encoder) {
            this.encoder = encoder;
        }
    }

}
