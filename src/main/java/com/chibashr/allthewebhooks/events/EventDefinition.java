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
    private final String parentBaseKey;

    public EventDefinition(
            String key,
            String category,
            String description,
            Map<String, String> predicateFields,
            List<String> wildcardExamples,
            String exampleYaml,
            EventContextBuilder<?> contextBuilder
    ) {
        this(key, category, description, predicateFields, wildcardExamples, exampleYaml, contextBuilder, null);
    }

    public EventDefinition(
            String key,
            String category,
            String description,
            Map<String, String> predicateFields,
            List<String> wildcardExamples,
            String exampleYaml,
            EventContextBuilder<?> contextBuilder,
            String parentBaseKey
    ) {
        this.key = key;
        this.category = category;
        this.description = description;
        this.predicateFields = predicateFields;
        this.wildcardExamples = wildcardExamples;
        this.exampleYaml = exampleYaml;
        this.contextBuilder = contextBuilder;
        this.parentBaseKey = parentBaseKey != null && !parentBaseKey.isEmpty() ? parentBaseKey : null;
    }

    /** True if this event is a sub-event of a base event (inherits predicates from parent). */
    public boolean isSubEvent() {
        return parentBaseKey != null;
    }

    /** The base event key this sub-event inherits from, or null if this is a base event. */
    public String getParentBaseKey() {
        return parentBaseKey;
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
