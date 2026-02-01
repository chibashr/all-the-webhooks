package com.chibashr.allthewebhooks.events;

import com.chibashr.allthewebhooks.util.LocationFormatter;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Builds predicate maps and context builders for discovered event classes by
 * reflecting on public getters (getX / isX). Uses manual reflection instead of
 * java.beans.Introspector to avoid the java.desktop dependency on server runtimes.
 */
final class DiscoveredEventBuilder {

    /** Property names to skip when discovering predicates (e.g. class, handlers). */
    private static final Set<String> SKIP_PROPERTIES = Set.of("class", "handlers");

    private DiscoveredEventBuilder() {
    }

    /**
     * Builds predicate fields (name -> type) for a discovered event: always
     * event.name and event.class, plus event.&lt;property&gt; for each discovered getter.
     */
    static Map<String, String> buildPredicates(Class<? extends Event> eventClass) {
        Map<String, String> predicates = new LinkedHashMap<>();
        predicates.put("event.name", "string");
        predicates.put("event.class", "string");
        for (Method m : getDiscoverableGetters(eventClass)) {
            String property = propertyNameFromGetter(m.getName());
            if (property == null || SKIP_PROPERTIES.contains(property)) {
                continue;
            }
            String type = predicateTypeFor(m.getReturnType());
            predicates.put("event." + property, type);
        }
        return Map.copyOf(predicates);
    }

    /**
     * Builds a context builder that fills EventContext with event.name, event.class,
     * and values from each discovered getter (formatted for conditions/messages).
     */
    @SuppressWarnings("unchecked")
    static EventContextBuilder<Event> buildContextBuilder(
            Class<? extends Event> eventClass,
            String key
    ) {
        Method[] getters = getDiscoverableGetters(eventClass);
        return (Event event) -> {
            EventContext ctx = new EventContext(key);
            ctx.put("event.name", key);
            ctx.put("event.class", event.getClass().getSimpleName());
            for (Method m : getters) {
                String property = propertyNameFromGetter(m.getName());
                if (property == null || SKIP_PROPERTIES.contains(property)) {
                    continue;
                }
                Object value;
                try {
                    value = m.invoke(event);
                } catch (Exception e) {
                    ctx.put("event." + property, "");
                    continue;
                }
                Object formatted = formatValue(value);
                ctx.put("event." + property, formatted);
                if (value instanceof Player player) {
                    ctx.setPlayer(player);
                }
                if (value instanceof World world) {
                    ctx.setWorld(world);
                }
            }
            return ctx;
        };
    }

    private static Method[] getDiscoverableGetters(Class<? extends Event> eventClass) {
        return java.util.Arrays.stream(eventClass.getMethods())
                .filter(m -> m.getParameterCount() == 0)
                .filter(m -> m.getReturnType() != void.class)
                .filter(m -> {
                    String name = m.getName();
                    return (name.startsWith("get") && name.length() > 3)
                            || (name.startsWith("is") && name.length() > 2
                            && (m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class));
                })
                .toArray(Method[]::new);
    }

    private static String propertyNameFromGetter(String methodName) {
        if (methodName.startsWith("get") && methodName.length() > 3) {
            String rest = methodName.substring(3);
            return rest.isEmpty() ? null : Character.toLowerCase(rest.charAt(0)) + rest.substring(1);
        }
        if (methodName.startsWith("is") && methodName.length() > 2) {
            String rest = methodName.substring(2);
            return rest.isEmpty() ? null : Character.toLowerCase(rest.charAt(0)) + rest.substring(1);
        }
        return null;
    }

    private static String predicateTypeFor(Class<?> returnType) {
        if (returnType == String.class || returnType == UUID.class
                || returnType != null && returnType.isEnum()) {
            return "string";
        }
        if (returnType == int.class || returnType == long.class
                || returnType == double.class || returnType == float.class
                || returnType == Number.class || Number.class.isAssignableFrom(returnType)) {
            return "number";
        }
        if (returnType == boolean.class || returnType == Boolean.class) {
            return "boolean";
        }
        return "string";
    }

    private static Object formatValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean
                || value instanceof UUID || value.getClass().isEnum()) {
            return value;
        }
        if (value instanceof Player p) {
            return p.getName();
        }
        if (value instanceof World w) {
            return w.getName();
        }
        if (value instanceof Block b) {
            return b.getType().name() + " " + LocationFormatter.format(b.getLocation());
        }
        if (value instanceof Location loc) {
            return LocationFormatter.format(loc);
        }
        if (value instanceof Entity e) {
            return e.getUniqueId().toString();
        }
        return String.valueOf(value);
    }
}
