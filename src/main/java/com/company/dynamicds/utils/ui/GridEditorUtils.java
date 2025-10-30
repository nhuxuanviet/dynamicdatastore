package com.company.dynamicds.utils.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Focusable;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.grid.editor.DataGridEditor;

public final class GridEditorUtils {

    private GridEditorUtils() {
        // prevent instantiation
    }

    public static <T> void setupInlineEditor(DataGrid<T> dataGrid) {
        DataGridEditor<T> editor = dataGrid.getEditor();
        dataGrid.addItemDoubleClickListener(event -> {
            editor.editItem(event.getItem());
            Component editorComponent = event.getColumn().getEditorComponent();
            if (editorComponent instanceof Focusable<?> focusable) {
                focusable.focus();
            }
        });
    }

}
