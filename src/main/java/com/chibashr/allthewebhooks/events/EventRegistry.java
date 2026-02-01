package com.chibashr.allthewebhooks.events;

import com.chibashr.allthewebhooks.config.EventConfig;
import com.chibashr.allthewebhooks.config.EventKeyMatcher;
import com.chibashr.allthewebhooks.util.LocationFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.world.TimeSkipEvent;

public class EventRegistry {
    private final Map<String, EventDefinition> baseDefinitions;
    private final Map<String, EventDefinition> definitions;

    private EventRegistry(Map<String, EventDefinition> baseDefinitions) {
        this.baseDefinitions = new LinkedHashMap<>(baseDefinitions);
        this.definitions = new LinkedHashMap<>(baseDefinitions);
    }

    public static EventRegistry createDefault() {
        Map<String, EventDefinition> definitions = new LinkedHashMap<>();

        definitions.put("player.join", new EventDefinition(
                "player.join",
                "player",
                "Fired when a player joins the server.",
                Map.of(
                        "player.name", "string",
                        "player.uuid", "uuid",
                        "world.name", "string"
                ),
                List.of("player.join.*"),
                "player.join:\n  message: player_join",
                (PlayerJoinEvent event) -> {
                    EventContext context = new EventContext("player.join");
                    Player player = event.getPlayer();
                    context.setPlayer(player);
                    context.setWorld(player.getWorld());
                    context.put("player.name", player.getName());
                    context.put("player.uuid", player.getUniqueId().toString());
                    context.put("world.name", player.getWorld().getName());
                    return context;
                }
        ));

        definitions.put("player.quit", new EventDefinition(
                "player.quit",
                "player",
                "Fired when a player leaves the server.",
                Map.of(
                        "player.name", "string",
                        "player.uuid", "uuid",
                        "world.name", "string"
                ),
                List.of("player.quit.*"),
                "player.quit:\n  message: player_quit",
                (PlayerQuitEvent event) -> {
                    EventContext context = new EventContext("player.quit");
                    Player player = event.getPlayer();
                    context.setPlayer(player);
                    context.setWorld(player.getWorld());
                    context.put("player.name", player.getName());
                    context.put("player.uuid", player.getUniqueId().toString());
                    context.put("world.name", player.getWorld().getName());
                    return context;
                }
        ));

        definitions.put("player.chat", new EventDefinition(
                "player.chat",
                "player",
                "Fired when a player sends a chat message.",
                Map.of(
                        "player.name", "string",
                        "player.uuid", "uuid",
                        "chat.message", "string",
                        "world.name", "string"
                ),
                List.of("player.chat.*"),
                "player.chat:\n  message: generic",
                (AsyncPlayerChatEvent event) -> {
                    EventContext context = new EventContext("player.chat");
                    Player player = event.getPlayer();
                    context.setPlayer(player);
                    context.setWorld(player.getWorld());
                    context.put("player.name", player.getName());
                    context.put("player.uuid", player.getUniqueId().toString());
                    context.put("chat.message", event.getMessage());
                    context.put("world.name", player.getWorld().getName());
                    return context;
                }
        ));

        definitions.put("player.command", new EventDefinition(
                "player.command",
                "player",
                "Fired when a player runs a command.",
                Map.of(
                        "player.name", "string",
                        "player.uuid", "uuid",
                        "command.raw", "string",
                        "world.name", "string"
                ),
                List.of("player.command.*"),
                "player.command:\n  message: generic",
                (PlayerCommandPreprocessEvent event) -> {
                    EventContext context = new EventContext("player.command");
                    Player player = event.getPlayer();
                    context.setPlayer(player);
                    context.setWorld(player.getWorld());
                    context.put("player.name", player.getName());
                    context.put("player.uuid", player.getUniqueId().toString());
                    context.put("command.raw", event.getMessage());
                    context.put("world.name", player.getWorld().getName());
                    return context;
                }
        ));

        definitions.put("player.death", new EventDefinition(
                "player.death",
                "player",
                "Fired when a player dies.",
                Map.of(
                        "player.name", "string",
                        "player.uuid", "uuid",
                        "world.name", "string",
                        "death.message", "string"
                ),
                List.of("player.death.*"),
                "player.death:\n  message: generic",
                (PlayerDeathEvent event) -> {
                    EventContext context = new EventContext("player.death");
                    Player player = event.getEntity();
                    context.setPlayer(player);
                    context.setWorld(player.getWorld());
                    context.put("player.name", player.getName());
                    context.put("player.uuid", player.getUniqueId().toString());
                    context.put("world.name", player.getWorld().getName());
                    context.put("death.message", event.getDeathMessage());
                    return context;
                }
        ));

        definitions.put("player.break.block", new EventDefinition(
                "player.break.block",
                "player",
                "Fired when a player breaks a block.",
                Map.of(
                        "player.name", "string",
                        "player.uuid", "uuid",
                        "player.gamemode", "string",
                        "block.type", "string",
                        "block.location", "string",
                        "world.name", "string"
                ),
                List.of("player.break.block.*", "player.break.block.<material>"),
                "player.break.block:\n  message: block_break\n  conditions:\n    block.type:\n      equals: DIAMOND_ORE",
                (BlockBreakEvent event) -> {
                    EventContext context = new EventContext("player.break.block");
                    Player player = event.getPlayer();
                    context.setPlayer(player);
                    context.setWorld(player.getWorld());
                    context.put("player.name", player.getName());
                    context.put("player.uuid", player.getUniqueId().toString());
                    context.put("player.gamemode", player.getGameMode().name());
                    context.put("block.type", event.getBlock().getType().name());
                    context.put("block.location", LocationFormatter.format(event.getBlock().getLocation()));
                    context.put("world.name", player.getWorld().getName());
                    return context;
                }
        ));

        definitions.put("player.place.block", new EventDefinition(
                "player.place.block",
                "player",
                "Fired when a player places a block.",
                Map.of(
                        "player.name", "string",
                        "player.uuid", "uuid",
                        "player.gamemode", "string",
                        "block.type", "string",
                        "block.location", "string",
                        "world.name", "string"
                ),
                List.of("player.place.block.*", "player.place.block.<material>"),
                "player.place.block:\n  message: generic",
                (BlockPlaceEvent event) -> {
                    EventContext context = new EventContext("player.place.block");
                    Player player = event.getPlayer();
                    context.setPlayer(player);
                    context.setWorld(player.getWorld());
                    context.put("player.name", player.getName());
                    context.put("player.uuid", player.getUniqueId().toString());
                    context.put("player.gamemode", player.getGameMode().name());
                    context.put("block.type", event.getBlockPlaced().getType().name());
                    context.put("block.location", LocationFormatter.format(event.getBlockPlaced().getLocation()));
                    context.put("world.name", player.getWorld().getName());
                    return context;
                }
        ));

        definitions.put("entity.damage.player", new EventDefinition(
                "entity.damage.player",
                "entity",
                "Fired when a player is damaged.",
                Map.of(
                        "player.name", "string",
                        "player.uuid", "uuid",
                        "damage.amount", "number",
                        "damage.cause", "string",
                        "world.name", "string"
                ),
                List.of("entity.damage.*"),
                "entity.damage.player:\n  message: player_damaged",
                (EntityDamageEvent event) -> {
                    if (!(event.getEntity() instanceof Player player)) {
                        return null;
                    }
                    EventContext context = new EventContext("entity.damage.player");
                    context.setPlayer(player);
                    context.setWorld(player.getWorld());
                    context.put("player.name", player.getName());
                    context.put("player.uuid", player.getUniqueId().toString());
                    context.put("damage.amount", event.getFinalDamage());
                    context.put("damage.cause", event.getCause().name());
                    context.put("world.name", player.getWorld().getName());
                    return context;
                }
        ));

        definitions.put("inventory.open", new EventDefinition(
                "inventory.open",
                "inventory",
                "Fired when a player opens an inventory.",
                Map.of(
                        "player.name", "string",
                        "player.uuid", "uuid",
                        "inventory.type", "string",
                        "world.name", "string"
                ),
                List.of("inventory.open.*"),
                "inventory.open:\n  message: generic",
                (InventoryOpenEvent event) -> {
                    if (!(event.getPlayer() instanceof Player player)) {
                        return null;
                    }
                    EventContext context = new EventContext("inventory.open");
                    context.setPlayer(player);
                    context.setWorld(player.getWorld());
                    context.put("player.name", player.getName());
                    context.put("player.uuid", player.getUniqueId().toString());
                    context.put("inventory.type", event.getInventory().getType().name());
                    context.put("world.name", player.getWorld().getName());
                    return context;
                }
        ));

        definitions.put("world.time.change", new EventDefinition(
                "world.time.change",
                "world",
                "Fired when the world time changes via a time skip.",
                Map.of(
                        "world.name", "string",
                        "world.time", "number",
                        "world.skip.reason", "string"
                ),
                List.of("world.time.*"),
                "world.time.change:\n  message: generic",
                (TimeSkipEvent event) -> {
                    EventContext context = new EventContext("world.time.change");
                    context.setWorld(event.getWorld());
                    context.put("world.name", event.getWorld().getName());
                    context.put("world.time", event.getWorld().getTime());
                    context.put("world.skip.reason", event.getSkipReason().name());
                    return context;
                }
        ));

        return new EventRegistry(definitions);
    }

