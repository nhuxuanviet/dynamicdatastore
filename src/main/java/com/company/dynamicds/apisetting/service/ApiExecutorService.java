package com.company.dynamicds.apisetting.service;

import com.company.dynamicds.apisetting.dto.ApiResponse;
import com.company.dynamicds.apisetting.entity.ApiSetting;
import com.company.dynamicds.apisetting.service.builder.HeaderBuilder;
import com.company.dynamicds.apisetting.service.builder.RequestBodyBuilder;
import com.company.dynamicds.apisetting.service.builder.UriBuilder;
import io.jmix.core.DataManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApiExecutorService {

    private final RestClient restClient;
    private final UriBuilder uriBuilder;
    private final HeaderBuilder headerBuilder;
    private final RequestBodyBuilder bodyBuilder;
    private final ScriptService scriptService;
    @Autowired
    protected DataManager dataManager;

    public ApiResponse sendRequest(ApiSetting setting) {
        Instant start = Instant.now();

        try {
            HttpMethod method = HttpMethod.valueOf(setting.getHttpMethod().name());
            URI uri = uriBuilder.buildUri(setting);
            HttpHeaders headers = headerBuilder.buildHeaders(setting);
            Object requestBody = bodyBuilder.buildApiBody(setting.getApiBody());

            log.debug("Sending {} {}", method, uri);
            log.trace("Headers: {}", sanitizeHeadersForLog(headers));
            if (requestBody != null) {
                log.trace("Body type: {}", requestBody.getClass().getSimpleName());
            }

            RestClient.RequestBodySpec requestSpec = restClient
                    .method(method)
                    .uri(uri)
                    .headers(httpHeaders -> httpHeaders.putAll(headers));

            if (supportsBody(method) && requestBody != null) {
                requestSpec = requestSpec.body(requestBody);
            }

            ResponseEntity<byte[]> response = requestSpec.exchange((req, res) -> {
                try (InputStream bodyStream = res.getBody()) {
                    byte[] bodyBytes = bodyStream.readAllBytes();
                    return ResponseEntity.status(res.getStatusCode())
                            .headers(res.getHeaders())
                            .body(bodyBytes);
                } catch (Exception e) {
                    log.warn("Failed to read response body: {}", e.getMessage());
                    return ResponseEntity.status(res.getStatusCode())
                            .headers(res.getHeaders())
                            .body(new byte[0]);
                }
            });

            Objects.requireNonNull(response, "ResponseEntity is null after exchange()");
            long duration = Duration.between(start, Instant.now()).toMillis();

            MediaType contentType = response.getHeaders().getContentType();
            String responseBodyText = convertBodyToText(response.getBody(), contentType);

            ApiResponse apiResponse = ApiResponse.builder()
                    .success(response.getStatusCode().is2xxSuccessful())
                    .statusCode(response.getStatusCode().value())
                    .statusText(response.getStatusCode().toString())
                    .mediaType(contentType != null ? contentType.toString() : "unknown")
                    .headers(response.getHeaders())
                    .bodyText(responseBodyText)
                    .bodyBytes(response.getBody())
                    .durationMs(duration)
                    .effectiveUri(uri)
                    .build();

            // Post-response script check
            executePostResponseScriptIfExists(setting, apiResponse);

            return apiResponse;

        } catch (RestClientResponseException ex) {
            long duration = Duration.between(start, Instant.now()).toMillis();
            HttpHeaders errorHeaders = Optional.ofNullable(ex.getResponseHeaders()).orElse(HttpHeaders.EMPTY);
            MediaType contentType = errorHeaders.getContentType();
            byte[] errorBody = ex.getResponseBodyAsByteArray();
            String errorText = convertBodyToText(errorBody, contentType);

            log.error("HTTP {} after {} ms: {}", ex.getStatusCode(), duration, ex.getMessage());

            ApiResponse errorResponse = ApiResponse.builder()
                    .success(false)
                    .statusCode(ex.getStatusCode().value())
                    .statusText(ex.getStatusText())
                    .mediaType(contentType != null ? contentType.toString() : "unknown")
                    .headers(errorHeaders)
                    .bodyText(errorText)
                    .bodyBytes(errorBody)
                    .durationMs(duration)
                    .errorMessage(ex.getMessage())
                    .build();

            executePostResponseScriptIfExists(setting, errorResponse);

            return errorResponse;

        } catch (Exception ex) {
            long duration = Duration.between(start, Instant.now()).toMillis();
            log.error("Request failed after {} ms: {}", duration, ex.getMessage(), ex);

            return ApiResponse.builder()
                    .success(false)
                    .statusCode(-1)
                    .statusText("CLIENT_ERROR")
                    .durationMs(duration)
                    .errorMessage(ex.getMessage())
                    .build();
        }
    }

    private void executePostResponseScriptIfExists(ApiSetting setting, ApiResponse apiResponse) {
        try {
            String script = setting.getPostResponseScript();
            if (script == null || script.isBlank()) {
                return;
            }

            log.debug("Executing post-response script...");

            Map<String, Object> bindings = Map.of(
                    "response", Map.of(
                            "statusCode", apiResponse.getStatusCode(),
                            "header", apiResponse.getHeaders(),
                            "body", apiResponse.getBodyText()
                    )
            );

            Object result = scriptService.execute(script, bindings);

            apiResponse.setBodyText(
                    result != null ? result.toString() : "(no output)"
            );
            log.info("Script executed successfully.");
        } catch (Exception e) {
            apiResponse.setScriptError(e.getMessage());
            log.warn("Script execution failed: {}", e.getMessage(), e);
        }
    }

    private boolean supportsBody(HttpMethod method) {
        return Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH).contains(method);
    }

    private String convertBodyToText(@Nullable byte[] bytes, @Nullable MediaType contentType) {
        if (bytes == null || !isTextLike(contentType)) return "";
        Charset charset = Optional.ofNullable(contentType)
                .map(MediaType::getCharset)
                .orElse(StandardCharsets.UTF_8);
        return new String(bytes, charset);
    }

    private boolean isTextLike(@Nullable MediaType contentType) {
        if (contentType == null) return true;
        if ("text".equalsIgnoreCase(contentType.getType())) return true;
        String subtype = contentType.getSubtype();
        return Stream.of("json", "xml", "html", "javascript", "x-www-form-urlencoded")
                .anyMatch(subtype::contains);
    }

    private HttpHeaders sanitizeHeadersForLog(HttpHeaders original) {
        HttpHeaders sanitized = new HttpHeaders();
        original.forEach((key, value) -> {
            if (isSensitiveHeader(key)) {
                sanitized.put(key, List.of("REDACTED"));
            } else {
                sanitized.put(key, value);
            }
        });
        return sanitized;
    }

    private boolean isSensitiveHeader(String key) {
        if (key == null) return false;
        return HttpHeaders.AUTHORIZATION.equalsIgnoreCase(key)
                || "X-API-KEY".equalsIgnoreCase(key);
    }


}
