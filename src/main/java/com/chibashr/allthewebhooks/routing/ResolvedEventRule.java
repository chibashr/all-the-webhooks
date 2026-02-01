package com.chibashr.allthewebhooks.routing;

import java.util.Collections;
import java.util.Map;

public class ResolvedEventRule {
    private final String eventKey;
    private final String matchedKey;
    private final boolean enabled;
    private final String webhook;
    private final String message;
    private final String permission;
    private final Map<String, Object> conditions;
    private final Integer rateLimitEventsPerSecond;

    public ResolvedEventRule(
            String eventKey,
            String matchedKey,
            boolean enabled,
            String webhook,
            String message,
            String permission,
            Map<String, Object> conditions,
            Integer rateLimitEventsPerSecond
    ) {
        this.eventKey = eventKey;
        this.matchedKey = matchedKey;
        this.enabled = enabled;
        this.webhook = webhook;
        this.message = message;
        this.permission = permission;
        this.conditions = conditions == null ? Map.of() : Collections.unmodifiableMap(conditions);
        this.rateLimitEventsPerSecond = rateLimitEventsPerSecond;
    }

    public static ResolvedEventRule disabled(String eventKey) {
        return new ResolvedEventRule(eventKey, null, false, null, null, null, Map.of(), null);
    }

    public String getEventKey() {
        return eventKey;
    }

    public String getMatchedKey() {
        return matchedKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getWebhook() {
        return webhook;
    }

    public String getMessage() {
        return message;
    }

    public String getPermission() {
        return permission;
    }

    public Map<String, Object> getConditions() {
        return conditions;
    }

    public Integer getRateLimitEventsPerSecond() {
        return rateLimitEventsPerSecond;
    }
}
