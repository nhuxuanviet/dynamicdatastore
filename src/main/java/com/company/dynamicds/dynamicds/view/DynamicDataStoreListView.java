package com.company.dynamicds.dynamicds.view;

import com.company.dynamicds.dynamicds.DynamicDataStoreRegister;
import com.company.dynamicds.dynamicds.entity.DynamicDataStoreConfig;
import com.company.dynamicds.enums.ActionColumnType;
import com.company.dynamicds.utils.renderer.DataGridRenderers;
import com.company.dynamicds.view.main.MainView;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.kit.action.ActionVariant;
import io.jmix.flowui.model.CollectionContainer;
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
    private DynamicDataStoreRegister dataStoreRegister;
    @Autowired
    private Notifications notifications;
    @Autowired
    private Dialogs dialogs;
    @Autowired
    private DataManager dataManager;

    @ViewComponent
    private CollectionContainer<DynamicDataStoreConfig> dynamicDataStoresDc;

    @Supply(to = "dynamicDataStoresDataGrid.actionColumn", subject = "renderer")
    private Renderer<DynamicDataStoreConfig> dynamicDataStoresDataGridActionColumnRenderer() {
        return DataGridRenderers.buildActionsColumn(uiComponents, EnumSet.of(ActionColumnType.ACTIVE, ActionColumnType.DELETE), this::handleGridAction);
    }

    private void handleGridAction(DynamicDataStoreConfig item, ActionColumnType actionType) {
        switch (actionType) {
            case ACTIVE:
                activateDataStore(item);
                break;
            case DELETE:
                deleteDataStore(item);
                break;
            default:
                break;
        }
    }

    private void activateDataStore(DynamicDataStoreConfig item) {
        try {
            dataStoreRegister.registerDataStore(item);

            notifications.create("Data Store Activated")
                    .withType(Notifications.Type.SUCCESS)
                    .withPosition(Notification.Position.TOP_END)
                    .show();

            log.info("Data store '{}' activated successfully", item.getStoreName());

        } catch (Exception e) {
            notifications.create("Activation Failed")
                    .withType(Notifications.Type.ERROR)
                    .withPosition(Notification.Position.TOP_END)
                    .show();

            log.error("Failed to activate data store '{}'", item.getStoreName(), e);
        }
    }

    private void deleteDataStore(DynamicDataStoreConfig item) {
        dialogs.createOptionDialog()
                .withHeader("Confirm Delete")
                .withText("Are you sure you want to delete data store '" + item.getStoreName() + "'?")
                .withActions(
                        new DialogAction(DialogAction.Type.YES)
                                .withText("Delete")
                                .withVariant(ActionVariant.DANGER)
                                .withHandler(e -> performDelete(item)),
                        new DialogAction(DialogAction.Type.NO)
                                .withText("Cancel")
                                .withVariant(ActionVariant.PRIMARY)
                )
                .open();
    }

    private void performDelete(DynamicDataStoreConfig item) {
        try {
            dataManager.remove(item);

            // Refresh collection
            dynamicDataStoresDc.getMutableItems().remove(item);

            notifications.create("Data Store Deleted")
                    .withType(Notifications.Type.SUCCESS)
                    .withPosition(Notification.Position.TOP_END)
                    .show();

            log.info("Data store '{}' deleted successfully", item.getStoreName());

        } catch (Exception e) {
            notifications.create("Delete Failed")
                    .withType(Notifications.Type.ERROR)
                    .withPosition(Notification.Position.TOP_END)
                    .show();

            log.error("Failed to delete data store '{}'", item.getStoreName(), e);
        }
    }


}