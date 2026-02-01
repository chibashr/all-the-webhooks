package com.chibashr.allthewebhooks.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginConfig {
    private final boolean validateOnStartup;
    private final boolean validateOnReload;
    private final boolean warnOnUnresolvedEvents;
    private final Map<String, WebhookDefinition> webhooks;
    private final boolean rateLimitEnabled;
    private final int rateLimitEventsPerSecond;
    private final String rateLimitOverflowBehavior;
    private final int rateLimitWarnThresholdPercent;
    private final boolean dispatchAsync;
    private final boolean foliaCompatible;
    private final boolean redactionEnabled;
    private final List<String> redactionFields;
    private final boolean consoleAlerts;
    private final boolean logInvalidEvents;
    private final boolean logWebhookFailures;
    private final boolean debugLogging;
    private final boolean documentationGenerateOnStartup;
    private final boolean documentationGenerateOnReload;
    private final String statsPermission;
    private final String reloadPermission;
    private final String docsPermission;
    private final String firePermission;

    private PluginConfig(Builder builder) {
        this.validateOnStartup = builder.validateOnStartup;
        this.validateOnReload = builder.validateOnReload;
        this.warnOnUnresolvedEvents = builder.warnOnUnresolvedEvents;
        this.webhooks = Collections.unmodifiableMap(new HashMap<>(builder.webhooks));
        this.rateLimitEnabled = builder.rateLimitEnabled;
        this.rateLimitEventsPerSecond = builder.rateLimitEventsPerSecond;
        this.rateLimitOverflowBehavior = builder.rateLimitOverflowBehavior;
        this.rateLimitWarnThresholdPercent = builder.rateLimitWarnThresholdPercent;
        this.dispatchAsync = builder.dispatchAsync;
        this.foliaCompatible = builder.foliaCompatible;
        this.redactionEnabled = builder.redactionEnabled;
        this.redactionFields = builder.redactionFields == null ? List.of() : List.copyOf(builder.redactionFields);
        this.consoleAlerts = builder.consoleAlerts;
        this.logInvalidEvents = builder.logInvalidEvents;
        this.logWebhookFailures = builder.logWebhookFailures;
        this.debugLogging = builder.debugLogging;
        this.documentationGenerateOnStartup = builder.documentationGenerateOnStartup;
        this.documentationGenerateOnReload = builder.documentationGenerateOnReload;
        this.statsPermission = builder.statsPermission;
        this.reloadPermission = builder.reloadPermission;
        this.docsPermission = builder.docsPermission;
        this.firePermission = builder.firePermission;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean shouldValidate() {
        return validateOnStartup || validateOnReload;
    }

    public boolean validateOnStartup() {
        return validateOnStartup;
    }

    public boolean validateOnReload() {
        return validateOnReload;
    }

    public boolean warnOnUnresolvedEvents() {
        return warnOnUnresolvedEvents;
    }

    public Map<String, WebhookDefinition> getWebhooks() {
        return webhooks;
    }

    public WebhookDefinition getWebhook(String name) {
        return webhooks.get(name);
    }

    public boolean isRateLimitEnabled() {
        return rateLimitEnabled;
    }

    public int getRateLimitEventsPerSecond() {
        return rateLimitEventsPerSecond;
    }

    public String getRateLimitOverflowBehavior() {
        return rateLimitOverflowBehavior;
    }

    public int getRateLimitWarnThresholdPercent() {
        return rateLimitWarnThresholdPercent;
    }

    public boolean isDispatchAsync() {
        return dispatchAsync;
    }

    public boolean isFoliaCompatible() {
        return foliaCompatible;
    }

    public boolean isRedactionEnabled() {
        return redactionEnabled;
    }

    public List<String> getRedactionFields() {
        return redactionFields;
    }

    public boolean isConsoleAlerts() {
        return consoleAlerts;
    }

    public boolean isLogInvalidEvents() {
        return logInvalidEvents;
    }

    public boolean isLogWebhookFailures() {
        return logWebhookFailures;
    }

    public boolean isDebugLogging() {
        return debugLogging;
    }

    public boolean documentationGenerateOnStartup() {
        return documentationGenerateOnStartup;
    }

    public boolean documentationGenerateOnReload() {
        return documentationGenerateOnReload;
    }

    public String statsPermission() {
        return statsPermission;
    }

    public String reloadPermission() {
        return reloadPermission;
    }

    public String docsPermission() {
        return docsPermission;
    }

    public String firePermission() {
        return firePermission;
    }

    public static class Builder {
        private boolean validateOnStartup = true;
        private boolean validateOnReload = true;
        private boolean warnOnUnresolvedEvents = true;
        private final Map<String, WebhookDefinition> webhooks = new HashMap<>();
        private boolean rateLimitEnabled = true;
        private int rateLimitEventsPerSecond = 100;
        private String rateLimitOverflowBehavior = "drop";
        private int rateLimitWarnThresholdPercent = 80;
        private boolean dispatchAsync = true;
        private boolean foliaCompatible = true;
        private boolean redactionEnabled = true;
        private List<String> redactionFields = List.of();
        private boolean consoleAlerts = true;
        private boolean logInvalidEvents = true;
        private boolean logWebhookFailures = true;
        private boolean debugLogging = false;
        private boolean documentationGenerateOnStartup = true;
        private boolean documentationGenerateOnReload = false;
        private String statsPermission = "allthewebhooks.stats";
        private String reloadPermission = "allthewebhooks.reload";
        private String docsPermission = "allthewebhooks.docs";
        private String firePermission = "allthewebhooks.fire";

        public Builder validateOnStartup(boolean value) {
            this.validateOnStartup = value;
            return this;
        }

        public Builder validateOnReload(boolean value) {
            this.validateOnReload = value;
            return this;
        }

        public Builder warnOnUnresolvedEvents(boolean value) {
            this.warnOnUnresolvedEvents = value;
            return this;
        }

        public Builder webhook(String name, WebhookDefinition definition) {
            if (name != null && definition != null) {
                webhooks.put(name, definition);
            }
            return this;
        }

        public Builder rateLimitEnabled(boolean value) {
            this.rateLimitEnabled = value;
            return this;
        }

        public Builder rateLimitEventsPerSecond(int value) {
            this.rateLimitEventsPerSecond = value;
            return this;
        }

        public Builder rateLimitOverflowBehavior(String value) {
            this.rateLimitOverflowBehavior = value == null ? "drop" : value;
            return this;
        }

        public Builder rateLimitWarnThresholdPercent(int value) {
            this.rateLimitWarnThresholdPercent = value;
            return this;
        }

        public Builder dispatchAsync(boolean value) {
            this.dispatchAsync = value;
            return this;
        }

        public Builder foliaCompatible(boolean value) {
            this.foliaCompatible = value;
            return this;
        }

        public Builder redactionEnabled(boolean value) {
            this.redactionEnabled = value;
            return this;
        }

        public Builder redactionFields(List<String> fields) {
            this.redactionFields = fields == null ? List.of() : fields;
            return this;
        }

        public Builder consoleAlerts(boolean value) {
            this.consoleAlerts = value;
            return this;
        }

        public Builder logInvalidEvents(boolean value) {
            this.logInvalidEvents = value;
            return this;
        }

        public Builder logWebhookFailures(boolean value) {
            this.logWebhookFailures = value;
            return this;
        }

        public Builder debugLogging(boolean value) {
            this.debugLogging = value;
            return this;
        }

        public Builder documentationGenerateOnStartup(boolean value) {
            this.documentationGenerateOnStartup = value;
            return this;
        }

        public Builder documentationGenerateOnReload(boolean value) {
            this.documentationGenerateOnReload = value;
            return this;
        }

        public Builder statsPermission(String value) {
            this.statsPermission = value == null ? "allthewebhooks.stats" : value;
            return this;
        }

        public Builder reloadPermission(String value) {
            this.reloadPermission = value == null ? "allthewebhooks.reload" : value;
            return this;
        }

        public Builder docsPermission(String value) {
            this.docsPermission = value == null ? "allthewebhooks.docs" : value;
            return this;
        }

        public Builder firePermission(String value) {
            this.firePermission = value == null ? "allthewebhooks.fire" : value;
            return this;
        }

        public PluginConfig build() {
            return new PluginConfig(this);
        }
    }
}
