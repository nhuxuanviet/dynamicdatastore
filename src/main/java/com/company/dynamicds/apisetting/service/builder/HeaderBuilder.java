package com.company.dynamicds.apisetting.service.builder;

import com.company.dynamicds.apisetting.entity.ApiAuthorizationSetting;
import com.company.dynamicds.apisetting.entity.ApiBody;
import com.company.dynamicds.apisetting.entity.ApiHeader;
import com.company.dynamicds.apisetting.entity.ApiSetting;
import com.company.dynamicds.apisetting.enums.ApiBodyType;
import com.company.dynamicds.apisetting.enums.ApiKeyPlacement;
import com.company.dynamicds.apisetting.enums.ApiRawType;
import com.company.dynamicds.utils.validation.StringValidation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * HeaderBuilder chịu trách nhiệm tạo HttpHeaders cho mỗi request:
 * - Xử lý Content-Type theo ApiBodyType và ApiRawType
 * - Thêm custom headers do người dùng nhập
 * - Thêm Authorization header (Basic, Bearer, API Key)
 */
@Component
@Slf4j
public class HeaderBuilder {

    public HttpHeaders buildHeaders(ApiSetting setting) {
        HttpHeaders headers = new HttpHeaders();

        MediaType contentType = resolveMediaType(setting);
        if (contentType != null) {
            headers.setContentType(contentType);
        }

        headers.setAccept(List.of(MediaType.ALL));

        List<ApiHeader> customHeaders = setting.getApiHeader();
        if (customHeaders != null && !customHeaders.isEmpty()) {
            customHeaders.stream()
                    .filter(h -> StringValidation.allNotNullOrEmpty(h.getKey(), h.getValue()))
                    .forEach(h -> headers.add(h.getKey(), h.getValue()));
        }

        ApiAuthorizationSetting auth = setting.getAuthorization();
        if (auth != null && auth.getAuthType() != null) {
            switch (auth.getAuthType()) {
                case BASIC_AUTH -> {
                    if (StringValidation.allNotNullOrEmpty(auth.getUsername(), auth.getPassword())) {
                        headers.setBasicAuth(auth.getUsername(), auth.getPassword());
                    } else {
                        log.warn("Missing BASIC_AUTH credentials");
                    }
                }
                case BEARER_TOKEN -> {
                    if (StringValidation.isNotNullOrEmpty(auth.getBearerToken())) {
                        headers.setBearerAuth(auth.getBearerToken());
                    } else {
                        log.warn("Missing bearer token for BEARER_TOKEN");
                    }
                }
                case API_KEY -> {
                    if (auth.getApiKeyPlacement() == ApiKeyPlacement.HEADER
                            && StringValidation.allNotNullOrEmpty(auth.getApiKeyName(), auth.getApiKeyValue())) {
                        headers.add(auth.getApiKeyName(), auth.getApiKeyValue());
                    }
                }
                case NO_AUTH -> log.debug("No authorization applied.");
                default -> log.warn("Unsupported authorization type: {}", auth.getAuthType());
            }
        }

        return headers;
    }

    private MediaType resolveMediaType(ApiSetting setting) {
        if (setting.getApiBody() == null) {
            return null;
        }

        ApiBody apiBody = setting.getApiBody();
        ApiBodyType bodyType = apiBody.getBodyType();
        ApiRawType rawType = apiBody.getRawType();

        if (bodyType == null) return null;

        return switch (bodyType) {
            case RAW -> rawType != null
                    ? MediaType.parseMediaType(rawType.getId())
                    : MediaType.TEXT_PLAIN;
            case FORM_URLENCODED -> MediaType.APPLICATION_FORM_URLENCODED;
            case FORM_DATA -> MediaType.MULTIPART_FORM_DATA;
            case BINARY -> MediaType.APPLICATION_OCTET_STREAM;
            case NONE -> null;
        };
    }

}
