package com.company.dynamicds.metapackage.view;

import com.company.dynamicds.metapackage.entity.MetaPackageFieldMapping;
import com.company.dynamicds.metapackage.entity.MetaPackageSource;
import com.company.dynamicds.utils.ui.GridEditorUtils;
import com.company.dynamicds.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.view.*;

/**
 * Detail view for MetaPackageSource
 * Allows editing source configuration and field mappings
 */
@Route(value = "meta-package-sources/:id", layout = MainView.class)
@ViewController(id = "MetaPackageSource.detail")
@ViewDescriptor(path = "meta-package-source-detail-view.xml")
@EditedEntityContainer("metaPackageSourceDc")
public class MetaPackageSourceDetailView extends StandardDetailView<MetaPackageSource> {

//    @ViewComponent
//    private DataGrid<MetaPackageFieldMapping> fieldMappingsDataGrid;

//    @Subscribe
//    public void onInit(final InitEvent event) {
//        // Setup inline editor for field mappings grid
//        GridEditorUtils.setupInlineEditor(fieldMappingsDataGrid);
//    }
}
