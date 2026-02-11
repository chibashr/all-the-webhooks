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
import java.util.function.Consumer;
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
        handleEventWithReport(context, null, false);
    }

    /**
     * Handles an event and optionally reports each step to the given consumer.
     * When report is non-null, messages describe rule match, checks, and outcome.
     * When dryRun is true, no webhook is dispatched but reporting still occurs.
     */
    public void handleEventWithReport(EventContext context, Consumer<String> report, boolean dryRun) {
        if (context == null) {
            if (report != null) {
                report.accept("[All the Webhooks] Fire: context is null.");
            }
            return;
        }

        ConfigurationSnapshot snapshot = configManager.getSnapshot();
        PluginConfig pluginConfig = snapshot.pluginConfig();
        World world = context.getWorld();
        String worldName = world == null ? null : world.getName();

        if (report != null) {
            report.accept("[All the Webhooks] Fire: eventKey=" + context.getEventKey()
                    + (worldName != null ? " world=" + worldName : ""));
        }

        ResolvedEventRule resolved = resolver.resolve(snapshot.eventConfig(), context.getEventKey(), worldName);
        if (resolved == null) {
            if (report != null) {
                report.accept("[All the Webhooks] No rule matched for " + context.getEventKey() + ".");
            }
            return;
        }
        if (report != null) {
            report.accept("[All the Webhooks] Rule matched: " + resolved.getMatchedKey()
                    + " (webhook=" + resolved.getWebhook() + " message=" + resolved.getMessage() + ").");
        }
        if (!resolved.isEnabled()) {
            if (report != null) {
                report.accept("[All the Webhooks] Rule is disabled; event not fired.");
            }
            return;
        }

        Player player = context.getPlayer();
        String permission = resolved.getPermission();
        if (permission != null && !permission.isEmpty()) {
            if (player == null || !player.hasPermission(permission)) {
                if (report != null) {
                    report.accept("[All the Webhooks] Permission check failed: " + permission + " (player="
                            + (player == null ? "null" : player.getName()) + ").");
                }
                statsTracker.incrementDropped(context.getEventKey());
                return;
            }
        }

        if (!ruleEngine.evaluate(resolved.getConditions(), context.getValues())) {
            if (report != null) {
                report.accept("[All the Webhooks] Conditions did not match; event not fired.");
            }
            statsTracker.incrementDropped(context.getEventKey());
            return;
        }

        if (!dispatcher.allowDispatch(context.getEventKey(), resolved.getRateLimitEventsPerSecond())) {
            if (report != null) {
                report.accept("[All the Webhooks] Rate limited; event not fired.");
            }
            statsTracker.incrementRateLimited(context.getEventKey());
            return;
        }

        String messageId = resolved.getMessage();
        String template = snapshot.messageConfig().getMessage(messageId);
        if (template == null) {
            if (report != null) {
                report.accept("[All the Webhooks] Missing message template: " + messageId + ".");
            }
            warningTracker.warnOnce("missing-message-runtime:" + resolved.getMatchedKey(),
                    "Missing message template for " + resolved.getMatchedKey() + ": " + messageId);
            statsTracker.incrementDropped(context.getEventKey());
            return;
        }

        String content = MessageResolver.resolve(template, context.getValues(), redactionPolicy, warningTracker, pluginConfig);
        if (report != null) {
            report.accept("[All the Webhooks] Message resolved (template=" + messageId + ").");
        }

        WebhookDefinition webhook = pluginConfig.getWebhook(resolved.getWebhook());
        if (webhook == null || webhook.url() == null || webhook.url().isEmpty()) {
            if (report != null) {
                report.accept("[All the Webhooks] Missing webhook: " + resolved.getWebhook() + ".");
            }
            warningTracker.warnOnce("missing-webhook:" + resolved.getMatchedKey(),
                    "Missing webhook definition for " + resolved.getMatchedKey() + ": " + resolved.getWebhook());
            statsTracker.incrementDropped(context.getEventKey());
            return;
        }

        String effectiveUsername = snapshot.messageConfig().getMessageUsername(messageId);
        if (effectiveUsername == null) {
            effectiveUsername = resolved.getWebhookUsername();
        }

        if (dryRun) {
            if (report != null) {
                report.accept("[All the Webhooks] Dry run: would dispatch to webhook " + resolved.getWebhook() + " (not sent).");
            }
            return;
        }

        dispatcher.dispatch(context.getEventKey(), webhook, content, effectiveUsername);
        if (report != null) {
            report.accept("[All the Webhooks] Dispatched to webhook " + resolved.getWebhook() + ".");
        }
    }
}
