package edu.brown.cs.systems.tracing;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;

/**
 * Provides static methods to access various useful information about the
 * current process
 */
public class Utils {

    private static Class<?> MainClass;
    private static String ProcessName;
    private static Integer ProcessID;
    private static String Host;

    public static Class<?> getMainClass() {
        if (MainClass == null) {
            Collection<StackTraceElement[]> stacks = Thread.getAllStackTraces().values();
            for (StackTraceElement[] currStack : stacks) {
                if (currStack.length == 0)
                    continue;
                StackTraceElement lastElem = currStack[currStack.length - 1];
                if (lastElem.getMethodName().equals("main")) {
                    try {
                        String mainClassName = lastElem.getClassName();
                        MainClass = Class.forName(mainClassName);
                    } catch (ClassNotFoundException e) {
                        // bad class name in line containing main?!
                        // shouldn't happen
                        e.printStackTrace();
                    }
                }
            }
        }
        return MainClass;
    }

    public static String getProcessName() {
        if (ProcessName == null) {
            Class<?> mainClass = getMainClass();
            if (mainClass == null)
                return "";
            else
                ProcessName = mainClass.getSimpleName();
        }
        return ProcessName;
    }

    public static int getProcessID() {
        if (ProcessID == null) {
            String procname = ManagementFactory.getRuntimeMXBean().getName();
            ProcessID = Integer.parseInt(procname.substring(0, procname.indexOf('@')));
        }
        return ProcessID;
    }

    public static String getHost() {
        if (Host == null) {
            try {
                Host = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                Host = "unknown";
            }
        }
        return Host;
    }
}