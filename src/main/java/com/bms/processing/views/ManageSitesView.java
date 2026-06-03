package com.bms.processing.views.manage;

import com.bms.processing.layouts.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@PageTitle("Manage Sites")
@PermitAll
@Route(value = "manage/sites", layout = MainLayout.class)
public class ManageSitesView extends VerticalLayout {

    public ManageSitesView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(
                new H2("Manage Sites"),
                new Span("Site management table coming soon.")
        );
    }
}