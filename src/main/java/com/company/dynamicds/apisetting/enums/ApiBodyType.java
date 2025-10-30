package com.company.dynamicds.apisetting.enums;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;


public enum ApiBodyType implements EnumClass<String> {

    NONE("NONE"),
    FORM_DATA("FORM_DATA"),
    FORM_URLENCODED("FORM_URLENCODED"),
    RAW("RAW"),
    BINARY("BINARY");

    private final String id;

    ApiBodyType(String id) {
        this.id = id;
    }

    @Nullable
    public static ApiBodyType fromId(String id) {
        for (ApiBodyType at : ApiBodyType.values()) {
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