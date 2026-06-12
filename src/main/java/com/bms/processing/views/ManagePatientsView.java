package com.bms.processing.views.manage;

import com.bms.processing.components.CaseRecordDialog;
import com.bms.processing.entity.CaseRecordEntity;
import com.bms.processing.layouts.MainLayout;
import com.bms.processing.service.AuditEventService;
import com.bms.processing.service.CaseRecordService;
import com.bms.processing.service.PatientFileService;
import com.bms.processing.service.DicomConfigService;
import com.bms.processing.service.DicomService;
import com.vaadin.flow.component.button.Button;
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


@PageTitle("Manage Patients")
@PermitAll
@Route(value = "manage/patients", layout = MainLayout.class)
public class ManagePatientsView extends VerticalLayout {

    private final CaseRecordService caseRecordService;
    private final AuditEventService auditEventService;

    private final Grid<CaseRecordEntity> grid =
            new Grid<>(CaseRecordEntity.class, false);

    private final TextField searchField = new TextField();

    private final PatientFileService patientFileService;
    private final String baseStoragePath;

    private final DicomConfigService dicomConfigService;
    private final DicomService dicomService;

    public ManagePatientsView(
            CaseRecordService caseRecordService,
            AuditEventService auditEventService,
            PatientFileService patientFileService,
            @Value("${prism.files.storage-path}") String baseStoragePath,
            DicomConfigService dicomConfigService,
            DicomService dicomService
    ) {
        this.caseRecordService = caseRecordService;
        this.auditEventService = auditEventService;
        this.patientFileService = patientFileService;
        this.baseStoragePath = baseStoragePath;
        this.dicomConfigService = dicomConfigService;
        this.dicomService = dicomService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Patient Management");
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "2rem")
                .set("font-weight", "700")
                .set("color", "#1e293b");

        Span subtitle = new Span("View, search, and manage all patients in the system.");
        subtitle.getStyle()
                .set("color", "#64748b")
                .set("font-size", "0.98rem");

        VerticalLayout titleBlock = new VerticalLayout(title, subtitle);
        titleBlock.setPadding(false);
        titleBlock.setSpacing(false);

        Button addPatientButton = new Button("+ Add Patient");
        addPatientButton.getStyle()
                .set("background", "#2563eb")
                .set("color", "#ffffff")
                .set("border-radius", "10px")
                .set("font-weight", "700");

        HorizontalLayout header = new HorizontalLayout(titleBlock, addPatientButton);
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);

        searchField.setPlaceholder("Search by last name, first name, patient ID, or site...");
        searchField.setClearButtonVisible(true);
        searchField.setWidth("520px");
        searchField.addValueChangeListener(event -> refreshGrid());

        configureGrid();
        refreshGrid();

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
                .setHeader("Last")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(CaseRecordEntity::getPatientFirstName)
                .setHeader("First")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(CaseRecordEntity::getPatientId)
                .setHeader("Patient ID")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(CaseRecordEntity::getSiteName)
                .setHeader("Site")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(record ->
                        record.getPatientStatus() != null
                                ? record.getPatientStatus().name().replace("_", " ")
                                : "")
                .setHeader("Status")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(record ->
                        record.getDateScanned() != null
                                ? record.getDateScanned().toString()
                                : "")
                .setHeader("Date Scanned")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addItemClickListener(event ->
                new CaseRecordDialog(
                        event.getItem(),
                        caseRecordService,
                        CaseRecordDialog.Mode.SUMMARY,
                        this::refreshGrid,
                        null,
                        auditEventService,
                        patientFileService,
                        baseStoragePath,
                        dicomConfigService,
                        dicomService
                ).open()
        );
    }

    private void refreshGrid() {
        String filter = searchField.getValue() == null
                ? ""
                : searchField.getValue().trim().toLowerCase();

        grid.setItems(
                caseRecordService.findAll().stream()
                        .filter(record ->
                                filter.isEmpty()
                                        || contains(record.getPatientLastName(), filter)
                                        || contains(record.getPatientFirstName(), filter)
                                        || contains(record.getPatientId(), filter)
                                        || contains(record.getSiteName(), filter)
                                        || contains(
                                                record.getPatientStatus() != null
                                                        ? record.getPatientStatus().name()
                                                        : "",
                                                filter
                                        )
                        )
                        .toList()
        );
    }

    private boolean contains(String value, String filter) {
        return value != null && value.toLowerCase().contains(filter);
    }
}