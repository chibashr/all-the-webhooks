package com.chibashr.allthewebhooks.events;

import org.bukkit.event.Event;

@FunctionalInterface
public interface EventContextBuilder<E extends Event> {
    EventContext build(E event);
}
