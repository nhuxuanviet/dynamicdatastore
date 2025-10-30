package com.company.dynamicds.enums;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;


public enum ActionColumnType implements EnumClass<String> {

    EDIT("EDIT"),
    SETTING("SETTING"),
    RELOAD("RELOAD"),
    DELETE("DELETE"),
    VIEW("VIEW"),
    LOCK("LOCK"),
    ACTIVE("ACTIVE");


    private final String id;

    ActionColumnType(String id) {
        this.id = id;
    }

    @Nullable
    public static ActionColumnType fromId(String id) {
        for (ActionColumnType at : ActionColumnType.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }

    public String getId() {
        return id;
    }
}