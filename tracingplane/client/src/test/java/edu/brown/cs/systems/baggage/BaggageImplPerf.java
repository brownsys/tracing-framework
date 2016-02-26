package edu.brown.cs.systems.baggage;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.junit.Test;

import com.google.protobuf.ByteString;

import edu.brown.cs.systems.baggage.BaggageImpl;
import junit.framework.TestCase;

public class BaggageImplPerf extends TestCase {

    private static final DecimalFormat format = new DecimalFormat("#.##");

    private void printTitle(String title, String description) {
        System.out.println(title +" | " + description + ":");
    }
    private void printResults(double duration, double count, double cycles) {
        System.out.println("  Time (s):     " + format.format(duration / 1000000000.0));
        System.out.println("  Count:        " + count);
        System.out.println("  Avg (ns):     " + format.format(duration / count));
        System.out.println("  CPU (cpu ns): " + format.format(cycles / count));
    }

    private static final ThreadMXBean tbean = ManagementFactory
            .getThreadMXBean();

    @Test
    public void testDuplicatedValueAddGet() {
        printTitle("ADD", "Duplicated Values; Same Key");
        BaggageImpl b = new BaggageImpl();
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        ByteString mykey = ByteString.copyFrom("k1".getBytes());
        ByteString md = ByteString.copyFrom("value".getBytes());
        int perIteration = 10000000;

        long startcpu = tbean.getCurrentThreadCpuTime();
        long start = System.nanoTime();
        int count = 0;
        for (int i = 0; i < perIteration; i++) {
            b.add(myname, mykey, md);
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
            b.get(myname, mykey);
            count++;
        }
        duration = System.nanoTime() - start;
        cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
    }

