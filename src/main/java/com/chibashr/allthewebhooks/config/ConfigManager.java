package com.chibashr.allthewebhooks.config;

import com.chibashr.allthewebhooks.events.EventRegistry;
import com.chibashr.allthewebhooks.util.WarningTracker;
import java.io.File;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {
    private final JavaPlugin plugin;
    private final WarningTracker warningTracker;
    private final EventRegistry registry;
    private ConfigurationSnapshot snapshot;

    public ConfigManager(JavaPlugin plugin, WarningTracker warningTracker, EventRegistry registry) {
        this.plugin = plugin;
        this.warningTracker = warningTracker;
        this.registry = registry;
    }

    public void reloadAll(boolean startup) {
        PluginConfig pluginConfig = loadPluginConfig();
        if (pluginConfig.getWebhook("default") == null
                || pluginConfig.getWebhook("default").url() == null
                || pluginConfig.getWebhook("default").url().isEmpty()) {
            warningTracker.warnOnce("missing-default-webhook",
                    "Default webhook is missing or empty in config.yaml.");
        }
        MessageConfig messageConfig = loadMessageConfig(pluginConfig);
        boolean validate = startup ? pluginConfig.validateOnStartup() : pluginConfig.validateOnReload();
        EventConfig eventConfig = loadEventConfig(pluginConfig, messageConfig, validate);
        snapshot = new ConfigurationSnapshot(pluginConfig, messageConfig, eventConfig);
    }

    public ConfigurationSnapshot getSnapshot() {
        return snapshot;
    }

    private PluginConfig loadPluginConfig() {
        File file = new File(plugin.getDataFolder(), "config.yaml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        PluginConfig.Builder builder = PluginConfig.builder();
        builder.validateOnStartup(yaml.getBoolean("plugin.validate-on-startup", true));
        builder.validateOnReload(yaml.getBoolean("plugin.validate-on-reload", true));
        builder.warnOnUnresolvedEvents(yaml.getBoolean("plugin.warn-on-unresolved-events", true));

        ConfigurationSection webhooks = yaml.getConfigurationSection("webhooks");
        if (webhooks != null) {
            for (String key : webhooks.getKeys(false)) {
                ConfigurationSection webhookSection = webhooks.getConfigurationSection(key);
                if (webhookSection == null) {
                    continue;
                }
                String url = webhookSection.getString("url", "");
                int timeout = webhookSection.getInt("timeout-ms", 5000);
                builder.webhook(key, new WebhookDefinition(url, timeout));
            }
        }

        builder.rateLimitEnabled(yaml.getBoolean("rate-limit.enabled", true));
        builder.rateLimitEventsPerSecond(yaml.getInt("rate-limit.events-per-second", 100));
        builder.rateLimitOverflowBehavior(yaml.getString("rate-limit.overflow-behavior", "drop"));
        builder.rateLimitWarnThresholdPercent(yaml.getInt("rate-limit.warn-threshold-percent", 80));

        builder.dispatchAsync(yaml.getBoolean("execution.dispatch-async", true));
        builder.foliaCompatible(yaml.getBoolean("execution.folia-compatible", true));

        builder.redactionEnabled(yaml.getBoolean("redaction.enabled", true));
        builder.redactionFields(yaml.getStringList("redaction.fields"));

        builder.consoleAlerts(yaml.getBoolean("logging.console-alerts", true));
        builder.logInvalidEvents(yaml.getBoolean("logging.log-invalid-events", true));
        builder.logWebhookFailures(yaml.getBoolean("logging.log-webhook-failures", true));
        builder.debugLogging(yaml.getBoolean("logging.debug", false));

        builder.documentationGenerateOnStartup(yaml.getBoolean("documentation.generate-on-startup", true));
        builder.documentationGenerateOnReload(yaml.getBoolean("documentation.generate-on-reload", false));

        builder.statsPermission(yaml.getString("commands.stats.permission", "allthewebhooks.stats"));
        builder.reloadPermission(yaml.getString("commands.reload.permission", "allthewebhooks.reload"));
        builder.docsPermission(yaml.getString("commands.docs.permission", "allthewebhooks.docs"));

        return builder.build();
    }

    private MessageConfig loadMessageConfig(PluginConfig pluginConfig) {
        File file = new File(plugin.getDataFolder(), "messages.yaml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        MessageConfig messageConfig = new MessageConfig();

        ConfigurationSection messages = yaml.getConfigurationSection("messages");
        if (messages != null) {
            for (String key : messages.getKeys(false)) {
                ConfigurationSection section = messages.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                messageConfig.put(key, section.getString("content", ""));
            }
        }

        if (pluginConfig.isDebugLogging()) {
            plugin.getLogger().info("Loaded " + messageConfig.getMessages().size() + " message templates.");
        }
        return messageConfig;
    }

    private EventConfig loadEventConfig(PluginConfig pluginConfig, MessageConfig messageConfig, boolean validate) {
        File file = new File(plugin.getDataFolder(), "events.yaml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        EventRuleDefaults defaults = EventRuleDefaults.fromSection(yaml.getConfigurationSection("defaults"));
        EventConfig eventConfig = new EventConfig(defaults);

        ConfigurationSection eventsSection = yaml.getConfigurationSection("events");
        if (eventsSection != null) {
            for (String key : eventsSection.getKeys(false)) {
                ConfigurationSection ruleSection = eventsSection.getConfigurationSection(key);
                EventRule rule = EventRule.fromSection(ruleSection);
                eventConfig.putEventRule(key, rule);
            }
        }

        ConfigurationSection worldsSection = yaml.getConfigurationSection("worlds");
        if (worldsSection != null) {
            for (String worldName : worldsSection.getKeys(false)) {
                ConfigurationSection worldSection = worldsSection.getConfigurationSection(worldName);
                if (worldSection == null) {
                    continue;
                }
                WorldEventConfig worldConfig = WorldEventConfig.fromSection(worldSection);
                eventConfig.putWorldConfig(worldName, worldConfig);
            }
        }

        if (validate) {
            validateEventConfig(pluginConfig, messageConfig, eventConfig, registry);
        }

        return eventConfig;
    }

    private void validateEventConfig(
            PluginConfig pluginConfig,
            MessageConfig messageConfig,
            EventConfig eventConfig,
            EventRegistry registry
    ) {
        if (!pluginConfig.shouldValidate()) {
            return;
        }

        for (String key : eventConfig.getAllConfiguredKeys()) {
            boolean matches = registry.getDefinitions().stream()
                    .anyMatch(definition -> EventKeyMatcher.matches(key, definition.getKey()));
            if (!matches && pluginConfig.warnOnUnresolvedEvents() && pluginConfig.isLogInvalidEvents()) {
                warningTracker.warnOnce("invalid-event:" + key,
                        "Configured event key does not match any supported event: " + key);
            }
        }

        for (Map.Entry<String, EventRule> entry : eventConfig.getEventRules().entrySet()) {
            String key = entry.getKey();
            EventRule rule = entry.getValue();
            String messageId = rule.getMessage();
            if (messageId != null && !messageId.isEmpty() && !messageConfig.hasMessage(messageId)) {
                warningTracker.warnOnce("missing-message:" + key,
                        "Event " + key + " references missing message id: " + messageId);
            }
        }

        for (Map.Entry<String, WorldEventConfig> entry : eventConfig.getWorldConfigs().entrySet()) {
            String worldName = entry.getKey();
            for (Map.Entry<String, EventRule> ruleEntry : entry.getValue().getEventRules().entrySet()) {
                String key = ruleEntry.getKey();
                String messageId = ruleEntry.getValue().getMessage();
                if (messageId != null && !messageId.isEmpty() && !messageConfig.hasMessage(messageId)) {
                    warningTracker.warnOnce("missing-message:" + worldName + ":" + key,
                            "World " + worldName + " event " + key + " references missing message id: " + messageId);
                }
            }
        }
    }
}
