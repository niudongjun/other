package com.jsyn.ratelimiter;

import java.util.concurrent.TimeUnit;

/**
 * description:
 *
 * @author : niudongjun
 * @date : 2022/6/23 10:38
 */
public abstract class SmoothRateLimiter extends RateLimiter {
    double storedPermits;
    double maxPermits;
    double stableIntervalMicros;
    private long nextFreeTicketMicros;

    private SmoothRateLimiter(SleepingStopwatch stopwatch) {
        super(stopwatch);
        this.nextFreeTicketMicros = 0L;
    }

    @Override
    final void doSetRate(double permitsPerSecond, long nowMicros) {
        this.resync(nowMicros);
        double stableIntervalMicros = (double) TimeUnit.SECONDS.toMicros(1L) / permitsPerSecond;
        this.stableIntervalMicros = stableIntervalMicros;
        this.doSetRate(permitsPerSecond, stableIntervalMicros);
    }

    abstract void doSetRate(double permitsPerSecond, double stableIntervalMicros);

    @Override
    final double doGetRate() {
        return (double)TimeUnit.SECONDS.toMicros(1L) / this.stableIntervalMicros;
    }

    @Override
    final long queryEarliestAvailable(long nowMicros) {
        return this.nextFreeTicketMicros;
    }

    @Override
    final long reserveEarliestAvailable(int requiredPermits, long nowMicros) {
        this.resync(nowMicros);
        long returnValue = this.nextFreeTicketMicros;
        double storedPermitsToSpend = Math.min(requiredPermits, this.storedPermits);
        double freshPermits = (double)requiredPermits - storedPermitsToSpend;
        long waitMicros = this.storedPermitsToWaitTime(this.storedPermits, storedPermitsToSpend) + (long)(freshPermits * this.stableIntervalMicros);
        this.nextFreeTicketMicros += waitMicros;
        this.storedPermits -= storedPermitsToSpend;
        return returnValue;
    }

    abstract long storedPermitsToWaitTime(double var1, double var3);

    private void resync(long nowMicros) {
        if (nowMicros > this.nextFreeTicketMicros) {
            this.storedPermits = Math.min(this.maxPermits, this.storedPermits + (double)(nowMicros - this.nextFreeTicketMicros) / this.stableIntervalMicros);
            this.nextFreeTicketMicros = nowMicros;
        }

    }

    static final class SmoothBursty extends SmoothRateLimiter {
        final double maxBurstSeconds;

        SmoothBursty(SleepingStopwatch stopwatch, double maxBurstSeconds) {
            super(stopwatch);
            this.maxBurstSeconds = maxBurstSeconds;
        }

        @Override
        void doSetRate(double permitsPerSecond, double stableIntervalMicros) {
            double oldMaxPermits = this.maxPermits;
            this.maxPermits = this.maxBurstSeconds * permitsPerSecond;
            if (oldMaxPermits == 1.0D / 0.0) {
                this.storedPermits = this.maxPermits;
            } else {
                this.storedPermits = oldMaxPermits == 0.0D ? 0.0D : this.storedPermits * this.maxPermits / oldMaxPermits;
            }

        }

        @Override
        long storedPermitsToWaitTime(double storedPermits, double permitsToTake) {
            return 0L;
        }
    }

    static final class SmoothWarmingUp extends SmoothRateLimiter {
        private final long warmupPeriodMicros;
        private double slope;
        private double halfPermits;

        SmoothWarmingUp(SleepingStopwatch stopwatch, long warmupPeriod, TimeUnit timeUnit) {
            super(stopwatch);
            this.warmupPeriodMicros = timeUnit.toMicros(warmupPeriod);
        }

        @Override
        void doSetRate(double permitsPerSecond, double stableIntervalMicros) {
            double oldMaxPermits = this.maxPermits;
            this.maxPermits = (double)this.warmupPeriodMicros / stableIntervalMicros;
            this.halfPermits = this.maxPermits / 2.0D;
            double coldIntervalMicros = stableIntervalMicros * 3.0D;
            this.slope = (coldIntervalMicros - stableIntervalMicros) / this.halfPermits;
            if (oldMaxPermits == 1.0D / 0.0) {
                this.storedPermits = 0.0D;
            } else {
                this.storedPermits = oldMaxPermits == 0.0D ? this.maxPermits : this.storedPermits * this.maxPermits / oldMaxPermits;
            }

        }

        @Override
        long storedPermitsToWaitTime(double storedPermits, double permitsToTake) {
            double availablePermitsAboveHalf = storedPermits - this.halfPermits;
            long micros = 0L;
            if (availablePermitsAboveHalf > 0.0D) {
                double permitsAboveHalfToTake = Math.min(availablePermitsAboveHalf, permitsToTake);
                micros = (long)(permitsAboveHalfToTake * (this.permitsToTime(availablePermitsAboveHalf) + this.permitsToTime(availablePermitsAboveHalf - permitsAboveHalfToTake)) / 2.0D);
                permitsToTake -= permitsAboveHalfToTake;
            }

            micros = (long)((double)micros + this.stableIntervalMicros * permitsToTake);
            return micros;
        }

        private double permitsToTime(double permits) {
            return this.stableIntervalMicros + permits * this.slope;
        }
    }
}

