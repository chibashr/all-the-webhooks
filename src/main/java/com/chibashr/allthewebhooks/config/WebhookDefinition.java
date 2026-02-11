package com.chibashr.allthewebhooks.config;

/**
 * Definition of a webhook endpoint (e.g. Discord).
 * Optional {@code username} is sent as the display name for the webhook message when set.
 */
public record WebhookDefinition(String url, int timeoutMs, String username) {

    public WebhookDefinition(String url, int timeoutMs) {
        this(url, timeoutMs, null);
    }
}
