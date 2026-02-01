package com.chibashr.allthewebhooks.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RedactionPolicy}.
 */
class RedactionPolicyTest {

    @Test
    void isRedacted_disabled_returnsFalse() {
        RedactionPolicy policy = new RedactionPolicy(false, List.of("player.name"));
        assertFalse(policy.isRedacted("player.name"));
    }

    @Test
    void isRedacted_nullField_returnsFalse() {
        RedactionPolicy policy = new RedactionPolicy(true, List.of("player.name"));
        assertFalse(policy.isRedacted(null));
    }

    @Test
    void isRedacted_emptyPatterns_returnsFalse() {
        RedactionPolicy policy = new RedactionPolicy(true, List.of());
        assertFalse(policy.isRedacted("player.name"));
    }

    @Test
    void isRedacted_nullPatterns_returnsFalse() {
        RedactionPolicy policy = new RedactionPolicy(true, null);
        assertFalse(policy.isRedacted("player.name"));
    }

    @Test
    void isRedacted_exactMatch_returnsTrue() {
        RedactionPolicy policy = new RedactionPolicy(true, List.of("player.name"));
        assertTrue(policy.isRedacted("player.name"));
    }

    @Test
    void isRedacted_noMatch_returnsFalse() {
        RedactionPolicy policy = new RedactionPolicy(true, List.of("player.name"));
        assertFalse(policy.isRedacted("player.uuid"));
    }

    @Test
    void isRedacted_wildcardMatch_returnsTrue() {
        RedactionPolicy policy = new RedactionPolicy(true, List.of("player.*"));
        assertTrue(policy.isRedacted("player.name"));
        assertTrue(policy.isRedacted("player.uuid"));
    }

    @Test
    void isRedacted_wildcardDoesNotMatchLonger_returnsTrue() {
        RedactionPolicy policy = new RedactionPolicy(true, List.of("player.*"));
        assertTrue(policy.isRedacted("player.name"));
    }

    @Test
    void isRedacted_patternMatchesPrefixOfValue() {
        RedactionPolicy policy = new RedactionPolicy(true, List.of("player.name"));
        assertTrue(policy.isRedacted("player.name"));
        assertTrue(policy.isRedacted("player.name.extra"), "pattern matches value with extra segments (prefix semantics)");
    }
}
