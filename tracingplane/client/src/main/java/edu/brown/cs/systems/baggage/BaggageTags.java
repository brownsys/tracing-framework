package edu.brown.cs.systems.baggage;

import java.nio.ByteBuffer;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

import edu.brown.cs.systems.baggage.BaggageContents;

/** Simple use of the baggage to propagate string tags with a request.
 * Useful for prototyping and as an example of how to use Baggage for your own namespace
 */
public class BaggageTags {

      private static final Logger log = LoggerFactory.getLogger(BaggageTags.class);

      /** Statically assign the namespace to use the byte '3' **/
      public static final ByteString BAGGAGE_TAG_NAMESPACE = ByteString.copyFrom(new byte[] { 0x05 });
      
      /** Within the BaggageTag namespace, there is only one field -- the tenant ID */
      public static final ByteString TAG_ID_BAGGAGE_FIELD = ByteString.copyFrom(new byte[] { 0x01 });

      /**
       * Checks the baggage being propagated in the current thread to see whether
       * it contains a tenant ID
       * 
       * @return true if the current thread has a tenant ID, false otherwise
       */
      public static boolean hasTenant() {
          return BaggageContents.contains(BAGGAGE_TAG_NAMESPACE, TAG_ID_BAGGAGE_FIELD);
      }

      /**
       * Get the current tenant ID from the baggage, if one is being propagated
       * 
       * @return the tenant ID being propagated in the baggage, or 0 if no tenant
       *         is being propagated
       */
      public static int[] getTenants() {
          if (BaggageContents.contains(BAGGAGE_TAG_NAMESPACE, TAG_ID_BAGGAGE_FIELD)) {
            Set<ByteString> tenantIds = BaggageContents.get(BAGGAGE_TAG_NAMESPACE, TAG_ID_BAGGAGE_FIELD);
            if (tenantIds.size() >= 1) {
              int[] tags = new int[tenantIds.size()];
              int i = 0;
              for (ByteString tenantId : tenantIds) {
                if (tenantId.size() == 4) {
                  tags[i] = tenantId.asReadOnlyByteBuffer().getInt();
                } else {
                  log.warn("Expected 4-byte tenantID, actually got {} bytes: {}", tenantId.size(), tenantId);
                  tags[i] = -1;
                }
                i++;
              }
              return tags;
            }

            // Remove erroneous tenant ID value
            BaggageContents.remove(BAGGAGE_TAG_NAMESPACE, TAG_ID_BAGGAGE_FIELD);
          }
          return null;
      }

      /**
       * Set the tenant ID for the current execution to the provided tenant ID
       * 
       * @param tenantId
       *            the tenant ID to set for the current execution
       */
      public static void setTenant(int tenantId) {
          byte[] tenantIdBytes = ByteBuffer.allocate(4).putInt(tenantId).array();
          BaggageContents.replace(BAGGAGE_TAG_NAMESPACE, TAG_ID_BAGGAGE_FIELD, ByteString.copyFrom(tenantIdBytes));
      }
}
