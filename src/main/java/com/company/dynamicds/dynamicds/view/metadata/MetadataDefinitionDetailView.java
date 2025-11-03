package com.company.dynamicds.dynamicds.view.metadata;

import com.company.dynamicds.dynamicds.DynamicDataStoreService;
import com.company.dynamicds.dynamicds.entity.DynamicDataStoreConfig;
import com.company.dynamicds.dynamicds.entity.MetadataDefinition;
import com.company.dynamicds.dynamicds.entity.MetadataField;
import com.company.dynamicds.utils.ui.GridEditorUtils;
import com.company.dynamicds.utils.validation.StringValidation;
import com.company.dynamicds.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.*;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Route(value = "metadata-definitions/:id", layout = MainView.class)
@ViewController(id = "dwh_MetadataDefinition.detail")
@ViewDescriptor(path = "metadata-definition-detail-view.xml")
@EditedEntityContainer("metadataDefinitionDc")
public class MetadataDefinitionDetailView extends StandardDetailView<MetadataDefinition> {
    @ViewComponent
    private CollectionContainer<DynamicDataStoreConfig> dynamicDataStoreConfigsDc;
    @ViewComponent
    private JmixComboBox<String> storeNameField;
    @Autowired
    private Notifications notifications;
    @Autowired
    private DynamicDataStoreService dynamicDataStoreService;
    @ViewComponent
    private CollectionLoader<DynamicDataStoreConfig> dynamicDataStoreConfigsDl;


    @ViewComponent
    private DataGrid<MetadataField> metadataFieldsDataGrid;
    @ViewComponent
    private CollectionPropertyContainer<MetadataField> metadataFieldsDc;
    @ViewComponent
    private DataContext dataContext;
    @Autowired
    private DataManager dataManager;

    @Subscribe
    public void onInit(final InitEvent event) {
        dynamicDataStoreConfigsDl.load();
        List<String> storeNames = dynamicDataStoreConfigsDc.getItems().stream()
                .map(DynamicDataStoreConfig::getStoreName)
                .toList();
        storeNameField.setItems(storeNames);
        GridEditorUtils.setupInlineEditor(metadataFieldsDataGrid);
    }

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        MetadataDefinition metadata = getEditedEntity();
        if (metadata.getStoreName() != null) {
            storeNameField.setValue(metadata.getStoreName());
        }
    }


    @Subscribe(id = "generateFromUrlButton", subject = "clickListener")
    public void onGenerateFromUrlButtonClick(final ClickEvent<JmixButton> event) {

        MetadataDefinition metadata = getEditedEntity();
        if (StringValidation.isNullOrEmpty(metadata.getUrl())
                || StringValidation.isNullOrEmpty(metadata.getName())
                || StringValidation.isNullOrEmpty(metadata.getStoreName())) {
            notifications.create("Please fill Name, Store Name and URL before generating.")
                    .withType(Notifications.Type.WARNING)
                    .show();
            return;
        }

        try {
            MetadataDefinition updatedMetadata = dynamicDataStoreService.generateFromApi(metadata);
            // Merge các MetadataField vào DataContext
            for (MetadataField field : updatedMetadata.getMetadataFields()) {
                dataContext.merge(field);
            }

            getEditedEntityContainer().setItem(updatedMetadata);
            metadataFieldsDc.setItems(updatedMetadata.getMetadataFields());
            notifications.create("Fields generated successfully").show();

        } catch (Exception e) {
            notifications.create("Failed to generate fields: " + e.getMessage())
                    .withType(Notifications.Type.ERROR)
                    .show();
        }


    }

    @Subscribe(id = "metadataDefinitionDc", target = Target.DATA_CONTAINER)
    public void onMetadataDefinitionDcItemChange(final InstanceContainer.ItemChangeEvent<MetadataDefinition> event) {

    }

    @Subscribe("metadataFieldsDataGrid.createInline")
    public void onCreateInlineAction(io.jmix.flowui.kit.action.ActionPerformedEvent event) {
        // Tạo entity mới
        MetadataField newField = dataManager.create(MetadataField.class);
        newField.setMetadataDefinition(getEditedEntity());

        // Đưa vào DataContext để quản lý state
        MetadataField merged = dataContext.merge(newField);

        // Thêm vào DC (sẽ hiện ngay trong grid)
        metadataFieldsDc.getMutableItems().add(merged);

        // Bắt đầu edit inline dòng mới
        metadataFieldsDataGrid.getEditor().editItem(merged);
    }

    @Subscribe("metadataFieldsDataGrid.editInline")
    public void onEditInlineAction(io.jmix.flowui.kit.action.ActionPerformedEvent event) {
        MetadataField selected = metadataFieldsDataGrid.getSingleSelectedItem();
        if (selected != null) {
            metadataFieldsDataGrid.getEditor().editItem(selected);
        }
    }


}