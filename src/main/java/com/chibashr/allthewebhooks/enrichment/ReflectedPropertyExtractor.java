package com.chibashr.allthewebhooks.enrichment;

import com.chibashr.allthewebhooks.events.EventContext;
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

/**
 * Reflects on classes to discover getters and extract values for context enrichment.
 * Shared logic for entity field discovery and runtime value extraction.
 */
final class ReflectedPropertyExtractor {

    private static final Set<String> SKIP_PROPERTIES = Set.of("class", "handlers");

    private ReflectedPropertyExtractor() {
    }

    static Map<String, String> buildFieldSpec(Class<?> cls, String prefix) {
        Map<String, String> spec = new LinkedHashMap<>();
        for (Method m : getDiscoverableGetters(cls)) {
            String property = propertyNameFromGetter(m.getName());
            if (property == null || SKIP_PROPERTIES.contains(property)) {
                continue;
            }
            Class<?> returnType = m.getReturnType();
            if (shouldSkipReturnType(returnType)) {
                continue;
            }
            String key = prefix + "." + toSnakeCase(property);
            String type = predicateTypeFor(returnType);
            spec.put(key, type);
        }
        return Map.copyOf(spec);
    }

    static void enrichFromObject(Object source, EventContext ctx, String prefix, Class<?> cls) {
        if (source == null) {
            return;
        }
        for (Method m : getDiscoverableGetters(cls)) {
            String property = propertyNameFromGetter(m.getName());
            if (property == null || SKIP_PROPERTIES.contains(property)) {
                continue;
            }
            Class<?> returnType = m.getReturnType();
            if (shouldSkipReturnType(returnType)) {
                continue;
            }
            Object value;
            try {
                value = m.invoke(source);
            } catch (Exception e) {
                ctx.put(prefix + "." + toSnakeCase(property), "");
                continue;
            }
            Object formatted = formatValue(value);
            ctx.put(prefix + "." + toSnakeCase(property), formatted);
        }
    }

    private static boolean shouldSkipReturnType(Class<?> returnType) {
        if (returnType.isArray()) {
            return true;
        }
        if (java.util.Collection.class.isAssignableFrom(returnType)) {
            return true;
        }
        if (java.util.Map.class.isAssignableFrom(returnType)) {
            return true;
        }
        String name = returnType.getName();
        if (name.startsWith("org.bukkit") || name.startsWith("io.paper")) {
            if (!returnType.equals(String.class)
                    && !returnType.equals(World.class)
                    && !returnType.equals(Location.class)
                    && !returnType.equals(Block.class)
                    && !returnType.equals(Player.class)
                    && !returnType.equals(Entity.class)
                    && !Number.class.isAssignableFrom(returnType)
                    && !returnType.isEnum()) {
                return true;
            }
        }
        return false;
    }

    private static Method[] getDiscoverableGetters(Class<?> cls) {
        return java.util.Arrays.stream(cls.getMethods())
                .filter(m -> m.getDeclaringClass() != Object.class)
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

    private static String toSnakeCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                if (sb.length() > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
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
        if (value instanceof java.io.File f) {
            return f.getName();
        }
        return String.valueOf(value);
    }
}
