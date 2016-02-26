package edu.brown.cs.systems.retro.throttling.cli;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import edu.brown.cs.systems.retro.throttling.ClusterThrottlingPoints;

public class ThrottlingServerCLI {

    /** A simple CLI for testing */
    public static void main(String[] args) {
        Run();
    }

    private static void PrintHelp() {
        StringBuffer sb = new StringBuffer();
        sb.append("usage:\n");
        sb.append("  - points (print known throttling points)\n");
        sb.append("  - tenants (print known tenants)\n");
        sb.append("  - throttle <tenantId> <point> <rate>\n");
        sb.append("  - clear <tenantId> <point>\n");
        sb.append("  - status (prints the tenants currently being throttled)\n");
        sb.append("  - help (prints this status message)\n");
        sb.append("  - exit\n");
        System.out.println(sb.toString());
    }

    private static void Run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        PrintHelp();
        ClusterThrottlingPoints.getThrottlingPoints(); // subscribes to pubsub
                                                       // stuff
        while (true) {
            System.out.print("> ");
            String line = null;

            try {
                line = reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                boolean exit = ParseAndExecuteCommand(line);
                if (exit)
                    break;
            } catch (Exception e) {
                System.err.println("exception when executing '" + line + "': " + e);
                e.printStackTrace();
            }
        }
    }

    private static final Map<String, Point> points = new HashMap<String, Point>();

    public synchronized static boolean ParseAndExecuteCommand(String line) throws FileNotFoundException {
        String[] tokens = line.split(" ");

        if (tokens.length == 0)
            return false;

        String cmd = tokens[0].trim();

        if (cmd.equalsIgnoreCase("help")) {
            PrintHelp();
        } else if (cmd.equalsIgnoreCase("tenants")) {
            Collection<String> pointNames = ClusterThrottlingPoints.getThrottlingPoints();
            Collection<Integer> tenants = ClusterThrottlingPoints.getTenants();
            StringBuilder b = new StringBuilder();
            for (Integer tenantId : tenants) {
                b.append("Tenant-");
                b.append(tenantId);
                for (String pointName : pointNames) {
                    b.append("  ");
                    b.append(pointName);
                    b.append("(");
                    b.append(ClusterThrottlingPoints.getTenantThrottlingPointInstanceCount(pointName, tenantId));
                    b.append(")");
                }
                b.append("\n");
            }
            System.out.println(b);
        } else if (cmd.equalsIgnoreCase("points")) {
            Collection<String> pointNames = ClusterThrottlingPoints.getThrottlingPoints();
            for (String pointName : pointNames) {
                long count = ClusterThrottlingPoints.getThrottlingPointInstanceCount(pointName);
                System.out.println(String.format("%s: %d instances", pointName, count));
            }
        } else if (cmd.equalsIgnoreCase("status")) {
            printStatus();
        } else if (cmd.equalsIgnoreCase("throttle")) {
            int tenantId = Integer.parseInt(tokens[1]);
            String throttlingPointName = tokens[2];
            double rate = Double.parseDouble(tokens[3]);
            if (!points.containsKey(throttlingPointName))
                points.put(throttlingPointName, new Point(throttlingPointName));
            points.get(throttlingPointName).setRate(tenantId, rate);
            points.get(throttlingPointName).sendUpdate();
        } else if (cmd.equalsIgnoreCase("clear")) {
            int tenantId = Integer.parseInt(tokens[1]);
            String throttlingPointName = tokens[2];
            Point p = points.get(throttlingPointName);
            if (p != null)
                p.clear(tenantId);
            points.get(throttlingPointName).sendUpdate();
            if (p.size() == 0)
                points.remove(throttlingPointName);
        } else if (cmd.equalsIgnoreCase("exit")) {
            return true;
        }

        return false;
    }

    private synchronized static void printStatus() {
        for (Point p : points.values())
            System.out.println(p);
    }

    private static class Point {
        private final String throttlingPointName;
        private final Map<Integer, Double> rates = new HashMap<Integer, Double>();

        private Point(String throttlingPointName) {
            this.throttlingPointName = throttlingPointName;
        }

        private void setRate(Integer tenantId, Double rate) {
            rates.put(tenantId, rate);
        }

        private void clear(Integer tenantId) {
            rates.remove(tenantId);
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            for (Integer tenantId : rates.keySet()) {
                b.append(throttlingPointName);
                b.append("\t");
                b.append(tenantId);
                b.append("\t");
                b.append(rates.get(tenantId));
                b.append("\n");
            }
            return b.toString();
        }

        private int size() {
            return rates.size();
        }

        private void sendUpdate() {
            ClusterThrottlingPoints.setThrottlingPointRates(throttlingPointName, rates);
        }
    }

}
