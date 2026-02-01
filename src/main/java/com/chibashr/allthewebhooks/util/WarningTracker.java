package com.chibashr.allthewebhooks.util;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class WarningTracker {
    private final Logger logger;
    private final Set<String> emitted = ConcurrentHashMap.newKeySet();

    public WarningTracker(Logger logger) {
        this.logger = logger;
    }

    public void warnOnce(String key, String message) {
        if (key == null || message == null) {
            return;
        }
        if (emitted.add(key)) {
            logger.warning(message);
        }
    }
}
