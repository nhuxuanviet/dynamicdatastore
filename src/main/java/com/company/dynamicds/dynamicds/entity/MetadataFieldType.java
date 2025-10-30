package com.company.dynamicds.dynamicds.entity;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;


public enum MetadataFieldType implements EnumClass<String> {

    STRING("STRING"),
    BOOLEAN("BOOLEAN"),
    INTEGER("INTEGER"),
    DOUBLE("DOUBLE");

    private final String id;

    MetadataFieldType(String id) {
        this.id = id;
    }

    @Nullable
    public static MetadataFieldType fromId(String id) {
        for (MetadataFieldType at : MetadataFieldType.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }

    public String getId() {
        return id;
    }

    public Class<?> getJavaClass() {
        return switch (this) {
            case STRING -> String.class;
            case INTEGER -> Integer.class;
            case DOUBLE -> Double.class;
            case BOOLEAN -> Boolean.class;
//            case DATE -> java.util.Date.class;
//            case LONG -> Long.class;
        };
    }


}