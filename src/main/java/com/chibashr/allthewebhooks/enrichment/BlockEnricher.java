package com.chibashr.allthewebhooks.enrichment;

import com.chibashr.allthewebhooks.events.EventContext;
import com.chibashr.allthewebhooks.util.LocationFormatter;
import java.util.Map;
import org.bukkit.block.Block;

/** Enriches context with block.* fields from a Block in scope. */
public final class BlockEnricher implements ContextEnricher<Block> {

    public static final BlockEnricher INSTANCE = new BlockEnricher();

    private static final Map<String, String> FIELD_SPEC = Map.of(
            "block.type", "string",
            "block.location", "string"
    );

    private BlockEnricher() {
    }

    @Override
    public String getEntityType() {
        return "block";
    }

    @Override
    public Map<String, String> getFieldSpec() {
        return FIELD_SPEC;
    }

    @Override
    public void enrich(EventContext ctx, Block entity) {
        if (entity == null) {
            return;
        }
        ctx.put("block.type", entity.getType().name());
        ctx.put("block.location", LocationFormatter.format(entity.getLocation()));
    }
}
