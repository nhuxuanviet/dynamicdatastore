package com.company.dynamicds.utils.renderer;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.company.dynamicds.entity.User;
import com.company.dynamicds.enums.ActionColumnType;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.pagination.SimplePagination;
import io.jmix.flowui.data.grid.ContainerDataGridItems;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

public final class DataGridRenderers {

    private DataGridRenderers() {
        // prevent instantiation
    }

    public static <T> Renderer<T> buildActionsColumn(UiComponents uiComponents, Set<ActionColumnType> actionColumnTypes, BiConsumer<T, ActionColumnType> onAction) {
        return new ComponentRenderer<>(item -> {  // ComponentRenderer<HorizontalLayout, T>
            HorizontalLayout hbox = uiComponents.create(HorizontalLayout.class);
            hbox.addClassNames("row-actions", "my-grid-row-action");
            hbox.setWidth("100%");

            for (ActionColumnType type : actionColumnTypes) {
                Icon icon = uiComponents.create(Icon.class);
                switch (type) {
                    case VIEW -> icon.setIcon(VaadinIcon.EYE);
                    case EDIT -> icon.setIcon(VaadinIcon.EDIT);
                    case SETTING -> icon.setIcon(VaadinIcon.COG);
                    case DELETE -> icon.setIcon(VaadinIcon.TRASH);
                    case RELOAD -> icon.setIcon(VaadinIcon.REFRESH);
                    case ACTIVE ->  icon.setIcon(VaadinIcon.CHECK);
                    case LOCK -> {
                        if (item instanceof User user) {
                            if (Boolean.TRUE.equals(user.getActive())) {
                                icon.setIcon(VaadinIcon.LOCK); // mở khóa
                            } else {
                                icon.setIcon(VaadinIcon.UNLOCK); // khóa
                            }
                        } else {
                            icon.setIcon(VaadinIcon.LOCK); // fallback
                        }
                    }

                }
                icon.addClassName("my-grid-row-button");
                icon.addClickListener(event -> onAction.accept(item, type));
                hbox.add(icon);
            }

            return hbox;
        });
    }

    public static <T> Renderer<T> buildIndexColumn(DataGrid<T> dataGrid, SimplePagination pagination) {
        return new TextRenderer<>(item -> {
            if (dataGrid.getItems() instanceof ContainerDataGridItems<T> containerItems) {
                List<T> list = containerItems.getContainer().getItems();

                int firstResult = pagination.getPaginationLoader().getFirstResult(); // offset hiện tại
                int rowIndex = list.indexOf(item); // index trong trang

                return String.valueOf(firstResult + rowIndex + 1);
            }
            return "";
        });
    }


}