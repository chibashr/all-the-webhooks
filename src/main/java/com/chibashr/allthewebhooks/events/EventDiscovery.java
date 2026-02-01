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
 * definitions with predicates and context derived from each event class's public getters.
 * Uses reflection (no java.desktop) to discover getX/isX methods and expose them as
 * predicates and context values.
 */
public final class EventDiscovery {

    private EventDiscovery() {
    }

    /**
     * Result of discovering one event type: the event class, its dot-notation key,
     * and an EventDefinition with predicates and context derived from the event's getters.
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
            Map<String, String> predicates = DiscoveredEventBuilder.buildPredicates(eventClass);
            String description = humanizeEventDescription(eventClass);
            EventDefinition definition = new EventDefinition(
                    key,
                    category,
                    description,
                    predicates,
                    List.of(key + ".*"),
                    key + ":\n  message: generic",
                    DiscoveredEventBuilder.buildContextBuilder(eventClass, key)
            );
            result.add(new DiscoveredEvent(eventClass, key, definition));
            addedKeys.add(key);
        }

        return result;
    }

    /**
     * Builds a short human-readable description from an event class name.
     * E.g. PlayerJoinEvent -> "Fired when a player joins.", BlockBreakEvent -> "Fired when a block breaks."
     */
    static String humanizeEventDescription(Class<? extends Event> eventClass) {
        String simple = eventClass.getSimpleName();
        if (simple.endsWith("Event")) {
            simple = simple.substring(0, simple.length() - 5);
        }
        if (simple.isEmpty()) {
            return "Bukkit/Paper event.";
        }
        String phrase = camelToPhrase(simple);
        return "Fired when " + phrase + ".";
    }

    private static String camelToPhrase(String camel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c) && sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toLowerCase(c));
        }
        String words = sb.toString().trim();
        if (words.isEmpty()) {
            return "this event occurs";
        }
        return words;
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
