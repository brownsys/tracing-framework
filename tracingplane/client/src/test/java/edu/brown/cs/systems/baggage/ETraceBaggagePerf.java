package edu.brown.cs.systems.baggage;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import com.google.protobuf.ByteString;

import edu.brown.cs.systems.baggage.Baggage;
import edu.brown.cs.systems.baggage.BaggageContents;
import edu.brown.cs.systems.baggage.DetachedBaggage;
import junit.framework.TestCase;

public class ETraceBaggagePerf extends TestCase {

    private static final DecimalFormat format = new DecimalFormat("#.##");

    private void printTitle(String title, String description) {
        System.out.println(title +" | " + description + ":");
    }
    private void printResults(double duration, double count, double cycles) {
        System.out.println("  Time:  " + format.format(duration / 1000000000.0)
                + " seconds");
        System.out.println("  Count: " + count);
        System.out.println("  Avg:   " + format.format(duration / count)
                + " ns");
        System.out.println("  CPU:   " + format.format(cycles / count)
                + " cpu ns");
    }

    private static final ThreadMXBean tbean = ManagementFactory
            .getThreadMXBean();

    @Before
    public void setUp() {
        Baggage.current.remove();
        Baggage.discard();
    }
    
    @After
    public void tearDown() {
        Baggage.discard();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        Baggage.current.remove();
    }

    @Test
    public void testDuplicatedValueAddGet() {
        printTitle("ADD", "Duplicated Values; Same Key");
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        ByteString mykey = ByteString.copyFrom("k1".getBytes());
        ByteString md = ByteString.copyFrom("value".getBytes());
        int perIteration = 10000000;

        long startcpu = tbean.getCurrentThreadCpuTime();
        long start = System.nanoTime();
        int count = 0;
        for (int i = 0; i < perIteration; i++) {
            BaggageContents.getNamespace(myname).add(mykey, md);
            count++;
        }
        long duration = System.nanoTime() - start;
        long cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);

        printTitle("GET", "Single Value; Same Key");
        perIteration = 10000000;

