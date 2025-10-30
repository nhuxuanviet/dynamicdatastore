package com.company.dynamicds.apisetting.view.config.fragment;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import io.jmix.flowui.component.codeeditor.CodeEditor;
import io.jmix.flowui.component.listbox.JmixListBox;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@FragmentDescriptor("scripts-fragment.xml")
@RequiredArgsConstructor
@Slf4j
public class ScriptsFragment extends Fragment<HorizontalLayout> {

    @ViewComponent
    private JmixListBox<String> scriptListBox;
    @ViewComponent
    private CodeEditor postResponseEditor;
    @ViewComponent
    private CodeEditor preRequestEditor;


    @Subscribe(target = Target.HOST_CONTROLLER)
    public void onHostInit(final View.InitEvent event) {
        List<String> items = List.of("Pre-request", "Post-response");
        scriptListBox.setItems(items);
        scriptListBox.setValue("Pre-request");
    }

    @Subscribe("scriptListBox")
    public void onScriptListBoxComponentValueChange(final AbstractField.ComponentValueChangeEvent<JmixListBox<?>, ?> event) {
        preRequestEditor.setVisible(event.getValue().equals("Pre-request"));
        postResponseEditor.setVisible(event.getValue().equals("Post-response"));



    }



}
