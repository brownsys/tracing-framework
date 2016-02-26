package edu.brown.cs.systems.retro.throttling.cli;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import edu.brown.cs.systems.baggage.Baggage;
import edu.brown.cs.systems.retro.Retro;
import edu.brown.cs.systems.retro.throttling.LocalThrottlingPoints;
import edu.brown.cs.systems.retro.throttling.ThrottlingPoint;
import edu.brown.cs.systems.retro.throttling.ThrottlingQueue;

public class ThrottlingClientCLI {

    /** A simple CLI for testing */
    public static void main(String[] args) {
        Run();
    }

    private static void PrintHelp() {
        StringBuffer sb = new StringBuffer();
        sb.append("usage:\n");
        sb.append("  - start <tenantid> <throttlingpointname>\n");
        sb.append("  - stop <tenantid> <throttlingpointname>\n");
        sb.append("  - startqueue <tenantid> <queueid> <num-concurrently-enqueued>\n");
        sb.append("  - stopqueue <tenantid> <queueid>\n");
        sb.append("  - status (prints the tenants currently consuming)\n");
        sb.append("  - reportInterval <interval_ms> (prints the tenants currently consuming at the specified rate)\n");
        sb.append("  - help (prints this status message)\n");
        sb.append("  - exit\n");
        System.out.println(sb.toString());
    }

    private static void Run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        PrintHelp();
        new RateUpdater().start();
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

    private static volatile Printer p = null;
    private static final Map<String, Consumer> consumers = new HashMap<String, Consumer>();
    private static final Map<String, QueueConsumer> queues = new HashMap<String, QueueConsumer>();

    public synchronized static boolean ParseAndExecuteCommand(String line) throws FileNotFoundException {
        String[] tokens = line.split(" ");

        if (tokens.length == 0)
            return false;

        String cmd = tokens[0].trim();

        if (cmd.equalsIgnoreCase("help")) {
            PrintHelp();
        } else if (cmd.equalsIgnoreCase("status")) {
            printStatus();
        } else if (cmd.equalsIgnoreCase("start")) {
            int tenantId = Integer.parseInt(tokens[1]);
            String throttlingPointName = tokens[2];
            String key = throttlingPointName + "-" + tenantId;
            if (!consumers.containsKey(key))
                consumers.put(key, new Consumer(tenantId, throttlingPointName));
        } else if (cmd.equalsIgnoreCase("stop")) {
            int tenantId = Integer.parseInt(tokens[1]);
            String throttlingPointName = tokens[2];
            String key = throttlingPointName + "-" + tenantId;
            Consumer c = consumers.remove(key);
            if (c != null)
                c.interrupt();
        } else if (cmd.equalsIgnoreCase("startqueue")) {
            int tenantId = Integer.parseInt(tokens[1]);
            String queueid = tokens[2];
            int numqueued = Integer.parseInt(tokens[3]);
            if (!queues.containsKey(queueid))
                queues.put(queueid, new QueueConsumer(queueid));
            queues.get(queueid).addTenant(tenantId, numqueued);
        } else if (cmd.equalsIgnoreCase("stopqueue")) {
            int tenantId = Integer.parseInt(tokens[1]);
            String queueid = tokens[2];
            if (queues.containsKey(queueid)) {
                if (queues.get(queueid).removeTenant(tenantId)) {
                    queues.remove(queueid).interrupt();
                }
            }
        } else if (cmd.equalsIgnoreCase("reportInterval")) {
            long interval = Long.parseLong(tokens[1]);
            if (p != null)
                p.interrupt();
            p = new Printer(interval);
        } else if (cmd.equalsIgnoreCase("exit")) {
            return true;
        }

        return false;
    }

    private synchronized static void printStatus() {
        for (Consumer c : consumers.values())
            System.out.println(c);
        for (QueueConsumer q : queues.values())
            System.out.println(q);
    }

    private synchronized static void updateRates() {
        for (Consumer c : consumers.values())
            c.updateRate();
        for (QueueConsumer q : queues.values())
            q.updateRate();
    }

