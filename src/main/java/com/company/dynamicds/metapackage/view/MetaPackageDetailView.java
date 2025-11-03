package com.company.dynamicds.metapackage.view;

import com.company.dynamicds.dynamicds.entity.DynamicDataStoreConfig;
import com.company.dynamicds.metapackage.entity.MetaPackage;
import com.company.dynamicds.metapackage.entity.MetaPackageFieldMapping;
import com.company.dynamicds.metapackage.entity.MetaPackageRelationship;
import com.company.dynamicds.metapackage.entity.MetaPackageSource;
import com.company.dynamicds.utils.ui.GridEditorUtils;
import com.company.dynamicds.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.kit.component.button.JmixButton;
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
    private DataGrid<MetaPackageRelationship> relationshipsDataGrid;

    @ViewComponent
    private CollectionContainer<DynamicDataStoreConfig> dynamicDataStoreConfigsDc;
    @ViewComponent
    private CollectionContainer<MetaPackageFieldMapping> fieldMappingsDc;
    @ViewComponent
    private CollectionPropertyContainer<MetaPackageRelationship> relationshipsDc;
    @ViewComponent
    private DataContext dataContext;
    @Autowired
    private DataManager dataManager;

    @Subscribe
    public void onInit(final InitEvent event) {
        dynamicDataStoreConfigsDl.load();
        // Setup inline editors for data grids
        GridEditorUtils.setupInlineEditor(sourcesDataGrid);
        GridEditorUtils.setupInlineEditor(fieldMappingsDataGrid);
        GridEditorUtils.setupInlineEditor(relationshipsDataGrid);
    }

    @Install(to = "relationshipsDataGrid.create", subject = "initializer")
    private void relationshipsCreateInitializer(MetaPackageRelationship e) {
        e.setMetaPackage(getEditedEntity());
        e.setIsActive(true); // default to active
    }

    @Autowired
    private Notifications notifications;

    @Subscribe("createInlineBtn")
    public void onCreateInlineBtnClick(final ClickEvent<JmixButton> event) {
        MetaPackageSource selectedSource = sourcesDataGrid.getSingleSelectedItem();
        if (selectedSource == null) {
            notifications.create("Hãy chọn một Source trước khi thêm mapping.")
                    .withType(Notifications.Type.WARNING)
                    .show();
            return;
        }

        MetaPackageFieldMapping e = dataManager.create(MetaPackageFieldMapping.class);
        e.setMetaPackageSource(selectedSource);

        e = dataContext.merge(e);
        fieldMappingsDc.getMutableItems().add(e);

        fieldMappingsDataGrid.getEditor().editItem(e);
    }

    @Subscribe("editInlineBtn")
    public void onEditInlineBtnClick(final ClickEvent<JmixButton> event) {
        MetaPackageFieldMapping sel = fieldMappingsDataGrid.getSingleSelectedItem();
        if (sel != null) {
            fieldMappingsDataGrid.getEditor().editItem(sel);
        } else {
            notifications.create("Hãy chọn một Field Mapping để sửa.")
                    .withType(Notifications.Type.WARNING)
                    .show();
        }
    }
}