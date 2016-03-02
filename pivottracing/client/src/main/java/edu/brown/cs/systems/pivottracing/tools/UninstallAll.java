package edu.brown.cs.systems.pivottracing.tools;

import edu.brown.cs.systems.pivottracing.PivotTracingClient;

public class UninstallAll {
    
    public static void main(String[] args) {
        PivotTracingClient.newInstance().uninstallAll();
    }

}
