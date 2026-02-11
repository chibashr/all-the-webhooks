package com.chibashr.allthewebhooks.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MessageConfig {
    private final Map<String, String> messages = new HashMap<>();
    private final Map<String, String> usernames = new HashMap<>();

    public void put(String id, String content) {
        if (id == null || id.isEmpty()) {
            return;
        }
        messages.put(id, content == null ? "" : content);
    }

    public void putUsername(String id, String username) {
        if (id == null || id.isEmpty() || username == null || username.isEmpty()) {
            return;
        }
        usernames.put(id, username);
    }

    public boolean hasMessage(String id) {
        return messages.containsKey(id);
    }

    public String getMessage(String id) {
        return messages.get(id);
    }

    /**
     * Optional Discord webhook display name for this message. Null if not set.
     */
    public String getMessageUsername(String id) {
        return usernames.get(id);
    }

    public Map<String, String> getMessages() {
        return Collections.unmodifiableMap(messages);
    }
}
