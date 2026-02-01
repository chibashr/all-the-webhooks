package com.chibashr.allthewebhooks.rules;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RuleEngine}.
 */
class RuleEngineTest {

    private final RuleEngine engine = new RuleEngine();

    @Test
    void evaluate_nullConditions_returnsTrue() {
        assertTrue(engine.evaluate(null, Map.of()));
    }

    @Test
    void evaluate_emptyConditions_returnsTrue() {
        assertTrue(engine.evaluate(Map.of(), Map.of("block.type", "STONE")));
    }

    @Test
    void evaluate_equals_match_returnsTrue() {
        Map<String, Object> conditions = Map.of("block.type", Map.of("equals", "DIAMOND_ORE"));
        Map<String, Object> context = Map.of("block.type", "DIAMOND_ORE");
        assertTrue(engine.evaluate(conditions, context));
    }

    @Test
    void evaluate_equals_noMatch_returnsFalse() {
        Map<String, Object> conditions = Map.of("block.type", Map.of("equals", "DIAMOND_ORE"));
        Map<String, Object> context = Map.of("block.type", "STONE");
        assertFalse(engine.evaluate(conditions, context));
    }

    @Test
    void evaluate_equals_caseInsensitive() {
        Map<String, Object> conditions = Map.of("block.type", Map.of("equals", "diamond_ore"));
        Map<String, Object> context = Map.of("block.type", "DIAMOND_ORE");
        assertTrue(engine.evaluate(conditions, context));
    }

    @Test
    void evaluate_equals_nullContextValue_matchesNullCondition() {
        Map<String, Object> conditions = Map.of("x", Map.of("equals", null));
        Map<String, Object> context = Map.of();
        assertTrue(engine.evaluate(conditions, context));
    }

    @Test
    void evaluate_shortForm_equals() {
        Map<String, Object> conditions = Map.of("block.type", "STONE");
        Map<String, Object> context = Map.of("block.type", "STONE");
        assertTrue(engine.evaluate(conditions, context));
    }

    @Test
    void evaluate_not_match_returnsFalse() {
        Map<String, Object> conditions = Map.of("block.type", Map.of("not", "STONE"));
        Map<String, Object> context = Map.of("block.type", "STONE");
        assertFalse(engine.evaluate(conditions, context));
    }

    @Test
    void evaluate_not_noMatch_returnsTrue() {
        Map<String, Object> conditions = Map.of("block.type", Map.of("not", "STONE"));
        Map<String, Object> context = Map.of("block.type", "DIAMOND_ORE");
        assertTrue(engine.evaluate(conditions, context));
    }

    @Test
    void evaluate_greaterThan_returnsTrueWhenGreater() {
        Map<String, Object> conditions = Map.of("damage.amount", Map.of("greater-than", 5));
        Map<String, Object> context = Map.of("damage.amount", 10);
        assertTrue(engine.evaluate(conditions, context));
    }

    @Test
    void evaluate_greaterThan_returnsFalseWhenEqual() {
        Map<String, Object> conditions = Map.of("damage.amount", Map.of("greater-than", 5));
        Map<String, Object> context = Map.of("damage.amount", 5);
        assertFalse(engine.evaluate(conditions, context));
    }

    @Test
    void evaluate_lessThan_returnsTrueWhenLess() {
        Map<String, Object> conditions = Map.of("damage.amount", Map.of("less-than", 10));
        Map<String, Object> context = Map.of("damage.amount", 5);
        assertTrue(engine.evaluate(conditions, context));
    }

    @Test
    void evaluate_equals_list_anyMatch_returnsTrue() {
        Map<String, Object> conditions = Map.of("block.type", Map.of("equals", List.of("STONE", "DIAMOND_ORE")));
        Map<String, Object> context = Map.of("block.type", "DIAMOND_ORE");
        assertTrue(engine.evaluate(conditions, context));
    }

    @Test
    void evaluate_multipleConditions_allMustMatch() {
        Map<String, Object> conditions = Map.of(
                "block.type", Map.of("equals", "DIAMOND_ORE"),
                "damage.amount", Map.of("greater-than", 0)
        );
        Map<String, Object> context = Map.of("block.type", "DIAMOND_ORE", "damage.amount", 5);
        assertTrue(engine.evaluate(conditions, context));
    }

    @Test
    void evaluate_multipleConditions_oneFails_returnsFalse() {
        Map<String, Object> conditions = Map.of(
                "block.type", Map.of("equals", "DIAMOND_ORE"),
                "damage.amount", Map.of("greater-than", 10)
        );
        Map<String, Object> context = Map.of("block.type", "DIAMOND_ORE", "damage.amount", 5);
        assertFalse(engine.evaluate(conditions, context));
    }
}
