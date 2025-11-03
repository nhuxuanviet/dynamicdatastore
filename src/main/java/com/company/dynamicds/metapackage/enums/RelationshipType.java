package com.company.dynamicds.metapackage.enums;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;

/**
 * Defines relationship types between data sources
 * Similar to database foreign key relationships
 */
public enum RelationshipType implements EnumClass<String> {

    /**
     * One-to-Many: One record in target matches many records in source
     * Example: One User has many Carts
     */
    ONE_TO_MANY("ONE_TO_MANY"),

    /**
     * Many-to-One: Many records in source match one record in target
     * Example: Many Carts belong to one User
     */
    MANY_TO_ONE("MANY_TO_ONE"),

    /**
     * Many-to-Many: Many records in source match many records in target
     * Example: Products and Carts (through cart items)
     */
    MANY_TO_MANY("MANY_TO_MANY");

    private final String id;

    RelationshipType(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Nullable
    public static RelationshipType fromId(String id) {
        for (RelationshipType type : RelationshipType.values()) {
            if (type.getId().equals(id)) {
                return type;
            }
        }
        return null;
    }
}
