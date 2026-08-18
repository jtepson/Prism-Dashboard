package com.bms.processing.views;

import com.bms.processing.entity.CaseRecordEntity;
import com.bms.processing.layouts.MainLayout;
import com.bms.processing.model.PatientStatus;
import com.bms.processing.service.CaseRecordService;
import com.bms.processing.service.InvalidWorkflowTransitionException;
import com.bms.processing.components.CaseRecordDialog;
import com.bms.processing.service.AuditEventService;
import com.bms.processing.entity.AuditEventEntity;
import com.bms.processing.service.PatientFileService;
import com.bms.processing.service.DicomConfigService;
import com.bms.processing.service.DicomService;
import com.bms.processing.service.DicomRetrieveService;
import com.bms.processing.service.CaseIssueService;
import com.bms.processing.model.CaseIssueStatus;
import com.bms.processing.service.CurrentUserService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Value;

@PageTitle("Errors")
@PermitAll
@Route(value = "errors", layout = MainLayout.class)
public class ErrorsView extends VerticalLayout {

    private final CaseRecordService caseRecordService;
    private final AuditEventService auditEventService;
    private final Grid<CaseRecordEntity> grid = new Grid<>(CaseRecordEntity.class, false);
    private final TextField searchField = new TextField();
    private final PatientFileService patientFileService;
    private final String baseStoragePath;
    private final DicomConfigService dicomConfigService;
    private final DicomService dicomService;
    private final DicomRetrieveService dicomRetrieveService;
    private final CaseIssueService caseIssueService;
    private final CurrentUserService currentUserService;

    public ErrorsView(
            CaseRecordService caseRecordService,
            AuditEventService auditEventService,
            PatientFileService patientFileService,
            @Value("${prism.files.storage-path}") String baseStoragePath,
            DicomConfigService dicomConfigService,
            DicomService dicomService,
            DicomRetrieveService dicomRetrieveService,
            CaseIssueService caseIssueService,
            CurrentUserService currentUserService
    ) {
            this.caseRecordService = caseRecordService;
            this.auditEventService = auditEventService;
            this.patientFileService = patientFileService;
            this.baseStoragePath = baseStoragePath;
            this.dicomConfigService = dicomConfigService;
            this.dicomService = dicomService;
            this.dicomRetrieveService = dicomRetrieveService;
            this.caseIssueService = caseIssueService;
            this.currentUserService = currentUserService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        searchField.setPlaceholder("Search last name, ID, or site");
		searchField.setClearButtonVisible(true);
		searchField.setWidth("420px");
		searchField.addValueChangeListener(event -> refreshErrorsGrid());

        HorizontalLayout header = new HorizontalLayout(MainLayout.pageTitle("Errors"));
        header.setWidthFull();

        configureGrid();
        refreshErrorsGrid();

        grid.addItemClickListener(event ->
                new CaseRecordDialog(
                        event.getItem(),
                        caseRecordService,
                        CaseRecordDialog.Mode.ERRORS,
                        this::refreshErrorsGrid,
                        null,
                        auditEventService,
                        patientFileService,
                        baseStoragePath,
                        dicomConfigService,
                        dicomService,
                        dicomRetrieveService,
                        currentUserService
                ).open()
        );

        add(header, searchField, grid);
        expand(grid);
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.setWidthFull();
        grid.addClassName("workflow-grid");

        grid.addThemeVariants(
                GridVariant.LUMO_ROW_STRIPES
    );


        grid.addColumn(CaseRecordEntity::getPatientLastName)
                .setHeader("Patient Last")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(CaseRecordEntity::getPatientFirstName)
                .setHeader("Patient First")
                .setAutoWidth(true);

        grid.addColumn(CaseRecordEntity::getPatientId)
                .setHeader("Patient ID")
                .setAutoWidth(true);

        grid.addColumn(CaseRecordEntity::getSiteName)
                .setHeader("Site Name")
                .setAutoWidth(true);

        grid.addColumn(record -> nullSafe(record.getFunder()))
                .setHeader("Funder")
                .setAutoWidth(true);

        grid.addComponentColumn(record -> buildStatusChip(formatEnum(record.getPatientStatus())))
        .setHeader("Status")
        .setAutoWidth(true);

        grid.addComponentColumn(record -> {
            Span noteFlag = new Span("!");
            noteFlag.getStyle()
                    .set("font-weight", "700")
                    .set("cursor", "pointer")
                    .set("color", "var(--lumo-error-text-color)");

            boolean hasNotes =
                    (record.getNotes() != null && !record.getNotes().trim().isEmpty()) ||
                    (record.getImekaErrorNote() != null && !record.getImekaErrorNote().trim().isEmpty()) ||
                    (record.getDuramapErrorNote() != null && !record.getDuramapErrorNote().trim().isEmpty()) ||
                    (record.getNeuroreaderErrorNote() != null && !record.getNeuroreaderErrorNote().trim().isEmpty());

            noteFlag.setVisible(hasNotes);
            noteFlag.getElement().setProperty("title", hasNotes ? "View notes" : "");

            noteFlag.getElement().addEventListener("click", e -> openNotesDialog(record))
                    .addEventData("event.stopPropagation()");

            return noteFlag;
        }).setHeader("Notes").setAutoWidth(true);

        grid.addComponentColumn(record -> {

        if (record.getPatientStatus() == PatientStatus.UPCOMING
                || record.getPatientStatus() == PatientStatus.VERIFYING) {

            Span waiting = new Span("Resolve from Upcoming");
            waiting.getStyle()
                    .set("font-size", "0.8rem")
                    .set("font-weight", "600")
                    .set("color", "var(--lumo-secondary-text-color)");

            return waiting;
        }

        if (record.getPatientStatus() == PatientStatus.ON_HOLD
                || record.getPatientStatus() == PatientStatus.MISSING_DATA
                || record.getPatientStatus() == PatientStatus.RESCAN_REQUIRED
                || record.getPatientStatus() == PatientStatus.REPORT_CORRECTION_REQUIRED) {

            Button reopenButton = new Button("Return to Processing");

            reopenButton.addClickListener(event -> {
                try {
                    caseRecordService.returnToProcessing(record);
                    refreshErrorsGrid();
                } catch (InvalidWorkflowTransitionException ex) {
                    showError(ex.getMessage());
                }
            });

            return reopenButton;
        }

        return new Span("-");
    }).setHeader("Action").setAutoWidth(true);
    }

