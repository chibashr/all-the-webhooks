package com.chibashr.allthewebhooks.util;

import com.chibashr.allthewebhooks.config.PluginConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MessageResolver}.
 */
@ExtendWith(MockitoExtension.class)
class MessageResolverTest {

    @Mock
    private WarningTracker warningTracker;

    @Test
    void resolve_nullTemplate_returnsEmpty() {
        String result = MessageResolver.resolve(
                null,
                Map.of(),
                null,
                warningTracker,
                PluginConfig.builder().validateOnStartup(false).validateOnReload(false).build()
        );
        assertEquals("", result);
    }

    @Test
    void resolve_noPlaceholders_returnsTemplateAsIs() {
        String result = MessageResolver.resolve(
                "Hello world",
                Map.of(),
                null,
                warningTracker,
                PluginConfig.builder().build()
        );
        assertEquals("Hello world", result);
    }

    @Test
    void resolve_singlePlaceholder_replaced() {
        String result = MessageResolver.resolve(
                "Player {player.name} joined",
                Map.of("player.name", "Steve"),
                null,
                warningTracker,
                PluginConfig.builder().build()
        );
        assertEquals("Player Steve joined", result);
    }

    @Test
    void resolve_multiplePlaceholders_replaced() {
        String result = MessageResolver.resolve(
                "{player.name} took {damage.amount} damage",
                Map.of("player.name", "Alex", "damage.amount", 5),
                null,
                warningTracker,
                PluginConfig.builder().build()
        );
        assertEquals("Alex took 5 damage", result);
    }

    @Test
    void resolve_missingPlaceholder_replacedWithEmpty() {
        String result = MessageResolver.resolve(
                "Hello {missing}",
                Map.of(),
                null,
                warningTracker,
                PluginConfig.builder().build()
        );
        assertEquals("Hello ", result);
    }

    @Test
    void resolve_missingPlaceholder_validationOn_warnsOnce() {
        PluginConfig config = PluginConfig.builder().validateOnStartup(true).build();
        MessageResolver.resolve("Hello {missing}", Map.of(), null, warningTracker, config);
        verify(warningTracker).warnOnce(anyString(), anyString());
    }

    @Test
    void resolve_missingPlaceholder_validationOff_noWarn() {
        PluginConfig config = PluginConfig.builder().validateOnStartup(false).validateOnReload(false).build();
        MessageResolver.resolve("Hello {missing}", Map.of(), null, warningTracker, config);
        verify(warningTracker, never()).warnOnce(anyString(), anyString());
    }

    @Test
    void resolve_redactedPlaceholder_replacedWithRedacted() {
        RedactionPolicy policy = new RedactionPolicy(true, java.util.List.of("player.name"));
        String result = MessageResolver.resolve(
                "Player {player.name} did something",
                Map.of("player.name", "SecretName"),
                policy,
                warningTracker,
                PluginConfig.builder().build()
        );
        assertEquals("Player [REDACTED] did something", result);
    }
}
