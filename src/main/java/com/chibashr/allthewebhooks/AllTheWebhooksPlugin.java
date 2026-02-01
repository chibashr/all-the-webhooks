package com.chibashr.allthewebhooks;

import com.chibashr.allthewebhooks.command.AdminCommand;
import com.chibashr.allthewebhooks.config.ConfigManager;
import com.chibashr.allthewebhooks.docs.DocumentationGenerator;
import com.chibashr.allthewebhooks.events.EventListener;
import com.chibashr.allthewebhooks.events.EventRegistry;
import com.chibashr.allthewebhooks.routing.EventRouter;
import com.chibashr.allthewebhooks.stats.StatsTracker;
import com.chibashr.allthewebhooks.util.WarningTracker;
import com.chibashr.allthewebhooks.webhook.WebhookDispatcher;
import java.io.File;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class AllTheWebhooksPlugin extends JavaPlugin {
    private ConfigManager configManager;
    private EventRegistry eventRegistry;
    private EventRouter eventRouter;
    private WebhookDispatcher webhookDispatcher;
    private DocumentationGenerator documentationGenerator;
    private StatsTracker statsTracker;
    private WarningTracker warningTracker;

    @Override
    public void onEnable() {
        ensureDefaultResources();

        warningTracker = new WarningTracker(getLogger());
        statsTracker = new StatsTracker();
        eventRegistry = EventRegistry.createDefault();
        configManager = new ConfigManager(this, warningTracker, eventRegistry);
        configManager.reloadAll(true);
        eventRegistry.updateFromConfig(configManager.getSnapshot().eventConfig());
        webhookDispatcher = new WebhookDispatcher(this, configManager, statsTracker, warningTracker);
        eventRouter = new EventRouter(configManager, webhookDispatcher, statsTracker, warningTracker);

        getServer().getPluginManager().registerEvents(new EventListener(eventRegistry, eventRouter), this);

        documentationGenerator = new DocumentationGenerator(this, eventRegistry);
        if (configManager.getSnapshot().pluginConfig().documentationGenerateOnStartup()) {
            documentationGenerator.generateAsync();
        }

        registerCommands();
        getLogger().info("All the Webhooks enabled.");
    }

    public void reloadAllTheWebhooks() {
        configManager.reloadAll(false);
        eventRegistry.updateFromConfig(configManager.getSnapshot().eventConfig());
        eventRouter.refresh();
        webhookDispatcher.reset();
        if (configManager.getSnapshot().pluginConfig().documentationGenerateOnReload()) {
            documentationGenerator.generateAsync();
        }
    }

    public StatsTracker getStatsTracker() {
        return statsTracker;
    }

    public DocumentationGenerator getDocumentationGenerator() {
        return documentationGenerator;
    }

    private void registerCommands() {
        PluginCommand command = getCommand("allthewebhooks");
        if (command == null) {
            getLogger().warning("Command registration failed for /allthewebhooks.");
            return;
        }
        command.setExecutor(new AdminCommand(this, configManager));
    }

    private void ensureDefaultResources() {
        saveResourceIfMissing("config.yaml");
        saveResourceIfMissing("events.yaml");
        saveResourceIfMissing("messages.yaml");
        saveResourceIfMissing("examples/events.example.yaml");
    }

    private void saveResourceIfMissing(String path) {
        File target = new File(getDataFolder(), path);
        if (target.exists()) {
            return;
        }
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        saveResource(path, false);
    }
}
