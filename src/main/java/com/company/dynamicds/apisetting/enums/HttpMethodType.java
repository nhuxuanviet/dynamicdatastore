package com.company.dynamicds.apisetting.enums;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;


public enum HttpMethodType implements EnumClass<String> {

    GET("GET"),
    //    HEAD("HEAD"),
    POST("POST"),
    PUT("PUT"),
    PATCH("PATCH"),
    DELETE("DELETE");
//    OPTIONS("OPTIONS"),
//    TRACE("TRACE");

    private final String id;

    HttpMethodType(String id) {
        this.id = id;
    }

    @Nullable
    public static HttpMethodType fromId(String id) {
        for (HttpMethodType at : HttpMethodType.values()) {
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