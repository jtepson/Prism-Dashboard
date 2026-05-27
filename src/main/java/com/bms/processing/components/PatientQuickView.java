package com.bms.processing.components;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.bms.processing.entity.CaseRecordEntity;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.UI;

public class PatientQuickView extends VerticalLayout {

    public PatientQuickView(
        CaseRecordEntity record,
        Runnable onClose,
        Runnable onOpenFullRecord
    ) {

        Button closeButton = new Button("✕", event -> {
            if (onClose != null) {
                onClose.run();
            }
        });

        closeButton.getStyle()
                .set("margin-left", "auto");

        setWidth("350px");
        setPadding(true);
        setSpacing(true);

        H3 title = new H3("Patient Details");

        title.getStyle()
                .set("margin", "0")
                .set("font-size", "1.3rem")
                .set("font-weight", "700");

        HorizontalLayout topBar = new HorizontalLayout(title, closeButton);
        topBar.setWidthFull();
        topBar.setAlignItems(Alignment.CENTER);
        topBar.setJustifyContentMode(JustifyContentMode.BETWEEN);

        String initials =
                (record.getPatientFirstName() != null && !record.getPatientFirstName().isBlank()
                        ? record.getPatientFirstName().substring(0, 1)
                        : "")
                +
                (record.getPatientLastName() != null && !record.getPatientLastName().isBlank()
                        ? record.getPatientLastName().substring(0, 1)
                        : "");

        Avatar avatar = new Avatar(initials);

        avatar.setWidth("72px");
        avatar.setHeight("72px");

        avatar.getStyle()
                .set("background", "#ede9fe")
                .set("color", "#5b21b6")
                .set("font-weight", "700");

        H4 patientName = new H4(
                record.getPatientLastName()
                        + ", "
                        + record.getPatientFirstName()
        );

        patientName.getStyle()
                .set("margin", "0")
                .set("font-size", "1rem")
                .set("font-weight", "700");

        Span patientId = new Span(
                record.getPatientId() == null
                        ? ""
                        : record.getPatientId()
        );

        patientId.getStyle()
                .set("color", "#64748b")
                .set("font-size", "0.9rem");

        VerticalLayout identityText = new VerticalLayout(patientName, patientId);
        identityText.setPadding(false);
        identityText.setSpacing(false);

        HorizontalLayout identitySection =
                new HorizontalLayout(avatar, identityText);

        identitySection.setAlignItems(Alignment.CENTER);
        identitySection.setSpacing(true);

        add(topBar, identitySection);

        add(sectionDivider());

        add(detailRow("Site", record.getSiteName()));
        add(detailRow("Date Scanned", record.getDateScanned() != null ? record.getDateScanned().toString() : ""));
        add(detailRow("Acquired Date", record.getImagesReceivedDate() != null ? record.getImagesReceivedDate().toString() : ""));
        add(detailRow("Date of Birth", record.getDateOfBirth() != null ? record.getDateOfBirth().toString() : ""));
        add(detailRow("Gender", record.getSex()));
        
        Span statusBadge = new Span(
                record.getPatientStatus() != null
                        ? record.getPatientStatus().name().replace("_", " ")
                        : "Unknown"
        );

        //Displaying pt status with a badge instead of text
        statusBadge.getStyle()
                .set("display", "inline-block")
                .set("padding", "0.3rem 0.7rem")
                .set("border-radius", "999px")
                .set("font-size", "0.8rem")
                .set("font-weight", "700")
                .set("background", "#e0f2fe")
                .set("color", "#075985");

        
        add(detailRow("Status", ""));
        add(statusBadge);

        add(sectionDivider());
    
        
        //Notes section for drawer
       H3 notesHeader = new H3("Notes");

        notesHeader.getStyle()
                .set("margin", "0")
                .set("font-size", "1rem")
                .set("font-weight", "700");

        Span notes = new Span(
                record.getNotes() == null || record.getNotes().isBlank()
                        ? "No notes."
                        : record.getNotes()
        );

        notes.getStyle()
                .set("white-space", "pre-wrap")
                .set("color", "#334155")
                .set("font-size", "0.9rem");

        add(notesHeader, notes);

        add(sectionDivider());

        H3 actionsHeader = new H3("Actions");

        actionsHeader.getStyle()
                .set("margin", "0")
                .set("font-size", "1rem")
                .set("font-weight", "700");

        add(actionsHeader);

        Button goToQueue = new Button("Go To Queue");
        goToQueue.addClickListener(event -> {
            if (record.getPatientStatus() == null) {
                return;
            }

            switch (record.getPatientStatus()) {
                case UPCOMING, VERIFYING -> UI.getCurrent().navigate("upcoming");
                case ACQUIRED, PROCESSING -> UI.getCurrent().navigate("processing");
                case PROCESSED, PROCESSED_WITH_ERRORS, PROCESSED_WITH_THIRD_PARTY_ERRORS -> UI.getCurrent().navigate("processed");
                case COMPLETED -> UI.getCurrent().navigate("completed");
                case ERROR -> UI.getCurrent().navigate("errors");
            }
        });

        Button openPatientPage = new Button("Open Full Record");
        openPatientPage.addClickListener(event -> {
            if (onOpenFullRecord != null) {
                onOpenFullRecord.run();
            }
        });
        openPatientPage.getElement().setProperty("title", "Future patient detail page");

        HorizontalLayout actionButtons =
                new HorizontalLayout(goToQueue, openPatientPage);

        actionButtons.setWidthFull();

        add(actionButtons);
    }

    private Div field(String label, String value) {

        Div wrapper = new Div();

        Span title = new Span(label + ": ");
        title.getStyle().set("font-weight", "600");

        Span content = new Span(value == null ? "" : value);

        wrapper.add(title, content);

        return wrapper;
    }

    private Div sectionDivider() {
    Div divider = new Div();
    divider.getStyle()
            .set("border-top", "1px solid #e2e8f0")
            .set("width", "100%")
            .set("margin", "1rem 0");
    return divider;
    }

    private HorizontalLayout detailRow(String label, String value) {
        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("color", "#64748b")
                .set("font-size", "0.85rem")
                .set("font-weight", "600");

        Span valueSpan = new Span(value == null || value.isBlank() ? "-" : value);
        valueSpan.getStyle()
                .set("color", "#0f172a")
                .set("font-size", "0.9rem")
                .set("font-weight", "600");

        HorizontalLayout row = new HorizontalLayout(labelSpan, valueSpan);
        row.setWidthFull();
        row.setJustifyContentMode(JustifyContentMode.BETWEEN);
        row.setAlignItems(Alignment.CENTER);

        return row;
    }
}