package com.bms.processing.views.manage;

import com.bms.processing.layouts.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@PageTitle("Manage Patients")
@PermitAll
@Route(value = "manage/patients", layout = MainLayout.class)
public class ManagePatientsView extends VerticalLayout {

    public ManagePatientsView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(
                new H2("Manage Patients"),
                new Span("Patient management table coming soon.")
        );
    }
}