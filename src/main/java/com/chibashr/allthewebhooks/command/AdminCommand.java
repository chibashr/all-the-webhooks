package com.chibashr.allthewebhooks.command;

import com.chibashr.allthewebhooks.AllTheWebhooksPlugin;
import com.chibashr.allthewebhooks.config.ConfigManager;
import com.chibashr.allthewebhooks.config.PluginConfig;
import com.chibashr.allthewebhooks.stats.StatsTracker;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

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
            sender.sendMessage("Usage: /allthewebhooks <reload|stats|docs generate>");
            return true;
        }

        PluginConfig config = configManager.getSnapshot().pluginConfig();
        String sub = args[0].toLowerCase();
        switch (sub) {
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
            default -> {
                sender.sendMessage("Unknown subcommand. Use reload, stats, or docs generate.");
                return true;
            }
        }
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
