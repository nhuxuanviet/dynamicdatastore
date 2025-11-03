package com.company.dynamicds.metapackage.service;

import io.jmix.core.entity.KeyValueEntity;
import io.jmix.core.querycondition.Condition;
import io.jmix.core.querycondition.LogicalCondition;
import io.jmix.core.querycondition.PropertyCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Processes Jmix Conditions (PropertyCondition, LogicalCondition) for in-memory filtering
 * Similar to RestFilterBuilder but operates on already-loaded data
 */
@Component
public class MetaPackageFilterProcessor {

    private static final Logger log = LoggerFactory.getLogger(MetaPackageFilterProcessor.class);

    /**
     * Apply filtering conditions to a list of KeyValueEntities
     * Supports PropertyCondition and LogicalCondition (AND/OR)
     */
    public List<KeyValueEntity> applyFilter(List<KeyValueEntity> entities, Condition condition) {
        if (condition == null || entities == null || entities.isEmpty()) {
            return entities;
        }

        return entities.stream()
                .filter(entity -> matchesCondition(entity, condition))
                .collect(Collectors.toList());
    }

    /**
     * Check if an entity matches the given condition
     */
    private boolean matchesCondition(KeyValueEntity entity, Condition condition) {
        if (condition instanceof PropertyCondition propertyCondition) {
            return matchesPropertyCondition(entity, propertyCondition);
        } else if (condition instanceof LogicalCondition logicalCondition) {
            return matchesLogicalCondition(entity, logicalCondition);
        }
        // Unknown condition type, skip filtering
        log.warn("Unknown condition type: {}", condition.getClass().getName());
        return true;
    }

    /**
     * Check if entity matches a PropertyCondition
     * Supports all Jmix operators: =, <>, >, <, >=, <=, contains, startsWith, endsWith, in, notIn, etc.
     */
    private boolean matchesPropertyCondition(KeyValueEntity entity, PropertyCondition condition) {
        String property = condition.getProperty();
        Object actualValue = entity.getValue(property);
        Object expectedValue = condition.getParameterValue();
        String operation = condition.getOperation();

        // Handle null values
        if ("is_set".equalsIgnoreCase(operation)) {
            return actualValue != null;
        }

        if (actualValue == null || expectedValue == null) {
            return false;
        }

        return switch (operation.toLowerCase()) {
            case "=", "equal" -> compareEquals(actualValue, expectedValue);
            case "<>", "not_equal" -> !compareEquals(actualValue, expectedValue);
            case ">", "greater" -> compareGreater(actualValue, expectedValue);
            case "<", "less" -> compareLess(actualValue, expectedValue);
            case ">=", "greater_or_equal" -> compareGreaterOrEqual(actualValue, expectedValue);
            case "<=", "less_or_equal" -> compareLessOrEqual(actualValue, expectedValue);
            case "contains" -> containsString(actualValue, expectedValue);
            case "doesnotcontain", "not_contains" -> !containsString(actualValue, expectedValue);
            case "startswith", "starts_with" -> startsWithString(actualValue, expectedValue);
            case "endswith", "ends_with" -> endsWithString(actualValue, expectedValue);
            case "in", "in_list" -> inCollection(actualValue, expectedValue);
            case "notin", "not_in_list" -> !inCollection(actualValue, expectedValue);
            default -> {
                log.warn("Unsupported operation: {}", operation);
                yield true;
            }
        };
    }

    /**
     * Check if entity matches a LogicalCondition (AND/OR)
     */
    private boolean matchesLogicalCondition(KeyValueEntity entity, LogicalCondition condition) {
        List<Condition> conditions = condition.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }

        boolean isAnd = "and".equalsIgnoreCase(condition.getType().name());

        if (isAnd) {
            // ALL conditions must match
            return conditions.stream()
                    .allMatch(c -> matchesCondition(entity, c));
        } else {
            // ANY condition must match
            return conditions.stream()
                    .anyMatch(c -> matchesCondition(entity, c));
        }
    }

    // ============ Comparison Methods ============

    private boolean compareEquals(Object actual, Object expected) {
        return actual.equals(expected);
    }

    @SuppressWarnings("unchecked")
    private boolean compareGreater(Object actual, Object expected) {
        if (actual instanceof Comparable && expected instanceof Comparable) {
            try {
                return ((Comparable<Object>) actual).compareTo(expected) > 0;
            } catch (ClassCastException e) {
                log.warn("Cannot compare {} with {}", actual.getClass(), expected.getClass());
                return false;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean compareLess(Object actual, Object expected) {
        if (actual instanceof Comparable && expected instanceof Comparable) {
            try {
                return ((Comparable<Object>) actual).compareTo(expected) < 0;
            } catch (ClassCastException e) {
                log.warn("Cannot compare {} with {}", actual.getClass(), expected.getClass());
                return false;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean compareGreaterOrEqual(Object actual, Object expected) {
        if (actual instanceof Comparable && expected instanceof Comparable) {
            try {
                return ((Comparable<Object>) actual).compareTo(expected) >= 0;
            } catch (ClassCastException e) {
                log.warn("Cannot compare {} with {}", actual.getClass(), expected.getClass());
                return false;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean compareLessOrEqual(Object actual, Object expected) {
        if (actual instanceof Comparable && expected instanceof Comparable) {
            try {
                return ((Comparable<Object>) actual).compareTo(expected) <= 0;
            } catch (ClassCastException e) {
                log.warn("Cannot compare {} with {}", actual.getClass(), expected.getClass());
                return false;
            }
        }
        return false;
    }

    private boolean containsString(Object actual, Object expected) {
        return actual.toString().contains(expected.toString());
    }

    private boolean startsWithString(Object actual, Object expected) {
        return actual.toString().startsWith(expected.toString());
    }

    private boolean endsWithString(Object actual, Object expected) {
        return actual.toString().endsWith(expected.toString());
    }

    @SuppressWarnings("unchecked")
    private boolean inCollection(Object actual, Object expected) {
        if (expected instanceof Collection) {
            return ((Collection<Object>) expected).contains(actual);
        }
        return false;
    }
}
