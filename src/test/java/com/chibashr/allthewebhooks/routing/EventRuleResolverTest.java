package com.chibashr.allthewebhooks.routing;

import com.chibashr.allthewebhooks.config.EventConfig;
import com.chibashr.allthewebhooks.config.EventRule;
import com.chibashr.allthewebhooks.config.EventRuleDefaults;
import com.chibashr.allthewebhooks.config.TestEventRuleFactory;
import com.chibashr.allthewebhooks.config.WorldEventConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link EventRuleResolver}.
 */
class EventRuleResolverTest {

    private EventRuleResolver resolver;
    private EventRuleDefaults defaults;

    @BeforeEach
    void setUp() {
        resolver = new EventRuleResolver();
        defaults = new EventRuleDefaults(true, "default", "generic", null);
    }

    @Test
    void resolve_noRules_returnsNull() {
        EventConfig config = new EventConfig(defaults);
        ResolvedEventRule result = resolver.resolve(config, "player.join", null);
        assertNull(result);
    }

    @Test
    void resolve_exactMatch_returnsRule() {
        EventConfig config = new EventConfig(defaults);
        config.putEventRule("player.join", TestEventRuleFactory.create("player_join", "default"));
        ResolvedEventRule result = resolver.resolve(config, "player.join", null);
        assertNotNull(result);
        assertTrue(result.isEnabled());
        assertEquals("player.join", result.getMatchedKey());
        assertEquals("player_join", result.getMessage());
    }

    @Test
    void resolve_wildcardMatch_returnsBestMatch() {
        EventConfig config = new EventConfig(defaults);
        config.putEventRule("player.*", TestEventRuleFactory.create("generic_player", "default"));
        config.putEventRule("player.join", TestEventRuleFactory.create("player_join", "default"));
        ResolvedEventRule result = resolver.resolve(config, "player.join", null);
        assertNotNull(result);
        assertEquals("player.join", result.getMatchedKey());
        assertEquals("player_join", result.getMessage());
    }

    @Test
    void resolve_wildcardOnly_returnsWildcardRule() {
        EventConfig config = new EventConfig(defaults);
        config.putEventRule("player.*", TestEventRuleFactory.create("generic_player", "default"));
        ResolvedEventRule result = resolver.resolve(config, "player.quit", null);
        assertNotNull(result);
        assertEquals("player.*", result.getMatchedKey());
        assertEquals("generic_player", result.getMessage());
    }

    @Test
    void resolve_defaultsApplied_whenRuleHasNulls() {
        EventConfig config = new EventConfig(defaults);
        config.putEventRule("player.join", TestEventRuleFactory.create(null, null, null, null, null, null));
        ResolvedEventRule result = resolver.resolve(config, "player.join", null);
        assertNotNull(result);
        assertTrue(result.isEnabled());
        assertEquals("default", result.getWebhook());
        assertEquals("generic", result.getMessage());
    }

    @Test
    void resolve_worldDisabled_returnsDisabled() throws Exception {
        EventConfig config = new EventConfig(defaults);
        WorldEventConfig worldConfig = createWorldEventConfig(false, Map.of(
                "player.join", TestEventRuleFactory.create("player_join", "default")
        ));
        config.putWorldConfig("world_nether", worldConfig);
        ResolvedEventRule result = resolver.resolve(config, "player.join", "world_nether");
        assertNotNull(result);
        assertFalse(result.isEnabled());
    }

    @Test
    void resolve_worldRuleOverridesGlobal() throws Exception {
        EventConfig config = new EventConfig(defaults);
        config.putEventRule("player.join", TestEventRuleFactory.create("global_join", "default"));
        WorldEventConfig worldConfig = createWorldEventConfig(true, Map.of(
                "player.join", TestEventRuleFactory.create("world_join", "default")
        ));
        config.putWorldConfig("world_nether", worldConfig);
        ResolvedEventRule result = resolver.resolve(config, "player.join", "world_nether");
        assertNotNull(result);
        assertTrue(result.isEnabled());
        assertEquals("player.join", result.getMatchedKey());
        assertEquals("world_join", result.getMessage());
    }

    /**
     * Create WorldEventConfig via reflection (constructor is private).
     */
    private static WorldEventConfig createWorldEventConfig(Boolean enabled, Map<String, EventRule> eventRules) throws Exception {
        Constructor<WorldEventConfig> ctor = WorldEventConfig.class.getDeclaredConstructor(Boolean.class, Map.class);
        ctor.setAccessible(true);
        return ctor.newInstance(enabled, eventRules);
    }
}
