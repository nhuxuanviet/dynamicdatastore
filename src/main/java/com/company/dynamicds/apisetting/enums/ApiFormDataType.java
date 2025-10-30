package com.company.dynamicds.apisetting.enums;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;


public enum ApiFormDataType implements EnumClass<String> {

    TEXT("TEXT"),
    FILE("FILE");

    private final String id;

    ApiFormDataType(String id) {
        this.id = id;
    }

    @Nullable
    public static ApiFormDataType fromId(String id) {
        for (ApiFormDataType at : ApiFormDataType.values()) {
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