package com.company.dynamicds.view.dynamicdatastore.metadata;


import com.company.dynamicds.dynamicds.DynamicMetaClassFactory;
import com.company.dynamicds.dynamicds.entity.MetadataDefinition;
import com.company.dynamicds.dynamicds.entity.MetadataField;
import com.company.dynamicds.dynamicds.entity.MetadataFieldType;
import com.company.dynamicds.view.main.MainView;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.LoadContext;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.model.KeyValueCollectionContainer;
import io.jmix.flowui.view.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Route(value = "manage-metadata-view", layout = MainView.class)
@ViewController(id = "dwh_ManageMetadataView")
@ViewDescriptor(path = "manage-metadata-view.xml")
@Slf4j
public class ManageMetadataView extends StandardView {

    @Autowired
    private DataManager dataManager;

    @Autowired
    private Notifications notifications;
    @ViewComponent
    private KeyValueCollectionContainer keyValueEntitiesDc;
    @ViewComponent
    private DataGrid<KeyValueEntity> keyValueEntitiesDataGrid;
    @ViewComponent
    private InstanceContainer<MetadataDefinition> metadataDefinitionDc;

    @Autowired
    private DynamicMetaClassFactory metaClassFactory;

    @Subscribe("metadataCombobox")
    public void onMetadataComboboxComponentValueChange(final AbstractField.ComponentValueChangeEvent<EntityComboBox<MetadataDefinition>, MetadataDefinition> event) {
        metadataDefinitionDc.setItem(event.getValue());
    }

    @Subscribe(id = "loadAllButton", subject = "clickListener")
    public void onLoadAllButtonClick(final ClickEvent<JmixButton> event) {
        var selected = metadataDefinitionDc.getItemOrNull();

        if (selected == null) {
            notifications.create("Please select metadata")
                    .withType(Notifications.Type.WARNING)
                    .show();
            return;
        }

        MetaClass metaClass = metaClassFactory.buildMetaClass(selected.getName(), selected.getMetadataFields(), selected.getStoreName());
        List<KeyValueEntity> entities = dataManager.loadList(new LoadContext<>(metaClass));
        keyValueEntitiesDc.setItems(entities);
        keyValueEntitiesDataGrid.removeAllColumns();

        if (entities.isEmpty()) {
            notifications.create("No records found")
                    .withType(Notifications.Type.WARNING)
                    .show();
            return;
        }

        //Tạo cột theo metadataFields
        for (MetadataField field : selected.getMetadataFields()) {
            String propName = field.getName();
            MetadataFieldType type = field.getFieldType();

            keyValueEntitiesDataGrid.addColumn(entity -> {
                        Object value = entity.getValue(propName);
                        return formatValueByType(value, type);
                    })
                    .setKey(propName)
                    .setHeader(propName)
                    .setSortable(true)
                    .setAutoWidth(true);
        }

        notifications.create("Loaded " + entities.size() + " records.")
                .withType(Notifications.Type.SUCCESS)
                .show();
    }

    private String formatValueByType(Object value, MetadataFieldType type) {
        if (value == null) return "";

        return switch (type) {
            case STRING -> value.toString();
            case INTEGER, DOUBLE -> String.valueOf(value);
//            case DATE -> {
//                if (value instanceof java.util.Date date) {
//                    yield new SimpleDateFormat("yyyy-MM-dd").format(date);
//                } else yield value.toString();
//            }
            case BOOLEAN -> value.toString();
            default -> value.toString();
        };
    }


}