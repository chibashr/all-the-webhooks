package com.chibashr.allthewebhooks.webhook;

public class RateLimiter {
    private volatile int maxPerSecond;
    private long windowStart;
    private int count;

    public RateLimiter(int maxPerSecond) {
        this.maxPerSecond = Math.max(1, maxPerSecond);
    }

    public synchronized void setLimit(int maxPerSecond) {
        this.maxPerSecond = Math.max(1, maxPerSecond);
    }

    public synchronized boolean tryAcquire() {
        long now = System.currentTimeMillis() / 1000;
        if (windowStart != now) {
            windowStart = now;
            count = 0;
        }
        if (count >= maxPerSecond) {
            return false;
        }
        count++;
        return true;
    }
}
