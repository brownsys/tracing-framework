package edu.brown.cs.systems.retro.throttling.throttlingpoints;

import edu.brown.cs.systems.resourcethrottling.ThrottlingMessages.ThrottlingPointSpecification;
import edu.brown.cs.systems.retro.Retro;
import edu.brown.cs.systems.retro.throttling.ThrottlingPoint;

/**
 * Only throttles once every n invocations to throttle for each thread
 */
public class BatchedThrottlingPoint implements ThrottlingPoint {

    final ThrottlingPoint wrapped;
    final int batchsize;

    private class Countdown {
        private int tenant = -1;
        private int countdown = 0;

        private boolean countdown() {
            int tenant = Retro.getTenant();
            if (this.tenant != tenant) {
                this.tenant = tenant;
                this.countdown = 0;
            }
            if (countdown == 0) {
                countdown = batchsize;
                return true;
            } else {
                countdown--;
                return false;
            }
        }
    }

    private ThreadLocal<Countdown> countdown = new ThreadLocal<Countdown>() {
        public Countdown initialValue() {
            return new Countdown();
        }
    };

    public BatchedThrottlingPoint(ThrottlingPoint wrapped, int batchsize) {
        if (batchsize < 0)
            batchsize = 1;
        this.wrapped = wrapped;
        this.batchsize = batchsize;
    }

    @Override
    public void throttle() {
        if (countdown.get().countdown()) {
            wrapped.throttle();
        }
    }

    @Override
    public void update(ThrottlingPointSpecification spec) {
        wrapped.update(spec);
    }

    @Override
    public void clearRates() {
        wrapped.clearRates();
    }

}
