package com.company.dynamicds.dynamicds.view.metadata;

import com.company.dynamicds.dynamicds.entity.MetadataDefinition;
import com.company.dynamicds.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;


@Route(value = "metadata-definitions", layout = MainView.class)
@ViewController(id = "dwh_MetadataDefinition.list")
@ViewDescriptor(path = "metadata-definition-list-view.xml")
@LookupComponent("metadataDefinitionsDataGrid")
@DialogMode(width = "64em")
public class MetadataDefinitionListView extends StandardListView<MetadataDefinition> {
}