package com.company.dynamicds.metapackage.view;

import com.company.dynamicds.metapackage.entity.MetaPackage;
import com.company.dynamicds.metapackage.entity.MetaPackageFieldMapping;
import com.company.dynamicds.metapackage.entity.MetaPackageSource;
import com.company.dynamicds.utils.ui.GridEditorUtils;
import com.company.dynamicds.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.view.*;

/**
 * Detail view for MetaPackage configuration
 * Allows editing sources and field mappings
 */
@Route(value = "meta-packages/:id", layout = MainView.class)
@ViewController(id = "dwh_MetaPackage.detail")
@ViewDescriptor(path = "meta-package-detail-view.xml")
@EditedEntityContainer("metaPackageDc")
public class MetaPackageDetailView extends StandardDetailView<MetaPackage> {

    @ViewComponent
    private DataGrid<MetaPackageSource> sourcesDataGrid;

    @ViewComponent
    private DataGrid<MetaPackageFieldMapping> fieldMappingsDataGrid;

    @Subscribe
    public void onInit(final InitEvent event) {
        // Setup inline editors for data grids
        GridEditorUtils.setupInlineEditor(sourcesDataGrid);
        GridEditorUtils.setupInlineEditor(fieldMappingsDataGrid);
    }
}
