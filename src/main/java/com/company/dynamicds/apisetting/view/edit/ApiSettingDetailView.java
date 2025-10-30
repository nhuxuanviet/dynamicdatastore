package com.company.dynamicds.apisetting.view.edit;

import com.vaadin.flow.router.Route;
import com.company.dynamicds.apisetting.entity.ApiSetting;
import com.company.dynamicds.apisetting.enums.HttpMethodType;
import com.company.dynamicds.view.main.MainView;
import io.jmix.flowui.view.*;

@Route(value = "api-settings/:id", layout = MainView.class)
@ViewController(id = "dwh_ApiSetting.detail")
@ViewDescriptor(path = "api-setting-detail-view.xml")
@EditedEntityContainer("apiSettingDc")
@PrimaryDetailView(ApiSetting.class)
public class ApiSettingDetailView extends StandardDetailView<ApiSetting> {

    @Subscribe
    public void onInitEntity(InitEntityEvent<ApiSetting> event) {
        ApiSetting apiSetting = event.getEntity();
        apiSetting.setHttpMethod(HttpMethodType.GET);
    }

}