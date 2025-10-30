package com.company.dynamicds.apisetting.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpHeaders;

import java.net.URI;

@Builder
@Getter
@Setter
public class ApiResponse {

    private boolean success;           // true nếu status < 400
    private int statusCode;            // ví dụ: 200, 404, 500
    private String statusText;         // ví dụ: OK, Not Found, Internal Server Error

    private String mediaType;          // ví dụ: application/json
    private HttpHeaders headers;       // tất cả headers trả về

    private String bodyText;           // nếu là text/json
    private byte[] bodyBytes;          // nếu là binary file

    private long durationMs;           // thời gian thực thi
    private URI effectiveUri;          // URL sau redirect
    private String errorMessage;

    private String finalBody;
    private String scriptError;
}
