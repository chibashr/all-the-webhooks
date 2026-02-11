package com.chibashr.allthewebhooks.enrichment;

import com.chibashr.allthewebhooks.events.EventContext;
import com.chibashr.allthewebhooks.util.LocationFormatter;
import java.util.Map;
import org.bukkit.World;

/** Enriches context with world.* fields from a World in scope. */
public final class WorldEnricher implements ContextEnricher<World> {

    public static final WorldEnricher INSTANCE = new WorldEnricher();

    private static final Map<String, String> FIELD_SPEC = Map.ofEntries(
            Map.entry("world.name", "string"),
            Map.entry("world.seed", "number"),
            Map.entry("world.environment", "string"),
            Map.entry("world.difficulty", "string"),
            Map.entry("world.min_height", "number"),
            Map.entry("world.max_height", "number"),
            Map.entry("world.hardcore", "boolean"),
            Map.entry("world.spawn_location", "string"),
            Map.entry("world.structures", "boolean"),
            Map.entry("world.folder", "string"),
            Map.entry("world.time", "number")
    );

    private WorldEnricher() {
    }

    @Override
    public String getEntityType() {
        return "world";
    }

    @Override
    public Map<String, String> getFieldSpec() {
        return FIELD_SPEC;
    }

    @Override
    public void enrich(EventContext ctx, World entity) {
        if (entity == null) {
            return;
        }
        ctx.put("world.name", entity.getName());
        ctx.put("world.seed", entity.getSeed());
        ctx.put("world.environment", entity.getEnvironment().name());
        ctx.put("world.difficulty", entity.getDifficulty().name());
        ctx.put("world.min_height", entity.getMinHeight());
        ctx.put("world.max_height", entity.getMaxHeight());
        ctx.put("world.hardcore", entity.isHardcore());
        ctx.put("world.spawn_location", LocationFormatter.format(entity.getSpawnLocation()));
        ctx.put("world.structures", entity.canGenerateStructures());
        ctx.put("world.folder", entity.getWorldFolder().getName());
        ctx.put("world.time", entity.getTime());
    }
}