        startcpu = tbean.getCurrentThreadCpuTime();
        start = System.nanoTime();
        count = 0;
        for (int i = 0; i < perIteration; i++) {
            BaggageContents.getNamespace(myname).get(mykey);
            count++;
        }
        duration = System.nanoTime() - start;
        cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
    }

    @Test
    public void testDistinctValueAddGet() {
        printTitle("ADD", "Distinct Values; Same Key");
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        ByteString mykey = ByteString.copyFrom("k1".getBytes());
        int perIteration = 100000;
        ByteString[] mytestvalues = new ByteString[perIteration];
        for (int i = 0; i < perIteration; i++)
            mytestvalues[i] = ByteString.copyFrom(Integer.toString(i).getBytes());

        long startcpu = tbean.getCurrentThreadCpuTime();
        long start = System.nanoTime();
        int count = 0;
        for (int i = 0; i < perIteration; i++) {
            BaggageContents.getNamespace(myname).add(mykey, mytestvalues[i]);
            count++;
        }
        long duration = System.nanoTime() - start;
        long cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);

        printTitle("GET", "a Set of Values; Same Key");
        perIteration = 10000000;

        startcpu = tbean.getCurrentThreadCpuTime();
        start = System.nanoTime();
        count = 0;
        for (int i = 0; i < perIteration; i++) {
            BaggageContents.getNamespace(myname).get(mykey);
            count++;
        }
        duration = System.nanoTime() - start;
        cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
    }

    @Test
    public void testDistinctKeyAddGetRemove() {
        printTitle("ADD", "Distinct Keys; Same Namespace");
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        ByteString myval = ByteString.copyFrom("v1".getBytes());
        int perIteration = 1000000;
        BaggageContents.getNamespace(myname).add(ByteString.copyFrom("init".getBytes()), myval);
        ByteString[] mytestkeys = new ByteString[perIteration];
        for (int i = 0; i < perIteration; i++)
            mytestkeys[i] = ByteString.copyFrom(Integer.toString(i).getBytes());

        long startcpu = tbean.getCurrentThreadCpuTime();
        long start = System.nanoTime();
        int count = 0;
        for (int i = 0; i < perIteration; i++) {
            BaggageContents.getNamespace(myname).add(mytestkeys[i], myval);
            count++;
        }
        long duration = System.nanoTime() - start;
        long cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);

        printTitle("GET", "Distinct Keys; Same Namespace");
        perIteration = 1000000;

        startcpu = tbean.getCurrentThreadCpuTime();
        start = System.nanoTime();
        count = 0;
        for (int i = 0; i < perIteration; i++) {
            BaggageContents.getNamespace(myname).get(mytestkeys[i]);
            count++;
        }
        duration = System.nanoTime() - start;
        cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);

        printTitle("REMOVE", "Distinct Keys; Same Namespace");
        perIteration = 1000000;

        startcpu = tbean.getCurrentThreadCpuTime();
        start = System.nanoTime();
        count = 0;
        for (int i = 0; i < perIteration; i++) {
            BaggageContents.getNamespace(myname).remove(mytestkeys[i]);
            count++;
        }
        duration = System.nanoTime() - start;
        cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
    }

    @Test
    public void testDistinctNamespaceAddGetRemove() {
        printTitle("ADD", "Distinct Namespaces");
        ByteString mykey = ByteString.copyFrom("k1".getBytes());
        ByteString myval = ByteString.copyFrom("v1".getBytes());
        int perIteration = 100000;
        ByteString[] mytestnames = new ByteString[perIteration];
        for (int i = 0; i < perIteration; i++)
            mytestnames[i] = ByteString.copyFrom(Integer.toString(i).getBytes());

        long startcpu = tbean.getCurrentThreadCpuTime();
        long start = System.nanoTime();
        int count = 0;
        for (int i = 0; i < perIteration; i++) {
            BaggageContents.getNamespace(mytestnames[i]).add(mykey, myval);
            count++;
        }
        long duration = System.nanoTime() - start;
        long cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);

        printTitle("GET", "Distinct Namespaces");
        perIteration = 100000;

        startcpu = tbean.getCurrentThreadCpuTime();
        start = System.nanoTime();
        count = 0;
        for (int i = 0; i < perIteration; i++) {
            BaggageContents.getNamespace(mytestnames[i]).get(mykey);
            count++;
        }
        duration = System.nanoTime() - start;
        cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);

        printTitle("REMOVE", "Distinct Keys + Remove Namespaces");
        perIteration = 100000;

        startcpu = tbean.getCurrentThreadCpuTime();
        start = System.nanoTime();
        count = 0;
        for (int i = 0; i < perIteration; i++) {
            BaggageContents.getNamespace(mytestnames[i]).remove(mykey);
            count++;
        }
        duration = System.nanoTime() - start;
        cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
    }

    @Test
    public void testValueReplace() {
        printTitle("REPLACE", "Values");
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        ByteString mykey = ByteString.copyFrom("k1".getBytes());
        int perIteration = 10000000;
        ByteString myvalue = ByteString.copyFrom("init".getBytes());
        
        BaggageContents.getNamespace(myname).add(mykey, myvalue);

        long startcpu = tbean.getCurrentThreadCpuTime();
        long start = System.nanoTime();
        int count = 0;
        for (int i = 0; i < perIteration; i++) {
            BaggageContents.getNamespace(myname).replace(mykey, myvalue);
            count++;
        }
        long duration = System.nanoTime() - start;
        long cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
    }

    @Test
    public void testIterableReplace() {
        printTitle("REPLACE", "Iterables");
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        ByteString mykey = ByteString.copyFrom("k1".getBytes());
        int perIteration = 10000;
        int iterableSize = 10000;
        ArrayList<ByteString> mylist = new ArrayList<ByteString>();
        for (int i = 0; i < iterableSize; i++) {
            mylist.add(ByteString.copyFrom(Integer.toString(i).getBytes()));
        }
        
        BaggageContents.getNamespace(myname).add(mykey, ByteString.copyFrom("init".getBytes()));

        long startcpu = tbean.getCurrentThreadCpuTime();
        long start = System.nanoTime();
        int count = 0;
        for (int i = 0; i < perIteration; i++) {
            BaggageContents.getNamespace(myname).replace(mykey, mylist);
            count++;
        }
        long duration = System.nanoTime() - start;
        long cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
    }

    @Test
    public void testNewKeyReplace() {
        printTitle("REPLACE", "New Keys");
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        ByteString myval = ByteString.copyFrom("v1".getBytes());
        int perIteration = 1000000;
        ByteString[] mytestkeys = new ByteString[perIteration];
        for (int i = 0; i < perIteration; i++)
            mytestkeys[i] = ByteString.copyFrom(Integer.toString(i).getBytes());

        long startcpu = tbean.getCurrentThreadCpuTime();
        long start = System.nanoTime();
        int count = 0;
        for (int i = 0; i < perIteration; i++) {
            BaggageContents.getNamespace(myname).replace(mytestkeys[i], myval);
            count++;
        }
        long duration = System.nanoTime() - start;
        long cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
    }

    @Test
    public void testNonExistKeyGet() {
        printTitle("GET", "Non-Exist Keys");
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        ByteString mykey = ByteString.copyFrom("v1".getBytes());
        int perIteration = 10000000;
        ByteString[] mytestkeys = new ByteString[perIteration];
        for (int i = 0; i < perIteration; i++)
            mytestkeys[i] = ByteString.copyFrom(Integer.toString(i).getBytes());
        
        BaggageContents.getNamespace(myname).add(mykey, ByteString.copyFrom("init".getBytes()));

        long startcpu = tbean.getCurrentThreadCpuTime();
        long start = System.nanoTime();
        int count = 0;
        for (int i = 0; i < perIteration; i++) {
            BaggageContents.getNamespace(myname).get(mytestkeys[i]);
            count++;
        }
        long duration = System.nanoTime() - start;
        long cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
    }

    @Test
    public void testNonExistNamespaceGet() {
        printTitle("GET", "Non-Exist Namespaces");
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        ByteString mykey = ByteString.copyFrom("v1".getBytes());
        int perIteration = 10000000;
        ByteString[] mytestnames = new ByteString[perIteration];
        for (int i = 0; i < perIteration; i++)
            mytestnames[i] = ByteString.copyFrom(Integer.toString(i).getBytes());
        
        BaggageContents.getNamespace(myname).add(mykey, ByteString.copyFrom("init".getBytes()));

        long startcpu = tbean.getCurrentThreadCpuTime();
        long start = System.nanoTime();
        int count = 0;
        for (int i = 0; i < perIteration; i++) {
            BaggageContents.getNamespace(mytestnames[i]).get(mykey);
            count++;
        }
        long duration = System.nanoTime() - start;
        long cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
    }

    @Test
    public void testBranchSerializeDeserialize() {
        printTitle("BRANCH", "10 namespaces * 10 keys * 100 values");
        int perIteration = 10000;
        
        for (int i = 0; i < 10; i++)
            for (int j = 0; j < 10; j++)
                for (int k = 0; k < 100; k++) {
                    BaggageContents.getNamespace(ByteString.copyFrom(Integer.toString(i).getBytes()))
                        .add(ByteString.copyFrom(Integer.toString(j).getBytes()), 
                                ByteString.copyFrom(Integer.toString(k).getBytes()));
                }

        long startcpu = tbean.getCurrentThreadCpuTime();
        long start = System.nanoTime();
        int count = 0;
        for (int i = 0; i < perIteration; i++) {
            Baggage.fork();
            count++;
        }
        long duration = System.nanoTime() - start;
        long cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
        
        printTitle("BRANCH_AS_BYTES", "10 namespaces * 10 keys * 100 values");
        perIteration = 10000;

        startcpu = tbean.getCurrentThreadCpuTime();
        start = System.nanoTime();
        count = 0;
        for (int i = 0; i < perIteration; i++) {
            Baggage.fork().toByteArray();
            count++;
        }
        duration = System.nanoTime() - start;
        cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
        
        printTitle("CONTINUE_FROM_BYTES", "10 namespaces * 10 keys * 100 values");
        perIteration = 10000;
        byte[] s = Baggage.fork().toByteArray();

        startcpu = tbean.getCurrentThreadCpuTime();
        start = System.nanoTime();
        count = 0;
        for (int i = 0; i < perIteration; i++) {
            Baggage.join(s);
            count++;
        }
        duration = System.nanoTime() - start;
        cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
    }

    @Test
    public void testDisjointNamespaceJoin() {
        printTitle("LOAD+CONTINUE", "2 Disjoint Namespaces * 10 keys * 100 values");
        ByteString myname1 = ByteString.copyFrom("namespace1".getBytes());
        ByteString myname2 = ByteString.copyFrom("namespace2".getBytes());
        int perIteration = 10000;
        
        for (int j = 0; j < 10; j++)
            for (int k = 0; k < 100; k++) {
                BaggageContents.getNamespace(myname1).add(
                        ByteString.copyFrom(Integer.toString(j).getBytes()), 
                        ByteString.copyFrom(Integer.toString(k).getBytes()));
            }
        DetachedBaggage a = Baggage.stop();
        for (int j = 0; j < 10; j++)
            for (int k = 0; k < 100; k++) {
                BaggageContents.getNamespace(myname2).add(
                        ByteString.copyFrom(Integer.toString(j).getBytes()), 
                        ByteString.copyFrom(Integer.toString(k).getBytes()));
            }
        DetachedBaggage b = Baggage.stop();

        DetachedBaggage[] duplicates = new DetachedBaggage[perIteration];
        for (int i = 0; i < perIteration; i++)
            duplicates[i] = a.split();
        long startcpu = tbean.getCurrentThreadCpuTime();
        long start = System.nanoTime();
        int count = 0;
        for (int i = 0; i < perIteration; i++) {
            Baggage.start(duplicates[i]);
            Baggage.join(b);
            count++;
        }
        long duration = System.nanoTime() - start;
        long cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
    }

    @Test
    public void testDisjointKeyJoin() {
        printTitle("LOAD+CONTINUE", "2 Disjoint Keys * 1000 values");
        ByteString myname1 = ByteString.copyFrom("namespace1".getBytes());
        ByteString mykey1 = ByteString.copyFrom("key1".getBytes());
        ByteString mykey2 = ByteString.copyFrom("key2".getBytes());
        int perIteration = 1000;

        for (int k = 0; k < 1000; k++) {
            BaggageContents.getNamespace(myname1).add(mykey1, 
                    ByteString.copyFrom(Integer.toString(k).getBytes()));
        }
        DetachedBaggage a = Baggage.stop();
        for (int k = 0; k < 1000; k++) {
            BaggageContents.getNamespace(myname1).add(mykey2, 
                    ByteString.copyFrom(Integer.toString(k).getBytes()));
        }
        DetachedBaggage b = Baggage.stop();

        DetachedBaggage[] duplicates = new DetachedBaggage[perIteration];
        for (int i = 0; i < perIteration; i++)
            duplicates[i] = a.split();
        long startcpu = tbean.getCurrentThreadCpuTime();
        long start = System.nanoTime();
        int count = 0;
        for (int i = 0; i < perIteration; i++) {
            Baggage.start(duplicates[i]);
            Baggage.join(b);
            count++;
        }
        long duration = System.nanoTime() - start;
        long cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
    }

    @Test
    public void testDisjointValueJoin() {
        printTitle("LOAD+CONTINUE", "2000 * Disjoint Values");
        ByteString myname1 = ByteString.copyFrom("namespace1".getBytes());
        ByteString mykey1 = ByteString.copyFrom("key1".getBytes());
        int perIteration = 10000;

        for (int k = 0; k < 1000; k++) {
            BaggageContents.getNamespace(myname1).add(mykey1, 
                    ByteString.copyFrom(Integer.toString(k).getBytes()));
        }
        DetachedBaggage a = Baggage.stop();
        for (int k = 0; k < 1000; k++) {
            BaggageContents.getNamespace(myname1).add(mykey1, 
                    ByteString.copyFrom(Integer.toString(k + 1000).getBytes()));
        }
        DetachedBaggage b = Baggage.stop();

        DetachedBaggage[] duplicates = new DetachedBaggage[perIteration];
        for (int i = 0; i < perIteration; i++)
            duplicates[i] = a.split();
        long startcpu = tbean.getCurrentThreadCpuTime();
        long start = System.nanoTime();
        int count = 0;
        for (int i = 0; i < perIteration; i++) {
            Baggage.start(duplicates[i]);
            Baggage.join(b);
            count++;
        }
        long duration = System.nanoTime() - start;
        long cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
    }

}
