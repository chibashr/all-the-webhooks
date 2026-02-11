package com.chibashr.allthewebhooks.enrichment;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Central registry of entity enrichers. Provides field specs for documentation
 * and validation, and enrichers for runtime context population.
 */
public final class EntityFieldRegistry {

    private static final Map<String, ContextEnricher<?>> ENRICHERS = Map.of(
            "world", WorldEnricher.INSTANCE,
            "player", PlayerEnricher.INSTANCE,
            "block", BlockEnricher.INSTANCE
    );

    private static final Map<String, Map<String, String>> FIELD_SPEC_CACHE = buildFieldSpecCache();

    private EntityFieldRegistry() {
    }

    private static Map<String, Map<String, String>> buildFieldSpecCache() {
        Map<String, Map<String, String>> cache = new LinkedHashMap<>();
        for (Map.Entry<String, ContextEnricher<?>> e : ENRICHERS.entrySet()) {
            cache.put(e.getKey(), e.getValue().getFieldSpec());
        }
        return Collections.unmodifiableMap(cache);
    }

    public static ContextEnricher<?> getEnricher(String entityType) {
        return ENRICHERS.get(entityType);
    }

    public static Set<String> getEntityTypes() {
        return ENRICHERS.keySet();
    }

    /**
     * Returns the field spec (name → type) for an entity type.
     * Used for documentation and validation.
     */
    public static Map<String, String> getFieldSpec(String entityType) {
        Map<String, String> spec = FIELD_SPEC_CACHE.get(entityType);
        return spec != null ? spec : Map.of();
    }

    /**
     * Returns the combined field spec for multiple entity types.
     * Order preserved for stable output.
     */
    public static Map<String, String> getCombinedFieldSpec(Iterable<String> entityTypes) {
        Map<String, String> combined = new LinkedHashMap<>();
        for (String type : entityTypes) {
            combined.putAll(getFieldSpec(type));
        }
        return combined;
    }
}
