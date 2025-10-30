package com.company.dynamicds.view.dynamicdatastore;

import com.company.dynamicds.dynamicds.DynamicDataStoreRegister;
import com.company.dynamicds.dynamicds.entity.DynamicDataStoreConfig;
import com.company.dynamicds.enums.ActionColumnType;
import com.company.dynamicds.utils.renderer.DataGridRenderers;
import com.company.dynamicds.view.main.MainView;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.Stores;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.view.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.EnumSet;


@Route(value = "dynamic-data-stores", layout = MainView.class)
@ViewController(id = "dwh_DynamicDataStore.list")
@ViewDescriptor(path = "dynamic-data-store-list-view.xml")
@LookupComponent("dynamicDataStoresDataGrid")
@DialogMode(width = "64em")
@Slf4j
public class DynamicDataStoreListView extends StandardListView<DynamicDataStoreConfig> {
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private Stores stores;
    @Autowired
    private DynamicDataStoreRegister dataStoreRegister;

    @Supply(to = "dynamicDataStoresDataGrid.actionColumn", subject = "renderer")
    private Renderer<DynamicDataStoreConfig> dynamicDataStoresDataGridActionColumnRenderer() {
        return DataGridRenderers.buildActionsColumn(uiComponents, EnumSet.of(ActionColumnType.ACTIVE, ActionColumnType.DELETE), this::handleGridAction);
    }

    private void handleGridAction(DynamicDataStoreConfig item, ActionColumnType actionType) {
        switch (actionType) {
            case ACTIVE:
                dataStoreRegister.registerDataStore(item);
                log.info(" DataStore [{}] registered!", item.getStoreName());
                log.info("===== ACTIVE DATA STORES =====");
                log.info("===============================");
                break;
            case DELETE:
                // Handle delete action
                break;
            default:
                break;
        }
    }


}