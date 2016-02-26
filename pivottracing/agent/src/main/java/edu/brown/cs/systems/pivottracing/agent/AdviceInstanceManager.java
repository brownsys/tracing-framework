package edu.brown.cs.systems.pivottracing.agent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Manages advice instances */
public class AdviceInstanceManager {

    private static final AtomicInteger adviceIdSeed = new AtomicInteger(0);
    private final AtomicReference<ActiveInstances> active = new AtomicReference<ActiveInstances>(new ActiveInstances());

    /** Registers an advice instance, returning the unique lookup id for the advice */
    public int register(Advice advice) {
        int adviceId = adviceIdSeed.getAndIncrement();
        ActiveInstances current, next;
        do {
            current = active.get();
            next = current.add(advice, adviceId);
        } while (!active.compareAndSet(current, next));
        return adviceId;
    }
    
    /** Look up an advice by ID.  Returns null if none found */
    public Advice lookup(int lookupId) {
        return active.get().lookup(lookupId);
    }
    
    /** Delete advice by ID */
    public void remove(int lookupId) {
        ActiveInstances current, next;
        do {
            current = active.get();
            next = current.remove(lookupId);
        } while (!active.compareAndSet(current, next));
    }
    
    /** Currently active instances */
    private static class ActiveInstances {
        public final Advice[] instances;
        public final int[] keys;
        
        public ActiveInstances() {
            this(new Advice[0], new int[0]);
        }
        
        public ActiveInstances(Advice[] instances, int[] keys) {
            this.instances = instances;
            this.keys = keys;
        }
        
        /** Get advice by key, return null if it doesn't exist */
        public Advice lookup(int key) {
            // http://algs4.cs.princeton.edu/11model/BinarySearch.java.html
            int lo = 0;
            int hi = keys.length - 1;
            while (lo <= hi) {
                // Key is in a[lo..hi] or not present.
                int mid = lo + (hi - lo) / 2;
                if      (key < keys[mid]) hi = mid - 1;
                else if (key > keys[mid]) lo = mid + 1;
                else return instances[mid];
            }
            return null;
        }
        
        /** Add advice with the specified key, returning a new object. The original is unmodified */
        public ActiveInstances add(Advice advice, int key) {
            Advice[] newInstances = new Advice[instances.length+1];
            int[] newKeys = new int[keys.length+1];
            
            // Copy up to midpoint
            int i = 0;
            while (i < keys.length && keys[i] < key) {
                newInstances[i] = instances[i];
                newKeys[i] = keys[i];
                i++;
            }
            
            // Copy new advice
            newInstances[i] = advice;
            newKeys[i] = key;
            
            // Copy remaining
            while (i < keys.length) {
                newInstances[i+1] = instances[i];
                newKeys[i+1] = keys[i];
                i++;
            }
            
            return new ActiveInstances(newInstances, newKeys);
        }
        
        /** Remove advice with the specified key, returning a new object.  The original is unmodified */
        public ActiveInstances remove(int key) {
            // Create arrays with same size as previous, in case of no match, and since could have multiples
            Advice[] extractedInstances = new Advice[instances.length];
            int[] extractedKeys = new int[keys.length];
            
            // Copy everything that doesn't match
            int count = 0;
            for (int i = 0; i < keys.length; i++) {
                if (keys[i] != key) {
                    extractedInstances[count] = instances[i];
                    extractedKeys[count] = keys[i];
                    count++;
                }
            }
            
            // Contract to correct size
            Advice[] newInstances = new Advice[count];
            int[] newKeys = new int[count];
            System.arraycopy(extractedInstances, 0, newInstances, 0, count);
            System.arraycopy(extractedKeys, 0, newKeys, 0, count);
            
            return new ActiveInstances(newInstances, newKeys);
        }
    }

}
