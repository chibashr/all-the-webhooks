package com.chibashr.allthewebhooks.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link EventRule}, especially parsing conditions with dotted keys.
 */
class EventRuleTest {

    @Test
    void fromSection_conditionsWithDottedKey_worldName_notList_parsesAsSingleCondition() throws Exception {
        String yaml = """
            events:
              world.load:
                message: world_load
                conditions:
                  world.name:
                    not:
                      - "world_nether"
                      - "world_the_end"
            """;
        YamlConfiguration yamlConfig = new YamlConfiguration();
        yamlConfig.load(new StringReader(yaml));
        ConfigurationSection section = yamlConfig.getConfigurationSection("events.world.load");
        assertNotNull(section, "events.world.load section must exist");

        EventRule rule = EventRule.fromSection(section);
        assertNotNull(rule);
        Map<String, Object> conditions = rule.getConditions();

        assertTrue(conditions.containsKey("world.name"),
                "Conditions must contain key 'world.name' (dotted key), not just 'world'. Got keys: " + conditions.keySet());
        Object cond = conditions.get("world.name");
        assertInstanceOf(Map.class, cond);
        @SuppressWarnings("unchecked")
        Map<String, Object> opMap = (Map<String, Object>) cond;
        assertNotNull(opMap.get("not"));
        assertInstanceOf(java.util.List.class, opMap.get("not"));
    }

    @Test
    void fromSection_conditionsWithShortFormDottedKey_parsesCorrectly() throws Exception {
        String yaml = """
            events:
              block.break:
                message: generic
                conditions:
                  block.type: STONE
            """;
        YamlConfiguration yamlConfig = new YamlConfiguration();
        yamlConfig.load(new StringReader(yaml));
        ConfigurationSection section = yamlConfig.getConfigurationSection("events.block.break");
        assertNotNull(section);

        EventRule rule = EventRule.fromSection(section);
        Map<String, Object> conditions = rule.getConditions();

        assertTrue(conditions.containsKey("block.type"));
        assertEquals("STONE", conditions.get("block.type"));
    }
}
