package com.bms.processing.views.manage;

import com.bms.processing.layouts.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@PageTitle("Manage Notifications")
@PermitAll
@Route(value = "manage/notifications", layout = MainLayout.class)
public class ManageNotificationsView extends VerticalLayout {

    public ManageNotificationsView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(
                new H2("Manage Notifications"),
                new Span("Notification management table coming soon.")
        );
    }
}