package edu.brown.cs.systems.retro.benchmarks.hdfs;

import java.util.Random;

import com.google.protobuf.ByteString;

import edu.brown.cs.systems.baggage.Baggage;
import edu.brown.cs.systems.baggage.BaggageContents;
import edu.brown.cs.systems.retro.Retro;

public class BenchmarkUtils {

    public static final Random r = new Random(22);
    
    public static void SetTenant(int tenantId) {
        Retro.setTenant(tenantId);
    }
    
    public static ByteString randomByteString(int size) {
        byte[] arr = new byte[size];
        r.nextBytes(arr);
        return ByteString.copyFrom(arr);
    }

    public static void PopulateCurrentBaggage(int nkeys, int bytesPerKey) {
        if (bytesPerKey <= 0) {
            return;
        }
        for (int i = 0; i < nkeys; i++) {
            ByteString namespace = randomByteString(2);
            ByteString key = randomByteString(4);
            ByteString value = randomByteString(bytesPerKey);
            BaggageContents.add(namespace, key, value);
        }
    }

    public static void StopTracing() {
        Baggage.stop();
    }

}
