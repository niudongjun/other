package com.jsyn.ratelimiter;

import java.io.Serializable;

/**
 * description: 令牌储藏对象
 *
 * @author : niudongjun
 * @date : 2022/6/23 10:28
 */
public class PermitsStorage implements Serializable {
    private static final long serialVersionUID = -5455103497450752741L;

    /**
     * 已储存的令牌数
     */
    private double storedPermits;

    /**
     * 最大令牌数
     */
    private double maxPermits;

    /**
     * 稳定间歇毫秒数
     */
    private double stableIntervalMicros;

    /**
     * 下一次刷新令牌数时间戳
     */
    private long nextFreeTicketMicros;

    /**
     * 最大突然流量时间长度
     */
    private double maxBurstSeconds;

    /**
     * 预热时间段毫秒数
     */
    private long warmupPeriodMicros;

    public double getStoredPermits() {
        return storedPermits;
    }

    public void setStoredPermits(double storedPermits) {
        this.storedPermits = storedPermits;
    }

    public double getMaxPermits() {
        return maxPermits;
    }

    public void setMaxPermits(double maxPermits) {
        this.maxPermits = maxPermits;
    }

    public double getStableIntervalMicros() {
        return stableIntervalMicros;
    }

    public void setStableIntervalMicros(double stableIntervalMicros) {
        this.stableIntervalMicros = stableIntervalMicros;
    }

    public long getNextFreeTicketMicros() {
        return nextFreeTicketMicros;
    }

    public void setNextFreeTicketMicros(long nextFreeTicketMicros) {
        this.nextFreeTicketMicros = nextFreeTicketMicros;
    }

    public double getMaxBurstSeconds() {
        return maxBurstSeconds;
    }

    public void setMaxBurstSeconds(double maxBurstSeconds) {
        this.maxBurstSeconds = maxBurstSeconds;
    }

    public long getWarmupPeriodMicros() {
        return warmupPeriodMicros;
    }

    public void setWarmupPeriodMicros(long warmupPeriodMicros) {
        this.warmupPeriodMicros = warmupPeriodMicros;
    }
}
