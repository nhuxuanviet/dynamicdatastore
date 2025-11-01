package com.company.dynamicds.metapackage.view;

import com.company.dynamicds.metapackage.entity.MetaPackage;
import com.company.dynamicds.metapackage.service.MetaPackageService;
import com.company.dynamicds.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * List view for MetaPackage management
 * Allows activation/deactivation of meta packages
 */
@Route(value = "meta-packages", layout = MainView.class)
@ViewController(id = "dwh_MetaPackage.list")
@ViewDescriptor(path = "meta-package-list-view.xml")
@LookupComponent("metaPackagesDataGrid")
@DialogMode(width = "64em")
public class MetaPackageListView extends StandardListView<MetaPackage> {

    @ViewComponent
    private DataGrid<MetaPackage> metaPackagesDataGrid;

    @Autowired
    private MetaPackageService metaPackageService;

    @Autowired
    private Notifications notifications;

    /**
     * Activate selected MetaPackage
     */
    @Subscribe("metaPackagesDataGrid.activate")
    public void onActivate(ActionPerformedEvent event) {
        MetaPackage selected = metaPackagesDataGrid.getSingleSelectedItem();
        if (selected == null) {
            notifications.create("Please select a MetaPackage").show();
            return;
        }

        try {
            metaPackageService.activate(selected);
            notifications.create("MetaPackage '" + selected.getName() + "' activated successfully")
                    .show();
            getViewData().loadAll();
        } catch (Exception e) {
            notifications.create("Failed to activate: " + e.getMessage())
                    .withType(Notifications.Type.ERROR)
                    .show();
        }
    }

    /**
     * Deactivate selected MetaPackage
     */
    @Subscribe("metaPackagesDataGrid.deactivate")
    public void onDeactivate(ActionPerformedEvent event) {
        MetaPackage selected = metaPackagesDataGrid.getSingleSelectedItem();
        if (selected == null) {
            notifications.create("Please select a MetaPackage").show();
            return;
        }

        try {
            metaPackageService.deactivate(selected);
            notifications.create("MetaPackage '" + selected.getName() + "' deactivated successfully")
                    .show();
            getViewData().loadAll();
        } catch (Exception e) {
            notifications.create("Failed to deactivate: " + e.getMessage())
                    .withType(Notifications.Type.ERROR)
                    .show();
        }
    }

    /**
     * Reload selected MetaPackage
     */
    @Subscribe("metaPackagesDataGrid.reload")
    public void onReload(ActionPerformedEvent event) {
        MetaPackage selected = metaPackagesDataGrid.getSingleSelectedItem();
        if (selected == null) {
            notifications.create("Please select a MetaPackage").show();
            return;
        }

        try {
            metaPackageService.reload(selected);
            notifications.create("MetaPackage '" + selected.getName() + "' reloaded successfully")
                    .show();
            getViewData().loadAll();
        } catch (Exception e) {
            notifications.create("Failed to reload: " + e.getMessage())
                    .withType(Notifications.Type.ERROR)
                    .show();
        }
    }
}
