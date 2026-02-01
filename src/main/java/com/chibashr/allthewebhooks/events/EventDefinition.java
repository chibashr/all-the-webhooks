package com.chibashr.allthewebhooks.events;

import java.util.List;
import java.util.Map;

public class EventDefinition {
    private final String key;
    private final String category;
    private final String description;
    private final Map<String, String> predicateFields;
    private final List<String> wildcardExamples;
    private final String exampleYaml;
    private final EventContextBuilder<?> contextBuilder;

    public EventDefinition(
            String key,
            String category,
            String description,
            Map<String, String> predicateFields,
            List<String> wildcardExamples,
            String exampleYaml,
            EventContextBuilder<?> contextBuilder
    ) {
        this.key = key;
        this.category = category;
        this.description = description;
        this.predicateFields = predicateFields;
        this.wildcardExamples = wildcardExamples;
        this.exampleYaml = exampleYaml;
        this.contextBuilder = contextBuilder;
    }

    public String getKey() {
        return key;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, String> getPredicateFields() {
        return predicateFields;
    }

    public List<String> getWildcardExamples() {
        return wildcardExamples;
    }

    public String getExampleYaml() {
        return exampleYaml;
    }

    public EventContextBuilder<?> getContextBuilder() {
        return contextBuilder;
    }
}
