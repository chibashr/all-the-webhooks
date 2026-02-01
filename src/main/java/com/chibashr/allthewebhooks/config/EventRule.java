package com.chibashr.allthewebhooks.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

public class EventRule {
    private final Boolean enabled;
    private final String webhook;
    private final String message;
    private final String requirePermission;
    private final Map<String, Object> conditions;
    private final Integer rateLimitEventsPerSecond;

    EventRule(
            Boolean enabled,
            String webhook,
            String message,
            String requirePermission,
            Map<String, Object> conditions,
            Integer rateLimitEventsPerSecond
    ) {
        this.enabled = enabled;
        this.webhook = webhook;
        this.message = message;
        this.requirePermission = requirePermission;
        this.conditions = conditions == null ? Map.of() : Collections.unmodifiableMap(conditions);
        this.rateLimitEventsPerSecond = rateLimitEventsPerSecond;
    }

    public static EventRule fromSection(ConfigurationSection section) {
        if (section == null) {
            return new EventRule(null, null, null, null, Map.of(), null);
        }
        Map<String, Object> conditions = new HashMap<>();
        ConfigurationSection conditionSection = section.getConfigurationSection("conditions");
        if (conditionSection != null) {
            for (String key : conditionSection.getKeys(false)) {
                conditions.put(key, conditionSection.get(key));
            }
        }

        Integer rateLimit = null;
        ConfigurationSection rateLimitSection = section.getConfigurationSection("rate-limit");
        if (rateLimitSection != null && rateLimitSection.contains("events-per-second")) {
            rateLimit = rateLimitSection.getInt("events-per-second");
        }

        return new EventRule(
                section.contains("enabled") ? section.getBoolean("enabled") : null,
                section.getString("webhook", null),
                section.getString("message", null),
                section.getString("require-permission", null),
                conditions,
                rateLimit
        );
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public String getWebhook() {
        return webhook;
    }

    public String getMessage() {
        return message;
    }

    public String getRequirePermission() {
        return requirePermission;
    }

    public Map<String, Object> getConditions() {
        return conditions;
    }

    public Integer getRateLimitEventsPerSecond() {
        return rateLimitEventsPerSecond;
    }
}
