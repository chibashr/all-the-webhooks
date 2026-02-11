package com.chibashr.allthewebhooks;

import com.chibashr.allthewebhooks.command.AdminCommand;
import com.chibashr.allthewebhooks.config.ConfigManager;
import com.chibashr.allthewebhooks.docs.DocumentationGenerator;
import com.chibashr.allthewebhooks.events.EventContext;
import com.chibashr.allthewebhooks.events.EventDiscovery;
import com.chibashr.allthewebhooks.events.EventListener;
import com.chibashr.allthewebhooks.events.EventRegistry;
import com.chibashr.allthewebhooks.events.SubEventDiscovery;
import com.chibashr.allthewebhooks.routing.EventRouter;
import com.chibashr.allthewebhooks.stats.StatsTracker;
import com.chibashr.allthewebhooks.util.WarningTracker;
import com.chibashr.allthewebhooks.webhook.WebhookDispatcher;
import java.io.File;
import java.util.List;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
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
        List<EventDiscovery.DiscoveredEvent> discovered = EventDiscovery.discover(
                getClass().getClassLoader(),
                eventRegistry.getBaseDefinitionKeys(),
                EventListener.getHandledEventClasses()
        );
        for (EventDiscovery.DiscoveredEvent d : discovered) {
            eventRegistry.addDiscoveredDefinition(d.definition());
        }
        SubEventDiscovery.discover(getServer(), eventRegistry, getLogger());
        configManager = new ConfigManager(this, warningTracker, eventRegistry);
        configManager.reloadAll(true);
        eventRegistry.updateFromConfig(configManager.getSnapshot().eventConfig());
        webhookDispatcher = new WebhookDispatcher(this, configManager, statsTracker, warningTracker);
        eventRouter = new EventRouter(configManager, webhookDispatcher, statsTracker, warningTracker);

        getServer().getPluginManager().registerEvents(new EventListener(eventRegistry, eventRouter), this);

        Listener discoveredListener = new Listener() {};
        PluginManager pm = getServer().getPluginManager();
        for (EventDiscovery.DiscoveredEvent d : discovered) {
            pm.registerEvent(
                    d.eventClass(),
                    discoveredListener,
                    EventPriority.MONITOR,
                    (listener, event) -> {
                        EventContext ctx = eventRegistry.buildContext(d.key(), (Event) event);
                        if (ctx != null) {
                            eventRouter.handleEvent(ctx);
                        }
                    },
                    this
            );
        }

        documentationGenerator = new DocumentationGenerator(this, eventRegistry);
        if (configManager.getSnapshot().pluginConfig().documentationGenerateOnStartup()) {
            documentationGenerator.generateAsync();
        }

        registerCommands();
        fireServerEnable();
        fireCatchUpEvents();
        getLogger().info("All the Webhooks enabled.");
    }

    @Override
    public void onDisable() {
        fireServerDisable();
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

    public EventRouter getEventRouter() {
        return eventRouter;
    }

    public EventRegistry getEventRegistry() {
        return eventRegistry;
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

    private void fireServerEnable() {
        EventContext context = new EventContext("server.enable");
        context.put("server.version", getServer().getVersion());
        context.put("server.minecraft_version", getServer().getBukkitVersion());
        context.put("server.name", getServer().getName());
        eventRouter.handleEvent(context);
    }

    /**
     * Emits events for state that already existed when the plugin enabled, so nothing is missed
     * because the plugin loads after the world (and after some players have already joined).
     * <ul>
     *   <li>world.load — for each world in server.getWorlds() (WorldLoadEvent does not fire for
     *       worlds loaded before the plugin).</li>
     *   <li>player.join — for each player in server.getOnlinePlayers() (PlayerJoinEvent did not
     *       fire for this plugin for players already online).</li>
     * </ul>
     */
    private void fireCatchUpEvents() {
        for (World world : getServer().getWorlds()) {
            EventContext context = eventRegistry.buildContextForWorldLoad(world);
            if (context != null) {
                eventRouter.handleEvent(context);
            }
        }
        for (Player player : getServer().getOnlinePlayers()) {
            EventContext context = eventRegistry.buildContextForPlayerJoin(player);
            if (context != null) {
                eventRouter.handleEvent(context);
            }
        }
    }

    private void fireServerDisable() {
        if (eventRouter == null) {
            return;
        }
        EventContext context = new EventContext("server.disable");
        context.put("server.version", getServer().getVersion());
        context.put("server.minecraft_version", getServer().getBukkitVersion());
        context.put("server.name", getServer().getName());
        context.put("server.reason", "shutdown");
        eventRouter.handleEvent(context);
    }
}
