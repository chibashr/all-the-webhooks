package com.chibashr.allthewebhooks.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.bukkit.configuration.ConfigurationSection;

public class EventRule {

    /** Operator keys used in conditions; dotted condition keys are those whose value map contains these. */
    private static final Set<String> OPERATOR_KEYS = Set.of(
            "equals", "not", "greater-than", "less-than",
            "greater-than-or-equal", "less-than-or-equal"
    );
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
            collectConditionsWithDottedKeys(conditionSection, conditions);
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

    /**
     * Collects condition keys including dotted paths (e.g. world.environment).
     * Bukkit's getKeys(false) only returns the first path segment, so we use getKeys(true)
     * and only add keys whose value is an operator map or a short-form primitive.
     */
    private static void collectConditionsWithDottedKeys(ConfigurationSection conditionSection, Map<String, Object> conditions) {
        Set<String> allKeys = new TreeSet<>((a, b) -> {
            int cmp = Integer.compare(a.length(), b.length());
            return cmp != 0 ? cmp : a.compareTo(b);
        });
        allKeys.addAll(conditionSection.getKeys(true));

        for (String key : allKeys) {
            Object value = conditionSection.get(key);
            if (value instanceof ConfigurationSection subsection) {
                boolean hasOperator = false;
                for (String subKey : subsection.getKeys(false)) {
                    if (OPERATOR_KEYS.contains(subKey)) {
                        hasOperator = true;
                        break;
                    }
                }
                if (hasOperator) {
                    conditions.put(key, subsection.getValues(false));
                }
            } else {
                if (value != null) {
                    boolean parentAlreadyAdded = false;
                    for (String existing : conditions.keySet()) {
                        if (key.startsWith(existing + ".")) {
                            parentAlreadyAdded = true;
                            break;
                        }
                    }
                    if (!parentAlreadyAdded) {
                        conditions.put(key, value);
                    }
                }
            }
        }
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
