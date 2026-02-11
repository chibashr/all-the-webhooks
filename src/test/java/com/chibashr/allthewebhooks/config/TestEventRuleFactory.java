package com.chibashr.allthewebhooks.config;

import java.util.Map;

/**
 * Test-only factory for creating {@link EventRule} instances.
 * Lives in the same package so the package-private EventRule constructor is visible.
 */
public final class TestEventRuleFactory {

    private TestEventRuleFactory() {
    }

    public static EventRule create(
            Boolean enabled,
            String webhook,
            String webhookUsername,
            String message,
            String requirePermission,
            Map<String, Object> conditions,
            Integer rateLimitEventsPerSecond
    ) {
        return new EventRule(
                enabled,
                webhook,
                webhookUsername,
                message,
                requirePermission,
                conditions == null ? Map.of() : conditions,
                rateLimitEventsPerSecond
        );
    }

    public static EventRule create(String message, String webhook) {
        return create(true, webhook, null, message, null, Map.of(), null);
    }
}
