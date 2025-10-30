package com.company.dynamicds.apisetting.view;

import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.company.dynamicds.apisetting.entity.ApiSetting;
import com.company.dynamicds.enums.ActionColumnType;
import com.company.dynamicds.utils.renderer.DataGridRenderers;
import com.company.dynamicds.view.main.MainView;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.action.list.EditAction;
import io.jmix.flowui.action.list.RemoveAction;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.view.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.EnumSet;


@Route(value = "api-settings", layout = MainView.class)
@ViewController(id = "dwh_ApiSetting.list")
@ViewDescriptor(path = "api-setting-list-view.xml")
@LookupComponent("apiSettingsDataGrid")
@DialogMode(width = "64em")
@Slf4j
public class ApiSettingListView extends StandardListView<ApiSetting> {

    @ViewComponent
    private DataGrid<ApiSetting> apiSettingsDataGrid;

    @ViewComponent("apiSettingsDataGrid.editAction")
    private EditAction<ApiSetting> apiSettingsDataGridEditAction;

    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private ViewNavigators viewNavigators;
    @ViewComponent("apiSettingsDataGrid.removeAction")
    private RemoveAction<ApiSetting> apiSettingsDataGridRemoveAction;

    @Supply(to = "apiSettingsDataGrid.actionColumn", subject = "renderer")
    private Renderer<ApiSetting> apiSettingsDataGridActionColumnRenderer() {
        return DataGridRenderers.buildActionsColumn(
                uiComponents,
                EnumSet.of(ActionColumnType.EDIT, ActionColumnType.SETTING, ActionColumnType.DELETE),
                this::handleGridAction);
    }

    private void handleGridAction(ApiSetting item, ActionColumnType actionType) {
        switch (actionType) {
            case EDIT -> {
                apiSettingsDataGrid.select(item);
                apiSettingsDataGridEditAction.actionPerform(apiSettingsDataGrid);
            }
            case SETTING -> openSettingDialog(item);
            case DELETE -> {
                apiSettingsDataGrid.select(item);
                apiSettingsDataGridRemoveAction.actionPerform(apiSettingsDataGrid);
            }
            default -> log.warn("Unsupported action type: {}", actionType);
        }

    }

    private void openSettingDialog(ApiSetting item) {

        viewNavigators.detailView(this, ApiSetting.class)
                .editEntity(item)
                .withViewId("dwh_ApiSetting.configDetail")
                .withRouteParameters(new RouteParameters("id", item.getId().toString()))
                .navigate();
    }

}