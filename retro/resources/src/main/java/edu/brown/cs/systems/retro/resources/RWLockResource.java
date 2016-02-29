package edu.brown.cs.systems.retro.resources;

import org.aspectj.lang.JoinPoint;

import edu.brown.cs.systems.retro.Retro;
import edu.brown.cs.systems.retro.aggregation.LocalResources;
import edu.brown.cs.systems.retro.aggregation.Resource.Operation;
import edu.brown.cs.systems.retro.aggregation.aggregators.LockAggregator;
import edu.brown.cs.systems.xtrace.XTrace;
import edu.brown.cs.systems.xtrace.logging.XTraceLogger;

public class RWLockResource {

    private static XTraceLogger xtrace = XTrace.getLogger("ReadWriteLock");

    private final String lockid;
    private final LockAggregator aggregator;

    public final Lock Read = new Lock(Operation.READ, "readlockrequest", "readlockacquire", "readlockrelease");
    public final Lock Write = new Lock(Operation.WRITE, "writelockrequest", "writelockacquire", "writelockrelease");
    public final Utilization SharedRead = new Utilization(Operation.WRITE);

    public RWLockResource(String lockid) {
        this.lockid = lockid;
        this.aggregator = LocalResources.getLockAggregator(lockid);
    }

    public class Lock {
        private final String rel;
        private final String acq;
        private final String req;
        private final Operation op;

        private Lock(Operation op, String req, String acq, String rel) {
            this.op = op;
            this.req = req;
            this.acq = acq;
            this.rel = rel;
        }

        public void request(long request_hrt, JoinPoint.StaticPart jp) {
            if (xtrace.valid())
                xtrace.log(jp, req, "Operation", req, "LockID", lockid, "LockRequest", request_hrt);
            if (aggregator.enabled())
                aggregator.starting(op, Retro.getTenant());
            CPUTracking.pauseTracking();
        }

        public void acquire(long request_hrt, long acquire_hrt, JoinPoint.StaticPart jp) {
            CPUTracking.continueTracking();
            if (xtrace.valid())
                xtrace.log(jp, acq, "Operation", acq, "LockID", lockid, "LockRequest", request_hrt, "LockAcquire", acquire_hrt);
        }

        public void release(long request_hrt, long acquire_hrt, long release_hrt, JoinPoint.StaticPart jp) {
            if (aggregator.enabled())
                aggregator.finished(op, Retro.getTenant(), release_hrt - acquire_hrt, release_hrt - request_hrt);
            if (xtrace.valid())
                xtrace.log(jp, rel, "Operation", rel, "LockID", lockid, "LockRequest", request_hrt, "LockAcquire", acquire_hrt);
        }
    }

    public class Utilization {
        private final int tenantClass;
        private final Operation op;

        private Utilization(Operation op) {
            this.op = op;
            this.tenantClass = -2;
        }

        public void acquire() {
            if (aggregator.enabled())
                aggregator.starting(op, tenantClass);
        }

        public void release(long acquire_hrt, long release_hrt) {
            if (aggregator.enabled())
                aggregator.finished(op, tenantClass, release_hrt - acquire_hrt, release_hrt - acquire_hrt);
        }
    }

}
