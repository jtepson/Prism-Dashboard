package com.bms.processing.components;

import com.bms.processing.entity.CaseRecordEntity;
import com.bms.processing.model.ThirdPartyStatus;
import com.bms.processing.service.CaseRecordService;
import com.bms.processing.service.InvalidWorkflowTransitionException;
import com.bms.processing.service.SiteService;
import com.bms.processing.entity.SiteEntity;
import com.bms.processing.entity.AuditEventEntity;
import com.bms.processing.service.AuditEventService;
import com.bms.processing.service.PatientFileService;
import com.bms.processing.service.DicomConfigService;
import com.bms.processing.service.DicomService;
import com.bms.processing.model.DicomStudyResult;
import com.vaadin.flow.component.grid.Grid;
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
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    private final AuditEventService auditEventService;
    private final SiteService siteService;
    private final CaseRecordEntity record;
    private final Mode mode;
    private final Runnable afterSave;

    //updated 6122026 to include patient file upload constructors and parameters
    private final PatientFileService patientFileService;
    private final String baseStoragePath;

    //updated 6122026 to also include DICCM fields
    private final DicomConfigService dicomConfigService;
    private final DicomService dicomService;

    public CaseRecordDialog(
        CaseRecordEntity record,
        CaseRecordService caseRecordService,
        Mode mode,
        Runnable afterSave,
        SiteService siteService,
        AuditEventService auditEventService,
        PatientFileService patientFileService,
        String baseStoragePath,
        DicomConfigService dicomConfigService,
        DicomService dicomService
    ) {
        this.record = record;
        this.caseRecordService = caseRecordService;
        this.siteService = siteService;
        this.auditEventService = auditEventService;
        this.mode = mode;
        this.afterSave = afterSave;
        this.patientFileService = patientFileService;
        this.baseStoragePath = baseStoragePath;
        this.dicomConfigService = dicomConfigService;
        this.dicomService = dicomService;

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

        Component siteField;
        final ComboBox<SiteEntity>[] editableSiteName = new ComboBox[1];

        if (mode == Mode.UPCOMING || mode == Mode.PROCESSING) {
        ComboBox<SiteEntity> siteName = new ComboBox<>("Site");
        siteName.setItems(siteService.getAllSites());
        siteName.setItemLabelGenerator(SiteEntity::getFacilityName);
        siteName.setWidthFull();
        siteService.getAllSites().stream()
                .filter(site -> site.getFacilityName().equals(record.getSiteName()))
                .findFirst()
                .ifPresent(siteName::setValue);
        siteField = siteName;
        editableSiteName[0] = siteName;
        } else {
        TextField siteName = new TextField("Site");
        siteName.setValue(nullSafe(record.getSiteName()));
        siteName.setReadOnly(true);
        siteField = siteName;
        }

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

        if (mode == Mode.PROCESSED || mode == Mode.COMPLETED || mode == Mode.ERRORS) {
                lastName.setReadOnly(true);
                firstName.setReadOnly(true);
                patientId.setReadOnly(true);
                dateOfBirth.setReadOnly(true);
                sex.setReadOnly(true);
                dateScanned.setReadOnly(true);
        }

        FormLayout patientForm = new FormLayout();
        patientForm.setWidthFull();
        patientForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );
        patientForm.add(lastName, firstName, patientId, siteField, dateOfBirth, dateScanned, sex);

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
        workflowDetails.setOpened(false);

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

        ComboBox<String> duramapStatus = new ComboBox<>("DuraMap Status");
        duramapStatus.setItems("NOT_SENT", "SENT", "ERROR");
        duramapStatus.setValue(
                record.getDuramapStatus() != null
                        ? record.getDuramapStatus().name()
                        : null
        );

        DatePicker duramapSentDate = new DatePicker("DuraMap Sent Date");
        duramapSentDate.setValue(record.getDuramapSentDate());

        ComboBox<String> neuroreaderStatus = new ComboBox<>("Neuroreader Status");
        neuroreaderStatus.setItems("NOT_SENT", "SENT", "ERROR");
        neuroreaderStatus.setValue(
                record.getNeuroreaderStatus() != null
                        ? record.getNeuroreaderStatus().name()
                        : null
        );

        DatePicker neuroreaderSentDate = new DatePicker("Neuroreader Sent Date");
        neuroreaderSentDate.setValue(record.getNeuroreaderSentDate());

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
        thirdPartyDetails.setOpened(false);

        TextField studyInstanceUid = new TextField("Study Instance UID");
        studyInstanceUid.setWidthFull();
        studyInstanceUid.setValue(nullSafe(record.getStudyInstanceUid()));

        TextField accessionNumber = new TextField("Accession Number");
        accessionNumber.setWidthFull();
        accessionNumber.setValue(nullSafe(record.getAccessionNumber()));

        FormLayout dicomForm = new FormLayout();
        dicomForm.setWidthFull();
        dicomForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );

        Button queryArchiveButton = new Button("Query Archive", new Icon(VaadinIcon.SEARCH));

        queryArchiveButton.addClickListener(event -> {

                try {

                        var config = dicomConfigService.getActiveConfiguration();

                        if (config == null) {
                                Notification.show(
                                        "No DICOM configuration found.",
                                        3000,
                                        Notification.Position.MIDDLE
                                );
                        return;
                        }

                        String queryPatientId = record.getPatientId();
                        String queryLastName = record.getPatientLastName();

                        var studies = dicomService.queryStudies(
                                config,
                                queryLastName,
                                queryPatientId
                        );

                        if (studies.isEmpty()) {
                                Notification.show(
                                        "No studies found.",
                                        3000,
                                        Notification.Position.MIDDLE
                                );
                                return;
                        }

                        openDicomStudyResultsDialog(
                                studies,
                                studyInstanceUid,
                                accessionNumber
                        );

                } catch (Exception ex) {

                        ex.printStackTrace();

                        Notification.show(
                                "Query failed: " + ex.getMessage(),
                                5000,
                                Notification.Position.MIDDLE
                        );
                }
        });

        Button findReportsButton = new Button(
                "Find Reports",
                new Icon(VaadinIcon.FILE_TEXT)
        );

        findReportsButton.addClickListener(event -> {

                if (studyInstanceUid.getValue().isBlank()) {

                                Notification.show(
                                        "Link a study first.",
                                        3000,
                                        Notification.Position.MIDDLE
                                );

                                return;
                }

                try {
                        var config = dicomConfigService.getActiveConfiguration();

                        if (config == null) {
                                Notification.show(
                                        "No DICOM configuration found.",
                                        3000,
                                        Notification.Position.MIDDLE
                                );
                                return;
                        }

                        var reports = dicomService.findReports(
                                config,
                                studyInstanceUid.getValue()
                        );

                        Notification.show(
                                "Found " + reports.size() + " report(s).",
                                3000,
                                Notification.Position.MIDDLE
                        );

                        } catch (Exception ex) {
                        ex.printStackTrace();

                        Notification.show(
                                "Report query failed: " + ex.getMessage(),
                                5000,
                                Notification.Position.MIDDLE
                        );
                }
        });

        Button clearDicomLinkButton = new Button("Clear Link", new Icon(VaadinIcon.CLOSE));
        clearDicomLinkButton.addClickListener(event -> {
                studyInstanceUid.clear();
                accessionNumber.clear();
        });

        VerticalLayout dicomActions = new VerticalLayout(
                queryArchiveButton,
                findReportsButton,
                clearDicomLinkButton
        );
        dicomActions.setPadding(false);
        dicomActions.setSpacing(true);

        dicomForm.add(
                buildDisplayField(
                        "Study Linked",
                        Boolean.TRUE.equals(record.getDicomLinked()) ? "Yes" : "No"
                ),
                studyInstanceUid,
                accessionNumber,
                dicomActions
        );

        Details dicomDetails = new Details("DICOM", dicomForm);
        dicomDetails.setOpened(false);

        TextArea notes = new TextArea("General Notes");
        notes.setWidthFull();
        notes.setValue(nullSafe(record.getNotes()));

        TextArea imekaError = new TextArea("IMEKA Error Note");
        imekaError.setWidthFull();
        imekaError.setValue(nullSafe(record.getImekaErrorNote()));

        TextArea finalWorkflowNotes = new TextArea("Final Workflow Notes");
        finalWorkflowNotes.setWidthFull();
        finalWorkflowNotes.setValue("");

        TextArea duramapError = new TextArea("DuraMap Error Note");
        duramapError.setWidthFull();
        duramapError.setValue(nullSafe(record.getDuramapErrorNote()));

        TextArea neuroreaderError = new TextArea("Neuroreader Error Note");
        neuroreaderError.setWidthFull();
        neuroreaderError.setValue(nullSafe(record.getNeuroreaderErrorNote()));

        imekaError.setVisible("ERROR".equals(imekaStatus.getValue()));

        duramapError.setVisible(
                "ERROR".equals(duramapStatus.getValue())
        );

        neuroreaderError.setVisible(
                "ERROR".equals(neuroreaderStatus.getValue())
        );

        imekaStatus.addValueChangeListener(event ->
                imekaError.setVisible(
                        "ERROR".equals(event.getValue())
                )
        );

        duramapStatus.addValueChangeListener(event ->
                duramapError.setVisible(
                        "ERROR".equals(event.getValue())
                )
        );

        neuroreaderStatus.addValueChangeListener(event ->
                neuroreaderError.setVisible(
                        "ERROR".equals(event.getValue())
                )
        );
        
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

        //activity section for pt history
        VerticalLayout historyLayout = new VerticalLayout();
        historyLayout.setPadding(false);
        historyLayout.setSpacing(false);
        historyLayout.setWidthFull();

        var auditEvents = auditEventService.getCaseEvents(record.getId());

        if (auditEvents.isEmpty()) {

        historyLayout.add(
                new Span("No activity recorded.")
        );

        } else {

        auditEvents.forEach(event -> {

                Span eventText = new Span(
                        formatActivityTime(event.getCreatedAt())
                                + "  "
                                + event.getMessage()
                );

                eventText.getStyle()
                        .set("padding", "0.35rem 0")
                        .set("display", "block");

                historyLayout.add(eventText);
        });
        }

        PatientFilesSection patientFilesSection =
                new PatientFilesSection(record, patientFileService, baseStoragePath);

        Details patientFilesDetails =
                new Details("Patient Files", patientFilesSection);

        patientFilesDetails.setOpened(false);

        Details activityHistoryDetails =
                new Details("Activity History", historyLayout);

        activityHistoryDetails.setOpened(false);

        VerticalLayout content = new VerticalLayout();
        content.add(patientDetails);
        switch (mode) {
            case SUMMARY -> {
                content.add(
                        workflowDetails,
                        thirdPartyDetails,
                        notesDetails,
                        dicomDetails,
                        patientFilesDetails,
                        activityHistoryDetails
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
                        notesDetails,
                        dicomDetails,
                        patientFilesDetails,
                        activityHistoryDetails
                );
            }
            case PROCESSED -> {

                FormLayout bmsReviewForm = new FormLayout();
                bmsReviewForm.setWidthFull();
                bmsReviewForm.setResponsiveSteps(
                        new FormLayout.ResponsiveStep("0", 1),
                        new FormLayout.ResponsiveStep("700px", 2)
                );

                bmsReviewForm.add(
                        invoiceSent,
                        buildDisplayField("Processed Date", formatDateTimeCompact(record.getProcessedDate())),
                        buildDisplayField("Completed Date", formatDateTimeCompact(record.getCompletedDate()))
                );

                bmsReviewForm.add(finalWorkflowNotes);

                Details bmsReviewDetails = new Details("BMS Review", bmsReviewForm);
                bmsReviewDetails.setOpened(true);

                content.add(
                        workflowDetails,
                        thirdPartyDetails,
                        bmsReviewDetails,
                        notesDetails,
                        dicomDetails,
                        patientFilesDetails,
                        activityHistoryDetails
                );
                }
            case COMPLETED -> {

                FormLayout archiveForm = new FormLayout();
                archiveForm.setWidthFull();
                archiveForm.setResponsiveSteps(
                        new FormLayout.ResponsiveStep("0", 1),
                        new FormLayout.ResponsiveStep("700px", 2)
                );

                archiveForm.add(
                        buildDisplayField("Processed Date", formatDateTimeCompact(record.getProcessedDate())),
                        buildDisplayField("Completed Date", formatDateTimeCompact(record.getCompletedDate())),
                        buildDisplayField("Invoice Sent", Boolean.TRUE.equals(record.getInvoiceSent()) ? "Yes" : "No"),
                        buildDisplayField("Final Workflow Notes", nullSafe(record.getFinalWorkflowNotes()))
                );

                Details archiveDetails = new Details("Archived Record", archiveForm);
                archiveDetails.setOpened(true);

                content.add(
                        workflowDetails,
                        thirdPartyDetails,
                        archiveDetails,
                        notesDetails,
                        dicomDetails,
                        patientFilesDetails,
                        activityHistoryDetails
                );
            }
            case ERRORS -> {

                FormLayout errorReviewForm = new FormLayout();
                errorReviewForm.setWidthFull();
                errorReviewForm.setResponsiveSteps(
                        new FormLayout.ResponsiveStep("0", 1),
                        new FormLayout.ResponsiveStep("700px", 2)
                );

                errorReviewForm.add(
                        buildDisplayField("Workflow Status", formatEnum(record.getPatientStatus())),
                        buildDisplayField("General Notes", nullSafe(record.getNotes())),
                        buildDisplayField("IMEKA Error Note", nullSafe(record.getImekaErrorNote())),
                        buildDisplayField("DuraMap Error Note", nullSafe(record.getDuramapErrorNote())),
                        buildDisplayField("Neuroreader Error Note", nullSafe(record.getNeuroreaderErrorNote()))
                );

                TextArea resolutionNotes = new TextArea("Resolution Notes");
                resolutionNotes.setWidthFull();
                resolutionNotes.setPlaceholder("Add resolution notes here later...");

                errorReviewForm.add(resolutionNotes);

                Details errorReviewDetails = new Details("Error Review", errorReviewForm);
                errorReviewDetails.setOpened(true);

                content.add(
                        workflowDetails,
                        thirdPartyDetails,
                        errorReviewDetails,
                        notesDetails,
                        dicomDetails,
                        patientFilesDetails,
                        activityHistoryDetails
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
                String selectedSiteName = record.getSiteName();
                if (editableSiteName[0] != null && editableSiteName[0].getValue() != null) {
                        selectedSiteName = editableSiteName[0].getValue().getFacilityName();
                }
                caseRecordService.updateSummaryIdentityFields(
                        record,
                        lastName.getValue(),
                        firstName.getValue(),
                        patientId.getValue(),
                        selectedSiteName
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

                caseRecordService.updateDicomLink(
                        record,
                        studyInstanceUid.getValue(),
                        accessionNumber.getValue()
                );

                if (afterSave != null) {
                    if (mode == Mode.PROCESSING) {

                        record.setNotes(notes.getValue());
                        record.setImekaErrorNote(imekaError.getValue());
                        record.setDuramapErrorNote(duramapError.getValue());
                        record.setNeuroreaderErrorNote(neuroreaderError.getValue());

                        caseRecordService.saveEditedCase(record);
                    }

                    if (mode == Mode.PROCESSED) {
                        record.setFinalWorkflowNotes(finalWorkflowNotes.getValue());
                        caseRecordService.updateInvoiceSent(record, invoiceSent.getValue());
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
        if (mode == Mode.COMPLETED) {
                getFooter().add(cancelButton);
        } else {
                getFooter().add(cancelButton, saveButton);
        }
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

    //activity history helper
    private String formatActivityTime(LocalDateTime value) {

        if (value == null) {
                return "";
        }

        int hour = value.getHour();
        int minute = value.getMinute();

        int displayHour = hour % 12;

        if (displayHour == 0) {
                displayHour = 12;
        }

        String amPm = hour >= 12 ? "PM" : "AM";

        return String.format(
                "%d:%02d %s",
                displayHour,
                minute,
                amPm
        );
    }

    private void openDicomStudyResultsDialog(
                List<DicomStudyResult> studies,
                TextField studyInstanceUid,
                TextField accessionNumber
        ) {
                Dialog dialog = new Dialog();
                dialog.setHeaderTitle("Select DICOM Study");
                dialog.setWidth("900px");

                Grid<DicomStudyResult> grid =
                        new Grid<>(DicomStudyResult.class, false);

                grid.setWidthFull();
                grid.setAllRowsVisible(true);

                grid.addColumn(DicomStudyResult::getPatientName)
                        .setHeader("Patient Name")
                        .setAutoWidth(true);

                grid.addColumn(DicomStudyResult::getPatientId)
                        .setHeader("Patient ID")
                        .setAutoWidth(true);

                grid.addColumn(DicomStudyResult::getStudyDate)
                        .setHeader("Study Date")
                        .setAutoWidth(true);

                grid.addColumn(DicomStudyResult::getAccessionNumber)
                        .setHeader("Accession")
                        .setAutoWidth(true);

                grid.addColumn(DicomStudyResult::getDescription)
                        .setHeader("Description")
                        .setAutoWidth(true);

                grid.addComponentColumn(study -> {
                        Button selectButton = new Button("Select", event -> {
                        studyInstanceUid.setValue(
                                study.getStudyInstanceUid() == null
                                        ? ""
                                        : study.getStudyInstanceUid()
                        );

                        accessionNumber.setValue(
                                study.getAccessionNumber() == null
                                        ? ""
                                        : study.getAccessionNumber()
                        );

                        dialog.close();

                        Notification.show(
                                "Study selected. Click Save to link it to this patient.",
                                3500,
                                Notification.Position.MIDDLE
                        );
                });

                return selectButton;
        }).setHeader("Select").setAutoWidth(true);

        grid.setItems(studies);

        dialog.add(grid);
        dialog.open();
        }

}