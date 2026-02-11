package com.chibashr.allthewebhooks.events;

import com.chibashr.allthewebhooks.enrichment.BlockEnricher;
import com.chibashr.allthewebhooks.enrichment.PlayerEnricher;
import com.chibashr.allthewebhooks.enrichment.WorldEnricher;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class EventContext {
    private final String eventKey;
    private final Map<String, Object> values = new HashMap<>();
    private Player player;
    private World world;
    private Block block;

    public EventContext(String eventKey) {
        this.eventKey = eventKey;
        put("event.name", eventKey);
    }

    public String getEventKey() {
        return eventKey;
    }

    public void setPlayer(Player player) {
        this.player = player;
        if (player != null) {
            PlayerEnricher.INSTANCE.enrich(this, player);
            setWorld(player.getWorld());
        }
    }

    public Player getPlayer() {
        return player;
    }

    public void setWorld(World world) {
        this.world = world;
        if (world != null) {
            WorldEnricher.INSTANCE.enrich(this, world);
        }
    }

    public World getWorld() {
        return world;
    }

    public void setBlock(Block block) {
        this.block = block;
        if (block != null) {
            BlockEnricher.INSTANCE.enrich(this, block);
        }
    }

    public Block getBlock() {
        return block;
    }

    public void put(String key, Object value) {
        if (key == null || key.isEmpty()) {
            return;
        }
        values.put(key, value);
    }

    public Object get(String key) {
        return values.get(key);
    }

    public Map<String, Object> getValues() {
        return Collections.unmodifiableMap(values);
    }
}
