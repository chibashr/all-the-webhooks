package com.chibashr.allthewebhooks.command;

import com.chibashr.allthewebhooks.AllTheWebhooksPlugin;
import com.chibashr.allthewebhooks.config.ConfigManager;
import com.chibashr.allthewebhooks.config.PluginConfig;
import com.chibashr.allthewebhooks.events.EventContext;
import com.chibashr.allthewebhooks.events.EventRegistry;
import com.chibashr.allthewebhooks.routing.EventRouter;
import com.chibashr.allthewebhooks.stats.StatsTracker;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminCommand implements CommandExecutor {
    private final AllTheWebhooksPlugin plugin;
    private final ConfigManager configManager;

    public AdminCommand(AllTheWebhooksPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /allthewebhooks <reload|stats|validate|docs generate|fire <eventKey> [key=value ...] [--dry-run]>");
            return true;
        }

        PluginConfig config = configManager.getSnapshot().pluginConfig();
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "fire" -> {
                if (!sender.hasPermission(config.firePermission())) {
                    sender.sendMessage("You do not have permission to fire events.");
                    return true;
                }
                return handleFire(sender, args);
            }
            case "reload" -> {
                if (!sender.hasPermission(config.reloadPermission())) {
                    sender.sendMessage("You do not have permission to reload All the Webhooks.");
                    return true;
                }
                plugin.reloadAllTheWebhooks();
                sender.sendMessage("All the Webhooks reloaded.");
                return true;
            }
            case "stats" -> {
                if (!sender.hasPermission(config.statsPermission())) {
                    sender.sendMessage("You do not have permission to view stats.");
                    return true;
                }
                sendStats(sender, plugin.getStatsTracker());
                return true;
            }
            case "docs" -> {
                if (args.length < 2 || !"generate".equalsIgnoreCase(args[1])) {
                    sender.sendMessage("Usage: /allthewebhooks docs generate");
                    return true;
                }
                if (!sender.hasPermission(config.docsPermission())) {
                    sender.sendMessage("You do not have permission to generate docs.");
                    return true;
                }
                plugin.getDocumentationGenerator().generateAsync();
                sender.sendMessage("Documentation generation started.");
                return true;
            }
            case "validate" -> {
                if (!sender.hasPermission(config.validatePermission())) {
                    sender.sendMessage("You do not have permission to validate config.");
                    return true;
                }
                return handleValidate(sender);
            }
            default -> {
                sender.sendMessage("Unknown subcommand. Use reload, stats, validate, docs generate, or fire.");
                return true;
            }
        }
    }

    private boolean handleValidate(CommandSender sender) {
        sender.sendMessage("Validating config.yaml, messages.yaml, events.yaml...");
        List<String> issues = configManager.runValidation();
        if (issues.isEmpty()) {
            sender.sendMessage("Validation passed. No issues found.");
            return true;
        }
        sender.sendMessage("Validation found " + issues.size() + " issue(s):");
        for (String issue : issues) {
            sender.sendMessage("  " + issue);
        }
        return true;
    }

    private boolean handleFire(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /allthewebhooks fire <eventKey> [key=value ...] [--dry-run]");
            sender.sendMessage("With no key=value: uses server/commanding player data (as the server would fire).");
            sender.sendMessage("Example: /allthewebhooks fire player.join");
            sender.sendMessage("Use --dry-run to report what would happen without dispatching.");
            return true;
        }
        String eventKey = args[1];
        boolean dryRun = false;
        List<String> pairs = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            if ("--dry-run".equalsIgnoreCase(args[i])) {
                dryRun = true;
            } else {
                pairs.add(args[i]);
            }
        }
        EventContext context;
        if (pairs.isEmpty()) {
            EventRegistry registry = plugin.getEventRegistry();
            if (registry == null) {
                sender.sendMessage("[All the Webhooks] Event registry not available.");
                return true;
            }
            context = registry.buildSyntheticContext(
                    eventKey,
                    sender instanceof Player ? (Player) sender : null,
                    plugin.getServer()
            );
        } else {
            context = new EventContext(eventKey);
            if (sender instanceof Player player) {
                context.setPlayer(player);
            }
            for (String pair : pairs) {
                int eq = pair.indexOf('=');
                if (eq > 0) {
                    String key = pair.substring(0, eq).trim();
                    String value = pair.substring(eq + 1).trim();
                    if (!key.isEmpty()) {
                        context.put(key, value);
                    }
                }
            }
            context.put("event.name", eventKey);
        }
        EventRouter router = plugin.getEventRouter();
        if (router == null) {
            sender.sendMessage("[All the Webhooks] Router not available.");
            return true;
        }
        router.handleEventWithReport(context, sender::sendMessage, dryRun);
        return true;
    }

    private void sendStats(CommandSender sender, StatsTracker stats) {
        sender.sendMessage("All the Webhooks stats:");
        sender.sendMessage("Events sent: " + stats.getSent());
        sender.sendMessage("Events dropped: " + stats.getDropped());
        sender.sendMessage("Webhook failures: " + stats.getWebhookFailures());
        sender.sendMessage("Rate limit hits: " + stats.getRateLimited());

        for (Map.Entry<String, LongAdder> entry : stats.getPerEventSent().entrySet()) {
            sender.sendMessage("Sent " + entry.getKey() + ": " + entry.getValue().sum());
        }
        for (Map.Entry<String, LongAdder> entry : stats.getPerEventDropped().entrySet()) {
            sender.sendMessage("Dropped " + entry.getKey() + ": " + entry.getValue().sum());
        }
        for (Map.Entry<String, LongAdder> entry : stats.getPerEventFailures().entrySet()) {
            sender.sendMessage("Failures " + entry.getKey() + ": " + entry.getValue().sum());
        }
        for (Map.Entry<String, LongAdder> entry : stats.getPerEventRateLimited().entrySet()) {
            sender.sendMessage("Rate limited " + entry.getKey() + ": " + entry.getValue().sum());
        }
    }
}
