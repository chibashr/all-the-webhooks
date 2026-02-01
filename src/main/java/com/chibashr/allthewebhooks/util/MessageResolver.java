package com.chibashr.allthewebhooks.util;

import com.chibashr.allthewebhooks.config.PluginConfig;
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
                    replacement = applyTransform(raw, transformSpec, key, warningTracker);
                }
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * Applies optional regex transform to the value. If transformSpec is null or invalid,
     * returns value unchanged. Uses a timeout to avoid ReDoS; on timeout or exception,
     * returns the original value and optionally logs once.
     */
    static String applyTransform(String value, String transformSpec, String key, WarningTracker warningTracker) {
        if (transformSpec == null || !transformSpec.startsWith(REGEX_PREFIX)) {
            return value;
        }
        RegexSpec spec = parseRegexSpec(transformSpec);
        if (spec == null) {
            return value;
        }
        try {
            Pattern pattern = Pattern.compile(spec.pattern);
            Callable<String> task = () -> pattern.matcher(value).replaceAll(spec.replacement);
            String result = REGEX_EXECUTOR.submit(task).get(REGEX_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            return result;
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
        String pattern = unescapeColons(rest.substring(0, colonIndex));
        String replacement = unescapeColons(rest.substring(colonIndex + 1));
        return new RegexSpec(pattern, replacement);
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

    private static String unescapeColons(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\\' && i + 1 < s.length() && s.charAt(i + 1) == ':') {
                sb.append(':');
                i++;
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