    private void refreshErrorsGrid() {
        String filter = searchField.getValue() == null
                ? ""
                : searchField.getValue().trim().toLowerCase();

        grid.setItems(
                caseIssueService.findByStatusWithCaseRecord(CaseIssueStatus.ACTIVE).stream()
                        .filter(issue -> Boolean.TRUE.equals(issue.getBlocking()))
                        .map(issue -> issue.getCaseRecord())
                        .distinct()
                        .filter(record ->
                                filter.isEmpty()
                                        || containsIgnoreCase(record.getPatientLastName(), filter)
                                        || containsIgnoreCase(record.getPatientFirstName(), filter)
                                        || containsIgnoreCase(record.getPatientId(), filter)
                                        || containsIgnoreCase(record.getSiteName(), filter)
                        )
                        .toList()
        );
    }

    private void openNotesDialog(CaseRecordEntity record) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Case Notes");
        dialog.setWidth("700px");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setWidthFull();

        addNoteSection(layout, "Processing Notes", record.getNotes());
        addNoteSection(layout, "IMEKA Error Notes", record.getImekaErrorNote());
        addNoteSection(layout, "DuraMap Error Notes", record.getDuramapErrorNote());
        addNoteSection(layout, "Neuroreader Error Notes", record.getNeuroreaderErrorNote());

        Button closeButton = new Button("Close", e -> dialog.close());

        dialog.add(layout);
        dialog.getFooter().add(closeButton);
        dialog.open();
    }

    private void addNoteSection(VerticalLayout parent, String title, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }

        H4 header = new H4(title);

        Div body = new Div();
        body.setText(value);
        body.getStyle()
                .set("white-space", "pre-wrap")
                .set("padding", "0.75rem")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "8px")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("width", "100%");

        parent.add(header, body);
    }

    private String formatEnum(Enum<?> value) {
        return value == null ? "" : value.name().replace("_", " ");
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private boolean containsIgnoreCase(String value, String filter) {
        return value != null && value.toLowerCase().contains(filter);
    }

    private Span buildStatusChip(String text) {
        Span chip = new Span(text == null ? "Unknown" : text);
        chip.getStyle()
                .set("display", "inline-block")
                .set("padding", "0.2rem 0.55rem")
                .set("border-radius", "999px")
                .set("font-size", "0.75rem")
                .set("font-weight", "600")
                .set("line-height", "1")
                .set("white-space", "nowrap");

        String value = text == null ? "" : text.toLowerCase();

        if (value.contains("processing")) {
            chip.getStyle().set("background", "#e3f2fd").set("color", "#0d47a1");
        } else if (value.contains("processed with third party errors") || value.contains("third party")) {
            chip.getStyle().set("background", "#fff3e0").set("color", "#e65100");
        } else if (value.contains("processed with errors") || value.contains("error")) {
            chip.getStyle().set("background", "#ffebee").set("color", "#b71c1c");
        } else if (value.contains("processed")) {
            chip.getStyle().set("background", "#e8f5e9").set("color", "#1b5e20");
        } else if (value.contains("completed")) {
            chip.getStyle().set("background", "#ede7f6").set("color", "#4527a0");
        } else if (value.contains("upcoming") || value.contains("verifying")) {
            chip.getStyle().set("background", "#f3e5f5").set("color", "#6a1b9a");
        } else if (value.contains("acquired")) {
            chip.getStyle().set("background", "#e0f7fa").set("color", "#006064");
        } else {
            chip.getStyle().set("background", "#eceff1").set("color", "#37474f");
        }

        return chip;
    }

    private void showError(String message) {
        com.vaadin.flow.component.notification.Notification.show(
                message,
                3500,
                com.vaadin.flow.component.notification.Notification.Position.MIDDLE
        );
    }

}