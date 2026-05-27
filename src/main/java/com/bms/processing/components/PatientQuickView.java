package com.bms.processing.components;

import com.bms.processing.entity.CaseRecordEntity;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class PatientQuickView extends VerticalLayout {

    public PatientQuickView(CaseRecordEntity record) {

        setWidth("350px");
        setPadding(true);
        setSpacing(true);

        add(
                new H3(
                        record.getPatientLastName()
                                + ", "
                                + record.getPatientFirstName()
                )
        );

        add(field("Patient ID", record.getPatientId()));
        add(field("Site", record.getSiteName()));
        add(field("DOB", String.valueOf(record.getDateOfBirth())));
        add(field("Sex", record.getSex()));
        add(field("Status",
                record.getPatientStatus() != null
                        ? record.getPatientStatus().name()
                        : ""));
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
        H3 notesHeader = new H3("Notes");

        Span notes = new Span(
                record.getNotes() == null
                        ? ""
                        : record.getNotes()
        );

        notes.getStyle()
                .set("white-space", "pre-wrap");

        add(notesHeader, notes);
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