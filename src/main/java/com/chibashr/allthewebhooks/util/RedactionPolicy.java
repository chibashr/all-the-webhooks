package com.chibashr.allthewebhooks.util;

import java.util.ArrayList;
import java.util.List;

public class RedactionPolicy {
    private final boolean enabled;
    private final List<String> patterns;

    public RedactionPolicy(boolean enabled, List<String> patterns) {
        this.enabled = enabled;
        this.patterns = patterns == null ? List.of() : List.copyOf(patterns);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isRedacted(String field) {
        if (!enabled || field == null) {
            return false;
        }
        for (String pattern : patterns) {
            if (matches(pattern, field)) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(String pattern, String value) {
        if (pattern == null) {
            return false;
        }
        String[] patternParts = pattern.split("\\.");
        String[] valueParts = value.split("\\.");
        if (patternParts.length > valueParts.length) {
            return false;
        }
        for (int i = 0; i < patternParts.length; i++) {
            String p = patternParts[i];
            if ("*".equals(p)) {
                continue;
            }
            if (!p.equals(valueParts[i])) {
                return false;
            }
        }
        return true;
    }
}