    @Test
    public void testDistinctValueAddGet() {
        printTitle("ADD", "Distinct Values; Same Key");
        BaggageImpl b = new BaggageImpl();
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
            b.add(myname, mykey, mytestvalues[i]);
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
            b.get(myname, mykey);
            count++;
        }
        duration = System.nanoTime() - start;
        cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
    }

    @Test
    public void testDistinctKeyAddGetRemove() {
        printTitle("ADD", "Distinct Keys; Same Namespace");
        BaggageImpl b = new BaggageImpl();
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        ByteString myval = ByteString.copyFrom("v1".getBytes());
        int perIteration = 1000000;
        b.add(myname, ByteString.copyFrom("init".getBytes()), myval);
        ByteString[] mytestkeys = new ByteString[perIteration];
        for (int i = 0; i < perIteration; i++)
            mytestkeys[i] = ByteString.copyFrom(Integer.toString(i).getBytes());

        long startcpu = tbean.getCurrentThreadCpuTime();
        long start = System.nanoTime();
        int count = 0;
        for (int i = 0; i < perIteration; i++) {
            b.add(myname, mytestkeys[i], myval);
            count++;
        }
        long duration = System.nanoTime() - start;
        long cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);

        printTitle("GET", "Distinct Keys; Same Namespace");

        startcpu = tbean.getCurrentThreadCpuTime();
        start = System.nanoTime();
        count = 0;
        for (int i = 0; i < perIteration; i++) {
            b.get(myname, mytestkeys[i]);
            count++;
        }
        duration = System.nanoTime() - start;
        cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);

        printTitle("REMOVE", "Distinct Keys; Same Namespace");

        startcpu = tbean.getCurrentThreadCpuTime();
        start = System.nanoTime();
        count = 0;
        for (int i = 0; i < perIteration; i++) {
            b.remove(myname, mytestkeys[i]);
            count++;
        }
        duration = System.nanoTime() - start;
        cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
    }

    @Test
    public void testDistinctNamespaceAddGetRemove() {
        printTitle("ADD", "Distinct Namespaces");
        BaggageImpl b = new BaggageImpl();
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
            b.add(mytestnames[i], mykey, myval);
            count++;
        }
        long duration = System.nanoTime() - start;
        long cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);

        printTitle("GET", "Distinct Namespaces");

        startcpu = tbean.getCurrentThreadCpuTime();
        start = System.nanoTime();
        count = 0;
        for (int i = 0; i < perIteration; i++) {
            b.get(mytestnames[i], mykey);
            count++;
        }
        duration = System.nanoTime() - start;
        cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);

        printTitle("REMOVE", "Distinct Keys + Remove Namespaces");

        startcpu = tbean.getCurrentThreadCpuTime();
        start = System.nanoTime();
        count = 0;
        for (int i = 0; i < perIteration; i++) {
            b.remove(mytestnames[i], mykey);
            count++;
        }
        duration = System.nanoTime() - start;
        cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
    }

    @Test
    public void testValueReplace() {
        printTitle("REPLACE", "Values");
        BaggageImpl b = new BaggageImpl();
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        ByteString mykey = ByteString.copyFrom("k1".getBytes());
        ByteString myvalue = ByteString.copyFrom("init".getBytes());
        int perIteration = 10000000;
        
        b.add(myname, mykey, myvalue);

        long startcpu = tbean.getCurrentThreadCpuTime();
        long start = System.nanoTime();
        int count = 0;
        for (int i = 0; i < perIteration; i++) {
            b.replace(myname, mykey, myvalue);
            count++;
        }
        long duration = System.nanoTime() - start;
        long cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
    }

    @Test
    public void testIterableReplace() {
        printTitle("REPLACE", "Iterables");
        BaggageImpl b = new BaggageImpl();
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        ByteString mykey = ByteString.copyFrom("k1".getBytes());
        int perIteration = 10000;
        int iterableSize = 10000;
        ArrayList<ByteString> mylist = new ArrayList<ByteString>();
        for (int i = 0; i < iterableSize; i++) {
            mylist.add(ByteString.copyFrom(Integer.toString(i).getBytes()));
        }
        
        b.add(myname, mykey, ByteString.copyFrom("init".getBytes()));

        long startcpu = tbean.getCurrentThreadCpuTime();
        long start = System.nanoTime();
        int count = 0;
        for (int i = 0; i < perIteration; i++) {
            b.replace(myname, mykey, mylist);
            count++;
        }
        long duration = System.nanoTime() - start;
        long cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
    }

    @Test
    public void testNewKeyReplace() {
        printTitle("REPLACE", "New Keys");
        BaggageImpl b = new BaggageImpl();
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
            b.replace(myname, mytestkeys[i], myval);
            count++;
        }
        long duration = System.nanoTime() - start;
        long cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
    }

    @Test
    public void testNonExistKeyGet() {
        printTitle("GET", "Non-Exist Keys");
        BaggageImpl b = new BaggageImpl();
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        ByteString mykey = ByteString.copyFrom("v1".getBytes());
        int perIteration = 10000000;
        
        b.add(myname, mykey, ByteString.copyFrom("init".getBytes()));
        ByteString mytestkeys[] = new ByteString[perIteration];
        for (int i = 0; i < perIteration; i++) {
            mytestkeys[i] = ByteString.copyFrom(Integer.toString(i).getBytes());
        }

        long startcpu = tbean.getCurrentThreadCpuTime();
        long start = System.nanoTime();
        int count = 0;
        for (int i = 0; i < perIteration; i++) {
            b.get(myname, mytestkeys[i]);
            count++;
        }
        long duration = System.nanoTime() - start;
        long cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
    }

    @Test
    public void testNonExistNamespaceGet() {
        printTitle("GET", "Non-Exist Namespaces");
        BaggageImpl b = new BaggageImpl();
        ByteString myname = ByteString.copyFrom("namespace".getBytes());
        ByteString mykey = ByteString.copyFrom("v1".getBytes());
        int perIteration = 10000000;
        ByteString[] mytestnames = new ByteString[perIteration];
        for (int i = 0; i < perIteration; i++)
            mytestnames[i] = ByteString.copyFrom(Integer.toString(i).getBytes());
        
        b.add(myname, mykey, ByteString.copyFrom("init".getBytes()));

        long startcpu = tbean.getCurrentThreadCpuTime();
        long start = System.nanoTime();
        int count = 0;
        for (int i = 0; i < perIteration; i++) {
            b.get(mytestnames[i], mykey);
            count++;
        }
        long duration = System.nanoTime() - start;
        long cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
    }

    @Test
    public void testSplitSerializeDeserialize() {
        printTitle("SPLIT", "10 namespaces * 10 keys * 100 values");
        BaggageImpl b = new BaggageImpl();
        int perIteration = 10000;
        
        for (int i = 0; i < 10; i++)
            for (int j = 0; j < 10; j++)
                for (int k = 0; k < 100; k++) {
                    b.add(ByteString.copyFrom(Integer.toString(i).getBytes()), 
                            ByteString.copyFrom(Integer.toString(j).getBytes()), 
                            ByteString.copyFrom(Integer.toString(k).getBytes()));
                }

        long startcpu = tbean.getCurrentThreadCpuTime();
        long start = System.nanoTime();
        int count = 0;
        for (int i = 0; i < perIteration; i++) {
            b.split();
            count++;
        }
        long duration = System.nanoTime() - start;
        long cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
        
        printTitle("SERIALIZE", "10 namespaces * 10 keys * 100 values");
        perIteration = 10000;

        startcpu = tbean.getCurrentThreadCpuTime();
        start = System.nanoTime();
        count = 0;
        for (int i = 0; i < perIteration; i++) {
            b.toByteArray();
            count++;
        }
        duration = System.nanoTime() - start;
        cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
        
        printTitle("DESERIALIZE", "10 namespaces * 10 keys * 100 values");
        perIteration = 10000;
        byte[] s = b.toByteArray();

        startcpu = tbean.getCurrentThreadCpuTime();
        start = System.nanoTime();
        count = 0;
        for (int i = 0; i < perIteration; i++) {
            BaggageImpl.deserialize(s);
            count++;
        }
        duration = System.nanoTime() - start;
        cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
    }

    @Test
    public void testDisjointNamespaceJoin() {
        printTitle("JOIN", "2 Disjoint Namespaces * 10 keys * 100 values");
        BaggageImpl a = new BaggageImpl();
        BaggageImpl b = new BaggageImpl();
        ByteString myname1 = ByteString.copyFrom("namespace1".getBytes());
        ByteString myname2 = ByteString.copyFrom("namespace2".getBytes());
        int perIteration = 10000;
        
        for (int j = 0; j < 10; j++)
            for (int k = 0; k < 100; k++) {
                a.add(myname1, 
                        ByteString.copyFrom(Integer.toString(j).getBytes()), 
                        ByteString.copyFrom(Integer.toString(k).getBytes()));
                b.add(myname2, 
                        ByteString.copyFrom(Integer.toString(j).getBytes()), 
                        ByteString.copyFrom(Integer.toString(k).getBytes()));
            }

        BaggageImpl[] duplicates = new BaggageImpl[perIteration];
        for (int i = 0; i < perIteration; i++)
            duplicates[i] = new BaggageImpl(a.contents);
        long startcpu = tbean.getCurrentThreadCpuTime();
        long start = System.nanoTime();
        int count = 0;
        for (int i = 0; i < perIteration; i++) {
            duplicates[i].merge(b);
            count++;
        }
        long duration = System.nanoTime() - start;
        long cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
    }

    @Test
    public void testDisjointKeyJoin() {
        printTitle("JOIN", "2 Disjoint Keys * 1000 values");
        BaggageImpl a = new BaggageImpl();
        BaggageImpl b = new BaggageImpl();
        ByteString myname1 = ByteString.copyFrom("namespace1".getBytes());
        ByteString mykey1 = ByteString.copyFrom("key1".getBytes());
        ByteString mykey2 = ByteString.copyFrom("key2".getBytes());
        int perIteration = 1000;
        
        for (int k = 0; k < 1000; k++) {
            a.add(myname1, mykey1, 
                    ByteString.copyFrom(Integer.toString(k).getBytes()));
            b.add(myname1, mykey2, 
                    ByteString.copyFrom(Integer.toString(k).getBytes()));
        }

        BaggageImpl[] duplicates = new BaggageImpl[perIteration];
        for (int i = 0; i < perIteration; i++)
            duplicates[i] = new BaggageImpl(a.contents);
        long startcpu = tbean.getCurrentThreadCpuTime();
        long start = System.nanoTime();
        int count = 0;
        for (int i = 0; i < perIteration; i++) {
            duplicates[i].merge(b);
            count++;
        }
        long duration = System.nanoTime() - start;
        long cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
    }

    @Test
    public void testDisjointValueJoin() {
        printTitle("JOIN", "2000 * Disjoint Values");
        BaggageImpl a = new BaggageImpl();
        BaggageImpl b = new BaggageImpl();
        ByteString myname1 = ByteString.copyFrom("namespace1".getBytes());
        ByteString mykey1 = ByteString.copyFrom("key1".getBytes());
        int perIteration = 100;
        
        for (int k = 0; k < 1000; k++) {
            a.add(myname1, mykey1, 
                    ByteString.copyFrom(Integer.toString(k).getBytes()));
            b.add(myname1, mykey1, 
                    ByteString.copyFrom(Integer.toString(k + 1000).getBytes()));
        }

        BaggageImpl[] duplicates = new BaggageImpl[perIteration];
        for (int i = 0; i < perIteration; i++)
            duplicates[i] = new BaggageImpl(a.contents);
        long startcpu = tbean.getCurrentThreadCpuTime();
        long start = System.nanoTime();
        int count = 0;
        for (int i = 0; i < perIteration; i++) {
            duplicates[i].merge(b);
            count++;
        }
        long duration = System.nanoTime() - start;
        long cycles = tbean.getCurrentThreadCpuTime() - startcpu;
        printResults(duration, count, cycles);
    }

}
