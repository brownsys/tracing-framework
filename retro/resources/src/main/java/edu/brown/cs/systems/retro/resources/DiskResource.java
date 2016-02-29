package edu.brown.cs.systems.retro.resources;

import org.aspectj.lang.JoinPoint;

import edu.brown.cs.systems.retro.Retro;
import edu.brown.cs.systems.retro.aggregation.LocalResources;
import edu.brown.cs.systems.retro.aggregation.Resource.Operation;
import edu.brown.cs.systems.retro.aggregation.aggregators.DiskAggregator;
import edu.brown.cs.systems.retro.throttling.RetroSchedulers;
import edu.brown.cs.systems.retro.throttling.Scheduler;
import edu.brown.cs.systems.xtrace.XTrace;
import edu.brown.cs.systems.xtrace.logging.XTraceLogger;

public enum DiskResource {
    Open(Operation.OPEN, "fileopen"), Read(Operation.READ, "fileread"), Write(Operation.WRITE, "filewrite"), Close(Operation.CLOSE, "fileclose"), Flush(
            Operation.FLUSH, "fileflush"), Sync(Operation.SYNC, "filesync"), FileOp(Operation.METADATA, "fileop"), TransferToNetwork(Operation.READ, "fileread"), TransferFromNetwork(
            Operation.WRITE, "filewrite"), TransferToLoopback(Operation.READ, "fileread"), TransferFromLoopback(Operation.WRITE, "filewrite");

    private static final Scheduler scheduler = RetroSchedulers.get("disk");
    private static DiskAggregator aggregator = LocalResources.getDiskAggregator();
    private static XTraceLogger xtrace = XTrace.getLogger("Disk");

    private final Operation optype;
    private final String opname;

    private DiskResource(Operation optype, String opname) {
        this.optype = optype;
        this.opname = opname;
    }

    private final ThreadLocal<Long> opstart = new ThreadLocal<Long>() {
        @Override
        public Long initialValue() {
            return 0L;
        }
    };

    public void starting(Object file, JoinPoint.StaticPart jp) {
        this.starting(file, 0, jp);
    }

    public void starting(Object file, JoinPoint.StaticPart jp, boolean schedule) {
        this.starting(file, 0, jp, schedule);
    }

    public void starting(Object file, long bytesEstimate, JoinPoint.StaticPart jp) {
        starting(file, bytesEstimate, jp, false);
    }

    public void starting(Object file, long bytesEstimate, JoinPoint.StaticPart jp, boolean schedule) {
        if (this == TransferFromNetwork || this == TransferToNetwork || this == TransferFromLoopback || this == TransferToLoopback)
            Network.startingDiskTransfer(this, jp);
        if (optype != null && aggregator.enabled())
            aggregator.starting(optype, Retro.getTenant());
        CPUTracking.pauseTracking();
        if (schedule)
            schedule(bytesEstimate);
        opstart.set(System.nanoTime());
    }

    public void finished(Object file, JoinPoint.StaticPart jp) {
        this.finished(file, 0, jp);
    }

    public void finished(Object file, JoinPoint.StaticPart jp, boolean schedule) {
        this.finished(file, 0, jp, schedule);
    }

    public void finished(Object file, long bytes, JoinPoint.StaticPart jp) {
        finished(file, bytes, jp, false);
    }

    public void finished(Object file, long bytes, JoinPoint.StaticPart jp, boolean schedule) {
        CPUTracking.continueTracking();
        if (schedule)
            complete();
        long latency = System.nanoTime() - opstart.get();
        if (optype != null && aggregator.enabled())
            aggregator.finished(optype, Retro.getTenant(), bytes, latency);
        if (opname != null && xtrace.valid())
            if (bytes > 0)
                xtrace.log(jp, opname, "Operation", opname, "Duration", latency, "Bytes", bytes, "File", file);
            else
                xtrace.log(jp, opname, "Operation", opname, "Duration", latency, "File", file);
        if (this == TransferFromNetwork || this == TransferToNetwork || this == TransferFromLoopback || this == TransferToLoopback)
            Network.finishedDiskTransfer(this, jp, latency, bytes);
    }

    private void schedule(long bytesEstimate) {
        scheduler.schedule(Math.max(1, bytesEstimate));
    }

    private void complete() {
        scheduler.complete();
    }
}