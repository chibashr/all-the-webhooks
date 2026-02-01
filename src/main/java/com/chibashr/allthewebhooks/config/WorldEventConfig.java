package com.chibashr.allthewebhooks.config;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

public class WorldEventConfig {
    private final Boolean enabled;
    private final Map<String, EventRule> eventRules;

    private WorldEventConfig(Boolean enabled, Map<String, EventRule> eventRules) {
        this.enabled = enabled;
        this.eventRules = eventRules;
    }

    public static WorldEventConfig fromSection(ConfigurationSection section) {
        Boolean enabled = section.contains("enabled") ? section.getBoolean("enabled") : null;
        Map<String, EventRule> eventRules = new HashMap<>();
        ConfigurationSection eventsSection = section.getConfigurationSection("events");
        if (eventsSection != null) {
            collectEventRules(eventsSection, "", eventRules);
        }
        return new WorldEventConfig(enabled, eventRules);
    }

    private static void collectEventRules(
            ConfigurationSection section,
            String prefix,
            Map<String, EventRule> eventRules
    ) {
        for (String key : section.getKeys(false)) {
            ConfigurationSection ruleSection = section.getConfigurationSection(key);
            if (ruleSection == null) {
                continue;
            }
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            if (hasRuleFields(ruleSection)) {
                eventRules.put(path, EventRule.fromSection(ruleSection));
            }
            if (!ruleSection.getKeys(false).isEmpty()) {
                collectEventRules(ruleSection, path, eventRules);
            }
        }
    }

    private static boolean hasRuleFields(ConfigurationSection section) {
        return section.contains("enabled")
                || section.contains("webhook")
                || section.contains("message")
                || section.contains("require-permission")
                || section.contains("conditions")
                || section.contains("rate-limit");
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Map<String, EventRule> getEventRules() {
        return eventRules;
    }
}
