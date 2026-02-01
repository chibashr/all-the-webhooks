package com.chibashr.allthewebhooks.config;

public record ConfigurationSnapshot(
        PluginConfig pluginConfig,
        MessageConfig messageConfig,
        EventConfig eventConfig
) {
}
