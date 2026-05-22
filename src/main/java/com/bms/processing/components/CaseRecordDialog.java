package com.bms.processing.components;

import com.bms.processing.entity.CaseRecordEntity;
import com.bms.processing.model.ThirdPartyStatus;
import com.bms.processing.service.CaseRecordService;
import com.bms.processing.service.InvalidWorkflowTransitionException;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextArea;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CaseRecordDialog extends Dialog {

    public enum Mode {
        SUMMARY,
        UPCOMING,
        PROCESSING,
        PROCESSED,
        COMPLETED,
        ERRORS
    }
    
    private final CaseRecordService caseRecordService;
    private final CaseRecordEntity record;
    private final Mode mode;
    private final Runnable afterSave;

    public CaseRecordDialog(
            CaseRecordEntity record,
            CaseRecordService caseRecordService,
            Mode mode,
            Runnable afterSave
    ) {
        this.record = record;
        this.caseRecordService = caseRecordService;
        this.mode = mode;
        this.afterSave = afterSave;

        setHeaderTitle("Patient Summary");
        setWidth("900px");

        buildDialog();
    }

    private void buildDialog() {
        TextField lastName = new TextField("Last Name");
        lastName.setValue(nullSafe(record.getPatientLastName()));

        TextField firstName = new TextField("First Name");
        firstName.setValue(nullSafe(record.getPatientFirstName()));

        TextField patientId = new TextField("Patient ID");
        patientId.setValue(nullSafe(record.getPatientId()));

        TextField siteName = new TextField("Site");
        siteName.setValue(nullSafe(record.getSiteName()));

        DatePicker dateOfBirth = new DatePicker("Date of Birth");
        dateOfBirth.setValue(record.getDateOfBirth());

        TextField sex = new TextField("Sex");
        sex.setValue(nullSafe(record.getSex()));

        DatePicker dateScanned = new DatePicker("Date Scanned");
        dateScanned.setValue(record.getDateScanned());

        TextField funder = new TextField("Funder");
        funder.setValue(nullSafe(record.getFunder()));

        Checkbox intakeSheetDone = new Checkbox("Intake Sheet Done");
        intakeSheetDone.setValue(Boolean.TRUE.equals(record.getIntakeSheetDone()));

        Checkbox intakeSheetSent = new Checkbox("Intake Sheet Sent");
        intakeSheetSent.setValue(Boolean.TRUE.equals(record.getIntakeSheetSent()));

        Checkbox invoiceSent = new Checkbox("Invoice Sent");
        invoiceSent.setValue(Boolean.TRUE.equals(record.getInvoiceSent()));

        FormLayout patientForm = new FormLayout();
        patientForm.setWidthFull();
        patientForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );
        patientForm.add(lastName, firstName, patientId, siteName, dateOfBirth, dateScanned, sex);

        Details patientDetails = new Details("Patient Information", patientForm);
        patientDetails.setOpened(true);

        FormLayout workflowForm = new FormLayout();
        workflowForm.setWidthFull();
        workflowForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );
        workflowForm.add(
                buildDisplayField(
                        "Date Scanned",
                        formatDate(record.getDateScanned())
                ),
                buildDisplayField(
                        "Acquired Date",
                        formatDate(record.getImagesReceivedDate())
                ),
                buildDisplayField(
                        "Workflow Status",
                        formatEnum(record.getPatientStatus())
                ),
                buildDisplayField(
                        "Invoice Sent",
                        Boolean.TRUE.equals(record.getInvoiceSent())
                                ? "Yes"
                                : "No"
                )
        );

        Details workflowDetails = new Details("Workflow Details", workflowForm);
        workflowDetails.setOpened(true);

        FormLayout thirdPartyForm = new FormLayout();
        thirdPartyForm.setWidthFull();
        thirdPartyForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );
        
        ComboBox<String> imekaStatus = new ComboBox<>("IMEKA Status");
        imekaStatus.setItems("NOT_SENT", "SENT", "UPLOADED", "ERROR");
        imekaStatus.setValue(
                record.getImekaStatus() != null
                        ? record.getImekaStatus().name()
                        : null
        );

        DatePicker imekaSentDate = new DatePicker("IMEKA Sent Date");
        imekaSentDate.setValue(record.getImekaSentDate());
        imekaSentDate.setReadOnly(record.getImekaStatus() == ThirdPartyStatus.UPLOADED);

        TextField imekaErrorNote = new TextField("IMEKA Error Note");
        imekaErrorNote.setValue(nullSafe(record.getImekaErrorNote()));
        imekaErrorNote.setVisible("ERROR".equals(imekaStatus.getValue()));

        imekaStatus.addValueChangeListener(event ->
                imekaErrorNote.setVisible("ERROR".equals(event.getValue()))
        );

        ComboBox<String> duramapStatus = new ComboBox<>("DuraMap Status");
        duramapStatus.setItems("NOT_SENT", "SENT", "ERROR");
        duramapStatus.setValue(
                record.getDuramapStatus() != null
                        ? record.getDuramapStatus().name()
                        : null
        );

        DatePicker duramapSentDate = new DatePicker("DuraMap Sent Date");
        duramapSentDate.setValue(record.getDuramapSentDate());

        TextField duramapErrorNote = new TextField("DuraMap Error Note");
        duramapErrorNote.setValue(nullSafe(record.getDuramapErrorNote()));
        duramapErrorNote.setVisible("ERROR".equals(duramapStatus.getValue()));

        duramapStatus.addValueChangeListener(event ->
                duramapErrorNote.setVisible("ERROR".equals(event.getValue()))
        );

        ComboBox<String> neuroreaderStatus = new ComboBox<>("Neuroreader Status");
        neuroreaderStatus.setItems("NOT_SENT", "SENT", "ERROR");
        neuroreaderStatus.setValue(
                record.getNeuroreaderStatus() != null
                        ? record.getNeuroreaderStatus().name()
                        : null
        );

        DatePicker neuroreaderSentDate = new DatePicker("Neuroreader Sent Date");
        neuroreaderSentDate.setValue(record.getNeuroreaderSentDate());

        TextField neuroreaderErrorNote = new TextField("Neuroreader Error Note");
        neuroreaderErrorNote.setValue(nullSafe(record.getNeuroreaderErrorNote()));
        neuroreaderErrorNote.setVisible("ERROR".equals(neuroreaderStatus.getValue()));

        neuroreaderStatus.addValueChangeListener(event ->
                neuroreaderErrorNote.setVisible("ERROR".equals(event.getValue()))
        );

        if (mode == Mode.PROCESSING) {
            if (!record.isMinorAtScan()) {
                thirdPartyForm.add(
                        imekaStatus,
                        imekaSentDate
                );

                if ("ERROR".equals(imekaStatus.getValue())) {
                    thirdPartyForm.add(
                            duramapStatus,
                            duramapSentDate
                    );
                }
            } else {
                thirdPartyForm.add(
                        duramapStatus,
                        duramapSentDate
                );
            }

            thirdPartyForm.add(
                    neuroreaderStatus,
                    neuroreaderSentDate
            );
        }
        
        Details thirdPartyDetails = new Details("Third Party Details", thirdPartyForm);
        thirdPartyDetails.setOpened(true);

        TextArea notes = new TextArea("General Notes");
        notes.setWidthFull();
        notes.setValue(nullSafe(record.getNotes()));

        TextArea imekaError = new TextArea("IMEKA Error Note");
        imekaError.setWidthFull();
        imekaError.setValue(nullSafe(record.getImekaErrorNote()));

        TextArea duramapError = new TextArea("DuraMap Error Note");
        duramapError.setWidthFull();
        duramapError.setValue(nullSafe(record.getDuramapErrorNote()));

        TextArea neuroreaderError = new TextArea("Neuroreader Error Note");
        neuroreaderError.setWidthFull();
        neuroreaderError.setValue(nullSafe(record.getNeuroreaderErrorNote()));
        
        VerticalLayout notesLayout = new VerticalLayout();
        notesLayout.setPadding(false);
        notesLayout.setSpacing(true);
        notesLayout.setWidthFull();

        if (mode == Mode.PROCESSING) {

            notesLayout.add(
                    notes,
                    imekaError,
                    duramapError,
                    neuroreaderError
            );

        } else {

            addReadOnlyNoteSection(notesLayout, "Notes", record.getNotes());
            addReadOnlyNoteSection(notesLayout, "IMEKA Error Note", record.getImekaErrorNote());
            addReadOnlyNoteSection(notesLayout, "DuraMap Error Note", record.getDuramapErrorNote());
            addReadOnlyNoteSection(notesLayout, "Neuroreader Error Note", record.getNeuroreaderErrorNote());

            if (notesLayout.getComponentCount() == 0) {
                notesLayout.add(new Span("No notes."));
            }
        }
        Details notesDetails = new Details("Notes", notesLayout);
        notesDetails.setOpened(false);

        VerticalLayout content = new VerticalLayout();
        content.add(patientDetails);
        switch (mode) {
            case SUMMARY -> {
                content.add(
                        workflowDetails,
                        thirdPartyDetails,
                        notesDetails
                );
            }

            case UPCOMING -> {

                FormLayout upcomingForm = new FormLayout();
                upcomingForm.setWidthFull();
                upcomingForm.setResponsiveSteps(
                        new FormLayout.ResponsiveStep("0", 1),
                        new FormLayout.ResponsiveStep("700px", 2)
                );

                upcomingForm.add(
                        dateOfBirth,
                        dateScanned,
                        funder,
                        intakeSheetDone,
                        intakeSheetSent,
                        invoiceSent
                );

                Details upcomingDetails = new Details(
                        "Upcoming Information",
                        upcomingForm
                );

                upcomingDetails.setOpened(true);

                content.add(
                        upcomingDetails,
                        notesDetails
                );
            }

            case PROCESSING -> {

                content.add(
                        workflowDetails,
                        thirdPartyDetails,
                        notesDetails
                );
            }
            case PROCESSED -> {
                content.add(
                        workflowDetails,
                        thirdPartyDetails,
                        notesDetails
                );
            }
            case COMPLETED -> {
                content.add(
                        workflowDetails,
                        thirdPartyDetails,
                        notesDetails
                );
            }
            case ERRORS -> {
                content.add(
                        workflowDetails,
                        thirdPartyDetails,
                        notesDetails
                );
            }
        }
        content.setPadding(false);
        content.setSpacing(true);
        content.setWidthFull();

        Button cancelButton = new Button("Cancel", e -> close());

        Button saveButton = new Button("Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(event -> {
            if (lastName.getValue().trim().isEmpty()) {
                showError("Last name is required.");
                return;
            }

            try {
                record.setDateOfBirth(dateOfBirth.getValue());
                record.setSex(sex.getValue());
                record.setDateScanned(dateScanned.getValue());
                record.setFunder(funder.getValue());
                record.setIntakeSheetDone(intakeSheetDone.getValue());
                record.setIntakeSheetSent(intakeSheetSent.getValue());
                record.setInvoiceSent(invoiceSent.getValue());
                caseRecordService.updateSummaryIdentityFields(
                        record,
                        lastName.getValue(),
                        firstName.getValue(),
                        patientId.getValue(),
                        siteName.getValue()
                );

                if (mode == Mode.PROCESSING) {
                    if (!record.isMinorAtScan()) {
                        caseRecordService.updateImekaStatus(
                                record,
                                ThirdPartyStatus.valueOf(imekaStatus.getValue()),
                                imekaError.getValue(),
                                imekaSentDate.getValue()
                        );
                    }
                    if (record.isMinorAtScan() || "ERROR".equals(imekaStatus.getValue())) {
                        caseRecordService.updateDuramapStatus(
                                record,
                                ThirdPartyStatus.valueOf(duramapStatus.getValue()),
                                duramapError.getValue(),
                                duramapSentDate.getValue()
                        );
                    }
                    caseRecordService.updateNeuroreaderStatus(
                            record,
                            ThirdPartyStatus.valueOf(neuroreaderStatus.getValue()),
                            neuroreaderError.getValue(),
                            neuroreaderSentDate.getValue()
                    );
                }

                if (afterSave != null) {
                    if (mode == Mode.PROCESSING) {

                        record.setNotes(notes.getValue());
                        record.setImekaErrorNote(imekaError.getValue());
                        record.setDuramapErrorNote(duramapError.getValue());
                        record.setNeuroreaderErrorNote(neuroreaderError.getValue());

                        caseRecordService.saveEditedCase(record);
                    }
                    afterSave.run();
                }

                close();
            } catch (InvalidWorkflowTransitionException ex) {
                showError(ex.getMessage());
            }
        });

        add(content);
        getFooter().add(cancelButton, saveButton);
    }

    private Component buildDisplayField(String label, String value) {
        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setPadding(false);
        wrapper.setSpacing(false);
        wrapper.getStyle().set("gap", "0.25rem");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "0.82rem")
                .set("font-weight", "600")
                .set("color", "#64748b");

        Div valueBox = new Div();
        valueBox.setText(value == null || value.isBlank() ? "-" : value);
        valueBox.getStyle()
                .set("padding", "0.65rem 0.75rem")
                .set("border", "1px solid #dbe3ee")
                .set("border-radius", "10px")
                .set("background", "#f8fafc")
                .set("color", "#1e293b")
                .set("min-height", "42px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("box-sizing", "border-box");

        wrapper.add(labelSpan, valueBox);
        return wrapper;
    }

    private void addReadOnlyNoteSection(VerticalLayout parent, String label, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "0.82rem")
                .set("font-weight", "600")
                .set("color", "#64748b");

        Div noteBox = new Div();
        noteBox.setText(value);
        noteBox.getStyle()
                .set("white-space", "pre-wrap")
                .set("padding", "0.75rem")
                .set("border", "1px solid #dbe3ee")
                .set("border-radius", "10px")
                .set("background", "#f8fafc")
                .set("color", "#1e293b")
                .set("width", "100%")
                .set("box-sizing", "border-box");

        parent.add(labelSpan, noteBox);
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 3500, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String formatEnum(Enum<?> value) {
        return value == null ? "" : value.name().replace("_", " ");
    }

    private String formatDate(LocalDate value) {
        return value == null ? "" : value.toString();
    }

    private String formatDateTimeCompact(LocalDateTime value) {
        if (value == null) {
            return "";
        }

        return String.format(
                "%02d-%02d-%02d %02d:%02d",
                value.getMonthValue(),
                value.getDayOfMonth(),
                value.getYear() % 100,
                value.getHour(),
                value.getMinute()
        );
    }


}