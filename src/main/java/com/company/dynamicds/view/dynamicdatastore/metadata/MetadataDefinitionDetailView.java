package com.company.dynamicds.view.dynamicdatastore.metadata;

import com.company.dynamicds.dynamicds.DynamicDataStoreService;
import com.company.dynamicds.dynamicds.entity.DynamicDataStoreConfig;
import com.company.dynamicds.dynamicds.entity.MetadataDefinition;
import com.company.dynamicds.dynamicds.entity.MetadataField;
import com.company.dynamicds.utils.ui.GridEditorUtils;
import com.company.dynamicds.utils.validation.StringValidation;
import com.company.dynamicds.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.model.CollectionPropertyContainer;
import io.jmix.flowui.model.InstanceContainer;
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


}