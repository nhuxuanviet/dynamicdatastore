package com.company.dynamicds.apisetting.view.config.fragment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.company.dynamicds.apisetting.dto.ApiResponse;
import com.company.dynamicds.apisetting.dto.ResponseHeader;
import com.company.dynamicds.apisetting.enums.ApiRawType;
import com.company.dynamicds.utils.validation.StringValidation;
import io.jmix.flowui.component.codeeditor.CodeEditor;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.kit.component.codeeditor.CodeEditorMode;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.view.ViewComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@FragmentDescriptor("api-response-fragment.xml")
@Slf4j
public class ApiResponseFragment extends Fragment<VerticalLayout> {

    @ViewComponent
    private CodeEditor responseBody;
    @ViewComponent
    private Span timeLabel;
    @ViewComponent
    private Span statusBadge;
    @ViewComponent
    private Span sizeLabel;
    @ViewComponent
    private CollectionContainer<ResponseHeader> responseHeadersDc;

    @Autowired
    private ObjectMapper objectMapper;
    @ViewComponent
    private DataContext dataContext;

    public void displayResponse(ApiResponse apiResponse) {
        if (!apiResponse.isSuccess() || !StringValidation.isNullOrEmpty(apiResponse.getErrorMessage())) {
            displayError(apiResponse);
        } else {
            displayBody(apiResponse);
        }
        displayStatusBar(apiResponse);
        displayHeaders(apiResponse);
    }

    private void displayBody(ApiResponse apiResponse) {
        String bodyText = apiResponse.getBodyText();

        if (StringValidation.isNullOrEmpty(bodyText)) {
            responseBody.setMode(CodeEditorMode.TEXT);
            responseBody.setValue("// Empty response");
            return;
        }

        ApiRawType mediaType = ApiRawType.fromMediaType(apiResponse.getMediaType());
        if (mediaType == null) {
            responseBody.setMode(CodeEditorMode.TEXT);
            responseBody.setValue(bodyText);
            return;
        }

        if (mediaType == ApiRawType.JSON) {
            try {
                Object json = objectMapper.readValue(bodyText, Object.class);
                String pretty = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
                responseBody.setMode(mediaType.getEditorMode());
                responseBody.setValue(pretty);
                return;
            } catch (JsonProcessingException e) {
                log.warn("Invalid JSON response", e);
            }
        }

        responseBody.setMode(mediaType.getEditorMode());
        responseBody.setValue(bodyText);
    }

    private void displayError(ApiResponse apiResponse) {
        String message = apiResponse.getErrorMessage();
        if (StringValidation.isNullOrEmpty(message)) {
            message = "Unknown error occurred while executing the request.";
        }

        responseBody.setMode(CodeEditorMode.TEXT);
        responseBody.setValue(message);
    }

    private void displayStatusBar(ApiResponse response) {
        statusBadge.setVisible(true);

        int code = response.getStatusCode();
        String text = response.getStatusText();

        if (response.isSuccess()) {
            statusBadge.setText(text);
            statusBadge.getElement().getThemeList().add("success");
        } else if (code >= 400 && code < 500) {
            statusBadge.setText(text);
            statusBadge.getElement().getThemeList().add("error");
        } else if (code >= 500) {
            statusBadge.setText(text);
            statusBadge.getElement().getThemeList().add("warning");
        } else {
            statusBadge.setText("Unknown Status");
            statusBadge.getElement().getThemeList().add("contrast");
        }

        // Hiển thị thời gian thực thi
        timeLabel.setText("Time: " + response.getDurationMs() + " ms");
        timeLabel.setVisible(true);

        // Hiển thị kích thước phản hồi
        String size = formatBytes(response.getBodyBytes() != null ? response.getBodyBytes().length : 0);
        sizeLabel.setText("Size: " + size);
        sizeLabel.setVisible(true);
    }

    private void displayHeaders(ApiResponse apiResponse) {
        HttpHeaders httpHeaders = apiResponse.getHeaders();
        List<ResponseHeader> responseHeaders = convertHeadersToResponseItems(httpHeaders);

        responseHeadersDc.getMutableItems().clear();
        responseHeadersDc.getMutableItems().addAll(responseHeaders);
    }

    private List<ResponseHeader> convertHeadersToResponseItems(HttpHeaders httpHeaders) {
        List<ResponseHeader> items = new ArrayList<>();
        if (httpHeaders == null || httpHeaders.isEmpty()) {
            return items;
        }

        for (Map.Entry<String, List<String>> headerEntry : httpHeaders.entrySet()) {
            String headerName = headerEntry.getKey();
            String headerValue = joinHeaderValues(headerEntry.getValue());
            ResponseHeader responseHeader = dataContext.create(ResponseHeader.class);
            headerName = capitalizeHeaderKey(headerName);
            responseHeader.setKey(headerName);
            responseHeader.setValue(headerValue);
            items.add(responseHeader);
        }
        return items;
    }

    private String capitalizeHeaderKey(String key) {
        if (key == null || key.isEmpty()) return key;
        return Arrays.stream(key.split("-"))
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
                .collect(Collectors.joining("-"));
    }


    private String joinHeaderValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join(", ", values);
    }


    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return new DecimalFormat("#.##").format(bytes / 1024.0) + " KB";
        } else {
            return new DecimalFormat("#.##").format(bytes / (1024.0 * 1024)) + " MB";
        }
    }
}
