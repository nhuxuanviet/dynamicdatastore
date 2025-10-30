package com.company.dynamicds.apisetting.service.builder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.company.dynamicds.apisetting.entity.ApiBody;
import com.company.dynamicds.apisetting.entity.ApiFormDataField;
import com.company.dynamicds.apisetting.enums.ApiBodyType;
import com.company.dynamicds.apisetting.enums.ApiFormDataType;
import com.company.dynamicds.apisetting.enums.ApiRawType;
import com.company.dynamicds.utils.validation.StringValidation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class RequestBodyBuilder {

    private final ObjectMapper objectMapper;

    public Object buildApiBody(ApiBody body) {
        if (body == null || body.getBodyType() == null || body.getBodyType() == ApiBodyType.NONE) {
            return null;
        }

        return switch (body.getBodyType()) {
            case RAW -> buildRaw(body);
            case FORM_URLENCODED -> buildFormUrlEncoded(body);
            case FORM_DATA -> buildFormData(body);
            case BINARY -> buildBinary(body);
            case NONE -> null;
        };
    }

    // ---------- RAW JSON / TEXT ----------
    private Object buildRaw(ApiBody body) {
        String rawContent = body.getRawContent();
        if (StringValidation.isNullOrEmpty(rawContent))
            return null;

        ApiRawType rawType = body.getRawType();
        if (rawType == ApiRawType.JSON) {
            try {
                // Trả về JsonNode để RestClient auto serialize
                return objectMapper.readTree(rawContent);
            } catch (JsonProcessingException e) {
                log.warn("Invalid JSON format. Sending as plain text.");
                return rawContent;
            }
        }
        return rawContent;
    }

    // ---------- FORM URL ENCODED ----------
    private Object buildFormUrlEncoded(ApiBody body) {
        String raw = body.getRawContent();
        if (StringValidation.isNullOrEmpty(raw)) {
            return new LinkedMultiValueMap<String, String>();
        }

        boolean looksEncoded = raw.matches(".*(%[0-9A-Fa-f]{2}|\\+).*");
        if (looksEncoded) {
            // Giữ nguyên, không re-encode
            return raw;
        }

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        for (String pair : raw.split("&")) {
            if (pair.isEmpty()) continue;
            String[] kv = pair.split("=", 2);
            String key = kv.length > 0 ? kv[0] : "";
            String value = kv.length > 1 ? kv[1] : "";
            if (StringValidation.isNotNullOrEmpty(key)) {
                map.add(key.trim(), value);
            }
        }
        return map;
    }

    // ---------- MULTIPART FORM DATA ----------
    private Object buildFormData(ApiBody body) {
        MultiValueMap<String, Object> multipartMap = new LinkedMultiValueMap<>();
        List<ApiFormDataField> fields = body.getFormDataFields();

        if (fields == null || fields.isEmpty()) {
            return multipartMap;
        }

        for (ApiFormDataField field : fields) {
            addFormDataPart(multipartMap, field);
        }

        return multipartMap;
    }

    private void addFormDataPart(MultiValueMap<String, Object> map, ApiFormDataField field) {
        String key = field.getKey();
        if (StringValidation.isNullOrEmpty(key)) {
            log.warn("Skip multipart field with empty key");
            return;
        }

        if (field.getDataType() == ApiFormDataType.FILE) {
            Resource filePart = toByteArrayResource(field.getFileContent(), field.getFileName());
            if (filePart != null) {
                map.add(key, filePart);
            } else {
                log.warn("Skip file part '{}' - content is empty.", key);
            }
        } else {
            // Text field
            String safeValue = field.getValue() != null ? field.getValue() : "";
            map.add(key, safeValue);
        }
    }

    // ---------- BINARY ----------
    private Object buildBinary(ApiBody body) {
        byte[] content = body.getBinaryFileContent();
        if (content == null || content.length == 0) {
            return null;
        }

        return new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return StringValidation.isNotNullOrEmpty(body.getBinaryFileName())
                        ? body.getBinaryFileName()
                        : "binaryFile";
            }
        };
    }

    private Resource toByteArrayResource(byte[] content, String fileName) {
        if (content == null || content.length == 0) return null;
        return new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return fileName != null ? fileName : "file";
            }
        };
    }
}
