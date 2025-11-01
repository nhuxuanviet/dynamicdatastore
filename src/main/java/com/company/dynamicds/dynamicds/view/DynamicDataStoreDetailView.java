package com.company.dynamicds.dynamicds.view;

import com.company.dynamicds.dynamicds.entity.DynamicDataStoreConfig;
import com.company.dynamicds.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

@Route(value = "dynamic-data-stores/:id", layout = MainView.class)
@ViewController(id = "dwh_DynamicDataStore.detail")
@ViewDescriptor(path = "dynamic-data-store-detail-view.xml")
@EditedEntityContainer("dynamicDataStoreDc")
public class DynamicDataStoreDetailView extends StandardDetailView<DynamicDataStoreConfig> {
}