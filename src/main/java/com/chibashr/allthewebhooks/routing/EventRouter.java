package com.chibashr.allthewebhooks.routing;

import com.chibashr.allthewebhooks.config.ConfigManager;
import com.chibashr.allthewebhooks.config.ConfigurationSnapshot;
import com.chibashr.allthewebhooks.config.PluginConfig;
import com.chibashr.allthewebhooks.config.WebhookDefinition;
import com.chibashr.allthewebhooks.events.EventContext;
import com.chibashr.allthewebhooks.rules.RuleEngine;
import com.chibashr.allthewebhooks.stats.StatsTracker;
import com.chibashr.allthewebhooks.util.MessageResolver;
import com.chibashr.allthewebhooks.util.RedactionPolicy;
import com.chibashr.allthewebhooks.util.WarningTracker;
import com.chibashr.allthewebhooks.webhook.WebhookDispatcher;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class EventRouter {
    private final ConfigManager configManager;
    private final WebhookDispatcher dispatcher;
    private final StatsTracker statsTracker;
    private final WarningTracker warningTracker;
    private final EventRuleResolver resolver = new EventRuleResolver();
    private final RuleEngine ruleEngine = new RuleEngine();

    private volatile RedactionPolicy redactionPolicy;

    public EventRouter(
            ConfigManager configManager,
            WebhookDispatcher dispatcher,
            StatsTracker statsTracker,
            WarningTracker warningTracker
    ) {
        this.configManager = configManager;
        this.dispatcher = dispatcher;
        this.statsTracker = statsTracker;
        this.warningTracker = warningTracker;
        refresh();
    }

    public void refresh() {
        PluginConfig config = configManager.getSnapshot().pluginConfig();
        this.redactionPolicy = new RedactionPolicy(config.isRedactionEnabled(), config.getRedactionFields());
    }

    public void handleEvent(EventContext context) {
        if (context == null) {
            return;
        }

        ConfigurationSnapshot snapshot = configManager.getSnapshot();
        PluginConfig pluginConfig = snapshot.pluginConfig();
        World world = context.getWorld();
        String worldName = world == null ? null : world.getName();

        ResolvedEventRule resolved = resolver.resolve(snapshot.eventConfig(), context.getEventKey(), worldName);
        if (resolved == null || !resolved.isEnabled()) {
            return;
        }

        Player player = context.getPlayer();
        String permission = resolved.getPermission();
        if (permission != null && !permission.isEmpty()) {
            if (player == null || !player.hasPermission(permission)) {
                statsTracker.incrementDropped(context.getEventKey());
                return;
            }
        }

        if (!ruleEngine.evaluate(resolved.getConditions(), context.getValues())) {
            statsTracker.incrementDropped(context.getEventKey());
            return;
        }

        if (!dispatcher.allowDispatch(context.getEventKey(), resolved.getRateLimitEventsPerSecond())) {
            statsTracker.incrementRateLimited(context.getEventKey());
            return;
        }

        String messageId = resolved.getMessage();
        String template = snapshot.messageConfig().getMessage(messageId);
        if (template == null) {
            warningTracker.warnOnce("missing-message-runtime:" + resolved.getMatchedKey(),
                    "Missing message template for " + resolved.getMatchedKey() + ": " + messageId);
            statsTracker.incrementDropped(context.getEventKey());
            return;
        }

        String content = MessageResolver.resolve(template, context.getValues(), redactionPolicy, warningTracker, pluginConfig);
        WebhookDefinition webhook = pluginConfig.getWebhook(resolved.getWebhook());
        if (webhook == null || webhook.url() == null || webhook.url().isEmpty()) {
            warningTracker.warnOnce("missing-webhook:" + resolved.getMatchedKey(),
                    "Missing webhook definition for " + resolved.getMatchedKey() + ": " + resolved.getWebhook());
            statsTracker.incrementDropped(context.getEventKey());
            return;
        }

        dispatcher.dispatch(context.getEventKey(), webhook, content);
    }
}
