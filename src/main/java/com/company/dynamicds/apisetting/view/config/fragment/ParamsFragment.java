package com.company.dynamicds.apisetting.view.config.fragment;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.grid.editor.EditorCancelEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.Renderer;
import com.company.dynamicds.apisetting.entity.ApiQueryParam;
import com.company.dynamicds.apisetting.entity.ApiSetting;
import com.company.dynamicds.enums.ActionColumnType;
import com.company.dynamicds.utils.renderer.DataGridRenderers;
import com.company.dynamicds.utils.ui.GridEditorUtils;
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

@FragmentDescriptor("params-fragment.xml")
@Slf4j
public class ParamsFragment extends Fragment<VerticalLayout> {

    @ViewComponent
    private DataContext dataContext;
    @ViewComponent
    private CollectionPropertyContainer<ApiQueryParam> apiQueryParamDc;
    @ViewComponent
    private DataGrid<ApiQueryParam> apiParamsesDataGrid;
    @ViewComponent
    private InstanceContainer<ApiSetting> apiSettingDc;
    @ViewComponent("apiParamsesDataGrid.remove")
    private RemoveAction<ApiQueryParam> apiParamsesDataGridRemove;

    @Subscribe
    public void onReady(ReadyEvent event) {
        apiParamsesDataGridRemove.setConfirmation(false);
    }

    @Subscribe(target = Target.HOST_CONTROLLER)
    public void onHostInit(final View.InitEvent event) {
        GridEditorUtils.setupInlineEditor(apiParamsesDataGrid);
    }


    @Subscribe(id = "createButton", subject = "clickListener")
    public void onCreateButtonClick(final ClickEvent<JmixButton> event) {
        ApiQueryParam newParam = createNewParam();
        apiQueryParamDc.getMutableItems().add(newParam);
        apiParamsesDataGrid.getEditor().editItem(newParam);
    }

    private ApiQueryParam createNewParam() {
        ApiQueryParam newParam = dataContext.create(ApiQueryParam.class);
        ApiSetting apiSetting = apiSettingDc.getItemOrNull();
        if (apiSetting != null) {
            newParam.setApiSetting(apiSetting);
        }
        return newParam;
    }

    @Install(to = "apiParamsesDataGrid.@editor", subject = "cancelListener")
    private void apiParamsesDataGridEditorCancelListener(final EditorCancelEvent<ApiQueryParam> editorCancelEvent) {
        ApiQueryParam item = editorCancelEvent.getItem();
        if (item.getKey() == null && item.getValue() == null) {
            apiQueryParamDc.getMutableItems().remove(item);
        }
    }

    @Supply(to = "apiParamsesDataGrid.actionColumn", subject = "renderer")
    private Renderer<ApiQueryParam> apiSettingsDataGridActionColumnRenderer() {
        return DataGridRenderers.buildActionsColumn(
                uiComponents,
                EnumSet.of(ActionColumnType.DELETE),
                this::handleQueryParamGridAction);
    }

    private void handleQueryParamGridAction(ApiQueryParam item, ActionColumnType actionType) {
        if (actionType == ActionColumnType.DELETE) {
            apiParamsesDataGrid.select(item);
            apiParamsesDataGridRemove.execute();
        } else {
            log.warn("Unsupported action type: {}", actionType);
        }

    }
}