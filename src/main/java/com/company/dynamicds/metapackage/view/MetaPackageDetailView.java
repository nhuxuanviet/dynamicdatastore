package com.company.dynamicds.metapackage.view;

import com.company.dynamicds.dynamicds.entity.DynamicDataStoreConfig;
import com.company.dynamicds.metapackage.entity.MetaPackage;
import com.company.dynamicds.metapackage.entity.MetaPackageFieldMapping;
import com.company.dynamicds.metapackage.entity.MetaPackageSource;
import com.company.dynamicds.utils.ui.GridEditorUtils;
import com.company.dynamicds.view.main.MainView;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.model.CollectionPropertyContainer;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Objects;

/**
 * Detail view for MetaPackage configuration
 * Allows editing sources and field mappings
 */
@Route(value = "meta-packages/:id", layout = MainView.class)
@ViewController(id = "MetaPackage.detail")
@ViewDescriptor(path = "meta-package-detail-view.xml")
@EditedEntityContainer("metaPackageDc")
public class MetaPackageDetailView extends StandardDetailView<MetaPackage> {

    @ViewComponent
    private DataGrid<MetaPackageSource> sourcesDataGrid;
    @ViewComponent
    private CollectionLoader<DynamicDataStoreConfig> dynamicDataStoreConfigsDl;
    @ViewComponent
    private DataGrid<MetaPackageFieldMapping> fieldMappingsDataGrid;
    @ViewComponent
    private JmixComboBox<String> storeNameField;

    @ViewComponent
    private CollectionContainer<DynamicDataStoreConfig> dynamicDataStoreConfigsDc;
    @ViewComponent
    private CollectionPropertyContainer<MetaPackageFieldMapping> fieldMappingsDc;
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
        // Setup inline editors for data grids
        GridEditorUtils.setupInlineEditor(sourcesDataGrid);
        GridEditorUtils.setupInlineEditor(fieldMappingsDataGrid);
    }

    @Install(to = "sourcesDataGrid.create", subject = "initializer")
    private void sourcesCreateInitializer(com.company.dynamicds.metapackage.entity.MetaPackageSource e) {
        e.setMetaPackage(getEditedEntity()); // gán cha, tránh may-not-be-null
    }

    @Subscribe("fieldMappingsDataGrid.create")
    public void onFieldMappingsDataGridCreate(final io.jmix.flowui.action.list.CreateAction.ActionPerformedEvent event) {
        // Prevent default behavior (opening detail view)
        event.preventDefault();

        // Get selected source
        MetaPackageSource selectedSource = sourcesDataGrid.getSingleSelectedItem();
        if (selectedSource == null) {
            return; // Cannot create field mapping without a source
        }

        // Create new MetaPackageFieldMapping
        MetaPackageFieldMapping newMapping = dataManager.create(MetaPackageFieldMapping.class);
        newMapping.setMetaPackageSource(selectedSource);

        // Merge into data context
        MetaPackageFieldMapping mergedMapping = dataContext.merge(newMapping);

        // Add to collection
        fieldMappingsDc.getMutableItems().add(mergedMapping);

        // Start editing the new row
        fieldMappingsDataGrid.getEditor().editItem(mergedMapping);
    }

    @Subscribe("fieldMappingsDataGrid.edit")
    public void onFieldMappingsDataGridEdit(final io.jmix.flowui.action.list.EditAction.ActionPerformedEvent event) {
        // Prevent default behavior (opening detail view)
        event.preventDefault();

        // Get selected item and start inline editing
        MetaPackageFieldMapping selectedMapping = fieldMappingsDataGrid.getSingleSelectedItem();
        if (selectedMapping != null) {
            fieldMappingsDataGrid.getEditor().editItem(selectedMapping);
        }
    }

}