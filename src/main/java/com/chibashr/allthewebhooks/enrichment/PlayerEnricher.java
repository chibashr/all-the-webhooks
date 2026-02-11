package com.chibashr.allthewebhooks.enrichment;

import com.chibashr.allthewebhooks.events.EventContext;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.entity.Player;

/** Enriches context with player.* fields from a Player in scope. */
public final class PlayerEnricher implements ContextEnricher<Player> {

    public static final PlayerEnricher INSTANCE = new PlayerEnricher();

    private final Map<String, String> fieldSpec;

    private PlayerEnricher() {
        Map<String, String> spec = new LinkedHashMap<>();
        spec.put("player.name", "string");
        spec.put("player.uuid", "string");
        spec.put("player.gamemode", "string");
        this.fieldSpec = Map.copyOf(spec);
    }

    @Override
    public String getEntityType() {
        return "player";
    }

    @Override
    public Map<String, String> getFieldSpec() {
        return fieldSpec;
    }

    @Override
    public void enrich(EventContext ctx, Player entity) {
        if (entity == null) {
            return;
        }
        ctx.put("player.name", entity.getName());
        ctx.put("player.uuid", entity.getUniqueId().toString());
        ctx.put("player.gamemode", entity.getGameMode().name());
    }
}
