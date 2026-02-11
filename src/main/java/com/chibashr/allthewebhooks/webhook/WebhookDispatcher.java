package com.chibashr.allthewebhooks.webhook;

import com.chibashr.allthewebhooks.config.ConfigManager;
import com.chibashr.allthewebhooks.config.PluginConfig;
import com.chibashr.allthewebhooks.config.WebhookDefinition;
import com.chibashr.allthewebhooks.stats.StatsTracker;
import com.chibashr.allthewebhooks.util.AsyncExecutor;
import com.chibashr.allthewebhooks.util.WarningTracker;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.plugin.java.JavaPlugin;

public class WebhookDispatcher {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final StatsTracker statsTracker;
    private final WarningTracker warningTracker;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final AsyncExecutor asyncExecutor;
    private final RateLimiter globalLimiter = new RateLimiter(100);
    private final Map<String, RateLimiter> eventLimiters = new ConcurrentHashMap<>();
    private final Set<String> disabledEvents = ConcurrentHashMap.newKeySet();

    public WebhookDispatcher(
            JavaPlugin plugin,
            ConfigManager configManager,
            StatsTracker statsTracker,
            WarningTracker warningTracker
    ) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.statsTracker = statsTracker;
        this.warningTracker = warningTracker;
        this.asyncExecutor = new AsyncExecutor(plugin);
    }

    public boolean allowDispatch(String eventKey, Integer eventLimitOverride) {
        PluginConfig config = configManager.getSnapshot().pluginConfig();
        if (!config.isRateLimitEnabled()) {
            return true;
        }
        if (disabledEvents.contains(eventKey)) {
            return false;
        }

        int globalLimit = Math.max(1, config.getRateLimitEventsPerSecond());
        globalLimiter.setLimit(globalLimit);

        RateLimiter limiter = globalLimiter;
        if (eventLimitOverride != null && eventLimitOverride > 0) {
            limiter = eventLimiters.computeIfAbsent(eventKey, key -> new RateLimiter(eventLimitOverride));
            limiter.setLimit(eventLimitOverride);
        }

        if (limiter.tryAcquire()) {
            return true;
        }

        if ("disable-event".equalsIgnoreCase(config.getRateLimitOverflowBehavior())) {
            disabledEvents.add(eventKey);
            warningTracker.warnOnce("rate-limit-disabled:" + eventKey,
                    "Rate limit exceeded, disabling event until reload: " + eventKey);
        }
        return false;
    }

    public void dispatch(String eventKey, WebhookDefinition webhook, String content, String username) {
        PluginConfig config = configManager.getSnapshot().pluginConfig();
        Runnable task = () -> sendWebhook(eventKey, webhook, content, username, config);
        if (config.isDispatchAsync()) {
            asyncExecutor.runAsync(task);
        } else {
            task.run();
        }
    }

    public void reset() {
        disabledEvents.clear();
        eventLimiters.clear();
    }

    private void sendWebhook(String eventKey, WebhookDefinition webhook, String content, String username, PluginConfig config) {
        try {
            String effectiveUsername = username != null && !username.isEmpty() ? username : webhook.username();
            StringBuilder json = new StringBuilder();
            json.append("{\"content\":\"").append(JsonEscaper.escape(content)).append("\"");
            if (effectiveUsername != null && !effectiveUsername.isEmpty()) {
                json.append(",\"username\":\"").append(JsonEscaper.escape(effectiveUsername)).append("\"");
            }
            json.append("}");
            String payload = json.toString();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhook.url()))
                    .timeout(Duration.ofMillis(webhook.timeoutMs()))
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .header("Content-Type", "application/json")
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .whenComplete((response, throwable) -> {
                        if (throwable != null) {
                            statsTracker.incrementWebhookFailure(eventKey);
                            if (config.isLogWebhookFailures()) {
                                plugin.getLogger().warning("Webhook failure for " + eventKey + ": " + throwable.getMessage());
                            }
                            return;
                        }
                        int status = response.statusCode();
                        if (status < 200 || status >= 300) {
                            statsTracker.incrementWebhookFailure(eventKey);
                            if (config.isLogWebhookFailures()) {
                                plugin.getLogger().warning("Webhook failed for " + eventKey + " with status " + status);
                            }
                        } else {
                            statsTracker.incrementSent(eventKey);
                        }
                    });
        } catch (Exception ex) {
            statsTracker.incrementWebhookFailure(eventKey);
            if (config.isLogWebhookFailures()) {
                plugin.getLogger().warning("Webhook exception for " + eventKey + ": " + ex.getMessage());
            }
        }
    }
}
