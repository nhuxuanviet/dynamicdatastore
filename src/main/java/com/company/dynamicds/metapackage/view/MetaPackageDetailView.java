package com.company.dynamicds.metapackage.view;

import com.company.dynamicds.metapackage.entity.MetaPackage;
import com.company.dynamicds.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

@Route(value = "meta-packages/:id", layout = MainView.class)
@ViewController(id = "dwh_MetaPackage.detail")
@ViewDescriptor(path = "meta-package-detail-view.xml")
@EditedEntityContainer("metaPackageDc")
public class MetaPackageDetailView extends StandardDetailView<MetaPackage> {
}