package com.company.dynamicds.apisetting.service.builder;

import com.company.dynamicds.apisetting.entity.ApiAuthorizationSetting;
import com.company.dynamicds.apisetting.entity.ApiQueryParam;
import com.company.dynamicds.apisetting.entity.ApiSetting;
import com.company.dynamicds.apisetting.enums.ApiAuthType;
import com.company.dynamicds.apisetting.enums.ApiKeyPlacement;
import com.company.dynamicds.utils.validation.StringValidation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 🔗 UriBuilder chịu trách nhiệm xây dựng URI đầy đủ cho request:
 * - Base URL hoặc Final URL (nếu user bật useRawUrl)
 * - Query parameters
 * - API key (nếu authType = API_KEY và placement = QUERY)
 * - Tự động encode khi build
 */
@Slf4j
@Component
public class UriBuilder {

    public URI buildUri(ApiSetting setting) {
        if (Boolean.TRUE.equals(setting.getUseRawUrl()) && StringValidation.isNotNullOrEmpty(setting.getFinalUrl())) {
            URI uriEncode = UriComponentsBuilder
                    .fromUriString(setting.getFinalUrl())
                    .encode(StandardCharsets.UTF_8)
                    .build()
                    .toUri();
            log.debug("Built URI: {}", uriEncode);
            return uriEncode;
        }

        String baseUrl = setting.getBaseUrl();
        if (StringValidation.isNullOrEmpty(baseUrl)) {
            throw new IllegalArgumentException("Base URL cannot be null or empty");
        }

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(baseUrl);

        List<ApiQueryParam> queryParams = setting.getApiQueryParam();
        if (queryParams != null && !queryParams.isEmpty()) {
            queryParams.stream()
                    .filter(param -> Boolean.TRUE.equals(param.getIsEnable())
                            && StringValidation.allNotNullOrEmpty(param.getKey(), param.getValue()))
                    .forEach(param -> uriBuilder.queryParam(param.getKey(), param.getValue()));
        }

        ApiAuthorizationSetting auth = setting.getAuthorization();
        if (auth != null &&
                auth.getAuthType() == ApiAuthType.API_KEY &&
                auth.getApiKeyPlacement() == ApiKeyPlacement.QUERY &&
                StringValidation.allNotNullOrEmpty(auth.getApiKeyName(), auth.getApiKeyValue())) {

            uriBuilder.queryParam(auth.getApiKeyName(), auth.getApiKeyValue());
        }

        URI uriEncode = uriBuilder.build()
                .encode(StandardCharsets.UTF_8)
                .toUri();
        log.debug("Built URI: {}", uriEncode);
        return uriEncode;
    }
}
