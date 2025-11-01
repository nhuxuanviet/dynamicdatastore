package com.company.dynamicds.metapackage.enums;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;

/**
 * Strategy for merging data from multiple metadata sources
 */
public enum MergeStrategy implements EnumClass<String> {

    /**
     * Sequentially append all records from all sources
     * Source A: [record1, record2]
     * Source B: [record3, record4]
     * Result: [record1, record2, record3, record4]
     */
    SEQUENTIAL("SEQUENTIAL"),

    /**
     * Cross join - Cartesian product of all sources
     * Source A: [a1, a2]
     * Source B: [b1, b2]
     * Result: [(a1+b1), (a1+b2), (a2+b1), (a2+b2)]
     */
    CROSS_JOIN("CROSS_JOIN"),

    /**
     * Nested - For each record from first source, fetch from other sources
     * Useful for master-detail relationships
     */
    NESTED("NESTED");

    private final String id;

    MergeStrategy(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Nullable
    public static MergeStrategy fromId(String id) {
        for (MergeStrategy value : MergeStrategy.values()) {
            if (value.getId().equals(id)) {
                return value;
            }
        }
        return null;
    }
}
