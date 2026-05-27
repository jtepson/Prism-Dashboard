package com.bms.processing.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.bms.processing.entity.CaseRecordEntity;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
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

        HorizontalLayout header = new HorizontalLayout(
                new H3(
                        record.getPatientLastName()
                                + ", "
                                + record.getPatientFirstName()
                ),
                closeButton
        );

        header.setWidthFull();

        add(header);

        add(field("Patient ID", record.getPatientId()));
        add(field("Site", record.getSiteName()));
        add(field("DOB", String.valueOf(record.getDateOfBirth())));
        add(field("Sex", record.getSex()));
        
        
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

        add(field("Status", ""));
        add(statusBadge);

        add(field(
        "Date Scanned",
        record.getDateScanned() != null
                ? record.getDateScanned().toString()
                : ""
        ));
        add(field(
                "Acquired Date",
                record.getImagesReceivedDate() != null
                        ? record.getImagesReceivedDate().toString()
                        : ""
        ));
        add(field(
                "Funder",
                record.getFunder()
        ));
        add(field(
                "Invoice Sent",
                Boolean.TRUE.equals(record.getInvoiceSent())
                        ? "Yes"
                        : "No"
        ));
        //Notes section for drawer
        H3 notesHeader = new H3("Notes");
        Span notes = new Span(
                record.getNotes() == null
                        ? ""
                        : record.getNotes()
        );
        notes.getStyle()
                .set("white-space", "pre-wrap");

        add(notesHeader, notes);

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

        add(new H3("Actions"), goToQueue, openPatientPage);
    }

    private Div field(String label, String value) {

        Div wrapper = new Div();

        Span title = new Span(label + ": ");
        title.getStyle().set("font-weight", "600");

        Span content = new Span(value == null ? "" : value);

        wrapper.add(title, content);

        return wrapper;
    }
}