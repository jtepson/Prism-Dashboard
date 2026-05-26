package com.bms.processing.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class DashboardWidget extends VerticalLayout {

    public DashboardWidget(String title, Component content) {

        setPadding(true);
        setSpacing(true);
        setWidthFull();

        getStyle()
                .set("background", "#ffffff")
                .set("border", "1px solid #dbe3ee")
                .set("border-radius", "16px")
                .set("box-shadow", "0 4px 12px rgba(15,23,42,0.06)");

        H3 header = new H3(title);

        add(header, content);
    }
}