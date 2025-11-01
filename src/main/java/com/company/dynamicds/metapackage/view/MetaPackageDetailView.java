package com.company.dynamicds.metapackage.view;

import com.company.dynamicds.dynamicds.entity.DynamicDataStoreConfig;
import com.company.dynamicds.metapackage.entity.MetaPackage;
import com.company.dynamicds.metapackage.entity.MetaPackageFieldMapping;
import com.company.dynamicds.metapackage.entity.MetaPackageSource;
import com.company.dynamicds.utils.ui.GridEditorUtils;
import com.company.dynamicds.view.main.MainView;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;

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

}