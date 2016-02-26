package edu.brown.cs.systems.baggage;

import java.util.Set;

/**
 * The contents of a specific key in the baggage
 */
public interface Namespace<K, V> {
    
    public Set<V> get(K key);
    
    public void add(K key, V value);
    
    public void replace(K key, V value);
    
    public void replace(K key, Iterable<? extends V> values);
    
    public void remove(K key);
    
    public boolean has(K key);
    
    public Set<K> keys();

}
