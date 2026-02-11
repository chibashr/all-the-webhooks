# Events

## Event discovery

On plugin initialization, **EventDiscovery** scans the classpath for Bukkit/Paper event classes (`org.bukkit.event`, `io.papermc.paper.event`). For each concrete `Event` subclass that is not already covered by built-in definitions or **EventListener**, it:

1. Computes a dot-notation event key from the class name (e.g. `BlockBreakEvent` → `block.break`).
2. Builds predicates and a context builder via **DiscoveredEventBuilder**: reflection on public getters (`getX` / `isX`) discovers properties; each is exposed as a predicate `event.<property>` with a type (`string`, `number`, `boolean`). Bukkit types (Player, World, Block, Location, Entity) are formatted to strings. `event.name` and `event.class` are always included; getter-derived predicates are added per event type.
3. Builds a human-readable description from the event class name (e.g. `PlayerJoinEvent` → "Fired when player join.").
4. Adds an **EventDefinition** to the registry with those predicates, description, and a context builder that fills the context from the event instance.
5. Registers a listener so that when the event fires, the router can dispatch a webhook if the user configured that key in `events.yaml`.

Built-in definitions (e.g. `player.join`, `server.enable`) and events handled by **EventListener** are skipped so there are no duplicate handlers. Docs are generated from **EventRegistry.getDefinitions()**, so discovered events appear in the generated documentation with their discovered predicates and descriptions.

## Sub-event discovery

**SubEventDiscovery** runs after EventDiscovery and scans Paper registries and Bukkit enums at startup to add sub-event definitions:

- **player.death.*** — From the DamageType registry. Each damage type (cactus, lava, drown, etc.) and its `.player` variant (when a mob/player caused the death) are registered. Fall death variants (`death.fell.*`) are also added. Event keys use the format `player.death.attack.dryout.player` (the leading `death.` is stripped to avoid redundancy).
- **entity.damage.player.*** — From `EntityDamageEvent.DamageCause` enum. Each cause (e.g. `drowning`, `fall`) is registered as `entity.damage.player.<cause>`.

When a player dies or takes damage, the context builder emits the most specific event key (e.g. `player.death.attack.lava`), so rules for that specific key or wildcards like `player.death.attack.*` match.

**DeathMessageKeyResolver** extracts the Minecraft death message key from `EntityDeathEvent.getDamageSource()` and builds the event key for routing.

**EventDefinition metadata for sub-events:** Sub-events set `parentBaseKey` (the base event they inherit from). Use `isSubEvent()` and `getParentBaseKey()` to style or group them in documentation, so sub-events appear in compact form under their base without duplicating full entries.

## Event rule nesting

`events.yaml` supports nested sections, but only leaf sections that contain rule fields (`message`, `webhook`, `enabled`, `require-permission`, `conditions`, `rate-limit`) are registered as rules. This prevents parent grouping nodes (like `events.player`) from matching every `player.*` event when only `events.player.join` is intended.

### Built-in world events

**world.load** — Fired when a world is loaded or created (`WorldLoadEvent`). Predicates: `world.name`, `world.seed`, `world.environment`, `world.difficulty`, `world.min_height`, `world.max_height`, `world.hardcore`, `world.spawn_location`, `world.structures`, `world.folder`. Use in `events.yaml` with `message: world_load` (or `generic`) and optional `conditions` on any of these fields. Condition keys may be dotted (e.g. `world.name` with `not: [world_nether, world_the_end]`) so only the main world matches when you exclude Nether/The End.

**Startup catch-up:** Because the plugin loads after the world (and after some players may have joined), it does not receive `WorldLoadEvent` or `PlayerJoinEvent` for state that already existed at enable. On enable the plugin emits synthetic events for that state so nothing is missed: a **world.load** context for each world in `server.getWorlds()`, and a **player.join** context for each player in `server.getOnlinePlayers()`.

### DiscoveredEventBuilder

**DiscoveredEventBuilder** uses manual reflection (no `java.desktop`) to discover getters and format values. Properties named `class` or `handlers` are skipped. Return types are mapped to predicate types; values are formatted for conditions and message placeholders (e.g. Player → name, Block → type and location string).
