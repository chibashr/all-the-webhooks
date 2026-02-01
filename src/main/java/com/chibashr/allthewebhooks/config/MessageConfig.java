package com.chibashr.allthewebhooks.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MessageConfig {
    private final Map<String, String> messages = new HashMap<>();

    public void put(String id, String content) {
        if (id == null || id.isEmpty()) {
            return;
        }
        messages.put(id, content == null ? "" : content);
    }

    public boolean hasMessage(String id) {
        return messages.containsKey(id);
    }

    public String getMessage(String id) {
        return messages.get(id);
    }

    public Map<String, String> getMessages() {
        return Collections.unmodifiableMap(messages);
    }
}
