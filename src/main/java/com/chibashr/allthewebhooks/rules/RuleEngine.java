package com.chibashr.allthewebhooks.rules;

import java.util.List;
import java.util.Map;

public class RuleEngine {
    public boolean evaluate(Map<String, Object> conditions, Map<String, Object> context) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            String field = entry.getKey();
            Object condition = entry.getValue();
            Object fieldValue = context.get(field);
            if (!evaluateCondition(fieldValue, condition)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private boolean evaluateCondition(Object fieldValue, Object condition) {
        if (condition instanceof Map<?, ?> map) {
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) map).entrySet()) {
                String operator = String.valueOf(entry.getKey());
                Object value = entry.getValue();
                if (!evaluateOperator(fieldValue, operator, value)) {
                    return false;
                }
            }
            return true;
        }
        return evaluateOperator(fieldValue, "equals", condition);
    }

    @SuppressWarnings("unchecked")
    private boolean evaluateOperator(Object fieldValue, String operator, Object value) {
        if ("equals".equalsIgnoreCase(operator)) {
            return matchesEquals(fieldValue, value);
        }
        if ("not".equalsIgnoreCase(operator)) {
            return !matchesEquals(fieldValue, value);
        }
        if ("greater-than".equalsIgnoreCase(operator)) {
            return compareNumbers(fieldValue, value) > 0;
        }
        if ("less-than".equalsIgnoreCase(operator)) {
            return compareNumbers(fieldValue, value) < 0;
        }
        if ("greater-than-or-equal".equalsIgnoreCase(operator)) {
            return compareNumbers(fieldValue, value) >= 0;
        }
        if ("less-than-or-equal".equalsIgnoreCase(operator)) {
            return compareNumbers(fieldValue, value) <= 0;
        }
        return false;
    }

    private boolean matchesEquals(Object fieldValue, Object conditionValue) {
        if (conditionValue instanceof List<?> list) {
            for (Object item : list) {
                if (matchesEquals(fieldValue, item)) {
                    return true;
                }
            }
            return false;
        }
        if (fieldValue == null) {
            return conditionValue == null;
        }
        return String.valueOf(fieldValue).equalsIgnoreCase(String.valueOf(conditionValue));
    }

    private int compareNumbers(Object fieldValue, Object conditionValue) {
        double fieldNumber = toNumber(fieldValue);
        double conditionNumber = toNumber(conditionValue);
        return Double.compare(fieldNumber, conditionNumber);
    }

    private double toNumber(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
