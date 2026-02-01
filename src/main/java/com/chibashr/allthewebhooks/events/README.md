# Events

## Event discovery

On plugin initialization, **EventDiscovery** scans the classpath for Bukkit/Paper event classes (`org.bukkit.event`, `io.papermc.paper.event`). For each concrete `Event` subclass that is not already covered by built-in definitions or **EventListener**, it:

1. Computes a dot-notation event key from the class name (e.g. `BlockBreakEvent` → `block.break`).
2. Adds a minimal **EventDefinition** to the registry (predicates: `event.name`, `event.class`; generic context builder).
3. Registers a listener so that when the event fires, the router can dispatch a webhook if the user configured that key in `events.yaml`.

Built-in definitions (e.g. `player.join`, `server.enable`) and events handled by **EventListener** are skipped so there are no duplicate handlers. Docs are generated from **EventRegistry.getDefinitions()**, so discovered events appear in the generated documentation automatically.
