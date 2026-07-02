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
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;

@PageTitle("Completed")
@PermitAll
@Route(value = "completed", layout = MainLayout.class)
public class CompletedView extends VerticalLayout {

    private final CaseRecordService caseRecordService;
    private final AuditEventService auditEventService;
    private final Grid<CaseRecordEntity> grid = new Grid<>(CaseRecordEntity.class, false);
    private final TextField searchField = new TextField();
    private final PatientFileService patientFileService;
    private final String baseStoragePath;
    private final DicomConfigService dicomConfigService;
    private final DicomService dicomService;
    private final DicomRetrieveService dicomRetrieveService;

    public CompletedView(
            CaseRecordService caseRecordService,
            AuditEventService auditEventService,
            PatientFileService patientFileService,
            @Value("${prism.files.storage-path}") String baseStoragePath,
            DicomConfigService dicomConfigService,
            DicomService dicomService,
            DicomRetrieveService dicomRetrieveService
    ) {
            this.caseRecordService = caseRecordService;
            this.auditEventService = auditEventService;
            this.patientFileService = patientFileService;
            this.baseStoragePath = baseStoragePath;
            this.dicomConfigService = dicomConfigService;
            this.dicomService = dicomService;
            this.dicomRetrieveService = dicomRetrieveService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Completed");
        Span subtitle = new Span("BMS-completed cases.");

        searchField.setPlaceholder("Search last name, ID, or site");
		searchField.setClearButtonVisible(true);
		searchField.setWidth("420px");
		searchField.addValueChangeListener(event -> refreshCompletedGrid());

        HorizontalLayout header = new HorizontalLayout(title);
        header.setWidthFull();

        configureGrid();
        refreshCompletedGrid();

        grid.addItemClickListener(event ->
                new CaseRecordDialog(
                        event.getItem(),
                        caseRecordService,
                        CaseRecordDialog.Mode.COMPLETED,
                        this::refreshCompletedGrid,
                        null,
                        auditEventService,
                        patientFileService,
                        baseStoragePath,
                        dicomConfigService,
                        dicomService,
                        dicomRetrieveService
                ).open()
        );

        add(header, subtitle, searchField, grid);
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

        grid.addColumn(record -> formatDateTime(record.getProcessedDate()))
                .setHeader("Processed Date")
                .setAutoWidth(true);

        grid.addColumn(record -> formatDateTime(record.getCompletedDate()))
                .setHeader("Completed Date")
                .setAutoWidth(true);

        grid.addComponentColumn(record -> buildStatusChip(formatEnum(record.getPatientStatus())))
                .setHeader("Status")
                .setAutoWidth(true);

        grid.addComponentColumn(record -> {
            Checkbox invoiceSent = new Checkbox();
            invoiceSent.setValue(Boolean.TRUE.equals(record.getInvoiceSent()));

            invoiceSent.addValueChangeListener(event -> {
                try {
                    caseRecordService.updateInvoiceSent(record, event.getValue());
                    refreshCompletedGrid();
                } catch (InvalidWorkflowTransitionException ex) {
                    Span message = new Span(ex.getMessage());
                    add(message);
                }
            });

            return invoiceSent;
        }).setHeader("Invoice Sent").setAutoWidth(true);

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
    }

    private void refreshCompletedGrid() {
        grid.setItems(
                caseRecordService.findAll().stream()
                        .filter(record -> record.getPatientStatus() == PatientStatus.COMPLETED)
                        .toList()
        );
    }

    private void openNotesDialog(CaseRecordEntity record) {
        com.vaadin.flow.component.dialog.Dialog dialog = new com.vaadin.flow.component.dialog.Dialog();
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

        com.vaadin.flow.component.button.Button closeButton =
                new com.vaadin.flow.component.button.Button("Close", e -> dialog.close());

        dialog.add(layout);
        dialog.getFooter().add(closeButton);
        dialog.open();
    }

    private void addNoteSection(VerticalLayout parent, String title, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }

        com.vaadin.flow.component.html.H4 header = new com.vaadin.flow.component.html.H4(title);
        com.vaadin.flow.component.html.Div body = new com.vaadin.flow.component.html.Div();
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

    //changing date format to yyyy-mm-dd - updated 7022026
    private String formatDateTime(LocalDateTime value) {
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

        if (value.contains("completed")) {
            chip.getStyle().set("background", "#ede7f6").set("color", "#4527a0");
        } else if (value.contains("processed")) {
            chip.getStyle().set("background", "#e8f5e9").set("color", "#1b5e20");
        } else {
            chip.getStyle().set("background", "#eceff1").set("color", "#37474f");
        }

        return chip;
    }

    private String formatEnum(Enum<?> value) {
        return value == null ? "" : value.name().replace("_", " ");
    }
}