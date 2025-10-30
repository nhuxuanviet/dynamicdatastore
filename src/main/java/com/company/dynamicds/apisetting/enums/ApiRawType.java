package com.company.dynamicds.apisetting.enums;

import io.jmix.core.metamodel.datatype.EnumClass;
import io.jmix.flowui.kit.component.codeeditor.CodeEditorMode;
import lombok.Getter;
import org.springframework.lang.Nullable;

@Getter
public enum ApiRawType implements EnumClass<String> {

    JSON("application/json", CodeEditorMode.JSON),
    TEXT("text/plain", CodeEditorMode.TEXT),
    XML("application/xml", CodeEditorMode.XML),
    HTML("text/html", CodeEditorMode.HTML),
    JAVASCRIPT("application/javascript", CodeEditorMode.JAVASCRIPT);

    private final String id;
    private final CodeEditorMode editorMode;

    ApiRawType(String id, CodeEditorMode editorMode) {
        this.id = id;
        this.editorMode = editorMode;
    }

    @Nullable
    public static ApiRawType fromId(String id) {
        if (id == null) return null;
        for (ApiRawType type : values()) {
            if (type.getId().equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }

    @Nullable
    public static ApiRawType fromMediaType(String mediaTypeHeader) {
        if (mediaTypeHeader == null) return null;
        String normalized = mediaTypeHeader.split(";", 2)[0].trim().toLowerCase();
        for (ApiRawType type : values()) {
            if (type.getId().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        return null;
    }

}
