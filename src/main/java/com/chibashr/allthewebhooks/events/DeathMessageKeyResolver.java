package com.chibashr.allthewebhooks.events;

import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.damage.DeathMessageType;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Resolves the Minecraft death message translation key from a death event.
 * Strips the leading "death." when nesting under player.death to avoid redundancy
 * (e.g. death.attack.dryout.player → attack.dryout.player).
 */
public final class DeathMessageKeyResolver {

    private static final String DEATH_PREFIX = "death.";
    private static final String PLAYER_SUFFIX = ".player";

    private DeathMessageKeyResolver() {
    }

    /**
     * Resolves the full death message key for use under player.death.
     * Returns the suffix with "death." stripped (e.g. attack.dryout.player).
     *
     * @param event the entity death event (e.g. PlayerDeathEvent)
     * @return the event key suffix, or "generic" if unavailable
     */
    public static String resolveEventKeySuffix(EntityDeathEvent event) {
        String fullKey = resolveFullMinecraftKey(event);
        return stripDeathPrefix(fullKey);
    }

    /**
     * Resolves the full Minecraft death message translation key (e.g. death.attack.dryout.player).
     *
     * @param event the entity death event
     * @return the full translation key, or "death.attack.generic" if unavailable
     */
    public static String resolveFullMinecraftKey(EntityDeathEvent event) {
        DamageSource source = event.getDamageSource();
        if (source == null) {
            return "death.attack.generic";
        }
        return resolveFullMinecraftKey(source);
    }

    /**
     * Resolves the full Minecraft death message key from a DamageSource.
     */
    public static String resolveFullMinecraftKey(DamageSource source) {
        if (source == null) {
            return "death.attack.generic";
        }
        DamageType type = source.getDamageType();
        if (type == null) {
            return "death.attack.generic";
        }
        String baseKey = type.getTranslationKey();
        if (baseKey == null || baseKey.isEmpty()) {
            return "death.attack.generic";
        }
        DeathMessageType msgType = type.getDeathMessageType();
        if (msgType == DeathMessageType.INTENTIONAL_GAME_DESIGN) {
            return "death.attack.badRespawnPoint";
        }
        boolean hasAttacker = source.getCausingEntity() != null;
        if (hasAttacker && !baseKey.endsWith(PLAYER_SUFFIX)) {
            return baseKey + PLAYER_SUFFIX;
        }
        return baseKey;
    }

    /**
     * Strips the leading "death." prefix for nesting under player.death.
     * E.g. death.attack.dryout.player → attack.dryout.player
     */
    public static String stripDeathPrefix(String minecraftKey) {
        if (minecraftKey == null || minecraftKey.isEmpty()) {
            return "generic";
        }
        if (minecraftKey.startsWith(DEATH_PREFIX)) {
            return minecraftKey.substring(DEATH_PREFIX.length());
        }
        return minecraftKey;
    }

    /**
     * Builds the full event key for player.death from an entity death event.
     * E.g. player.death.attack.dryout.player
     */
    public static String buildPlayerDeathEventKey(EntityDeathEvent event) {
        String suffix = resolveEventKeySuffix(event);
        return "player.death." + suffix;
    }
}
