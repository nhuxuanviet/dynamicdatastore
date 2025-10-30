package com.company.dynamicds.apisetting.enums;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;


public enum ApiAuthType implements EnumClass<String> {

    NO_AUTH("NO_AUTH"),
    BASIC_AUTH("BASIC_AUTH"),
    BEARER_TOKEN("BEARER_TOKEN"),
    API_KEY("API_KEY");

    private final String id;

    ApiAuthType(String id) {
        this.id = id;
    }

    @Nullable
    public static ApiAuthType fromId(String id) {
        for (ApiAuthType at : ApiAuthType.values()) {
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