package com.company.dynamicds.apisetting.view.config;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import com.company.dynamicds.apisetting.dto.ApiResponse;
import com.company.dynamicds.apisetting.entity.ApiAuthorizationSetting;
import com.company.dynamicds.apisetting.entity.ApiBody;
import com.company.dynamicds.apisetting.entity.ApiQueryParam;
import com.company.dynamicds.apisetting.entity.ApiSetting;
import com.company.dynamicds.apisetting.enums.ApiAuthType;
import com.company.dynamicds.apisetting.enums.ApiBodyType;
import com.company.dynamicds.apisetting.enums.ApiKeyPlacement;
import com.company.dynamicds.apisetting.enums.ApiRawType;
import com.company.dynamicds.apisetting.service.ApiExecutorService;
import com.company.dynamicds.apisetting.service.builder.UriBuilder;
import com.company.dynamicds.apisetting.view.config.fragment.ApiResponseFragment;
import com.company.dynamicds.view.main.MainView;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;

@Route(value = "api-settings/config/:id", layout = MainView.class)
@ViewController(id = "dwh_ApiSetting.configDetail")
@ViewDescriptor(path = "api-setting-config-view.xml")
@EditedEntityContainer("apiSettingDc")
@Slf4j
public class ApiSettingConfigView extends StandardDetailView<ApiSetting> {

    @Autowired
    private UriBuilder uriBuilder;

    @ViewComponent
    private InstanceContainer<ApiSetting> apiSettingDc;
    @ViewComponent
    private JmixButton sendButton;
    @Autowired
    private ApiExecutorService apiExecutorService;
    @Autowired
    private Notifications notifications;
    @ViewComponent
    private ApiResponseFragment apiResponseFragment;

    @ViewComponent
    private DataContext dataContext;

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {

        ApiSetting apiSetting = getEditedEntity();
        if (apiSetting.getAuthorization() == null) {
            ApiAuthorizationSetting auth = dataContext.create(ApiAuthorizationSetting.class);
            auth.setAuthType(ApiAuthType.NO_AUTH);
            auth.setApiKeyPlacement(ApiKeyPlacement.HEADER);
            apiSetting.setAuthorization(auth);
        }

        if (apiSetting.getApiBody() == null) {
            ApiBody body = dataContext.create(ApiBody.class);
            body.setBodyType(ApiBodyType.NONE);
            body.setRawType(ApiRawType.JSON);
            apiSetting.setApiBody(body);
        }
        buildAndUpdateFinalUrl();
    }

    private void buildAndUpdateFinalUrlWithBaseUrl(String baseUrl) {
        ApiSetting apiSetting = apiSettingDc.getItemOrNull();
        if (apiSetting == null) {
            return;
        }

        if (Boolean.TRUE.equals(apiSetting.getUseRawUrl())) {
            return;
        }

        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            apiSetting.setFinalUrl("");
            return;
        }

        try {
            apiSetting.setBaseUrl(baseUrl);

            URI uri = uriBuilder.buildUri(apiSetting);
            apiSetting.setFinalUrl(uri.toString());

            log.debug("Built final URL: {}", uri);

        } catch (Exception e) {
            log.error("Failed to build URL: {}", e.getMessage());
            apiSetting.setFinalUrl("Invalid URL: " + e.getMessage());
        }
    }

    private void buildAndUpdateFinalUrl() {
        ApiSetting apiSetting = apiSettingDc.getItemOrNull();
        if (apiSetting == null) {
            return;
        }

        String baseUrl = apiSetting.getBaseUrl();
        buildAndUpdateFinalUrlWithBaseUrl(baseUrl);
    }

    @Subscribe("baseUrlField")
    public void onBaseUrlFieldComponentValueChange1(final AbstractField.ComponentValueChangeEvent<TypedTextField<String>, String> event) {
        if (!event.isFromClient()) return;
        String newBaseUrl = event.getValue();
        buildAndUpdateFinalUrlWithBaseUrl(newBaseUrl);
    }

    @Subscribe(id = "sendButton", subject = "clickListener")
    public void onSendButtonClick(final ClickEvent<JmixButton> event) {
        ApiSetting apiSetting = apiSettingDc.getItemOrNull();
        sendButton.setEnabled(false);
        sendButton.setText("Sending...");
        try {
            log.info("Sending request to: {}", apiSetting.getFinalUrl());

            // Send request
            ApiResponse response = apiExecutorService.sendRequest(apiSetting);

            // Handle response
            apiResponseFragment.displayResponse(response);

        } catch (Exception e) {
            log.error("Failed to send request", e);
            notifications.create("Request Failed", e.getMessage()).withType(Notifications.Type.ERROR).show();
        } finally {
            sendButton.setEnabled(true);
            sendButton.setText("Send");
        }
    }

//    @Subscribe("useRawUrlField")
//    public void onUseRawUrlFieldComponentValueChange(final AbstractField.ComponentValueChangeEvent<JmixCheckbox, Boolean> event) {
//        if (!event.isFromClient()) return;
//        if (Boolean.TRUE.equals(event.getValue())) {
//            finalUrlField.setReadOnly(false);
//            log.debug("Raw URL mode enabled - manual editing allowed");
//        } else {
//            finalUrlField.setReadOnly(true);
//            buildAndUpdateFinalUrl();
//        }
//    }


    @Subscribe(id = "apiQueryParamDc", target = Target.DATA_CONTAINER)
    public void onApiQueryParamDcItemPropertyChange(final InstanceContainer.ItemPropertyChangeEvent<ApiQueryParam> event) {
        if ("key".equals(event.getProperty()) || "value".equals(event.getProperty())) {
            buildAndUpdateFinalUrl();
        }
    }

    @Subscribe(id = "apiQueryParamDc", target = Target.DATA_CONTAINER)
    public void onApiQueryParamDcCollectionChange(final CollectionContainer.CollectionChangeEvent<ApiQueryParam> event) {
        buildAndUpdateFinalUrl();
    }

    @Subscribe(id = "authorizationDc", target = Target.DATA_CONTAINER)
    public void onAuthorizationDcItemPropertyChange(final InstanceContainer.ItemPropertyChangeEvent<ApiAuthorizationSetting> event) {
        if ("apiKeyPlacement".equals(event.getProperty()) || "apiKeyName".equals(event.getProperty()) || "apiKeyValue".equals(event.getProperty())) {
            buildAndUpdateFinalUrl();
        }

    }

}