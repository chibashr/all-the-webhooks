# Events

## Event discovery

On plugin initialization, **EventDiscovery** scans the classpath for Bukkit/Paper event classes (`org.bukkit.event`, `io.papermc.paper.event`). For each concrete `Event` subclass that is not already covered by built-in definitions or **EventListener**, it:

1. Computes a dot-notation event key from the class name (e.g. `BlockBreakEvent` → `block.break`).
2. Builds predicates and a context builder via **DiscoveredEventBuilder**: reflection on public getters (`getX` / `isX`) discovers properties; each is exposed as a predicate `event.<property>` with a type (`string`, `number`, `boolean`). Bukkit types (Player, World, Block, Location, Entity) are formatted to strings. `event.name` and `event.class` are always included; getter-derived predicates are added per event type.
3. Builds a human-readable description from the event class name (e.g. `PlayerJoinEvent` → "Fired when player join.").
4. Adds an **EventDefinition** to the registry with those predicates, description, and a context builder that fills the context from the event instance.
5. Registers a listener so that when the event fires, the router can dispatch a webhook if the user configured that key in `events.yaml`.

Built-in definitions (e.g. `player.join`, `server.enable`) and events handled by **EventListener** are skipped so there are no duplicate handlers. Docs are generated from **EventRegistry.getDefinitions()**, so discovered events appear in the generated documentation with their discovered predicates and descriptions.

### DiscoveredEventBuilder

**DiscoveredEventBuilder** uses manual reflection (no `java.desktop`) to discover getters and format values. Properties named `class` or `handlers` are skipped. Return types are mapped to predicate types; values are formatted for conditions and message placeholders (e.g. Player → name, Block → type and location string).
