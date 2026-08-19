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
import com.bms.processing.service.CurrentUserService;
import com.bms.processing.components.CaseIssueDialog;
import com.bms.processing.entity.CaseIssueEntity;
import com.bms.processing.service.CaseIssueService;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Value;

@PageTitle("Processed")
@PermitAll
@Route(value = "processed", layout = MainLayout.class)
public class ProcessedView extends VerticalLayout {

    private final CaseRecordService caseRecordService;
    private final AuditEventService auditEventService;
    private final Grid<CaseRecordEntity> grid = new Grid<>(CaseRecordEntity.class, false);
    private final TextField searchField = new TextField();
    private final PatientFileService patientFileService;
    private final String baseStoragePath;
    private final DicomConfigService dicomConfigService;
    private final DicomService dicomService;
    private final DicomRetrieveService dicomRetrieveService;
    private final CurrentUserService currentUserService;
    private final CaseIssueService caseIssueService;

    public ProcessedView(
            CaseRecordService caseRecordService,
            AuditEventService auditEventService,
            PatientFileService patientFileService,
            @Value("${prism.files.storage-path}") String baseStoragePath,
            DicomConfigService dicomConfigService,
            DicomService dicomService,
            DicomRetrieveService dicomRetrieveService,
            CurrentUserService currentUserService,
            CaseIssueService caseIssueService
    ) {
            this.caseRecordService = caseRecordService;
            this.auditEventService = auditEventService;
            this.patientFileService = patientFileService;
            this.baseStoragePath = baseStoragePath;
            this.dicomConfigService = dicomConfigService;
            this.dicomService = dicomService;
            this.dicomRetrieveService = dicomRetrieveService;
            this.currentUserService = currentUserService;
            this.caseIssueService = caseIssueService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        searchField.setPlaceholder("Search last name, ID, or site");
		searchField.setClearButtonVisible(true);
		searchField.setWidth("420px");
		searchField.addValueChangeListener(event -> refreshProcessedGrid());

        HorizontalLayout header = new HorizontalLayout(MainLayout.pageTitle("Processed"));
        header.setWidthFull();

        configureGrid();
        refreshProcessedGrid();
        
        grid.addItemClickListener(event ->
                new CaseRecordDialog(
                        event.getItem(),
                        caseRecordService,
                        CaseRecordDialog.Mode.PROCESSED,
                        this::refreshProcessedGrid,
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

        grid.addColumn(record -> formatDateTime(record.getProcessedDate()))
                .setHeader("Processed Date")
                .setAutoWidth(true);

        grid.addComponentColumn(record -> {
            var activeIssues = caseIssueService.findActiveByCaseRecord(record);

            if (activeIssues.isEmpty()) {
                return new Span("");
            }

            long blockingCount = activeIssues.stream()
                    .filter(issue -> Boolean.TRUE.equals(issue.getBlocking()))
                    .count();

            Button issueButton = new Button(
                    activeIssues.size() == 1
                            ? "Issue"
                            : "Issues (" + activeIssues.size() + ")"
            );

            issueButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

            if (blockingCount > 0) {
                issueButton.getStyle()
                        .set("color", "var(--lumo-error-text-color)");
            }

            issueButton.addClickListener(event -> {
                CaseIssueEntity issue = activeIssues.get(0);

                new CaseIssueDialog(
                        record,
                        issue,
                        caseIssueService,
                        currentUserService,
                        this::refreshProcessedGrid
                ).open();
            });

            return issueButton;
        })
        .setHeader("Issues")
        .setAutoWidth(true);

        // reworked this to account for centralized tracking of invoice data - updated 08182026
        grid.addComponentColumn(record -> {
            DatePicker invoiceSentDate = new DatePicker();
            invoiceSentDate.setValue(record.getInvoiceSentDate());
            invoiceSentDate.setWidth("150px");

            boolean canEditInvoice =
                    currentUserService.isAdmin()
                            || currentUserService.isBms();

            invoiceSentDate.setReadOnly(!canEditInvoice);

            if (canEditInvoice) {
                invoiceSentDate.addValueChangeListener(event -> {
                    try {
                        caseRecordService.updateInvoiceSentDate(
                                record,
                                event.getValue(),
                                currentUserService.getUsername()
                        );

                        refreshProcessedGrid();

                    } catch (InvalidWorkflowTransitionException ex) {
                        Span message = new Span(ex.getMessage());
                        add(message);
                    }
                });
            }

            return invoiceSentDate;
        })
        .setHeader("Invoice Sent Date")
        .setAutoWidth(true);

        grid.addComponentColumn(record -> {
            Button completeButton = new Button("Mark Completed");
            completeButton.addThemeVariants(
                    ButtonVariant.LUMO_SMALL,
                    ButtonVariant.LUMO_PRIMARY,
                    ButtonVariant.LUMO_SUCCESS
            );

            completeButton.addClickListener(event -> {
                try {
                    caseRecordService.markCompleted(record);
                    refreshProcessedGrid();
                } catch (InvalidWorkflowTransitionException ex) {
                    showError(ex.getMessage());
                }
            });

            return completeButton;
        }).setHeader("Action").setAutoWidth(true);
    }

    private void refreshProcessedGrid() {
        String filter = searchField.getValue() == null
                ? ""
                : searchField.getValue().trim().toLowerCase();

        grid.setItems(
                caseRecordService.findAll().stream()
                        .filter(record -> record.getPatientStatus() == PatientStatus.PROCESSED)
                        .filter(record -> filter.isEmpty()
                                || containsIgnoreCase(record.getPatientLastName(), filter)
                                || containsIgnoreCase(record.getPatientFirstName(), filter)
                                || containsIgnoreCase(record.getPatientId(), filter)
                                || containsIgnoreCase(record.getSiteName(), filter)
                                || containsIgnoreCase(record.getFunder(), filter))
                        .toList()
        );
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

    private String formatEnum(Enum<?> value) {
        return value == null ? "" : value.name().replace("_", " ");
    }

    //changing date format to yyyy-mm-dd - updated 7022026
    private String formatDateTime(java.time.LocalDateTime value) {
        return value == null
                ? ""
                : value.toLocalDate().toString();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private boolean containsIgnoreCase(String value, String filter) {
        return value != null && value.toLowerCase().contains(filter);
    }

    private void showError(String message) {
        com.vaadin.flow.component.notification.Notification notification =
                com.vaadin.flow.component.notification.Notification.show(
                        message,
                        3500,
                        com.vaadin.flow.component.notification.Notification.Position.MIDDLE
                );

        notification.addThemeVariants(
                com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR
        );
    }
}
