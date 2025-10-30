package com.company.dynamicds.apisetting.view.config.fragment;

import com.company.dynamicds.apisetting.entity.ApiHeader;
import com.company.dynamicds.apisetting.entity.ApiSetting;
import com.company.dynamicds.enums.ActionColumnType;
import com.company.dynamicds.utils.renderer.DataGridRenderers;
import com.company.dynamicds.utils.ui.GridEditorUtils;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.grid.editor.EditorCancelEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.Renderer;
import io.jmix.flowui.action.list.RemoveAction;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionPropertyContainer;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.*;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumSet;

@FragmentDescriptor("header-fragment.xml")
@Slf4j
public class HeaderFragment extends Fragment<VerticalLayout> {

    @ViewComponent
    private DataGrid<ApiHeader> apiHeadersDataGrid;
    @ViewComponent
    private DataContext dataContext;
    @ViewComponent
    private CollectionPropertyContainer<ApiHeader> apiHeaderDc;

    @ViewComponent
    private InstanceContainer<ApiSetting> apiSettingDc;

    @ViewComponent("apiHeadersDataGrid.remove")
    private RemoveAction<ApiHeader> apiHeadersDataGridRemove;

    @Subscribe
    public void onReady(ReadyEvent event) {
        apiHeadersDataGridRemove.setConfirmation(false);
    }

    @Subscribe(target = Target.HOST_CONTROLLER)
    public void onHostInit(final View.InitEvent event) {
        GridEditorUtils.setupInlineEditor(apiHeadersDataGrid);
    }

    @Subscribe(id = "createButton", subject = "clickListener")
    public void onCreateButtonClick(final ClickEvent<JmixButton> event) {
        ApiHeader newHeader = createNewHeader();
        apiHeaderDc.getMutableItems().add(newHeader);
        apiHeadersDataGrid.getEditor().editItem(newHeader);
    }


    private ApiHeader createNewHeader() {
        ApiHeader newHeader = dataContext.create(ApiHeader.class);
        ApiSetting apiSetting = apiSettingDc.getItemOrNull();
        if (apiSetting != null) {
            newHeader.setApiSetting(apiSetting);
        }
        return newHeader;
    }

    @Install(to = "apiHeadersDataGrid.@editor", subject = "cancelListener")
    private void apiHeadersDataGridEditorCancelListener(final EditorCancelEvent<ApiHeader> event) {
        ApiHeader item = event.getItem();
        if (item.getKey() == null && item.getValue() == null) {
            apiHeaderDc.getMutableItems().remove(item);
        }
    }

    @Supply(to = "apiHeadersDataGrid.actionColumn", subject = "renderer")
    private Renderer<ApiHeader> apiSettingsDataGridActionColumnRenderer() {
        return DataGridRenderers.buildActionsColumn(
                uiComponents,
                EnumSet.of(ActionColumnType.DELETE),
                this::handleHeaderGridAction);
    }

    private void handleHeaderGridAction(ApiHeader item, ActionColumnType actionType) {
        if (actionType == ActionColumnType.DELETE) {
            apiHeadersDataGrid.select(item);
            apiHeadersDataGridRemove.execute();
        } else {
            log.warn("Unsupported action type: {}", actionType);
        }
    }

}