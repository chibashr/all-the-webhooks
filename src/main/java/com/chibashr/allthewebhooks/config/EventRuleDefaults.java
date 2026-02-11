package com.chibashr.allthewebhooks.config;

import org.bukkit.configuration.ConfigurationSection;

public class EventRuleDefaults {
    private final boolean enabled;
    private final String webhook;
    private final String webhookUsername;
    private final String message;
    private final String requirePermission;

    public EventRuleDefaults(boolean enabled, String webhook, String webhookUsername, String message, String requirePermission) {
        this.enabled = enabled;
        this.webhook = webhook;
        this.webhookUsername = webhookUsername;
        this.message = message;
        this.requirePermission = requirePermission;
    }

    public static EventRuleDefaults fromSection(ConfigurationSection section) {
        if (section == null) {
            return new EventRuleDefaults(true, "default", null, "generic", null);
        }
        String webhookUsername = section.getString("webhook-username", null);
        if (webhookUsername != null && webhookUsername.isEmpty()) {
            webhookUsername = null;
        }
        return new EventRuleDefaults(
                section.getBoolean("enabled", true),
                section.getString("webhook", "default"),
                webhookUsername,
                section.getString("message", "generic"),
                section.getString("require-permission", null)
        );
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getWebhook() {
        return webhook;
    }

    public String getWebhookUsername() {
        return webhookUsername;
    }

    public String getMessage() {
        return message;
    }

    public String getRequirePermission() {
        return requirePermission;
    }
}
