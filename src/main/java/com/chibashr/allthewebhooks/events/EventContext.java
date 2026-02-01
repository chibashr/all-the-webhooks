package com.chibashr.allthewebhooks.events;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.world.World;

public class EventContext {
    private final String eventKey;
    private final Map<String, Object> values = new HashMap<>();
    private Player player;
    private World world;

    public EventContext(String eventKey) {
        this.eventKey = eventKey;
        put("event.name", eventKey);
    }

    public String getEventKey() {
        return eventKey;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public World getWorld() {
        return world;
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
