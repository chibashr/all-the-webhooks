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
            for (String key : eventsSection.getKeys(false)) {
                ConfigurationSection ruleSection = eventsSection.getConfigurationSection(key);
                eventRules.put(key, EventRule.fromSection(ruleSection));
            }
        }
        return new WorldEventConfig(enabled, eventRules);
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Map<String, EventRule> getEventRules() {
        return eventRules;
    }
}