    public void updateFromConfig(EventConfig config) {
        definitions.clear();
        definitions.putAll(baseDefinitions);
        if (config == null) {
            return;
        }
        for (String key : config.getAllConfiguredKeys()) {
            if (!definitions.containsKey(key)) {
                EventDefinition derived = deriveDefinition(key);
                if (derived != null) {
                    definitions.put(key, derived);
                }
            }
        }
    }

    public Collection<EventDefinition> getDefinitions() {
        return definitions.values();
    }

    public EventDefinition getDefinition(String key) {
        return definitions.get(key);
    }

    @SuppressWarnings("unchecked")
    public <E extends org.bukkit.event.Event> EventContext buildContext(String key, E event) {
        EventDefinition definition = definitions.get(key);
        if (definition == null) {
            return null;
        }
        EventContextBuilder<E> builder = (EventContextBuilder<E>) definition.getContextBuilder();
        if (builder == null) {
            return null;
        }
        return builder.build(event);
    }

    private EventDefinition deriveDefinition(String key) {
        EventDefinition base = findBestBaseDefinition(key);
        if (base == null) {
            return null;
        }
        String category = base.getCategory();
        String description = "Derived from " + base.getKey() + ".";
        Map<String, String> predicates = base.getPredicateFields();
        List<String> wildcards = base.getWildcardExamples();
        String example = key + ":\n  message: generic";
        return new EventDefinition(
                key,
                category,
                description,
                predicates,
                wildcards,
                example,
                null
        );
    }

    private EventDefinition findBestBaseDefinition(String key) {
        String bestKey = null;
        EventKeyMatcher.MatchScore bestScore = EventKeyMatcher.MatchScore.NO_MATCH;
        for (String candidate : baseDefinitions.keySet()) {
            EventKeyMatcher.MatchScore score = EventKeyMatcher.score(candidate, key);
            if (score.isBetterThan(bestScore)) {
                bestScore = score;
                bestKey = candidate;
            }
        }
        if (bestKey == null) {
            return null;
        }
        return baseDefinitions.get(bestKey);
    }
}
