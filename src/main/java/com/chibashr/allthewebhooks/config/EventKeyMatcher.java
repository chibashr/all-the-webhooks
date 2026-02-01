package com.chibashr.allthewebhooks.config;

public final class EventKeyMatcher {
    private EventKeyMatcher() {
    }

    public static boolean matches(String configuredKey, String eventKey) {
        if (configuredKey == null || eventKey == null) {
            return false;
        }
        String[] configuredParts = configuredKey.split("\\.");
        String[] eventParts = eventKey.split("\\.");
        if (configuredParts.length > eventParts.length) {
            return false;
        }
        for (int i = 0; i < configuredParts.length; i++) {
            String configured = configuredParts[i];
            if ("*".equals(configured)) {
                continue;
            }
            if (!configured.equals(eventParts[i])) {
                return false;
            }
        }
        return true;
    }

    public static MatchScore score(String configuredKey, String eventKey) {
        if (!matches(configuredKey, eventKey)) {
            return MatchScore.NO_MATCH;
        }
        String[] configuredParts = configuredKey.split("\\.");
        int depth = configuredParts.length;
        int specificity = 0;
        for (String part : configuredParts) {
            if (!"*".equals(part)) {
                specificity++;
            }
        }
        return new MatchScore(depth, specificity);
    }

    public record MatchScore(int depth, int specificity) {
        public static final MatchScore NO_MATCH = new MatchScore(-1, -1);

        public boolean isBetterThan(MatchScore other) {
            if (other == null) {
                return true;
            }
            if (depth != other.depth) {
                return depth > other.depth;
            }
            return specificity > other.specificity;
        }
    }
}
