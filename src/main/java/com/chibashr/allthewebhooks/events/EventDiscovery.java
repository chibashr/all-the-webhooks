package com.chibashr.allthewebhooks.events;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.event.Event;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

/**
 * Discovers Bukkit/Paper event classes on the classpath and produces event keys and
 * minimal definitions so the plugin can offer webhooks for all events.
 */
public final class EventDiscovery {

    private static final Map<String, String> DISCOVERED_PREDICATES = Map.of(
            "event.name", "string",
            "event.class", "string"
    );

    private EventDiscovery() {
    }

    /**
     * Result of discovering one event type: the event class, its dot-notation key,
     * and a minimal EventDefinition with a generic context builder.
     */
    public record DiscoveredEvent(
            Class<? extends Event> eventClass,
            String key,
            EventDefinition definition
    ) {
    }

    /**
     * Scans org.bukkit.event and io.papermc.paper.event for concrete Event subclasses,
     * computes an event key for each, and returns definitions only for events not
     * already covered by base definitions or by the dedicated listener.
     *
     * @param classLoader     classloader that can load Bukkit/Paper event classes
     * @param existingKeys    keys already defined (e.g. base definitions); these are skipped
     * @param handledClasses  event classes already handled by EventListener; these are skipped
     * @return list of discovered event class, key, and definition (no duplicates by key)
     */
    public static List<DiscoveredEvent> discover(
            ClassLoader classLoader,
            Set<String> existingKeys,
            Set<Class<? extends Event>> handledClasses
    ) {
        List<DiscoveredEvent> result = new ArrayList<>();
        Set<String> addedKeys = new java.util.HashSet<>();

        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .setClassLoaders(new ClassLoader[]{classLoader})
                        .forPackages("org.bukkit.event", "io.papermc.paper.event")
        );

        @SuppressWarnings("unchecked")
        Set<Class<? extends Event>> eventClasses = reflections.getSubTypesOf(Event.class);

        for (Class<? extends Event> eventClass : eventClasses) {
            if (eventClass == Event.class) {
                continue;
            }
            if (Modifier.isAbstract(eventClass.getModifiers())) {
                continue;
            }
            String key = eventKeyFromClass(eventClass);
            if (existingKeys.contains(key) || addedKeys.contains(key)) {
                continue;
            }
            if (handledClasses != null && handledClasses.contains(eventClass)) {
                continue;
            }
            String category = key.contains(".") ? key.substring(0, key.indexOf('.')) : "event";
            EventDefinition definition = new EventDefinition(
                    key,
                    category,
                    "Discovered Bukkit/Paper event.",
                    DISCOVERED_PREDICATES,
                    List.of(key + ".*"),
                    key + ":\n  message: generic",
                    (Event e) -> {
                        EventContext ctx = new EventContext(key);
                        ctx.put("event.name", key);
                        ctx.put("event.class", e.getClass().getSimpleName());
                        return ctx;
                    }
            );
            result.add(new DiscoveredEvent(eventClass, key, definition));
            addedKeys.add(key);
        }

        return result;
    }

    /**
     * Computes a dot-notation event key from an Event class: simple name with "Event"
     * stripped, camelCase split into lowercase segments, joined by dots.
     * E.g. PlayerJoinEvent -> player.join, BlockBreakEvent -> block.break.
     */
    public static String eventKeyFromClass(Class<? extends Event> eventClass) {
        String simple = eventClass.getSimpleName();
        if (simple.endsWith("Event")) {
            simple = simple.substring(0, simple.length() - 5);
        }
        if (simple.isEmpty()) {
            return "event";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < simple.length(); i++) {
            char c = simple.charAt(i);
            if (Character.isUpperCase(c) && sb.length() > 0) {
                sb.append('.');
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }
}
