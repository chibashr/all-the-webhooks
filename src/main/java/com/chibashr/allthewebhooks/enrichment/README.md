# Context Enrichment

Context enrichment automatically populates event context with fields from entities in scope (World, Player, Block). This avoids duplicating field definitions and keeps documentation accurate.

## How it works

1. **Entity field registry** — `EntityFieldRegistry` holds enrichers for `world`, `player`, and `block`. Each enricher knows its field spec (for docs) and how to populate context.

2. **Scope derivation** — `ScopeDerivation.deriveEnrichedFrom(eventKey)` determines which entities are in scope from the event key (e.g. `player.death` → player, world; `player.break.block` → player, world, block).

3. **Enrichment at runtime** — When `EventContext.setPlayer()`, `setWorld()`, or `setBlock()` is called, the corresponding enricher runs and populates `player.*`, `world.*`, or `block.*` fields.

4. **Effective predicates** — `EventDefinition.getEffectivePredicateFields()` merges event-specific predicates with enriched entity fields for documentation and validation.

## Adding new enrichment types

1. Implement `ContextEnricher<T>` (e.g. `LocationEnricher`).
2. Register in `EntityFieldRegistry.ENRICHERS`.
3. Add scope derivation rules in `ScopeDerivation.deriveEnrichedFrom()` for event keys that have that entity in scope.

## Entity field specs

- **World** — world.name, world.environment, world.difficulty, world.seed, world.min_height, world.max_height, world.hardcore, world.spawn_location, world.structures, world.folder, world.time
- **Player** — player.name, player.uuid, player.gamemode
- **Block** — block.type, block.location
