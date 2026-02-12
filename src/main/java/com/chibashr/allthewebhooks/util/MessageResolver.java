package com.chibashr.allthewebhooks.util;

import com.chibashr.allthewebhooks.config.PluginConfig;
import java.util.ArrayList;
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
    private static final String REGEX_PREFIX = "regex:";
    private static final String MAP_PREFIX = "map:";
    private static final long REGEX_TIMEOUT_MS = 300;

    private static final ExecutorService REGEX_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "message-resolver-regex");
        t.setDaemon(true);
        return t;
    });

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
                if (value == null) {
                    replacement = "";
                    if (pluginConfig.shouldValidate()) {
                        warningTracker.warnOnce("missing-placeholder:" + key,
                                "Missing placeholder value for {" + key + "}");
                    }
                } else {
                    String raw = String.valueOf(value);
                    replacement = applyTransforms(raw, transformSpec, key, warningTracker);
                }
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
        if (spec.startsWith(REGEX_PREFIX)) {
            return applyRegexTransform(value, spec, key, warningTracker);
        }
        if (spec.startsWith(MAP_PREFIX)) {
            return applyMapTransform(value, spec, key, warningTracker);
        }
        return value;
    }

    private static String applyRegexTransform(String value, String spec, String key, WarningTracker warningTracker) {
        RegexSpec regexSpec = parseRegexSpec(spec);
        if (regexSpec == null) {
            return value;
        }
        try {
            Pattern pattern = Pattern.compile(regexSpec.pattern);
            Callable<String> task = () -> pattern.matcher(value).replaceAll(regexSpec.replacement);
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

    private static String applyMapTransform(String value, String spec, String key, WarningTracker warningTracker) {
        List<String> pairs = parseMapSpec(spec);
        if (pairs == null) {
            return value;
        }
        for (int i = 0; i < pairs.size() - 1; i += 2) {
            if (value.equals(pairs.get(i))) {
                return pairs.get(i + 1);
            }
        }
        return value;
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

    /**
     * Parses "regex:pattern:replacement" with \: escaping in pattern and replacement.
     * Returns null if the spec is invalid (e.g. no unescaped colon to separate pattern and replacement).
     */
    static RegexSpec parseRegexSpec(String transformSpec) {
        if (transformSpec == null || !transformSpec.startsWith(REGEX_PREFIX)) {
            return null;
        }
        String rest = transformSpec.substring(REGEX_PREFIX.length());
        int colonIndex = findFirstUnescapedColon(rest);
        if (colonIndex < 0) {
            return null;
        }
        String pattern = unescape(rest.substring(0, colonIndex));
        String replacement = unescape(rest.substring(colonIndex + 1));
        return new RegexSpec(pattern, replacement);
    }

    /**
     * Parses "map:key1:value1:key2:value2:..." with \: escaping. Returns null if invalid (odd number of parts).
     */
    static List<String> parseMapSpec(String transformSpec) {
        if (transformSpec == null || !transformSpec.startsWith(MAP_PREFIX)) {
            return null;
        }
        String rest = transformSpec.substring(MAP_PREFIX.length());
        List<String> parts = splitByUnescapedColon(rest);
        if (parts.size() % 2 != 0) {
            return null;
        }
        return parts;
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

    static final class RegexSpec {
        final String pattern;
        final String replacement;

        RegexSpec(String pattern, String replacement) {
            this.pattern = pattern;
            this.replacement = replacement;
        }
    }
}
