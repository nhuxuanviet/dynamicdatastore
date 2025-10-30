package com.company.dynamicds.apisetting.view.config.fragment;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.Renderer;
import com.company.dynamicds.apisetting.entity.ApiBody;
import com.company.dynamicds.apisetting.entity.ApiFormDataField;
import com.company.dynamicds.apisetting.entity.ApiUrlEncodedField;
import com.company.dynamicds.apisetting.enums.ApiBodyType;
import com.company.dynamicds.apisetting.enums.ApiFormDataType;
import com.company.dynamicds.apisetting.enums.ApiRawType;
import com.company.dynamicds.enums.ActionColumnType;
import com.company.dynamicds.utils.renderer.DataGridRenderers;
import com.company.dynamicds.utils.ui.GridEditorUtils;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.action.list.RemoveAction;
import io.jmix.flowui.component.codeeditor.CodeEditor;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.grid.editor.DataGridEditor;
import io.jmix.flowui.component.radiobuttongroup.JmixRadioButtonGroup;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.component.upload.FileUploadField;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.kit.component.codeeditor.CodeEditorMode;
import io.jmix.flowui.model.CollectionPropertyContainer;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.model.InstancePropertyContainer;
import io.jmix.flowui.view.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.EnumSet;

@FragmentDescriptor("body-fragment.xml")
@Slf4j
public class BodyFragment extends Fragment<VerticalLayout> {

    @ViewComponent
    private JmixComboBox<ApiRawType> rawTypeField;
    @ViewComponent
    private CodeEditor rawBodyField;
    @ViewComponent
    private VerticalLayout formDataFields;
    @ViewComponent
    private DataGrid<ApiFormDataField> apiFormDataFieldsDataGrid;
    @ViewComponent
    private CollectionPropertyContainer<ApiFormDataField> formDataFieldsDc;
    @ViewComponent
    private DataContext dataContext;
    @Autowired
    private UiComponents uiComponents;
    @ViewComponent
    private InstancePropertyContainer<ApiBody> apiBodyDc;
    @ViewComponent
    private FileUploadField fileUploadField;
    @ViewComponent
    private DataGrid<ApiUrlEncodedField> apiUrlEncodedFieldsDataGrid;
    @ViewComponent
    private CollectionPropertyContainer<ApiUrlEncodedField> urlFormEncodedFieldDc;
    @ViewComponent
    private VerticalLayout urlFormEncodedField;
    @ViewComponent("apiFormDataFieldsDataGrid.remove")
    private RemoveAction<ApiFormDataField> apiFormDataFieldsDataGridRemove;
    @ViewComponent("apiUrlEncodedFieldsDataGrid.remove")
    private RemoveAction<ApiUrlEncodedField> apiUrlEncodedFieldsDataGridRemove;

    @Subscribe(target = Target.HOST_CONTROLLER)
    public void onHostInit(final View.InitEvent event) {
        GridEditorUtils.setupInlineEditor(apiFormDataFieldsDataGrid);
        GridEditorUtils.setupInlineEditor(apiUrlEncodedFieldsDataGrid);
        setupEditorColumn();
    }

    @Subscribe
    public void onReady(ReadyEvent event) {
        apiFormDataFieldsDataGridRemove.setConfirmation(false);
        apiUrlEncodedFieldsDataGridRemove.setConfirmation(false);
    }

    private void setupEditorColumn() {
        DataGridEditor<ApiFormDataField> editor = apiFormDataFieldsDataGrid.getEditor();

        editor.setColumnEditorComponent("value", generationContext -> {
            ApiFormDataField selected = generationContext.getItem();
            if (selected.getDataType() == ApiFormDataType.FILE) {
                FileUploadField upload = uiComponents.create(FileUploadField.class);
                upload.setMaxFileSize(50 * 1024 * 1024); // 50MB
                upload.setValueSource(generationContext.getValueSourceProvider().getValueSource("fileContent"));
                upload.setFileNameVisible(true);
                return upload;
            }

            @SuppressWarnings("unchecked")
            TypedTextField<String> textField = uiComponents.create(TypedTextField.class);
            textField.setValueSource(generationContext.getValueSourceProvider().getValueSource("value"));

            return textField;
        });

    }


