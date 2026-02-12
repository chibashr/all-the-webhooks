package com.chibashr.allthewebhooks.util;

import com.chibashr.allthewebhooks.config.PluginConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void resolve_regexTransform_extractsWorldFolderName() {
        // Extract second-to-last path segment (e.g. "hardcore-26" from "2026/02/hardcore-26/world")
        String result = MessageResolver.resolve(
                "**{world.name|regex:.*/([^/]+)/[^/]+$:$1}** has been loaded",
                Map.of("world.name", "2026/02/hardcore-26/world"),
                null,
                warningTracker,
                PluginConfig.builder().build()
        );
        assertEquals("**hardcore-26** has been loaded", result);
    }

    @Test
    void resolve_placeholderWithoutPipe_unchangedBehavior() {
        String result = MessageResolver.resolve(
                "Hello {player.name}",
                Map.of("player.name", "Steve"),
                null,
                warningTracker,
                PluginConfig.builder().build()
        );
        assertEquals("Hello Steve", result);
    }

    @Test
    void resolve_redactedWithRegexTransform_redactedNotTransformed() {
        RedactionPolicy policy = new RedactionPolicy(true, java.util.List.of("world.name"));
        String result = MessageResolver.resolve(
                "World {world.name|regex:.*/([^/]+)/[^/]+$:$1}",
                Map.of("world.name", "2026/02/hardcore-26/world"),
                policy,
                warningTracker,
                PluginConfig.builder().build()
        );
        assertEquals("World [REDACTED]", result);
    }

    @Test
    void resolve_missingPlaceholderWithRegex_emptyString() {
        String result = MessageResolver.resolve(
                "X{missing|regex:.*:y}Z",
                Map.of(),
                null,
                warningTracker,
                PluginConfig.builder().build()
        );
        assertEquals("XZ", result);
    }

    @Test
    void resolve_invalidRegex_fallbackToUntransformedValue() {
        String result = MessageResolver.resolve(
                "Val: {key|regex:[invalid:}",
                Map.of("key", "hello"),
                null,
                warningTracker,
                PluginConfig.builder().build()
        );
        assertEquals("Val: hello", result);
    }

    @Test
    void parseRegexSpec_escapedColon_preservedInPatternAndReplacement() {
        MessageResolver.RegexSpec spec = MessageResolver.parseRegexSpec("regex:foo\\:bar:baz\\:qux");
        assertNotNull(spec);
        assertEquals("foo:bar", spec.pattern);
        assertEquals("baz:qux", spec.replacement);
    }

    @Test
    void parseRegexSpec_noColon_returnsNull() {
        assertNull(MessageResolver.parseRegexSpec("regex:noColonHere"));
    }

    @Test
    void parseRegexSpec_notRegexPrefix_returnsNull() {
        assertNull(MessageResolver.parseRegexSpec("other:foo:bar"));
    }

    @Test
    void resolve_mapTransform_trueToHardcore() {
        String result = MessageResolver.resolve(
                "Mode: {world.hardcore|map:true:hardcore:false:normal}",
                Map.of("world.hardcore", "true"),
                null,
                warningTracker,
                PluginConfig.builder().build()
        );
        assertEquals("Mode: hardcore", result);
    }

    @Test
    void resolve_mapTransform_falseToNormal() {
        String result = MessageResolver.resolve(
                "Mode: {world.hardcore|map:true:hardcore:false:normal}",
                Map.of("world.hardcore", "false"),
                null,
                warningTracker,
                PluginConfig.builder().build()
        );
        assertEquals("Mode: normal", result);
    }

    @Test
    void resolve_mapTransform_noMatch_unchanged() {
        String result = MessageResolver.resolve(
                "Mode: {world.hardcore|map:true:hardcore:false:normal}",
                Map.of("world.hardcore", "unknown"),
                null,
                warningTracker,
                PluginConfig.builder().build()
        );
        assertEquals("Mode: unknown", result);
    }

    @Test
    void resolve_chainedRegexTransforms_trueFalseToHardcoreNormal() {
        String result = MessageResolver.resolve(
                "Mode: {key|regex:^true$:hardcore|regex:^false$:normal}",
                Map.of("key", "true"),
                null,
                warningTracker,
                PluginConfig.builder().build()
        );
        assertEquals("Mode: hardcore", result);
    }

    @Test
    void resolve_chainedRegexTransforms_falseToNormal() {
        String result = MessageResolver.resolve(
                "Mode: {key|regex:^true$:hardcore|regex:^false$:normal}",
                Map.of("key", "false"),
                null,
                warningTracker,
                PluginConfig.builder().build()
        );
        assertEquals("Mode: normal", result);
    }

    @Test
    void parseMapSpec_validPairs_returnsList() {
        java.util.List<String> pairs = MessageResolver.parseMapSpec("map:true:hardcore:false:normal");
        assertNotNull(pairs);
        assertEquals(java.util.List.of("true", "hardcore", "false", "normal"), pairs);
    }

    @Test
    void parseMapSpec_oddParts_returnsNull() {
        assertNull(MessageResolver.parseMapSpec("map:true:hardcore:false"));
    }

    @Test
    void parseMapSpec_notMapPrefix_returnsNull() {
        assertNull(MessageResolver.parseMapSpec("regex:foo:bar"));
    }

    @Test
    void splitByUnescapedPipe_singleTransform_noSplit() {
        assertEquals(java.util.List.of("regex:^true$:hardcore"), MessageResolver.splitByUnescapedPipe("regex:^true$:hardcore"));
    }

    @Test
    void splitByUnescapedPipe_twoTransforms_splits() {
        assertEquals(
                java.util.List.of("regex:^true$:hardcore", "regex:^false$:normal"),
                MessageResolver.splitByUnescapedPipe("regex:^true$:hardcore|regex:^false$:normal")
        );
    }

    @Test
    void parseMapSpec_escapedColonInKey_preservesColon() {
        java.util.List<String> pairs = MessageResolver.parseMapSpec("map:foo\\:bar:baz");
        assertNotNull(pairs);
        assertEquals("foo:bar", pairs.get(0));
        assertEquals("baz", pairs.get(1));
    }

    @Test
    void resolve_mapWithEscapedColon_preservesColon() {
        String result = MessageResolver.resolve(
                "Val: {key|map:foo\\:bar:baz}",
                Map.of("key", "foo:bar"),
                null,
                warningTracker,
                PluginConfig.builder().build()
        );
        assertEquals("Val: baz", result);
    }
}
