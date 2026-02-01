package com.chibashr.allthewebhooks.routing;

import com.chibashr.allthewebhooks.config.EventConfig;
import com.chibashr.allthewebhooks.config.EventKeyMatcher;
import com.chibashr.allthewebhooks.config.EventRule;
import com.chibashr.allthewebhooks.config.EventRuleDefaults;
import com.chibashr.allthewebhooks.config.WorldEventConfig;
import java.util.Map;

public class EventRuleResolver {
    public ResolvedEventRule resolve(EventConfig config, String eventKey, String worldName) {
        EventRuleDefaults defaults = config.getDefaults();
        WorldEventConfig worldConfig = worldName == null ? null : config.getWorldConfigs().get(worldName);

        if (worldConfig != null && worldConfig.getEnabled() != null && !worldConfig.getEnabled()) {
            return ResolvedEventRule.disabled(eventKey);
        }

        ResolvedEventRule resolved = resolveFromRules(
                worldConfig == null ? Map.of() : worldConfig.getEventRules(),
                eventKey,
                defaults
        );
        if (resolved != null) {
            return resolved;
        }
        return resolveFromRules(config.getEventRules(), eventKey, defaults);
    }

    private ResolvedEventRule resolveFromRules(
            Map<String, EventRule> rules,
            String eventKey,
            EventRuleDefaults defaults
    ) {
        String bestKey = null;
        EventKeyMatcher.MatchScore bestScore = EventKeyMatcher.MatchScore.NO_MATCH;
        for (String configuredKey : rules.keySet()) {
            EventKeyMatcher.MatchScore score = EventKeyMatcher.score(configuredKey, eventKey);
            if (score.isBetterThan(bestScore)) {
                bestScore = score;
                bestKey = configuredKey;
            }
        }
        if (bestKey == null) {
            return null;
        }
        EventRule rule = rules.get(bestKey);
        boolean enabled = rule.getEnabled() == null ? defaults.isEnabled() : rule.getEnabled();
        String webhook = rule.getWebhook() == null ? defaults.getWebhook() : rule.getWebhook();
        String message = rule.getMessage() == null ? defaults.getMessage() : rule.getMessage();
        String permission = rule.getRequirePermission() == null ? defaults.getRequirePermission() : rule.getRequirePermission();
        return new ResolvedEventRule(
                eventKey,
                bestKey,
                enabled,
                webhook,
                message,
                permission,
                rule.getConditions(),
                rule.getRateLimitEventsPerSecond()
        );
    }
}