    @Subscribe(id = "formDataFieldsDc", target = Target.DATA_CONTAINER)
    public void onFormDataFieldsDcItemPropertyChange(final InstanceContainer.ItemPropertyChangeEvent<ApiFormDataField> event) {
        if (!"dataType".equals(event.getProperty())) {
            return;
        }

        DataGridEditor<ApiFormDataField> editor = apiFormDataFieldsDataGrid.getEditor();
        ApiFormDataField editing = editor.getItem(); // item hiện đang mở editor (có thể null)
        if (editing != null && editing.equals(event.getItem())) {
            editor.cancel();
            getUI().ifPresent(ui -> ui.beforeClientResponse(apiFormDataFieldsDataGrid,
                    ignored -> editor.editItem(editing))); // reopen để generator chạy lại -> FileUpload/TextField đổi ngay
        }
    }

    @Subscribe(id = "createFormDataBtn", subject = "clickListener")
    public void onCreateFormDataBtnClick(final ClickEvent<JmixButton> event) {
        ApiFormDataField field = createNewFormDataField();
        formDataFieldsDc.getMutableItems().add(field);
        apiFormDataFieldsDataGrid.getEditor().editItem(field);
    }

    @Subscribe(id = "createUrlEncodedBtn", subject = "clickListener")
    public void onCreateUrlEncodedBtnClick(final ClickEvent<JmixButton> event) {
        ApiUrlEncodedField field = createNewUrlEncodedField();
        urlFormEncodedFieldDc.getMutableItems().add(field);
        apiUrlEncodedFieldsDataGrid.getEditor().editItem(field);
    }

    private ApiFormDataField createNewFormDataField() {
        ApiFormDataField field = dataContext.create(ApiFormDataField.class);
        ApiBody apiBody = apiBodyDc.getItemOrNull();
        if (apiBody != null) {
            field.setApiBody(apiBody);
        }
        return field;
    }

    private ApiUrlEncodedField createNewUrlEncodedField() {
        ApiUrlEncodedField field = dataContext.create(ApiUrlEncodedField.class);
        ApiBody apiBody = apiBodyDc.getItemOrNull();
        if (apiBody != null) {
            field.setApiBody(apiBody);
        }
        return field;
    }

    @Subscribe("bodyTypeField")
    public void onBodyTypeFieldComponentValueChange(
            AbstractField.ComponentValueChangeEvent<JmixRadioButtonGroup<ApiBodyType>, ApiBodyType> event) {

        ApiBodyType selectedType = event.getValue();
        rawTypeField.setVisible(selectedType == ApiBodyType.RAW);
        rawBodyField.setVisible(selectedType == ApiBodyType.RAW);
        fileUploadField.setVisible(selectedType == ApiBodyType.BINARY);
        formDataFields.setVisible(selectedType == ApiBodyType.FORM_DATA);
        urlFormEncodedField.setVisible(selectedType == ApiBodyType.FORM_URLENCODED);
    }

    @Subscribe("rawTypeField")
    public void onRawTypeFieldComponentValueChange(final AbstractField.ComponentValueChangeEvent<JmixComboBox<ApiRawType>, ApiRawType> event) {
        ApiRawType selectedType = event.getValue();
        if (selectedType == null) {
            rawBodyField.setMode(CodeEditorMode.TEXT);
            return;
        }

        rawBodyField.setMode(selectedType.getEditorMode());
    }

    @Supply(to = "apiFormDataFieldsDataGrid.actionColumn", subject = "renderer")
    private Renderer<ApiFormDataField> apiFormDataFieldRenderer() {
        return DataGridRenderers.buildActionsColumn(
                uiComponents,
                EnumSet.of(ActionColumnType.DELETE),
                this::handleFormDataFieldsGridAction);
    }

    private void handleFormDataFieldsGridAction(ApiFormDataField item, ActionColumnType actionType) {
        if (actionType == ActionColumnType.DELETE) {
            apiFormDataFieldsDataGrid.select(item);
            apiFormDataFieldsDataGridRemove.execute();
        } else {
            log.warn("Unsupported action type: {}", actionType);
        }
    }

    @Supply(to = "apiUrlEncodedFieldsDataGrid.actionColumn", subject = "renderer")
    private Renderer<ApiUrlEncodedField> apiUrlEncodedFieldsRenderer() {
        return DataGridRenderers.buildActionsColumn(
                uiComponents,
                EnumSet.of(ActionColumnType.DELETE),
                this::handleUrlEncodedFieldsGridAction);
    }

    private void handleUrlEncodedFieldsGridAction(ApiUrlEncodedField item, ActionColumnType actionType) {
        if (actionType == ActionColumnType.DELETE) {
            apiUrlEncodedFieldsDataGrid.select(item);
            apiUrlEncodedFieldsDataGridRemove.execute();
        } else {
            log.warn("Unsupported action type: {}", actionType);
        }
    }

}