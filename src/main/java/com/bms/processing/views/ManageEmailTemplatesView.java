package com.bms.processing.views.manage;

import com.bms.processing.layouts.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@PageTitle("Manage Email Templates")
@PermitAll
@Route(value = "manage/email-templates", layout = MainLayout.class)
public class ManageEmailTemplatesView extends VerticalLayout {

    public ManageEmailTemplatesView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(
                new H2("Manage Email Templates"),
                new Span("Email Template management table coming soon.")
        );
    }
}