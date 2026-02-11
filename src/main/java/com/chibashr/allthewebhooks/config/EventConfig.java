package com.chibashr.allthewebhooks.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EventConfig {
    private final EventRuleDefaults defaults;
    private final Map<String, EventRule> eventRules = new HashMap<>();
    private final Map<String, WorldEventConfig> worldConfigs = new HashMap<>();

    public EventConfig(EventRuleDefaults defaults) {
        this.defaults = defaults;
    }

    public void putEventRule(String key, EventRule rule) {
        if (key == null || key.isEmpty()) {
            return;
        }
        eventRules.put(key, rule == null ? new EventRule(null, null, null, null, null, Map.of(), null) : rule);
    }

    public void putWorldConfig(String worldName, WorldEventConfig worldConfig) {
        if (worldName == null || worldName.isEmpty() || worldConfig == null) {
            return;
        }
        worldConfigs.put(worldName, worldConfig);
    }

    public EventRuleDefaults getDefaults() {
        return defaults;
    }

    public Map<String, EventRule> getEventRules() {
        return Collections.unmodifiableMap(eventRules);
    }

    public Map<String, WorldEventConfig> getWorldConfigs() {
        return Collections.unmodifiableMap(worldConfigs);
    }

    public Set<String> getAllConfiguredKeys() {
        Set<String> keys = new HashSet<>(eventRules.keySet());
        for (WorldEventConfig worldConfig : worldConfigs.values()) {
            keys.addAll(worldConfig.getEventRules().keySet());
        }
        return keys;
    }
}
