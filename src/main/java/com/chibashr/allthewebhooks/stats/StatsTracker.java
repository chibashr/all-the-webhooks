package com.chibashr.allthewebhooks.stats;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class StatsTracker {
    private final LongAdder sent = new LongAdder();
    private final LongAdder dropped = new LongAdder();
    private final LongAdder webhookFailures = new LongAdder();
    private final LongAdder rateLimited = new LongAdder();
    private final Map<String, LongAdder> perEventSent = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> perEventDropped = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> perEventFailures = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> perEventRateLimited = new ConcurrentHashMap<>();

    public void incrementSent(String eventKey) {
        sent.increment();
        perEventSent.computeIfAbsent(eventKey, key -> new LongAdder()).increment();
    }

    public void incrementDropped(String eventKey) {
        dropped.increment();
        perEventDropped.computeIfAbsent(eventKey, key -> new LongAdder()).increment();
    }

    public void incrementWebhookFailure(String eventKey) {
        webhookFailures.increment();
        perEventFailures.computeIfAbsent(eventKey, key -> new LongAdder()).increment();
    }

    public void incrementRateLimited(String eventKey) {
        rateLimited.increment();
        perEventRateLimited.computeIfAbsent(eventKey, key -> new LongAdder()).increment();
    }

    public long getSent() {
        return sent.sum();
    }

    public long getDropped() {
        return dropped.sum();
    }

    public long getWebhookFailures() {
        return webhookFailures.sum();
    }

    public long getRateLimited() {
        return rateLimited.sum();
    }

    public Map<String, LongAdder> getPerEventSent() {
        return Collections.unmodifiableMap(perEventSent);
    }

    public Map<String, LongAdder> getPerEventDropped() {
        return Collections.unmodifiableMap(perEventDropped);
    }

    public Map<String, LongAdder> getPerEventFailures() {
        return Collections.unmodifiableMap(perEventFailures);
    }

    public Map<String, LongAdder> getPerEventRateLimited() {
        return Collections.unmodifiableMap(perEventRateLimited);
    }
}
