package com.chibashr.allthewebhooks.events;

import com.chibashr.allthewebhooks.config.EventConfig;
import com.chibashr.allthewebhooks.config.EventKeyMatcher;
import com.chibashr.allthewebhooks.util.LocationFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Server;
import org.bukkit.World;
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
import org.bukkit.event.world.WorldLoadEvent;

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

        definitions.put("world.load", new EventDefinition(
                "world.load",
                "world",
                "Fired when a world is loaded or created.",
                Map.of(
                        "world.name", "string",
                        "world.seed", "number",
                        "world.environment", "string",
                        "world.difficulty", "string",
                        "world.min_height", "number",
                        "world.max_height", "number",
                        "world.hardcore", "boolean",
                        "world.spawn_location", "string",
                        "world.structures", "boolean",
                        "world.folder", "string"
                ),
                List.of("world.load.*"),
                "world.load:\n  message: generic",
                (WorldLoadEvent event) -> {
                    World w = event.getWorld();
                    EventContext context = new EventContext("world.load");
                    context.setWorld(w);
                    context.put("world.name", w.getName());
                    context.put("world.seed", w.getSeed());
                    context.put("world.environment", w.getEnvironment().name());
                    context.put("world.difficulty", w.getDifficulty().name());
                    context.put("world.min_height", w.getMinHeight());
                    context.put("world.max_height", w.getMaxHeight());
                    context.put("world.hardcore", w.isHardcore());
                    context.put("world.spawn_location", LocationFormatter.format(w.getSpawnLocation()));
                    context.put("world.structures", w.canGenerateStructures());
                    context.put("world.folder", w.getWorldFolder().getName());
                    return context;
                }
        ));

        definitions.put("server.enable", new EventDefinition(
                "server.enable",
                "server",
                "Fired when the server has finished starting (plugin enabled).",
                Map.of(
                        "server.version", "string",
                        "server.minecraft_version", "string",
                        "server.name", "string"
                ),
                List.of("server.enable.*"),
                "server.enable:\n  message: server_enable",
                null
        ));

        definitions.put("server.disable", new EventDefinition(
                "server.disable",
                "server",
                "Fired when the server is shutting down (plugin disabling).",
                Map.of(
                        "server.version", "string",
                        "server.minecraft_version", "string",
                        "server.name", "string",
                        "server.reason", "string"
                ),
                List.of("server.disable.*"),
                "server.disable:\n  message: server_disable",
                null
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

    public Set<String> getBaseDefinitionKeys() {
        return Collections.unmodifiableSet(baseDefinitions.keySet());
    }

    public void addDiscoveredDefinition(EventDefinition definition) {
        if (definition == null) {
            return;
        }
        String key = definition.getKey();
        baseDefinitions.put(key, definition);
        definitions.put(key, definition);
    }

    public Collection<EventDefinition> getDefinitions() {
        return definitions.values();
    }

    public EventDefinition getDefinition(String key) {
        return definitions.get(key);
    }

    /**
     * Builds a synthetic event context as the server would produce for the given event key,
     * using the commanding player (if present) and server state. Use when firing manually
     * without supplying key=value pairs, e.g. to test emissions.
     */
    public EventContext buildSyntheticContext(String eventKey, Player player, Server server) {
        EventContext ctx = new EventContext(eventKey);

        if (eventKey.equals("server.enable") || eventKey.equals("server.disable")) {
            ctx.put("server.version", server.getVersion());
            ctx.put("server.minecraft_version", server.getBukkitVersion());
            ctx.put("server.name", server.getName());
            if (eventKey.equals("server.disable")) {
                ctx.put("server.reason", "manual");
            }
            return ctx;
        }

        World world = player != null ? player.getWorld() : (server.getWorlds().isEmpty() ? null : server.getWorlds().get(0));
        if (player != null) {
            ctx.setPlayer(player);
            ctx.setWorld(player.getWorld());
            ctx.put("player.name", player.getName());
            ctx.put("player.uuid", player.getUniqueId().toString());
            ctx.put("world.name", player.getWorld().getName());
        } else if (world != null) {
            ctx.setWorld(world);
            ctx.put("world.name", world.getName());
        }

        if (eventKey.equals("player.chat") || eventKey.startsWith("player.chat.")) {
            ctx.put("chat.message", "(manual test)");
            return ctx;
        }
        if (eventKey.equals("player.command") || eventKey.startsWith("player.command.")) {
            ctx.put("command.raw", "/allthewebhooks fire " + eventKey);
            return ctx;
        }
        if (eventKey.equals("player.death") || eventKey.startsWith("player.death.")) {
            ctx.put("death.message", "(manual test)");
            return ctx;
        }
        if (eventKey.equals("player.break.block") || eventKey.startsWith("player.break.block.")) {
            ctx.put("player.gamemode", player != null ? player.getGameMode().name() : "SURVIVAL");
            ctx.put("block.type", "STONE");
            ctx.put("block.location", world != null ? "0,0,0," + world.getName() : "0,0,0");
            return ctx;
        }
        if (eventKey.equals("player.place.block") || eventKey.startsWith("player.place.block.")) {
            ctx.put("player.gamemode", player != null ? player.getGameMode().name() : "SURVIVAL");
            ctx.put("block.type", "STONE");
            ctx.put("block.location", world != null ? "0,0,0," + world.getName() : "0,0,0");
            return ctx;
        }
        if (eventKey.equals("entity.damage.player") || eventKey.startsWith("entity.damage.")) {
            ctx.put("damage.amount", 0);
            ctx.put("damage.cause", "CUSTOM");
            return ctx;
        }
        if (eventKey.equals("inventory.open") || eventKey.startsWith("inventory.open.")) {
            ctx.put("inventory.type", "CHEST");
            return ctx;
        }
        if (eventKey.equals("world.time.change") || eventKey.startsWith("world.time.")) {
            ctx.put("world.time", world != null ? world.getTime() : 0L);
            ctx.put("world.skip.reason", "CUSTOM");
            return ctx;
        }
        if (eventKey.equals("world.load") || eventKey.startsWith("world.load.")) {
            if (world != null) {
                ctx.put("world.seed", world.getSeed());
                ctx.put("world.environment", world.getEnvironment().name());
                ctx.put("world.difficulty", world.getDifficulty().name());
                ctx.put("world.min_height", world.getMinHeight());
                ctx.put("world.max_height", world.getMaxHeight());
                ctx.put("world.hardcore", world.isHardcore());
                ctx.put("world.spawn_location", LocationFormatter.format(world.getSpawnLocation()));
                ctx.put("world.structures", world.canGenerateStructures());
                ctx.put("world.folder", world.getWorldFolder().getName());
            } else {
                ctx.put("world.seed", 0L);
                ctx.put("world.environment", "NORMAL");
                ctx.put("world.difficulty", "NORMAL");
                ctx.put("world.min_height", -64);
                ctx.put("world.max_height", 320);
                ctx.put("world.hardcore", false);
                ctx.put("world.spawn_location", "0,0,0");
                ctx.put("world.structures", true);
                ctx.put("world.folder", "");
            }
            return ctx;
        }

        if (player != null && (eventKey.equals("player.join") || eventKey.startsWith("player.join.")
                || eventKey.equals("player.quit") || eventKey.startsWith("player.quit."))) {
            return ctx;
        }

        if (player == null && eventKey.startsWith("player.")) {
            ctx.put("player.name", "Console");
            ctx.put("player.uuid", "");
        }

        return ctx;
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

    /**
     * Builds a world.load event context from an already-loaded World.
     * Use at plugin startup to emit world.load for worlds that were loaded before the plugin enabled
     * (and thus did not fire WorldLoadEvent for this plugin).
     */
    public EventContext buildContextForWorldLoad(World world) {
        if (world == null) {
            return null;
        }
        EventContext context = new EventContext("world.load");
        context.setWorld(world);
        context.put("world.name", world.getName());
        context.put("world.seed", world.getSeed());
        context.put("world.environment", world.getEnvironment().name());
        context.put("world.difficulty", world.getDifficulty().name());
        context.put("world.min_height", world.getMinHeight());
        context.put("world.max_height", world.getMaxHeight());
        context.put("world.hardcore", world.isHardcore());
        context.put("world.spawn_location", LocationFormatter.format(world.getSpawnLocation()));
        context.put("world.structures", world.canGenerateStructures());
        context.put("world.folder", world.getWorldFolder().getName());
        return context;
    }

    /**
     * Builds a player.join event context from a Player already online.
     * Use at plugin startup to emit player.join for players who were already on the server when the
     * plugin enabled (and thus did not fire PlayerJoinEvent for this plugin).
     */
    public EventContext buildContextForPlayerJoin(Player player) {
        if (player == null || !player.isOnline()) {
            return null;
        }
        EventContext context = new EventContext("player.join");
        context.setPlayer(player);
        context.setWorld(player.getWorld());
        context.put("player.name", player.getName());
        context.put("player.uuid", player.getUniqueId().toString());
        context.put("world.name", player.getWorld().getName());
        return context;
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
