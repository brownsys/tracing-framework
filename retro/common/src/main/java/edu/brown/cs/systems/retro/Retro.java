package edu.brown.cs.systems.retro;

import java.nio.ByteBuffer;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

import edu.brown.cs.systems.baggage.BaggageContents;

public class Retro {

    private static final Logger log = LoggerFactory.getLogger(Retro.class);

    /** For now, Retro is statically assigned the namespace for the byte '2' **/
    public static final ByteString RETRO_BAGGAGE_NAMESPACE = ByteString.copyFrom(new byte[] { 0x02 });
    
    /** Within the Retro namespace, there is only one field -- the tenant ID */
    public static final ByteString TENANT_ID_BAGGAGE_FIELD = ByteString.copyFrom(new byte[] { 0x01 });

    /**
     * Checks the baggage being propagated in the current thread to see whether
     * it contains a tenant ID
     * 
     * @return true if the current thread has a tenant ID, false otherwise
     */
    public static boolean hasTenant() {
        return BaggageContents.contains(RETRO_BAGGAGE_NAMESPACE, TENANT_ID_BAGGAGE_FIELD);
    }

    /**
     * Get the current tenant ID from the baggage, if one is being propagated
     * 
     * @return the tenant ID being propagated in the baggage, or 0 if no tenant
     *         is being propagated
     */
    public static int getTenant() {
        if (BaggageContents.contains(RETRO_BAGGAGE_NAMESPACE, TENANT_ID_BAGGAGE_FIELD)) {
            Set<ByteString> tenantIds = BaggageContents.get(RETRO_BAGGAGE_NAMESPACE, TENANT_ID_BAGGAGE_FIELD);
            if (tenantIds.size() == 1) {
                ByteString tenantId = tenantIds.iterator().next();
                if (tenantId.size() == 4) {
                    return tenantId.asReadOnlyByteBuffer().getInt();
                }
                log.warn("Expected 4-byte tenantID, actually got {} bytes: {}", tenantId.size(), tenantId);
            }
            if (tenantIds.size() > 1) {
                log.warn("Execution has {} tenant IDs", tenantIds.size());
            }

            // Remove erroneous tenant ID value
            BaggageContents.remove(RETRO_BAGGAGE_NAMESPACE, TENANT_ID_BAGGAGE_FIELD);
        }
        return 0;
    }

    /**
     * Set the tenant ID for the current execution to the provided tenant ID
     * 
     * @param tenantId
     *            the tenant ID to set for the current execution
     */
    public static void setTenant(int tenantId) {
        byte[] tenantIdBytes = ByteBuffer.allocate(4).putInt(tenantId).array();
        BaggageContents.replace(RETRO_BAGGAGE_NAMESPACE, TENANT_ID_BAGGAGE_FIELD, ByteString.copyFrom(tenantIdBytes));
    }

}
