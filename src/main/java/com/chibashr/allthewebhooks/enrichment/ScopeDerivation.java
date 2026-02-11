package com.chibashr.allthewebhooks.enrichment;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Derives which entity types are in scope for an event based on its key.
 * Used for automatic context enrichment and documentation.
 */
public final class ScopeDerivation {

    private ScopeDerivation() {
    }

    /**
     * Derives enriched entity types from an event key.
     * E.g. "player.death.attack.lava" → ["player", "world"]
     * "player.break.block" → ["player", "world", "block"]
     *
     * @param eventKey the event key (e.g. player.death, world.load)
     * @return list of entity type ids that are in scope (player implies world)
     */
    public static List<String> deriveEnrichedFrom(String eventKey) {
        if (eventKey == null || eventKey.isEmpty()) {
            return List.of();
        }
        Set<String> scope = new LinkedHashSet<>();
        String[] tokens = eventKey.toLowerCase().split("\\.");

        if (arrayContains(tokens, "player")) {
            scope.add("player");
            scope.add("world");
        }
        if (arrayContains(tokens, "world")) {
            scope.add("world");
        }
        if (arrayContains(tokens, "block")) {
            scope.add("block");
        }
        if (arrayContains(tokens, "entity") && arrayContains(tokens, "player")) {
            scope.add("player");
            scope.add("world");
        }
        if (arrayContains(tokens, "inventory")) {
            scope.add("player");
            scope.add("world");
        }

        return List.copyOf(scope);
    }

    private static boolean arrayContains(String[] arr, String s) {
        for (String t : arr) {
            if (t.equals(s)) {
                return true;
            }
        }
        return false;
    }
}
