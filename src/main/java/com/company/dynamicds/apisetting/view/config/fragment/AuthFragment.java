package com.company.dynamicds.apisetting.view.config.fragment;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.company.dynamicds.apisetting.enums.ApiAuthType;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import lombok.extern.slf4j.Slf4j;

@FragmentDescriptor("auth-fragment.xml")
@Slf4j
public class AuthFragment extends Fragment<VerticalLayout> {

    @ViewComponent
    private VerticalLayout noAuth;
    @ViewComponent
    private VerticalLayout basicAuthFields;
    @ViewComponent
    private VerticalLayout bearerTokenFields;
    @ViewComponent
    private VerticalLayout apiKeyFields;

    @Subscribe("apiAuthTypeField")
    public void onApiAuthTypeFieldComponentValueChange(final AbstractField.ComponentValueChangeEvent<JmixComboBox<ApiAuthType>, ApiAuthType> event) {
        updateVisibility(event.getValue());
    }

    private void updateVisibility(ApiAuthType authType) {
        noAuth.setVisible(authType == ApiAuthType.NO_AUTH);
        basicAuthFields.setVisible(authType == ApiAuthType.BASIC_AUTH);
        bearerTokenFields.setVisible(authType == ApiAuthType.BEARER_TOKEN);
        apiKeyFields.setVisible(authType == ApiAuthType.API_KEY);
    }

}