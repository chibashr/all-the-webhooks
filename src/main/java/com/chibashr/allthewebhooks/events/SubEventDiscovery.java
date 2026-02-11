package com.chibashr.allthewebhooks.events;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.damage.DamageType;
import org.bukkit.damage.DeathMessageType;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Discovers sub-event keys from Paper registries and Bukkit enums at startup.
 * Adds definitions for player.death.* (from DamageType registry) and
 * entity.damage.player.* (from DamageCause enum) so they are configurable
 * and documented.
 */
public final class SubEventDiscovery {

    /** Fall death message suffixes (death.fell.*) not derived from DamageType.getTranslationKey(). */
    private static final List<String> FALL_DEATH_SUFFIXES = Arrays.asList(
            "fell.accident.generic",
            "fell.accident.ladder",
            "fell.accident.vines",
            "fell.accident.weeping_vines",
            "fell.accident.twisting_vines",
            "fell.accident.scaffolding",
            "fell.accident.other_climbable",
            "fell.accident.water",
            "fell.killer",
            "fell.assist",
            "fell.assist.item",
            "fell.finish",
            "fell.finish.item"
    );

    private SubEventDiscovery() {
    }

    /**
     * Discovers sub-events from registries and enums, adds them to the registry.
     *
     * @param server  the server (for registry access)
     * @param registry the event registry to add definitions to
     * @param logger   for warnings (e.g. registry unavailable)
     */
    public static void discover(Server server, EventRegistry registry, Logger logger) {
        discoverDeathEvents(server, registry, logger);
        discoverEntityDamageEvents(registry);
    }

    private static void discoverDeathEvents(Server server, EventRegistry registry, Logger logger) {
        EventDefinition base = registry.getDefinition("player.death");
        if (base == null) {
            return;
        }
        Map<String, String> predicates = new LinkedHashMap<>(base.getPredicateFields());
        if (!predicates.containsKey("death.message.key")) {
            predicates.put("death.message.key", "string");
        }
        Map<String, String> predicateMap = Map.copyOf(predicates);

        Set<String> keys = new LinkedHashSet<>();

        try {
            var registryAccess = RegistryAccess.registryAccess();
            var damageRegistry = registryAccess.getRegistry(RegistryKey.DAMAGE_TYPE);
            for (DamageType type : damageRegistry) {
                if (type.getDeathMessageType() == DeathMessageType.INTENTIONAL_GAME_DESIGN) {
                    keys.add("player.death.attack.badRespawnPoint");
                    continue;
                }
                String baseKey = type.getTranslationKey();
                if (baseKey == null || baseKey.isEmpty()) {
                    continue;
                }
                String suffix = DeathMessageKeyResolver.stripDeathPrefix(baseKey);
                keys.add("player.death." + suffix);
                if (!suffix.endsWith(".player")) {
                    keys.add("player.death." + suffix + ".player");
                }
            }
        } catch (Exception e) {
            logger.warning("Could not scan DamageType registry: " + e.getMessage());
        }

        for (String fallSuffix : FALL_DEATH_SUFFIXES) {
            keys.add("player.death." + fallSuffix);
        }

        for (String key : keys) {
            if (registry.getDefinition(key) != null) {
                continue;
            }
            String suffix = key.substring("player.death.".length());
            String category = "player";
            String description = "Fired when a player dies (" + suffix + ").";
            String firstSegment = suffix.contains(".") ? suffix.substring(0, suffix.indexOf('.')) : suffix;
            List<String> wildcards = List.of("player.death.*", "player.death." + firstSegment + ".*");
            String minecraftKey = "death." + suffix;
            String example = key + ":\n  message: generic\n  conditions:\n    death.message.key:\n      equals: " + minecraftKey;
            EventDefinition def = new EventDefinition(
                    key,
                    category,
                    description,
                    predicateMap,
                    wildcards,
                    example,
                    null
            );
            registry.addDiscoveredDefinition(def);
        }
    }

    private static void discoverEntityDamageEvents(EventRegistry registry) {
        EventDefinition base = registry.getDefinition("entity.damage.player");
        if (base == null) {
            return;
        }
        Map<String, String> predicates = new LinkedHashMap<>(base.getPredicateFields());
        Map<String, String> predicateMap = Map.copyOf(predicates);

        for (EntityDamageEvent.DamageCause cause : EntityDamageEvent.DamageCause.values()) {
            String suffix = cause.name().toLowerCase();
            String key = "entity.damage.player." + suffix;
            if (registry.getDefinition(key) != null) {
                continue;
            }
            String category = "entity";
            String description = "Fired when a player takes damage from " + cause.name() + ".";
            List<String> wildcards = List.of("entity.damage.*", "entity.damage.player.*");
            String example = key + ":\n  message: player_damaged\n  conditions:\n    damage.cause:\n      equals: " + cause.name();
            EventDefinition def = new EventDefinition(
                    key,
                    category,
                    description,
                    predicateMap,
                    wildcards,
                    example,
                    null
            );
            registry.addDiscoveredDefinition(def);
        }
    }
}
