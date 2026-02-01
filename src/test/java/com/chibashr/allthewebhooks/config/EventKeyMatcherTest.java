package com.chibashr.allthewebhooks.config;

import com.chibashr.allthewebhooks.config.EventKeyMatcher.MatchScore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link EventKeyMatcher}.
 */
class EventKeyMatcherTest {

    @Test
    void matches_nullConfigured_returnsFalse() {
        assertFalse(EventKeyMatcher.matches(null, "player.join"));
    }

    @Test
    void matches_nullEventKey_returnsFalse() {
        assertFalse(EventKeyMatcher.matches("player.join", null));
    }

    @Test
    void matches_exactMatch_returnsTrue() {
        assertTrue(EventKeyMatcher.matches("player.join", "player.join"));
    }

    @Test
    void matches_exactMismatch_returnsFalse() {
        assertFalse(EventKeyMatcher.matches("player.join", "player.quit"));
    }

    @Test
    void matches_configuredLongerThanEvent_returnsFalse() {
        assertFalse(EventKeyMatcher.matches("player.join.extra", "player.join"));
    }

    @Test
    void matches_wildcardSingleSegment_matches() {
        assertTrue(EventKeyMatcher.matches("player.*", "player.join"));
        assertTrue(EventKeyMatcher.matches("player.*", "player.quit"));
        assertFalse(EventKeyMatcher.matches("player.*", "entity.damage"));
    }

    @Test
    void matches_wildcardMultipleSegments_matches() {
        assertTrue(EventKeyMatcher.matches("player.*.*", "player.break.block"));
        assertTrue(EventKeyMatcher.matches("*.damage.*", "entity.damage.player"));
    }

    @Test
    void matches_wildcardNotAtEnd_matchesExactRest() {
        assertTrue(EventKeyMatcher.matches("player.*.block", "player.break.block"));
        assertFalse(EventKeyMatcher.matches("player.*.block", "player.break.chest"));
    }

    @Test
    void score_noMatch_returnsNoMatch() {
        assertEquals(EventKeyMatcher.MatchScore.NO_MATCH, EventKeyMatcher.score("player.quit", "player.join"));
    }

    @Test
    void score_exactMatch_returnsDepthAndSpecificity() {
        MatchScore score = EventKeyMatcher.score("player.join", "player.join");
        assertNotEquals(EventKeyMatcher.MatchScore.NO_MATCH, score);
        assertEquals(2, score.depth());
        assertEquals(2, score.specificity());
    }

    @Test
    void score_wildcardMatch_lowerSpecificity() {
        MatchScore exact = EventKeyMatcher.score("player.join", "player.join");
        MatchScore wildcard = EventKeyMatcher.score("player.*", "player.join");
        assertTrue(exact.specificity() > wildcard.specificity());
        assertTrue(wildcard.isBetterThan(EventKeyMatcher.MatchScore.NO_MATCH));
    }

    @Test
    void score_deeperMatch_betterThanShallower() {
        MatchScore deep = EventKeyMatcher.score("player.break.block", "player.break.block");
        MatchScore shallow = EventKeyMatcher.score("player.*", "player.break.block");
        assertTrue(deep.isBetterThan(shallow));
    }

    @Test
    void score_sameDepth_moreSpecificityBetter() {
        MatchScore moreSpecific = EventKeyMatcher.score("player.break.block", "player.break.block");
        MatchScore lessSpecific = EventKeyMatcher.score("player.break.*", "player.break.block");
        assertTrue(moreSpecific.isBetterThan(lessSpecific));
    }

    @Test
    void noMatch_isBetterThan_returnsFalseForAnyRealScore() {
        MatchScore noMatch = EventKeyMatcher.MatchScore.NO_MATCH;
        MatchScore real = EventKeyMatcher.score("player.*", "player.join");
        assertFalse(noMatch.isBetterThan(real));
    }
}
