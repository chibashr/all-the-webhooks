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
    void resolve_missingPlaceholderWithRegex_appliedToEmpty() {
        String result = MessageResolver.resolve(
                "X{missing|regex:.*:y}Z",
                Map.of(),
                null,
                warningTracker,
                PluginConfig.builder().build()
        );
        assertEquals("XyZ", result);
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
    void parseTransformSpec_escapedColon_preservedInRegexArgs() {
        MessageResolver.TransformSpec spec = MessageResolver.parseTransformSpec("regex:foo\\:bar:baz\\:qux");
        assertNotNull(spec);
        assertEquals("regex", spec.name);
        assertEquals(java.util.List.of("foo:bar", "baz:qux"), spec.args);
    }

    @Test
    void parseTransformSpec_noArgs_nameOnly() {
        MessageResolver.TransformSpec spec = MessageResolver.parseTransformSpec("trim");
        assertNotNull(spec);
        assertEquals("trim", spec.name);
        assertEquals(java.util.List.of(), spec.args);
    }

    @Test
    void parseTransformSpec_otherName_parsed() {
        MessageResolver.TransformSpec spec = MessageResolver.parseTransformSpec("other:foo:bar");
        assertNotNull(spec);
        assertEquals("other", spec.name);
        assertEquals(java.util.List.of("foo", "bar"), spec.args);
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
    void parseTransformSpec_mapValidArgs() {
        MessageResolver.TransformSpec spec = MessageResolver.parseTransformSpec("map:true:hardcore:false:normal");
        assertNotNull(spec);
        assertEquals("map", spec.name);
        assertEquals(java.util.List.of("true", "hardcore", "false", "normal"), spec.args);
    }

    @Test
    void parseTransformSpec_regexArgs() {
        MessageResolver.TransformSpec spec = MessageResolver.parseTransformSpec("regex:foo:bar");
        assertNotNull(spec);
        assertEquals("regex", spec.name);
        assertEquals(java.util.List.of("foo", "bar"), spec.args);
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
    void parseTransformSpec_escapedColonInMapArgs_preservesColon() {
        MessageResolver.TransformSpec spec = MessageResolver.parseTransformSpec("map:foo\\:bar:baz");
        assertNotNull(spec);
        assertEquals("map", spec.name);
        assertEquals("foo:bar", spec.args.get(0));
        assertEquals("baz", spec.args.get(1));
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

    @Test
    void resolve_trim_removesWhitespace() {
        String result = MessageResolver.resolve(
                "[{key|trim}]",
                Map.of("key", "  hello  "),
                null,
                warningTracker,
                PluginConfig.builder().build()
        );
        assertEquals("[hello]", result);
    }

    @Test
    void resolve_lower_lowercases() {
        String result = MessageResolver.resolve(
                "{key|lower}",
                Map.of("key", "HELLO"),
                null,
                warningTracker,
                PluginConfig.builder().build()
        );
        assertEquals("hello", result);
    }

    @Test
    void resolve_upper_uppercases() {
        String result = MessageResolver.resolve(
                "{key|upper}",
                Map.of("key", "hello"),
                null,
                warningTracker,
                PluginConfig.builder().build()
        );
        assertEquals("HELLO", result);
    }

    @Test
    void resolve_default_emptyUsesFallback() {
        String result = MessageResolver.resolve(
                "{key|default:N/A}",
                Map.of("key", ""),
                null,
                warningTracker,
                PluginConfig.builder().build()
        );
        assertEquals("N/A", result);
    }

    @Test
    void resolve_default_missingUsesFallback() {
        String result = MessageResolver.resolve(
                "{missing|default:Unknown}",
                Map.of(),
                null,
                warningTracker,
                PluginConfig.builder().build()
        );
        assertEquals("Unknown", result);
    }

    @Test
    void resolve_truncate_limitsLength() {
        String result = MessageResolver.resolve(
                "[{key|truncate:5}]",
                Map.of("key", "hello world"),
                null,
                warningTracker,
                PluginConfig.builder().build()
        );
        assertEquals("[hello]", result);
    }

    @Test
    void resolve_replace_literalReplace() {
        String result = MessageResolver.resolve(
                "{key|replace:foo:bar}",
                Map.of("key", "foofoo"),
                null,
                warningTracker,
                PluginConfig.builder().build()
        );
        assertEquals("barbar", result);
    }

    @Test
    void resolve_lastPathSegment_extractsLast() {
        String result = MessageResolver.resolve(
                "{key|last-path-segment}",
                Map.of("key", "2026/02/hardcore-26/world"),
                null,
                warningTracker,
                PluginConfig.builder().build()
        );
        assertEquals("world", result);
    }

    @Test
    void resolve_firstPathSegment_extractsFirst() {
        String result = MessageResolver.resolve(
                "{key|first-path-segment}",
                Map.of("key", "2026/02/hardcore-26/world"),
                null,
                warningTracker,
                PluginConfig.builder().build()
        );
        assertEquals("2026", result);
    }

    @Test
    void resolve_chainedtrimLowerDefault() {
        String result = MessageResolver.resolve(
                "[{key|trim|lower|default:none}]",
                Map.of("key", "  HELLO  "),
                null,
                warningTracker,
                PluginConfig.builder().build()
        );
        assertEquals("[hello]", result);
    }

    @Test
    void resolve_chainedLastPathSegmentThenUpper() {
        String result = MessageResolver.resolve(
                "{key|last-path-segment|upper}",
                Map.of("key", "path/to/world"),
                null,
                warningTracker,
                PluginConfig.builder().build()
        );
        assertEquals("WORLD", result);
    }
}
