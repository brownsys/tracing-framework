package edu.brown.cs.systems.retro.resources;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

import org.aspectj.lang.JoinPoint;

import edu.brown.cs.systems.retro.Retro;
import edu.brown.cs.systems.retro.aggregation.LocalResources;
import edu.brown.cs.systems.retro.aggregation.Resource.Operation;
import edu.brown.cs.systems.retro.aggregation.aggregators.NetworkAggregator;
import edu.brown.cs.systems.xtrace.XTrace;
import edu.brown.cs.systems.xtrace.logging.XTraceLogger;

public enum Network {
    Connect(Operation.CONNECT, "netconnect", LocalResources.getNetworkUplinkAggregator()), Read(Operation.READ, "netread", LocalResources
            .getNetworkDownlinkAggregator()), Write(Operation.WRITE, "netwrite", LocalResources.getNetworkUplinkAggregator()), Flush(Operation.FLUSH,
            "netflush", LocalResources.getNetworkUplinkAggregator()), Close(Operation.CLOSE, "netclose", LocalResources.getNetworkUplinkAggregator()), LoopbackRead(
            Operation.READ, "loopback-read", LocalResources.getLoopbackDownlinkAggregator()), LoopbackWrite(Operation.WRITE, "loopback-write", LocalResources
            .getLoopbackUplinkAggregator());

    public static boolean isLoopback(InetAddress addr) {
        if (addr == null)
            return false;

        // Check if the address is a valid special local or loop back
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress())
            return true;

        // Check if the address is defined on any interface
        try {
            return NetworkInterface.getByInetAddress(addr) != null;
        } catch (SocketException e) {
            return false;
        }
    }

    public static boolean isLoopback(Socket socket) {
        return isLoopback(socket.getInetAddress());
    }

    public static boolean isLoopback(SocketChannel ch) {
        return isLoopback(ch.socket());
    }

    public static Network Write(InetAddress addr) {
        if (isLoopback(addr))
            return LoopbackWrite;
        else
            return Write;
    }

    public static Network Write(SocketChannel socket) {
        if (isLoopback(socket))
            return LoopbackWrite;
        else
            return Write;
    }

    public static Network Read(SocketChannel socket) {
        if (isLoopback(socket))
            return LoopbackRead;
        else
            return Read;
    }

    private static final XTraceLogger xtrace = XTrace.getLogger("Network");

    private final Operation op;
    private final String opname;
    private final NetworkAggregator aggregator;

    private Network(Operation op, String opname, NetworkAggregator aggregator) {
        this.op = op;
        this.opname = opname;
        this.aggregator = aggregator;
    }

    private static final ThreadLocal<Boolean> ignoring = new ThreadLocal<Boolean>() {
        @Override
        public Boolean initialValue() {
            return false;
        }
    };

    public static void ignore(boolean ignore) {
        ignoring.set(ignore);
    }

    public static boolean ignoring() {
        return ignoring.get();
    }

    private final ThreadLocal<Long> opstart = new ThreadLocal<Long>() {
        @Override
        public Long initialValue() {
            return 0L;
        }
    };

    public void starting(Object connection, JoinPoint.StaticPart jp) {
        if (!ignoring() && aggregator.enabled())
            aggregator.starting(op, Retro.getTenant());
        CPUTracking.pauseTracking();
        opstart.set(System.nanoTime());
    }

    public void finished(Object connection, JoinPoint.StaticPart jp) {
        finished(connection, 0, jp);
    }

    public void finished(Object connection, long bytes, JoinPoint.StaticPart jp) {
        CPUTracking.continueTracking();
        long latency = System.nanoTime() - opstart.get();
        if (!ignoring() && aggregator.enabled())
            aggregator.finished(op, Retro.getTenant(), bytes, latency);
        if (xtrace.valid())
            if (bytes > 0)
                xtrace.log(jp, opname, "Operation", opname, "Duration", latency, "Bytes", bytes, "Connection", connection);
            else
                xtrace.log(jp, opname, "Operation", opname, "Duration", latency, "Connection", connection);
    }

    public void alreadyStarted(Object connection) {
        if (aggregator.enabled())
            aggregator.starting(op, Retro.getTenant());
    }

    public void alreadyFinished(Object connection, long bytes) {
        if (aggregator.enabled())
            aggregator.finished(op, Retro.getTenant(), bytes, 0);
        if (xtrace.valid())
            if (bytes > 0)
                xtrace.log(opname, "Operation", opname, "Duration", 0, "Bytes", bytes, "Connection", connection);
            else
                xtrace.log(opname, "Operation", opname, "Duration", 0, "Connection", connection);
    }

    public static void startingDiskTransfer(DiskResource diskop, JoinPoint.StaticPart jp) {
        if (diskop == DiskResource.TransferFromNetwork && Read.aggregator.enabled())
            Read.aggregator.starting(Read.op, Retro.getTenant());
        else if (diskop == DiskResource.TransferToNetwork && Write.aggregator.enabled())
            Write.aggregator.starting(Write.op, Retro.getTenant());
        else if (diskop == DiskResource.TransferFromLoopback && LoopbackRead.aggregator.enabled())
            LoopbackRead.aggregator.starting(Read.op, Retro.getTenant());
        else if (diskop == DiskResource.TransferToLoopback && LoopbackWrite.aggregator.enabled())
            LoopbackWrite.aggregator.starting(Write.op, Retro.getTenant());

    }

    public static void finishedDiskTransfer(DiskResource diskop, JoinPoint.StaticPart jp, long latency, long bytes) {
        if (diskop == DiskResource.TransferFromNetwork && Read.aggregator.enabled())
            Read.aggregator.finished(Read.op, Retro.getTenant(), bytes, latency);
        else if (diskop == DiskResource.TransferToNetwork && Write.aggregator.enabled())
            Write.aggregator.finished(Write.op, Retro.getTenant(), bytes, latency);
        else if (diskop == DiskResource.TransferFromNetwork && LoopbackRead.aggregator.enabled())
            LoopbackRead.aggregator.finished(Read.op, Retro.getTenant(), bytes, latency);
        else if (diskop == DiskResource.TransferToNetwork && LoopbackWrite.aggregator.enabled())
            LoopbackWrite.aggregator.finished(Write.op, Retro.getTenant(), bytes, latency);

        if (diskop == DiskResource.TransferFromNetwork && xtrace.valid())
            xtrace.log(jp, Read.opname, "Operation", Read.opname, "Duration", latency, "Bytes", bytes);
        else if (diskop == DiskResource.TransferToNetwork && xtrace.valid())
            xtrace.log(jp, Write.opname, "Operation", Write.opname, "Duration", latency, "Bytes", bytes);
    }
}
