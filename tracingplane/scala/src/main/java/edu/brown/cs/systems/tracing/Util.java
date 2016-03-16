package edu.brown.cs.systems.tracing;

import scala.concurrent.Future;

public class Util {
    
    public static void process(Future<?> f) {
        System.out.println(f.getClass().getName());
        System.out.println(f instanceof Future);
    }

}
