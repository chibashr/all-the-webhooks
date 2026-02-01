package com.chibashr.allthewebhooks.util;

import com.chibashr.allthewebhooks.config.PluginConfig;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageResolver {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)\\}");

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
            String replacement;
            if (redactionPolicy != null && redactionPolicy.isRedacted(placeholder)) {
                replacement = "[REDACTED]";
            } else {
                Object value = context.get(placeholder);
                if (value == null) {
                    replacement = "";
                    if (pluginConfig.shouldValidate()) {
                        warningTracker.warnOnce("missing-placeholder:" + placeholder,
                                "Missing placeholder value for {" + placeholder + "}");
                    }
                } else {
                    replacement = String.valueOf(value);
                }
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
