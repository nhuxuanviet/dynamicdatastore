package com.company.dynamicds.metapackage.view;

import com.company.dynamicds.metapackage.entity.MetaPackageFieldMapping;
import com.company.dynamicds.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;

/**
 * Detail view for MetaPackageFieldMapping
 * Allows editing field mapping configuration
 */
@Route(value = "meta-package-field-mappings/:id", layout = MainView.class)
@ViewController(id = "MetaPackageFieldMapping.detail")
@ViewDescriptor(path = "meta-package-field-mapping-detail-view.xml")
@EditedEntityContainer("metaPackageFieldMappingDc")
public class MetaPackageFieldMappingDetailView extends StandardDetailView<MetaPackageFieldMapping> {
}
