package com.chibashr.allthewebhooks.enrichment;

import com.chibashr.allthewebhooks.events.EventContext;
import java.util.Map;

/**
 * Populates event context with fields from an entity in scope (World, Player, Block, etc.).
 * Field specs are discovered via reflection for accurate documentation and validation.
 */
public interface ContextEnricher<T> {

    /** Entity type id used for scope derivation and docs (e.g. "world", "player", "block"). */
    String getEntityType();

    /** Field name → type for documentation and validation. Single source of truth. */
    Map<String, String> getFieldSpec();

    /** Populate context when entity is in scope. */
    void enrich(EventContext ctx, T entity);
}
