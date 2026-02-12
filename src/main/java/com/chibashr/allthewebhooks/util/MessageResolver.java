package com.chibashr.allthewebhooks.util;

import com.chibashr.allthewebhooks.config.PluginConfig;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageResolver {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)\\}");
    private static final long REGEX_TIMEOUT_MS = 300;

    private static final ExecutorService REGEX_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "message-resolver-regex");
        t.setDaemon(true);
        return t;
    });

    /** Interface for transforms: apply(value, args) -> result. */
    @FunctionalInterface
    interface ValueTransform {
        String apply(String value, List<String> args);
    }

    private static final Map<String, ValueTransform> TRANSFORM_REGISTRY = new LinkedHashMap<>();

    static {
        TRANSFORM_REGISTRY.put("trim", (v, a) -> v.trim());
        TRANSFORM_REGISTRY.put("lower", (v, a) -> v.toLowerCase());
        TRANSFORM_REGISTRY.put("upper", (v, a) -> v.toUpperCase());
        TRANSFORM_REGISTRY.put("default", (v, a) -> (v == null || v.isEmpty()) && !a.isEmpty() ? a.get(0) : v);
        TRANSFORM_REGISTRY.put("truncate", (v, a) -> {
            if (a.isEmpty()) return v;
            try {
                int max = Integer.parseInt(a.get(0));
                if (max < 0) return v;
                return v.length() <= max ? v : v.substring(0, max);
            } catch (NumberFormatException e) {
                return v;
            }
        });
        TRANSFORM_REGISTRY.put("replace", (v, a) -> {
            if (a.size() < 2) return v;
            return v.replace(a.get(0), a.get(1));
        });
        TRANSFORM_REGISTRY.put("last-path-segment", (v, a) -> {
            if (v == null || v.isEmpty()) return v;
            int last = v.lastIndexOf('/');
            return last < 0 ? v : v.substring(last + 1);
        });
        TRANSFORM_REGISTRY.put("first-path-segment", (v, a) -> {
            if (v == null || v.isEmpty()) return v;
            int first = v.indexOf('/');
            return first < 0 ? v : v.substring(0, first);
        });
        TRANSFORM_REGISTRY.put("map", (v, a) -> {
            if (a.size() % 2 != 0) return v;
            for (int i = 0; i < a.size() - 1; i += 2) {
                if (v.equals(a.get(i))) return a.get(i + 1);
            }
            return v;
        });
    }

    private MessageResolver() {
    }

    public static String resolve(
            String template,
            Map<String, Object> context,
            RedactionPolicy redactionPolicy,
            WarningTracker warningTracker,
            PluginConfig pluginConfig
    ) {
        if (template == null) {
            return "";
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String key;
            String transformSpec;
            int pipe = placeholder.indexOf('|');
            if (pipe < 0) {
                key = placeholder;
                transformSpec = null;
            } else {
                key = placeholder.substring(0, pipe).trim();
                transformSpec = placeholder.substring(pipe + 1);
            }
            String replacement;
            if (redactionPolicy != null && redactionPolicy.isRedacted(key)) {
                replacement = "[REDACTED]";
            } else {
                Object value = context.get(key);
                if (value == null && pluginConfig.shouldValidate()) {
                    warningTracker.warnOnce("missing-placeholder:" + key,
                            "Missing placeholder value for {" + key + "}");
                }
                String raw = value == null ? "" : String.valueOf(value);
                replacement = applyTransforms(raw, transformSpec, key, warningTracker);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * Applies optional transforms to the value. Transforms are chained with <code>|</code>;
     * use <code>\|</code> for a literal pipe inside a transform. If transformSpec is null or
     * invalid, returns value unchanged.
     */
    static String applyTransforms(String value, String transformSpec, String key, WarningTracker warningTracker) {
        if (transformSpec == null || transformSpec.isEmpty()) {
            return value;
        }
        List<String> specs = splitByUnescapedPipe(transformSpec);
        String current = value;
        for (String spec : specs) {
            String trimmed = spec.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            current = applySingleTransform(current, trimmed, key, warningTracker);
        }
        return current;
    }

    private static String applySingleTransform(String value, String spec, String key, WarningTracker warningTracker) {
        TransformSpec parsed = parseTransformSpec(spec);
        if (parsed == null) {
            return value;
        }
        if ("regex".equals(parsed.name)) {
            return applyRegexTransform(value, parsed.args, key, warningTracker);
        }
        ValueTransform transform = TRANSFORM_REGISTRY.get(parsed.name);
        if (transform == null) {
            return value;
        }
        try {
            return transform.apply(value, parsed.args);
        } catch (Exception e) {
            if (warningTracker != null) {
                warningTracker.warnOnce("placeholder-transform-error:" + key + ":" + parsed.name,
                        "Transform " + parsed.name + " failed for placeholder {" + key + "}: " + e.getMessage() + ", using untransformed value");
            }
            return value;
        }
    }

    private static String applyRegexTransform(String value, List<String> args, String key, WarningTracker warningTracker) {
        if (args.size() < 2) {
            return value;
        }
        String pattern = args.get(0);
        String replacement = args.get(1);
        try {
            Pattern p = Pattern.compile(pattern);
            Callable<String> task = () -> p.matcher(value).replaceAll(replacement);
            return REGEX_EXECUTOR.submit(task).get(REGEX_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            if (warningTracker != null) {
                warningTracker.warnOnce("placeholder-regex-timeout:" + key,
                        "Regex transform timed out for placeholder {" + key + "}, using untransformed value");
            }
            return value;
        } catch (Exception e) {
            if (warningTracker != null) {
                warningTracker.warnOnce("placeholder-regex-error:" + key,
                        "Regex transform failed for placeholder {" + key + "}: " + e.getMessage() + ", using untransformed value");
            }
            return value;
        }
    }

    /** Parsed transform: name and args. For regex, args = [pattern, replacement]. */
    static final class TransformSpec {
        final String name;
        final List<String> args;

        TransformSpec(String name, List<String> args) {
            this.name = name;
            this.args = args;
        }
    }

    /**
     * Parses "name" or "name:arg1:arg2:..." into TransformSpec. First unescaped colon separates name from args;
     * args are split by unescaped colon. Use \: for literal colon.
     */
    static TransformSpec parseTransformSpec(String spec) {
        if (spec == null || spec.isEmpty()) {
            return null;
        }
        int colonIndex = findFirstUnescapedColon(spec);
        if (colonIndex < 0) {
            return new TransformSpec(spec.trim(), List.of());
        }
        String name = unescape(spec.substring(0, colonIndex)).trim();
        String rest = spec.substring(colonIndex + 1);
        return new TransformSpec(name, splitByUnescapedColon(rest));
    }

    /**
     * Splits by unescaped <code>|</code>. Use <code>\|</code> for literal pipe.
     * Only unescapes <code>\|</code> in the result; <code>\:</code> is left for each transform to handle.
     */
    static List<String> splitByUnescapedPipe(String s) {
        List<String> result = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\\' && i + 1 < s.length()) {
                i++;
                continue;
            }
            if (s.charAt(i) == '|') {
                result.add(unescapePipeOnly(s.substring(start, i)));
                start = i + 1;
            }
        }
        result.add(unescapePipeOnly(s.substring(start)));
        return result;
    }

    /** Unescapes only <code>\|</code> to <code>|</code>; leaves <code>\:</code> unchanged. */
    private static String unescapePipeOnly(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\\' && i + 1 < s.length() && s.charAt(i + 1) == '|') {
                sb.append('|');
                i++;
            } else {
                sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }

    private static List<String> splitByUnescapedColon(String s) {
        List<String> result = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\\' && i + 1 < s.length()) {
                i++;
                continue;
            }
            if (s.charAt(i) == ':') {
                result.add(unescape(s.substring(start, i)));
                start = i + 1;
            }
        }
        result.add(unescape(s.substring(start)));
        return result;
    }

    private static int findFirstUnescapedColon(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\\' && i + 1 < s.length()) {
                i++;
                continue;
            }
            if (s.charAt(i) == ':') {
                return i;
            }
        }
        return -1;
    }

    /** Unescapes \: and \| to : and |. */
    private static String unescape(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                if (next == ':' || next == '|') {
                    sb.append(next);
                    i++;
                } else {
                    sb.append(s.charAt(i));
                }
            } else {
                sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }

}
