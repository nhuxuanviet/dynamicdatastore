package com.company.dynamicds.apisetting.enums;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;


public enum ApiKeyPlacement implements EnumClass<String> {

    HEADER("HEADER"),
    QUERY("QUERY");
    private final String id;

    ApiKeyPlacement(String id) {
        this.id = id;
    }

    @Nullable
    public static ApiKeyPlacement fromId(String id) {
        for (ApiKeyPlacement at : ApiKeyPlacement.values()) {
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