    private static class RateUpdater extends Thread {
        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(1000);
                    updateRates();
                }
            } catch (Exception e) {
                // do nothing
            }
        }
    }

    private static class Printer extends Thread {

        private long interval;

        public Printer(long interval) {
            this.interval = interval;
            this.start();
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(interval);
                    printStatus();
                }
            } catch (Exception e) {
                // do nothing
            }
        }
    }

    private static class QueueConsumer extends Thread {

        private final String queueid;
        private final ThrottlingQueue<Object> queue;

        public QueueConsumer(String queueid) {
            this.queueid = queueid;
            this.queue = LocalThrottlingPoints.getThrottlingQueue(queueid);
            this.start();
        }

        private Collection<Integer> activeTenants = new HashSet<Integer>();
        private Map<Integer, Long> consumedByTenant = new HashMap<Integer, Long>();
        private Map<Integer, Double> tenantRates = new HashMap<Integer, Double>();
        private long lastUpdate = System.currentTimeMillis();

        public void addTenant(int tenantId, int numtoadd) {
            synchronized (activeTenants) {
                if (!activeTenants.contains(tenantId)) {
                    activeTenants.add(tenantId);
                    Retro.setTenant(tenantId);
                    for (int i = 0; i < numtoadd; i++) {
                        queue.add(new Object());
                    }
                    Baggage.discard();
                }
            }
        }

        public boolean removeTenant(int tenantId) {
            synchronized (activeTenants) {
                activeTenants.remove(tenantId);
                return activeTenants.isEmpty();
            }
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Baggage.start();
                    queue.poll();
                    int tenantId = Retro.getTenant();
                    synchronized (consumedByTenant) {
                        if (!consumedByTenant.containsKey(tenantId))
                            consumedByTenant.put(tenantId, 1L);
                        else
                            consumedByTenant.put(tenantId, consumedByTenant.get(tenantId) + 1);
                    }
                    synchronized (activeTenants) {
                        if (activeTenants.contains(tenantId)) {
                            queue.add(new Object());
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Queue " + queue + " terminated");
            }
        }

        public synchronized void updateRate() {
            long currentTime = System.currentTimeMillis();
            long duration = currentTime - lastUpdate;
            lastUpdate = currentTime;

            tenantRates.clear();
            synchronized (consumedByTenant) {
                for (int tenantId : consumedByTenant.keySet()) {
                    tenantRates.put(tenantId, consumedByTenant.get(tenantId) * 1000 / (double) duration);
                }
                consumedByTenant.clear();
            }
        }

        public String tenantToString(int tenantId) {
            return String.format("Tenant=%d DequeueRate=%.2f", tenantId, tenantRates.get(tenantId));
        }

        @Override
        public synchronized String toString() {
            StringBuilder b = new StringBuilder();
            b.append("Queue=");
            b.append(queueid);
            b.append('\n');
            for (Integer tenantId : tenantRates.keySet()) {
                b.append('\t');
                b.append(tenantToString(tenantId));
                b.append('\n');
            }
            return b.toString();
        }
    }

    private static class Consumer extends Thread {

        private final int tenantId;
        private final String throttlingPointName;
        private final ThrottlingPoint throttlingPoint;

        public Consumer(int tenantId, String throttlingPointName) {
            this.tenantId = tenantId;
            this.throttlingPointName = throttlingPointName;
            this.throttlingPoint = LocalThrottlingPoints.getThrottlingPoint(throttlingPointName);
            this.start();
        }

        private volatile double rate = 0.0;
        private long lastUpdate = System.currentTimeMillis();
        private AtomicLong consumed = new AtomicLong(0);

        @Override
        public void run() {
            Retro.setTenant(tenantId);
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    throttlingPoint.throttle();
                    consumed.getAndIncrement();
                }
            } catch (Exception e) {
                System.out.println("Consumer: " + tenantId + " " + throttlingPoint + " terminated.");
            }
        }

        public void updateRate() {
            long currentTime = System.currentTimeMillis();
            long duration = currentTime - lastUpdate;
            lastUpdate = currentTime;
            rate = consumed.getAndSet(0) * 1000 / (double) duration;
        }

        @Override
        public String toString() {
            return String.format("Tenant=%d Point=%s Rate=%.2f", tenantId, throttlingPointName, rate);
        }
    }

}
