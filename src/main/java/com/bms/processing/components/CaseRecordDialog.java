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
import com.bms.processing.model.DicomReportResult;
import com.bms.processing.service.DicomRetrieveService;
import com.bms.processing.service.CurrentUserService;
import com.bms.processing.entity.CaseIssueEntity;
import com.bms.processing.model.CaseIssueStatus;
import com.bms.processing.service.CaseIssueService;
import com.bms.processing.model.PatientStatus;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.DialogVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

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

    //updated 6152026 to gear towards retrievals for DICOM
    private final DicomRetrieveService dicomRetrieveService;
    
    private PatientFilesSection patientFilesSection;
    private DatePicker imekaUploadedDate;

    private final Map<String, String[]> pendingDicomDemographicChanges =
        new LinkedHashMap<>();

    private final CurrentUserService currentUserService;
    private final CaseIssueService caseIssueService;

    // deleted temp constructor, leaving remaining one that works with currentuserservice - updated 08182026
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
                DicomService dicomService,
                DicomRetrieveService dicomRetrieveService,
                CurrentUserService currentUserService,
                CaseIssueService caseIssueService
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
                this.dicomRetrieveService = dicomRetrieveService;
                this.currentUserService = currentUserService;
                this.caseIssueService = caseIssueService;

                setWidth("960px");
                setHeight("820px");
                setMaxWidth("95vw");
                setMaxHeight("92vh");

                addThemeVariants(DialogVariant.LUMO_NO_PADDING);

                buildDialog();
        }

    private void buildDialog() {
        Span dialogTitle = new Span("Patient Summary");
        dialogTitle.getStyle()
                .set("font-size", "1.05rem")
                .set("font-weight", "700")
                .set("color", "white");

        Button closeButton = new Button(
                VaadinIcon.CLOSE_SMALL.create()
        );

        closeButton.addThemeVariants(
                ButtonVariant.LUMO_TERTIARY_INLINE
        );

        closeButton.getStyle()
                .set("color", "white")
                .set("min-width", "36px")
                .set("width", "36px")
                .set("height", "36px");

        closeButton.addClickListener(event -> close());

        HorizontalLayout dialogHeader = new HorizontalLayout(
                dialogTitle,
                closeButton
        );

        dialogHeader.setWidthFull();
        dialogHeader.setAlignItems(
                FlexComponent.Alignment.CENTER
        );

        dialogHeader.setJustifyContentMode(
                FlexComponent.JustifyContentMode.BETWEEN
        );

        dialogHeader.getStyle()
                .set("background", "#004f50")
                .set("padding", "0.9rem 1.35rem")
                .set("box-sizing", "border-box")
                .set("border-radius", "12px 12px 0 0")
                .set("min-height", "64px");

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

        DatePicker invoiceSentDate = new DatePicker("Invoice Sent Date");
        invoiceSentDate.setValue(record.getInvoiceSentDate());

        boolean canEditInvoice =
                currentUserService != null
                        && (currentUserService.isAdmin()
                        || currentUserService.isBms());

        invoiceSentDate.setReadOnly(!canEditInvoice);

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

        VerticalLayout patientSection = buildOverviewSection(
                "Patient Information",
                patientForm
        );

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
                        "Invoice Sent Date",
                        formatDate(record.getInvoiceSentDate())
                )
        );

        VerticalLayout workflowSection = buildOverviewSection(
                "Workflow Details",
                workflowForm
        );

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

        imekaUploadedDate = new DatePicker("IMEKA Uploaded Date");
        imekaUploadedDate.setValue(
                record.getImekaUploadedDate() != null
                        ? record.getImekaUploadedDate().toLocalDate()
                        : null
        );
        imekaUploadedDate.setReadOnly(true);

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

        //updated 8122026, reworked in order to correct issue #23 with the pt management dialog
        //not showing the third party details.
        //Third party processing details remain visible now in all workflow modes, along with
        //processing edits, all other modes provide a read only vendor status summary. 
        if (mode == Mode.PROCESSING) {

                if (!record.isMinorAtScan()) {
                        thirdPartyForm.add(
                                imekaStatus,
                                imekaSentDate,
                                imekaUploadedDate
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

                } else {

                thirdPartyForm.add(
                        buildDisplayField(
                                "IMEKA Status",
                                formatEnum(record.getImekaStatus())
                        ),
                        buildDisplayField(
                                "IMEKA Sent Date",
                                formatDate(record.getImekaSentDate())
                        ),
                        buildDisplayField(
                                "IMEKA Uploaded Date",
                                formatDateTimeCompact(record.getImekaUploadedDate())
                        ),

                        buildDisplayField(
                                "Neuroreader Status",
                                formatEnum(record.getNeuroreaderStatus())
                        ),
                        buildDisplayField(
                                "Neuroreader Sent Date",
                                formatDate(record.getNeuroreaderSentDate())
                        ),

                        buildDisplayField(
                                "DuraMap Status",
                                formatEnum(record.getDuramapStatus())
                        ),
                        buildDisplayField(
                                "DuraMap Sent Date",
                                formatDate(record.getDuramapSentDate())
                        )
                );
        }
        
        VerticalLayout thirdPartySection = buildOverviewSection(
                "Third Party Processing",
                thirdPartyForm
        );

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

        
        Button queryArchiveButton = new Button("Link Study", new Icon(VaadinIcon.SEARCH));

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
                                accessionNumber,
                                lastName,
                                firstName,
                                patientId,
                                sex,
                                dateOfBirth,
                                dateScanned
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

                        //updated 6152026 for a different notif logic for reports found, rewired found framework to dicomreportresults
                        if (reports.isEmpty()) {
                                Notification.show(
                                        "No reports found.",
                                        3000,
                                        Notification.Position.MIDDLE
                                );
                                return;
                        }
                        
                        // changed to pass imekastatus into the helper here - updated 6252026
                        openDicomReportResultsDialog(
                                reports,
                                imekaStatus,
                                imekaSentDate
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

        //new dicom tab layout - 08202026
        queryArchiveButton.setText("Link Study");
        findReportsButton.setText("Find Reports");
        clearDicomLinkButton.setText("Clear Link");

        queryArchiveButton.setIcon(null);
        findReportsButton.setIcon(null);
        clearDicomLinkButton.setIcon(null);

        queryArchiveButton.setWidth("160px");
        findReportsButton.setWidth("160px");
        clearDicomLinkButton.setWidth("160px");

        clearDicomLinkButton.getStyle()
                .set("color", "var(--lumo-error-text-color)");

        HorizontalLayout dicomActions = new HorizontalLayout(
                queryArchiveButton,
                findReportsButton,
                clearDicomLinkButton
        );

        dicomActions.setPadding(false);
        dicomActions.setSpacing(true);
        dicomActions.setWidthFull();

        studyInstanceUid.setPlaceholder("Enter Study Instance UID");
        accessionNumber.setPlaceholder("Enter Accession Number");

        VerticalLayout dicomPanel = new VerticalLayout();
        dicomPanel.setPadding(false);
        dicomPanel.setSpacing(true);
        dicomPanel.setWidthFull();

        dicomPanel.add(
                dicomActions,
                studyInstanceUid,
                accessionNumber
        );

        dicomPanel.getStyle()
                .set("border", "1px solid #e2e8f0")
                .set("border-radius", "10px")
                .set("background", "#ffffff")
                .set("padding", "1.25rem")
                .set("box-sizing", "border-box");

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
        
        //Updated notes section on dialog to allow for issues to be tracked appropriately - updated 08192026
        VerticalLayout notesLayout = new VerticalLayout();
        notesLayout.setPadding(false);
        notesLayout.setSpacing(true);
        notesLayout.setWidthFull();

        TextArea generalNotes = new TextArea("General Notes");
        generalNotes.setWidthFull();
        generalNotes.setValue(nullSafe(record.getNotes()));

        boolean canEditGeneralNotes =
                mode == Mode.UPCOMING
                        || mode == Mode.PROCESSING
                        || mode == Mode.PROCESSED;

        generalNotes.setReadOnly(!canEditGeneralNotes);

        VerticalLayout activeIssuesLayout = new VerticalLayout();
        activeIssuesLayout.setPadding(false);
        activeIssuesLayout.setSpacing(true);
        activeIssuesLayout.setWidthFull();

        var activeIssues = caseIssueService.findActiveByCaseRecord(record);

        if (activeIssues.isEmpty()) {
                Span empty = new Span("No active issues.");
                empty.getStyle()
                        .set("font-size", "0.88rem")
                        .set("color", "#64748b");

                activeIssuesLayout.add(empty);

        } else {
                for (CaseIssueEntity issue : activeIssues) {

                        VerticalLayout issueCard = new VerticalLayout();
                        issueCard.setPadding(false);
                        issueCard.setSpacing(false);
                        issueCard.setWidthFull();

                        Span title = new Span(issue.getTitle());
                        title.getStyle()
                                .set("font-weight", "700")
                                .set("font-size", "0.95rem")
                                .set("color", "#1e293b");

                        Span source = new Span(
                                formatEnum(issue.getIssueSource())
                        );

                        source.getStyle()
                                .set("font-size", "0.8rem")
                                .set("color", "#64748b");

                        Span note = new Span(
                                nullSafe(issue.getDescription())
                        );

                        note.getStyle()
                                .set("font-size", "0.88rem")
                                .set("color", "#334155")
                                .set("white-space", "pre-wrap")
                                .set("margin-top", "0.35rem");

                        String createdText = "Reported";

                        if (issue.getCreatedAt() != null) {
                                createdText += " "
                                        + formatDateTimeCompact(issue.getCreatedAt());
                        }

                        if (issue.getCreatedBy() != null
                                && !issue.getCreatedBy().isBlank()) {
                                createdText += " by " + issue.getCreatedBy();
                        }

                        Span created = new Span(createdText);
                        created.getStyle()
                                .set("font-size", "0.78rem")
                                .set("color", "#64748b")
                                .set("margin-top", "0.35rem");

                        Button editButton = new Button("Edit");
                        editButton.addThemeVariants(
                                ButtonVariant.LUMO_SMALL,
                                ButtonVariant.LUMO_TERTIARY
                        );

                        editButton.addClickListener(event ->
                                new CaseIssueDialog(
                                        record,
                                        issue,
                                        caseIssueService,
                                        currentUserService,
                                        () -> {
                                                if (afterSave != null) {
                                                        afterSave.run();
                                                }
                                        }
                                ).open()
                        );

                        Button resolveButton = new Button("Resolve");
                        resolveButton.addThemeVariants(
                                ButtonVariant.LUMO_SMALL,
                                ButtonVariant.LUMO_PRIMARY
                        );

                        resolveButton.addClickListener(event ->
                                new CaseIssueDialog(
                                        record,
                                        issue,
                                        caseIssueService,
                                        currentUserService,
                                        () -> {
                                                if (afterSave != null) {
                                                        afterSave.run();
                                                }
                                        }
                                ).open()
                        );

                        HorizontalLayout actions = new HorizontalLayout(
                                editButton,
                                resolveButton
                        );

                        actions.setPadding(false);
                        actions.setSpacing(true);
                        actions.setMargin(false);

                        HorizontalLayout footer = new HorizontalLayout(
                                created,
                                actions
                        );

                        footer.setWidthFull();
                        footer.setPadding(false);
                        footer.setSpacing(true);
                        footer.setAlignItems(
                                com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER
                        );
                        footer.setJustifyContentMode(
                                com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN
                        );

                        issueCard.add(
                                title,
                                source,
                                note,
                                footer
                        );

                        issueCard.getStyle()
                                .set("border", "1px solid #e2e8f0")
                                .set("border-radius", "10px")
                                .set("background", "#ffffff")
                                .set("padding", "0.85rem")
                                .set("box-sizing", "border-box");

                        if (Boolean.TRUE.equals(issue.getBlocking())) {
                                issueCard.getStyle()
                                        .set("border-left", "3px solid var(--lumo-error-color)");
                        }

                        activeIssuesLayout.add(issueCard);
                }
        }

        VerticalLayout resolvedIssuesLayout = new VerticalLayout();
        resolvedIssuesLayout.setPadding(false);
        resolvedIssuesLayout.setSpacing(true);
        resolvedIssuesLayout.setWidthFull();

        var resolvedIssues = caseIssueService.findByCaseRecord(record).stream()
                .filter(issue -> issue.getStatus() == CaseIssueStatus.RESOLVED)
                .sorted((a, b) -> {
                if (a.getResolvedAt() == null && b.getResolvedAt() == null) {
                        return 0;
                }
                if (a.getResolvedAt() == null) {
                        return 1;
                }
                if (b.getResolvedAt() == null) {
                        return -1;
                }

                return b.getResolvedAt().compareTo(a.getResolvedAt());
                })
                .toList();

        if (resolvedIssues.isEmpty()) {
        resolvedIssuesLayout.add(new Span("No resolved issues."));
        } else {
        for (CaseIssueEntity issue : resolvedIssues) {
                VerticalLayout issueCard = new VerticalLayout();
                issueCard.setPadding(true);
                issueCard.setSpacing(false);
                issueCard.setWidthFull();

                Span title = new Span(issue.getTitle());
                title.getStyle().set("font-weight", "700");

                Span source = new Span(
                        "Source: "
                                + formatEnum(issue.getIssueSource())
                );

                Span note = new Span(
                        "Issue: "
                                + nullSafe(issue.getDescription())
                );

                Span resolution = new Span(
                        "Resolution: "
                                + nullSafe(issue.getResolutionNote())
                );

                Span resolvedBy = new Span(
                        "Resolved by "
                                + nullSafe(issue.getResolvedBy())
                                + (issue.getResolvedAt() != null
                                ? " on " + formatDateTimeCompact(issue.getResolvedAt())
                                : "")
                );

                source.getStyle()
                        .set("font-size", "0.82rem")
                        .set("color", "#64748b");

                note.getStyle()
                        .set("font-size", "0.88rem")
                        .set("color", "#334155");

                resolution.getStyle()
                        .set("font-size", "0.88rem")
                        .set("color", "#334155");

                resolvedBy.getStyle()
                        .set("font-size", "0.8rem")
                        .set("color", "#64748b");

                issueCard.add(
                        title,
                        source,
                        note,
                        resolution,
                        resolvedBy
                );

                issueCard.getStyle()
                        .set("border", "1px solid #e2e8f0")
                        .set("border-radius", "10px")
                        .set("background", "#ffffff")
                        .set("padding", "0.75rem")
                        .set("gap", "0.2rem");

                resolvedIssuesLayout.add(issueCard);
        }
        }

        //updated 08192026 for better alignment in notes section
        Span generalNotesHeader = new Span("General Notes");
        generalNotesHeader.getStyle()
                .set("font-weight", "700")
                .set("font-size", "0.95rem");

        Span activeIssuesHeader = new Span("Active Issues");
        activeIssuesHeader.getStyle()
                .set("font-weight", "700")
                .set("font-size", "0.95rem")
                .set("margin-top", "0.75rem");

        Span resolvedIssuesHeader = new Span("Resolved Issues");
        resolvedIssuesHeader.getStyle()
                .set("font-weight", "700")
                .set("font-size", "0.95rem")
                .set("margin-top", "0.75rem");

        notesLayout.add(
                generalNotesHeader,
                generalNotes,
                activeIssuesHeader,
                activeIssuesLayout,
                resolvedIssuesHeader,
                resolvedIssuesLayout
        );

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

        //new audit events tab design - 08202026
        auditEvents.forEach(event -> {

                HorizontalLayout row = new HorizontalLayout();
                row.setWidthFull();
                row.setPadding(false);
                row.setSpacing(true);
                row.setAlignItems(
                        com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.START
                );

                row.getStyle()
                        .set("padding", "0.75rem 0")
                        .set("border-bottom", "1px solid #e2e8f0");

                Span time = new Span(
                        formatActivityDateTime(event.getCreatedAt())
                );

                time.getStyle()
                        .set("font-size", "0.8rem")
                        .set("font-weight", "600")
                        .set("color", "#64748b")
                        .set("min-width", "145px")
                        .set("flex-shrink", "0");

                VerticalLayout eventContent = new VerticalLayout();
                eventContent.setPadding(false);
                eventContent.setSpacing(false);
                eventContent.setWidthFull();

                Span eventType = new Span(
                        formatAuditEventType(event.getEventType())
                );

                eventType.getStyle()
                        .set("font-size", "0.88rem")
                        .set("font-weight", "700")
                        .set("color", "#1e293b");

                Span message = new Span(
                        event.getMessage() == null
                                ? ""
                                : event.getMessage()
                );

                message.getStyle()
                        .set("font-size", "0.84rem")
                        .set("color", "#64748b")
                        .set("white-space", "normal");

                eventContent.add(
                        eventType,
                        message
                );

                row.add(
                        time,
                        eventContent
                );

                historyLayout.add(row);
        });
        }

        patientFilesSection =
                new PatientFilesSection(record, patientFileService, baseStoragePath);

        Details patientFilesDetails =
                new Details("Patient Files", patientFilesSection);

        patientFilesDetails.setOpened(false);

        Details activityHistoryDetails =
                new Details("Activity History", historyLayout);

        activityHistoryDetails.setOpened(false);

        VerticalLayout overviewContent = new VerticalLayout();
        overviewContent.setPadding(false);
        overviewContent.setSpacing(true);
        overviewContent.setWidthFull();

        VerticalLayout issuesContent = new VerticalLayout();
        issuesContent.setPadding(false);
        issuesContent.setSpacing(true);
        issuesContent.setWidthFull();

        VerticalLayout filesContent = new VerticalLayout();
        filesContent.setPadding(false);
        filesContent.setSpacing(true);
        filesContent.setWidthFull();

        VerticalLayout dicomContent = new VerticalLayout();
        dicomContent.setPadding(false);
        dicomContent.setSpacing(true);
        dicomContent.setWidthFull();

        VerticalLayout activityContent = new VerticalLayout();
        activityContent.setPadding(false);
        activityContent.setSpacing(true);
        activityContent.setWidthFull();


        // new tabs: overview, issues, files, dicom, and activity. Redesigning - updated 08202026
        issuesContent.add(
                notesLayout
        );
        filesContent.add(
                patientFilesSection
        );
        dicomContent.add(
                dicomPanel
        );
        activityContent.add(
                historyLayout
        );

        switch (mode) {

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
                        invoiceSentDate
                );

                VerticalLayout upcomingSection = buildOverviewSection(
                        "Upcoming Information",
                        upcomingForm
                );

                overviewContent.add(
                        patientSection,
                        upcomingSection
                );
        }

        case PROCESSING -> {
                overviewContent.add(
                        patientSection,
                        workflowSection,
                        thirdPartySection
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
                        invoiceSentDate,
                        buildDisplayField(
                                "Processed Date",
                                formatDateTimeCompact(record.getProcessedDate())
                        ),
                        buildDisplayField(
                                "Completed Date",
                                formatDateTimeCompact(record.getCompletedDate())
                        )
                );

                bmsReviewForm.add(finalWorkflowNotes);

                VerticalLayout bmsReviewSection = buildOverviewSection(
                        "BMS Review",
                        bmsReviewForm
                );

                overviewContent.add(
                        patientSection,
                        workflowSection,
                        thirdPartySection,
                        bmsReviewSection
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
                        buildDisplayField(
                                "Processed Date",
                                formatDateTimeCompact(record.getProcessedDate())
                        ),
                        buildDisplayField(
                                "Completed Date",
                                formatDateTimeCompact(record.getCompletedDate())
                        ),
                        buildDisplayField(
                                "Invoice Sent Date",
                                formatDate(record.getInvoiceSentDate())
                        ),
                        buildDisplayField(
                                "Final Workflow Notes",
                                nullSafe(record.getFinalWorkflowNotes())
                        )
                );

                VerticalLayout archiveSection = buildOverviewSection(
                        "Archived Record",
                        archiveForm
                );

                overviewContent.add(
                        patientSection,
                        workflowSection,
                        thirdPartySection,
                        archiveSection
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
                        buildDisplayField(
                                "Workflow Status",
                                formatEnum(record.getPatientStatus())
                        ),
                        buildDisplayField(
                                "General Notes",
                                nullSafe(record.getNotes())
                        )
                );

                TextArea resolutionNotes =
                        new TextArea("Resolution Notes");

                resolutionNotes.setWidthFull();

                errorReviewForm.add(resolutionNotes);

                VerticalLayout errorReviewSection = buildOverviewSection(
                        "Error Review",
                        errorReviewForm
                );

                overviewContent.add(
                        patientSection,
                        workflowSection,
                        thirdPartySection,
                        errorReviewSection
                );
        }

        case SUMMARY -> {
                overviewContent.add(
                        patientSection,
                        workflowSection,
                        thirdPartySection
                );
        }
        }

        //actual tab builds here. - 08202026
        Tab overviewTab = new Tab("Overview");
        Tab issuesTab = new Tab("Issues");
        Tab filesTab = new Tab("Files");
        Tab dicomTab = new Tab("DICOM");
        Tab activityTab = new Tab("Activity");

        Tabs tabs = new Tabs(
                overviewTab,
                issuesTab,
                filesTab,
                dicomTab,
                activityTab
        );

        tabs.setWidthFull();

        tabs.getStyle()
                .set("background", "#ffffff")
                .set("border-bottom", "1px solid #e2e8f0")
                .set("padding", "0 1rem");

        for (Tab tab : List.of(
                overviewTab,
                issuesTab,
                filesTab,
                dicomTab,
                activityTab
        )) {
                tab.getStyle()
                        .set("font-size", "0.9rem")
                        .set("font-weight", "500")
                        .set("padding-left", "0.75rem")
                        .set("padding-right", "0.75rem");
        }

        Div tabContent = new Div();
        tabContent.setWidthFull();

        tabContent.getStyle()
                .set("padding", "1rem 1.25rem 1.25rem")
                .set("box-sizing", "border-box")
                .set("background", "#f8fafc")
                .set("overflow-y", "auto")
                .set("flex", "1")
                .set("min-height", "0");

        Map<Tab, Component> tabPages = new LinkedHashMap<>();

        tabPages.put(overviewTab, overviewContent);
        tabPages.put(issuesTab, issuesContent);
        tabPages.put(filesTab, filesContent);
        tabPages.put(dicomTab, dicomContent);
        tabPages.put(activityTab, activityContent);

        tabPages.values().forEach(page ->
                page.setVisible(false)
        );

        overviewContent.setVisible(true);

        tabs.addSelectedChangeListener(event -> {
        tabPages.values().forEach(page ->
                page.setVisible(false)
        );

        Component selectedPage =
                tabPages.get(event.getSelectedTab());

        if (selectedPage != null) {
                selectedPage.setVisible(true);
        }
        });

        tabContent.add(
                overviewContent,
                issuesContent,
                filesContent,
                dicomContent,
                activityContent
        );

        VerticalLayout content = new VerticalLayout(
                dialogHeader,
                tabs,
                tabContent
        );

        content.setPadding(false);
        content.setSpacing(false);
        content.setWidthFull();
        content.setHeightFull();

        content.getStyle()
                .set("overflow", "hidden");

        Button moveCaseButton = new Button("Move Case");
        moveCaseButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

        moveCaseButton.addClickListener(event ->
                openMoveCaseDialog()
        );

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

                if (!pendingDicomDemographicChanges.isEmpty()) {
                        pendingDicomDemographicChanges.forEach((fieldName, values) ->
                                auditEventService.logTimelineEvent(
                                        "DICOM_DEMOGRAPHIC_UPDATE",
                                        record,
                                        fieldName,
                                        values[0],
                                        values[1],
                                        "SYSTEM"
                                )
                        );

                        pendingDicomDemographicChanges.clear();
                }

                if (afterSave != null) {
                    if (mode == Mode.PROCESSING) {

                        record.setNotes(generalNotes.getValue());
                        record.setImekaErrorNote(imekaError.getValue());
                        record.setDuramapErrorNote(duramapError.getValue());
                        record.setNeuroreaderErrorNote(neuroreaderError.getValue());

                        caseRecordService.saveEditedCase(record);
                    }

                    if (mode == Mode.PROCESSED) {
                        record.setFinalWorkflowNotes(finalWorkflowNotes.getValue());
                        caseRecordService.saveEditedCase(record);
                    }

                    if (canEditInvoice) {
                        caseRecordService.updateInvoiceSentDate(
                                record,
                                invoiceSentDate.getValue(),
                                currentUserService.getUsername()
                        );
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
                getFooter().add(
                        moveCaseButton,
                        cancelButton
                );
        } else {
                getFooter().add(
                        moveCaseButton,
                        cancelButton,
                        saveButton
                );
        }

    }

    private void openMoveCaseDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Move Case");
        dialog.setWidth("500px");

        ComboBox<PatientStatus> destination =
                new ComboBox<>("Move To");

        boolean isAdmin = currentUserService.isAdmin();

        if (isAdmin) {
                destination.setItems(
                        PatientStatus.UPCOMING,
                        PatientStatus.ACQUIRED,
                        PatientStatus.PROCESSING,
                        PatientStatus.PROCESSED,
                        PatientStatus.COMPLETED
                );
        } else {
                int currentOrder = workflowOrder(record.getPatientStatus());

                destination.setItems(
                        java.util.stream.Stream.of(
                                        PatientStatus.UPCOMING,
                                        PatientStatus.ACQUIRED,
                                        PatientStatus.PROCESSING,
                                        PatientStatus.PROCESSED,
                                        PatientStatus.COMPLETED
                                )
                                .filter(status ->
                                        workflowOrder(status) < currentOrder
                                )
                                .toList()
                );
        }

        destination.setItemLabelGenerator(this::formatEnum);
        destination.setWidthFull();

        TextArea reason = new TextArea("Reason");
        reason.setWidthFull();
        reason.setMinHeight("120px");
        reason.setRequired(true);

        Button cancel = new Button("Cancel", e -> dialog.close());

        Button move = new Button("Move Case");
        move.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        move.addClickListener(event -> {
                if (destination.getValue() == null) {
                showError("Destination workflow status is required.");
                return;
                }

                if (reason.getValue() == null
                        || reason.getValue().trim().isEmpty()) {
                showError("A reason is required.");
                return;
                }

                try {
                if (isAdmin) {
                        caseRecordService.adminOverrideWorkflowStatus(
                                record,
                                destination.getValue(),
                                reason.getValue().trim(),
                                currentUserService.getUsername()
                        );
                } else {
                        caseRecordService.rollbackWorkflowStatus(
                                record,
                                destination.getValue(),
                                reason.getValue().trim(),
                                currentUserService.getUsername()
                        );
                }

                if (afterSave != null) {
                        afterSave.run();
                }

                dialog.close();
                close();

                } catch (InvalidWorkflowTransitionException ex) {
                showError(ex.getMessage());
                }
        });

        VerticalLayout content = new VerticalLayout(
                destination,
                reason
        );
        content.setPadding(false);
        content.setSpacing(true);

        dialog.add(content);
        dialog.getFooter().add(cancel, move);
        dialog.open();
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

    private String buildPatientName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();

        if (first.isEmpty()) {
                return last;
        }

        if (last.isEmpty()) {
                return first;
        }

        return last + ", " + first;
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
                TextField accessionNumber,
                TextField lastName,
                TextField firstName,
                TextField patientId,
                TextField sex,
                DatePicker dateOfBirth,
                DatePicker dateScanned
    ) {
                Dialog dialog = new Dialog();
                dialog.setHeaderTitle("Select DICOM Study");
                dialog.setWidth("900px");
        

                Grid<DicomStudyResult> grid =
                        new Grid<>(DicomStudyResult.class, false);

                grid.setWidthFull();
                grid.setAllRowsVisible(true);

                // updated grid here with a cleaner look for dcm results, updated 08172026
                grid.addColumn(study -> nullSafe(study.getParsedLastName()))
                        .setHeader("Last Name")
                        .setAutoWidth(true);

                grid.addColumn(study -> nullSafe(study.getParsedFirstName()))
                        .setHeader("First Name")
                        .setAutoWidth(true);

                grid.addColumn(study -> nullSafe(study.getPatientId()))
                        .setHeader("Patient ID")
                        .setAutoWidth(true);

                grid.addColumn(study -> {
                        LocalDate dob = parseDicomDate(study.getPatientBirthDate());
                        return dob != null ? dob.toString() : "";
                        })
                        .setHeader("Date of Birth")
                        .setAutoWidth(true);

                grid.addColumn(study -> nullSafe(study.getPatientSex()))
                        .setHeader("Sex")
                        .setAutoWidth(true);

                grid.addColumn(study -> {
                        LocalDate studyDate = parseDicomDate(study.getStudyDate());
                        return studyDate != null ? studyDate.toString() : "";
                        })
                        .setHeader("Study Date")
                        .setAutoWidth(true);

                grid.addColumn(study -> nullSafe(study.getStudyInstanceUid()))
                        .setHeader("Study UID")
                        .setAutoWidth(true);

                grid.addColumn(study -> nullSafe(study.getAccessionNumber()))
                        .setHeader("Accession")
                        .setAutoWidth(true);

                grid.addColumn(study -> nullSafe(study.getDescription()))
                        .setHeader("Description")
                        .setAutoWidth(true);

                grid.addComponentColumn(study -> {
                        Button selectButton = new Button("Select", event -> {
                                //removed previous linking block, now accounts for manual selection of pt data choice updated 8122026
                                openStudyLinkConfirmationDialog(
                                        study,
                                        studyInstanceUid,
                                        accessionNumber,
                                        lastName,
                                        firstName,
                                        patientId,
                                        sex,
                                        dateOfBirth,
                                        dateScanned,
                                        dialog
                                );
                });
                return selectButton;
        }).setHeader("Select").setAutoWidth(true);

        grid.setItems(studies);

        dialog.add(grid);
        dialog.open();
        }

        private void openDicomReportResultsDialog(
                List<DicomReportResult> reports,
                ComboBox<String> imekaStatus,
                DatePicker imekaSentDate
        ) {
                Dialog dialog = new Dialog();
                dialog.setHeaderTitle("DICOM Reports");
                dialog.setWidth("900px");

                Grid<DicomReportResult> grid =
                        new Grid<>(DicomReportResult.class, false);

                grid.setWidthFull();
                grid.setAllRowsVisible(true);

                grid.addColumn(DicomReportResult::getSeriesDescription)
                        .setHeader("Series")
                        .setAutoWidth(true);

                grid.addColumn(DicomReportResult::getContentDate)
                        .setHeader("Content Date")
                        .setAutoWidth(true);

                grid.addColumn(DicomReportResult::getInstanceNumber)
                        .setHeader("Instance")
                        .setAutoWidth(true);

                grid.addColumn(DicomReportResult::getSopInstanceUid)
                        .setHeader("SOP Instance UID")
                        .setAutoWidth(true);

                grid.addComponentColumn(report -> {
                        Button retrieveButton = new Button("Retrieve", event -> {
                        var retrieveResult = dicomRetrieveService.retrieveReport(
                                record.getId(),
                                report,
                                baseStoragePath
                        );

                        // changing this to take into account for dcm retrieves and uploads to automatically change status - updated 6302026
                        if (retrieveResult.isSuccess()) {
                                LocalDate today = LocalDate.now();

                                record.setImekaStatus(ThirdPartyStatus.UPLOADED);
                                record.setImekaUploadedDate(LocalDateTime.now());
                                imekaUploadedDate.setValue(
                                        record.getImekaUploadedDate().toLocalDate()
                                );

                                if (record.getImekaSentDate() == null) {
                                        record.setImekaSentDate(today);
                                }

                                if (imekaStatus != null) {
                                        imekaStatus.setValue(ThirdPartyStatus.UPLOADED.name());
                                }

                                if (imekaSentDate != null && imekaSentDate.getValue() == null) {
                                        imekaSentDate.setValue(today);
                                }

                                patientFilesSection.refresh();
                        }

                        Notification.show(
                                retrieveResult.getMessage(),
                                3000,
                                Notification.Position.MIDDLE
                        );
                });

                return retrieveButton;
        }).setHeader("Action").setAutoWidth(true);

                grid.setItems(reports);

                dialog.add(grid);
                dialog.open();
        }

        //validates DICOM demographics before linking a study.
        // A selected study may be linked despite a mismatch, but DICOM demographics
        // must never overwrite the existing Prism patient record updated 8122026
        private void openStudyLinkConfirmationDialog(
                DicomStudyResult study,
                TextField studyInstanceUid,
                TextField accessionNumber,
                TextField lastName,
                TextField firstName,
                TextField patientId,
                TextField sex,
                DatePicker dateOfBirth,
                DatePicker dateScanned,
                Dialog studyResultsDialog
        ) {
                Dialog confirmDialog = new Dialog();
                confirmDialog.setHeaderTitle("Verify Study Link");
                confirmDialog.setWidth("760px");

                //blocks updating patient info to db if duplicate exists - updated 08122026
                if (caseRecordService.isStudyLinkedToAnotherCase(
                                record,
                                study.getStudyInstanceUid()
                        )) {
                                Notification.show(
                                        "This DICOM study is already linked to another patient case.",
                                        4000,
                                        Notification.Position.MIDDLE
                                );

                        return;
                }

                LocalDate dicomDob = parseDicomDate(study.getPatientBirthDate());
                LocalDate dicomStudyDate = parseDicomDate(study.getStudyDate());

                boolean patientIdMatches = valuesMatch(
                        record.getPatientId(),
                        study.getPatientId()
                );

                boolean lastNameMatches = valuesMatch(
                        record.getPatientLastName(),
                        study.getParsedLastName()
                );

                boolean firstNameMatches = valuesMatch(
                        record.getPatientFirstName(),
                        study.getParsedFirstName()
                );

                boolean dobMatches = datesMatch(
                        record.getDateOfBirth(),
                        dicomDob
                );

                boolean sexMatches = valuesMatch(
                        record.getSex(),
                        study.getPatientSex()
                );

                boolean studyDateMatches = datesMatch(
                        record.getDateScanned(),
                        dicomStudyDate
                );

                boolean demographicsMatch =
                        patientIdMatches
                                && lastNameMatches
                                && firstNameMatches
                                && dobMatches
                                && sexMatches
                                && studyDateMatches;

                Span message = new Span(
                        demographicsMatch
                                ? "Patient information matches the selected DICOM study."
                                : "Warning: The selected DICOM study does not fully match this patient record."
                );

                if (!demographicsMatch) {
                        message.getStyle()
                                .set("color", "var(--lumo-error-text-color)")
                                .set("font-weight", "700");
                } else {
                        message.getStyle()
                                .set("color", "var(--lumo-success-text-color)")
                                .set("font-weight", "700");
                }

                FormLayout comparisonForm = new FormLayout();
                comparisonForm.setWidthFull();
                comparisonForm.setResponsiveSteps(
                        new FormLayout.ResponsiveStep("0", 1),
                        new FormLayout.ResponsiveStep("700px", 2)
                );

                comparisonForm.add(
                        buildComparisonField(
                                "Prism Patient ID",
                                record.getPatientId(),
                                patientIdMatches
                        ),
                        buildComparisonField(
                                "DICOM Patient ID",
                                study.getPatientId(),
                                patientIdMatches
                        ),

                        buildComparisonField(
                                "Prism Name",
                                buildPatientName(
                                        record.getPatientFirstName(),
                                        record.getPatientLastName()
                                ),
                                firstNameMatches && lastNameMatches
                        ),
                        buildComparisonField(
                                "DICOM Name",
                                buildPatientName(
                                        study.getParsedFirstName(),
                                        study.getParsedLastName()
                                ),
                                firstNameMatches && lastNameMatches
                        ),

                        buildComparisonField(
                                "Prism DOB",
                                formatDate(record.getDateOfBirth()),
                                dobMatches
                        ),
                        buildComparisonField(
                                "DICOM DOB",
                                formatDate(dicomDob),
                                dobMatches
                        ),

                        buildComparisonField(
                                "Prism Sex",
                                record.getSex(),
                                sexMatches
                        ),
                        buildComparisonField(
                                "DICOM Sex",
                                study.getPatientSex(),
                                sexMatches
                        ),

                        buildComparisonField(
                                "Prism Scan Date",
                                formatDate(record.getDateScanned()),
                                studyDateMatches
                        ),
                        buildComparisonField(
                                "DICOM Study Date",
                                formatDate(dicomStudyDate),
                                studyDateMatches
                        )
                );

                Button cancelButton = new Button(
                        "Cancel",
                        event -> confirmDialog.close()
                );

                //allows for manual selection of updating patient info instead of doing it automatically - updated 8122026
                if (demographicsMatch) {

                        Button confirmLinkButton = new Button("Confirm Link");
                        confirmLinkButton.addThemeVariants(
                                ButtonVariant.LUMO_PRIMARY,
                                ButtonVariant.LUMO_SUCCESS
                        );
                        confirmLinkButton.addClickListener(event -> {

                                studyInstanceUid.setValue(
                                        nullSafe(study.getStudyInstanceUid())
                                );

                                accessionNumber.setValue(
                                        nullSafe(study.getAccessionNumber())
                                );

                                confirmDialog.close();
                                studyResultsDialog.close();

                                Notification.show(
                                        "Study selected. Click Save to complete the link.",
                                        3500,
                                        Notification.Position.MIDDLE
                                );
                        });
                        confirmDialog.getFooter().add(
                                cancelButton,
                                confirmLinkButton
                        );

                } else {

                        Button keepCurrentButton =
                                new Button("Keep Current Patient Info");
                        Button updateFromDicomButton =
                                new Button("Update from DICOM Study");
                        updateFromDicomButton.addThemeVariants(
                                ButtonVariant.LUMO_PRIMARY
                        );
                        confirmDialog.getFooter().add(
                                cancelButton,
                                keepCurrentButton,
                                updateFromDicomButton
                        );

                        keepCurrentButton.addClickListener(event -> {

                                studyInstanceUid.setValue(
                                        nullSafe(study.getStudyInstanceUid())
                                );

                                accessionNumber.setValue(
                                        nullSafe(study.getAccessionNumber())
                                );

                                confirmDialog.close();
                                studyResultsDialog.close();

                                Notification.show(
                                        "Study selected. Current patient information was retained.",
                                        3500,
                                        Notification.Position.MIDDLE
                                );
                        });

                        updateFromDicomButton.addClickListener(event -> {

                                // added audit events for dicom section - updated 08172026
                                String oldPatientId = patientId.getValue();
                                String oldLastName = lastName.getValue();
                                String oldFirstName = firstName.getValue();
                                String oldSex = sex.getValue();
                                LocalDate oldDob = dateOfBirth.getValue();
                                LocalDate oldScanDate = dateScanned.getValue();

                                studyInstanceUid.setValue(
                                        nullSafe(study.getStudyInstanceUid())
                                );

                                accessionNumber.setValue(
                                        nullSafe(study.getAccessionNumber())
                                );

                                if (study.getParsedLastName() != null
                                        && !study.getParsedLastName().isBlank()) {
                                        lastName.setValue(study.getParsedLastName().trim());
                                }

                                if (study.getParsedFirstName() != null
                                        && !study.getParsedFirstName().isBlank()) {
                                        firstName.setValue(study.getParsedFirstName().trim());
                                }

                                if (study.getPatientId() != null
                                        && !study.getPatientId().isBlank()) {
                                        patientId.setValue(study.getPatientId().trim());
                                }

                                if (study.getPatientSex() != null
                                        && !study.getPatientSex().isBlank()) {
                                        sex.setValue(study.getPatientSex().trim());
                                }

                                if (dicomDob != null) {
                                        dateOfBirth.setValue(dicomDob);
                                }

                                if (dicomStudyDate != null) {
                                        dateScanned.setValue(dicomStudyDate);
                                }

                                pendingDicomDemographicChanges.clear();

                                if (!oldPatientId.equals(patientId.getValue())) {
                                        pendingDicomDemographicChanges.put(
                                                "Patient ID",
                                                new String[]{oldPatientId, patientId.getValue()}
                                        );
                                }

                                if (!oldLastName.equals(lastName.getValue())) {
                                        pendingDicomDemographicChanges.put(
                                                "Last Name",
                                                new String[]{oldLastName, lastName.getValue()}
                                        );
                                }

                                if (!oldFirstName.equals(firstName.getValue())) {
                                        pendingDicomDemographicChanges.put(
                                                "First Name",
                                                new String[]{oldFirstName, firstName.getValue()}
                                        );
                                }

                                if (!oldSex.equals(sex.getValue())) {
                                        pendingDicomDemographicChanges.put(
                                                "Sex",
                                                new String[]{oldSex, sex.getValue()}
                                        );
                                }

                                if (!java.util.Objects.equals(oldDob, dateOfBirth.getValue())) {
                                        pendingDicomDemographicChanges.put(
                                                "Date of Birth",
                                                new String[]{
                                                        oldDob != null ? oldDob.toString() : "",
                                                        dateOfBirth.getValue() != null
                                                                ? dateOfBirth.getValue().toString()
                                                                : ""
                                                }
                                        );
                                }

if (!java.util.Objects.equals(oldScanDate, dateScanned.getValue())) {
        pendingDicomDemographicChanges.put(
                "Date Scanned",
                new String[]{
                        oldScanDate != null ? oldScanDate.toString() : "",
                        dateScanned.getValue() != null
                                ? dateScanned.getValue().toString()
                                : ""
                }
        );
}

                                confirmDialog.close();
                                studyResultsDialog.close();

                                Notification.show(
                                        "Study selected and patient information updated from DICOM.",
                                        3500,
                                        Notification.Position.MIDDLE
                                );
                        });
                }

                        VerticalLayout content = new VerticalLayout(
                                message,
                                comparisonForm
                        );
                        content.setPadding(false);
                        content.setSpacing(true);
                        content.setWidthFull();

                        confirmDialog.add(content);
                        confirmDialog.open();
        }

        private Component buildComparisonField(
        String label,
        String value,
        boolean matches
) {
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
        valueBox.setText(
                value == null || value.isBlank()
                        ? "-"
                        : value
        );

        valueBox.getStyle()
                .set("padding", "0.65rem 0.75rem")
                .set("border-radius", "10px")
                .set("min-height", "42px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("box-sizing", "border-box");

        if (matches) {
                valueBox.getStyle()
                        .set("border", "1px solid var(--lumo-success-color-50pct)")
                        .set("background", "var(--lumo-success-color-10pct)");
        } else {
                valueBox.getStyle()
                        .set("border", "1px solid var(--lumo-error-color-50pct)")
                        .set("background", "var(--lumo-error-color-10pct)");
        }

        wrapper.add(labelSpan, valueBox);

        return wrapper;
        }

        private boolean valuesMatch(String prismValue, String dicomValue) {
                if ((prismValue == null || prismValue.isBlank())
                        && (dicomValue == null || dicomValue.isBlank())) {
                        return true;
                }

                if (prismValue == null || dicomValue == null) {
                        return false;
                }

                return prismValue.trim().equalsIgnoreCase(dicomValue.trim());
        }

        private boolean datesMatch(LocalDate prismDate, LocalDate dicomDate) {
                if (prismDate == null && dicomDate == null) {
                        return true;
                }

                if (prismDate == null || dicomDate == null) {
                        return false;
                }

                return prismDate.equals(dicomDate);
        }

        private LocalDate parseDicomDate(String value) {
        if (value == null || value.length() != 8) {
                return null;
        }

        return LocalDate.of(
                        Integer.parseInt(value.substring(0, 4)),
                        Integer.parseInt(value.substring(4, 6)),
                        Integer.parseInt(value.substring(6, 8))
                );
        }

        private int workflowOrder(PatientStatus status) {
                return switch (status) {
                        case UPCOMING -> 0;
                        case ACQUIRED -> 1;
                        case PROCESSING -> 2;
                        case PROCESSED -> 3;
                        case COMPLETED -> 4;
                        default -> -1;
                };
        }

        //helper for new dialog design - 08202026
        private VerticalLayout buildOverviewSection(
                String title,
                Component content
        ) {
                VerticalLayout section = new VerticalLayout();
                section.setPadding(false);
                section.setSpacing(false);
                section.setWidthFull();

                Span sectionTitle = new Span(title);
                sectionTitle.getStyle()
                        .set("font-size", "0.95rem")
                        .set("font-weight", "700")
                        .set("color", "#1e293b")
                        .set("margin-bottom", "0.45rem");

                Div card = new Div();
                card.setWidthFull();
                card.add(content);

                card.getStyle()
                        .set("border", "1px solid #e2e8f0")
                        .set("border-radius", "10px")
                        .set("background", "#ffffff")
                        .set("padding", "1rem")
                        .set("box-sizing", "border-box");

                section.add(
                        sectionTitle,
                        card
                );

                return section;
        }

        //activity history helper
        private String formatAuditEventType(String eventType) {
                if (eventType == null || eventType.isBlank()) {
                        return "Activity";
                }

                String formatted = eventType
                        .replace("_", " ")
                        .toLowerCase();

                String[] words = formatted.split(" ");
                StringBuilder result = new StringBuilder();

                for (String word : words) {
                        if (word.isBlank()) {
                                continue;
                        }

                        if (!result.isEmpty()) {
                                result.append(" ");
                        }

                        result.append(
                                Character.toUpperCase(word.charAt(0))
                        );

                        if (word.length() > 1) {
                                result.append(word.substring(1));
                        }
                }

                return result.toString();
        }

        //time and date for activity log dialog - 08202026
        private String formatActivityDateTime(LocalDateTime value) {
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
                        "%02d/%02d/%02d  %d:%02d %s",
                        value.getMonthValue(),
                        value.getDayOfMonth(),
                        value.getYear() % 100,
                        displayHour,
                        minute,
                        amPm
                );
        }

}