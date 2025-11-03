package com.company.dynamicds.metapackage.view;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.company.dynamicds.metapackage.datastore.MetaPackageMetaClassFactory;
import com.company.dynamicds.metapackage.entity.MetaPackage;
import com.company.dynamicds.metapackage.service.MetaPackageExecutor;
import com.company.dynamicds.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;

import io.jmix.core.DataManager;
import io.jmix.core.Metadata;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.model.KeyValueCollectionContainer;
import io.jmix.flowui.view.DialogMode;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

@Route(value = "meta-packages-data", layout = MainView.class)
@ViewController(id = "MetaPackage.data")
@ViewDescriptor(path = "meta-package-data-view.xml")
@DialogMode(width = "90em", height = "70em")
public class MetaPackageDataView extends StandardView {

    @ViewComponent
    private EntityComboBox<MetaPackage> metaPackageField;

    @ViewComponent
    private DataGrid<KeyValueEntity> dataGrid;

    @ViewComponent
    private KeyValueCollectionContainer dataDc;

    @ViewComponent
    private CollectionLoader<MetaPackage> activeMetaPackagesDl;

    @Autowired
    private MetaPackageMetaClassFactory metaClassFactory;

    @Autowired
    private Notifications notifications;

    @Autowired
    private DataManager dataManager;

    @Autowired
    private Metadata metadata;

    @Autowired
    private MetaPackageExecutor metaPackageExecutor;

    @Subscribe
    public void onInit(final InitEvent event) {
        activeMetaPackagesDl.load();

        // Thiết lập metaClass và label generator bằng code để chắc chắn hợp lệ
        MetaClass mpClass = metadata.findClass(MetaPackage.class);
        if (mpClass != null) {
            metaPackageField.setMetaClass(mpClass);
        }
        metaPackageField.setItemLabelGenerator(MetaPackage::getName);
    }

    @Subscribe("loadBtn")
    public void onLoadBtnClick(final ClickEvent<JmixButton> event) {
        MetaPackage selected = metaPackageField.getValue();
        if (selected == null) {
            notifications.create("Chọn một MetaPackage đang active").withType(Notifications.Type.WARNING).show();
            return;
        }

        // Tạo MetaClass động từ cấu hình field mappings
        MetaClass metaClass = metaClassFactory.createMetaClass(selected, selected.getStoreName());
        // Cấu hình cột theo các property trong MetaClass
        rebuildGridColumns(metaClass);

        // Tải dữ liệu qua MetaPackageExecutor để tránh NPE keyValueMapper
        List<KeyValueEntity> items = metaPackageExecutor.executeLoad(selected, null, null, null, null);
        dataDc.setItems(items);
    }

    private void rebuildGridColumns(MetaClass metaClass) {
        // Xoá các cột cũ
        List<DataGrid.Column<KeyValueEntity>> existing = new ArrayList<>(dataGrid.getColumns());
        existing.forEach(dataGrid::removeColumn);

        // Thêm cột mới dựa trên các property của MetaClass
        metaClass.getProperties().forEach(p -> {
            String property = p.getName();
            DataGrid.Column<KeyValueEntity> column = dataGrid.addColumn(entity -> entity.getValue(property));
            column.setKey(property);
            column.setHeader(property);
            column.setSortable(true);
            column.setAutoWidth(true);
        });
    }
}


