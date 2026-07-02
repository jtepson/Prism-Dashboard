package com.bms.processing.views;

import com.bms.processing.layouts.MainLayout;
import com.bms.processing.entity.CaseRecordEntity;
import com.bms.processing.model.PatientStatus;
import com.bms.processing.service.CaseRecordService;
import com.bms.processing.service.InvalidWorkflowTransitionException;
import com.bms.processing.components.CaseRecordDialog;
import com.bms.processing.components.SiteDialog;
import com.bms.processing.entity.SiteEntity;
import com.bms.processing.service.SiteService;
import com.bms.processing.service.AuditEventService;
import com.bms.processing.model.PatientStatus;
import com.bms.processing.service.PatientFileService;
import com.bms.processing.service.DicomConfigService;
import com.bms.processing.service.DicomService;
import com.bms.processing.service.DicomRetrieveService;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.combobox.ComboBox;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Value;

@PageTitle("Upcoming")
@PermitAll
@Route(value = "upcoming", layout = MainLayout.class)
public class UpcomingView extends VerticalLayout {

    private final SiteService siteService;
    private final CaseRecordService caseRecordService;
    private final AuditEventService auditEventService;
    private final Grid<CaseRecordEntity> grid = new Grid<>(CaseRecordEntity.class, false);
    private final TextField searchField = new TextField();
    private final PatientFileService patientFileService;
    private final String baseStoragePath;
    private final DicomConfigService dicomConfigService;
    private final DicomService dicomService;
    private final DicomRetrieveService dicomRetrieveService;

    public UpcomingView(
                CaseRecordService caseRecordService,
                SiteService siteService,
                AuditEventService auditEventService,
                PatientFileService patientFileService,
                @Value("${prism.files.storage-path}") String baseStoragePath,
                DicomConfigService dicomConfigService,
                DicomService dicomService,
                DicomRetrieveService dicomRetrieveService
    ) {
        this.caseRecordService = caseRecordService;
        this.siteService = siteService;
        this.auditEventService = auditEventService;
        this.patientFileService = patientFileService;
        this.baseStoragePath = baseStoragePath;
        this.dicomConfigService = dicomConfigService;
        this.dicomService = dicomService;
        this.dicomRetrieveService = dicomRetrieveService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Upcoming");
        Span subtitle = new Span("Upcoming patients for the BMS team.");

        searchField.setPlaceholder("Search last name, ID, or site");
		searchField.setClearButtonVisible(true);
		searchField.setWidth("420px");
		searchField.addValueChangeListener(event -> refreshUpcomingGrid());

        Button addPatientButton = new Button("Add Patient");
        addPatientButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        addPatientButton.addClickListener(event -> openAddPatientDialog());

        HorizontalLayout headerRow = new HorizontalLayout(title, addPatientButton);
        headerRow.setWidthFull();
        headerRow.setJustifyContentMode(JustifyContentMode.BETWEEN);
        headerRow.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);

        configureGrid();
        refreshUpcomingGrid();

        grid.addItemClickListener(event ->
                new CaseRecordDialog(
                        event.getItem(),
                        caseRecordService,
                        CaseRecordDialog.Mode.UPCOMING,
                        this::refreshUpcomingGrid,
                        siteService,
                        auditEventService,
                        patientFileService,
                        baseStoragePath,
                        dicomConfigService,
                        dicomService,
                        dicomRetrieveService
                ).open()
        );

        add(headerRow, subtitle, searchField, grid);
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
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(CaseRecordEntity::getPatientFirstName)
                .setHeader("Patient First")
                .setSortable(true)
                .setAutoWidth(true);
    
        grid.addColumn(CaseRecordEntity::getPatientId)
                .setHeader("Patient ID")
                .setAutoWidth(true);
    
        grid.addColumn(CaseRecordEntity::getSiteName)
                .setHeader("Site Name")
                .setAutoWidth(true);
    
        grid.addColumn(row -> row.getDateScanned() != null ? row.getDateScanned().toString() : "")
                .setHeader("Date Scanned")
                .setAutoWidth(true);
    
        grid.addColumn(row -> row.getFunder() != null ? row.getFunder() : "")
                .setHeader("Funder")
                .setAutoWidth(true);
    
        grid.addColumn(row -> Boolean.TRUE.equals(row.getIntakeSheetDone()) ? "Yes" : "No")
                .setHeader("Intake Sheet Done")
                .setAutoWidth(true);
    
        grid.addComponentColumn(record -> {
            Button markReceivedButton = new Button("Mark Received");
            markReceivedButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
                
            //Updated listener, audit logging will now be found in caserecordservice 6012026
            markReceivedButton.addClickListener(event -> {
                try {

                        PatientStatus oldStatus = record.getPatientStatus();

                        caseRecordService.markImagesReceived(record);
                        refreshUpcomingGrid();

                } catch (InvalidWorkflowTransitionException ex) {
                        showError(ex.getMessage());
                }
            });
                
            return markReceivedButton;
        }).setHeader("").setAutoWidth(true);     
    }

    private void refreshUpcomingGrid() {
        String filter = searchField.getValue() == null ? "" : searchField.getValue().trim().toLowerCase();
    
        grid.setItems(
                caseRecordService.findAll().stream()
                        .filter(record ->
                                record.getPatientStatus() == PatientStatus.UPCOMING ||
                                record.getPatientStatus() == PatientStatus.VERIFYING)
                        .filter(record -> filter.isEmpty()
                                || containsIgnoreCase(record.getPatientLastName(), filter)
                                || containsIgnoreCase(record.getPatientId(), filter)
                                || containsIgnoreCase(record.getSiteName(), filter))
                        .toList()
        );
    }

    private void openEditPatientDialog(CaseRecordEntity record) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Patient");
        dialog.setWidth("900px");
    
        TextField lastName = new TextField("Patient Last");
        lastName.setValue(record.getPatientLastName() != null ? record.getPatientLastName() : "");
    
        TextField firstName = new TextField("Patient First");
        firstName.setValue(record.getPatientFirstName() != null ? record.getPatientFirstName() : "");
    
        TextField patientId = new TextField("Patient ID");
        patientId.setValue(record.getPatientId() != null ? record.getPatientId() : "");
    
        ComboBox<SiteEntity> siteName = new ComboBox<>("Site Name");
        siteName.setItems(siteService.getAllSites());
        siteName.setItemLabelGenerator(SiteEntity::getFacilityName);
        siteName.setWidthFull();
    
        TextField funder = new TextField("Funder");
        funder.setValue(record.getFunder() != null ? record.getFunder() : "");
    
        DatePicker dateOfBirth = new DatePicker("Date of Birth");
        dateOfBirth.setValue(record.getDateOfBirth());
    
        DatePicker dateScanned = new DatePicker("Date Scanned");
        dateScanned.setValue(record.getDateScanned());
    
        Checkbox intakeSheetDone = new Checkbox("Intake Sheet Done");
        intakeSheetDone.setValue(Boolean.TRUE.equals(record.getIntakeSheetDone()));
    
        Checkbox intakeSheetSent = new Checkbox("Intake Sheet Sent");
        intakeSheetSent.setValue(Boolean.TRUE.equals(record.getIntakeSheetSent()));
    
        Checkbox invoiceSent = new Checkbox("Invoice Sent");
        invoiceSent.setValue(Boolean.TRUE.equals(record.getInvoiceSent()));
    
        FormLayout formLayout = new FormLayout();
        formLayout.setWidthFull();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );
    
        formLayout.add(
                lastName, firstName,
                patientId, siteName,
                dateOfBirth, dateScanned,
                funder, intakeSheetDone,
                intakeSheetSent, invoiceSent
        );
    
        Button saveButton = new Button("Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    
        Button cancelButton = new Button("Cancel", e -> dialog.close());
    
        saveButton.addClickListener(event -> {

            if (lastName.getValue().trim().isEmpty()) {
                showError("Last name is required.");
                return;
            }

            record.setPatientLastName(lastName.getValue().trim());
            record.setPatientFirstName(firstName.getValue().trim());
            record.setPatientId(patientId.getValue().trim());
            record.setSiteName(
                    siteName.getValue() != null
                            ? siteName.getValue().getFacilityName()
                            : ""
            );
            record.setDateOfBirth(dateOfBirth.getValue());
            record.setDateScanned(dateScanned.getValue());
            record.setFunder(funder.getValue().trim());
            record.setIntakeSheetDone(intakeSheetDone.getValue());
            record.setIntakeSheetSent(intakeSheetSent.getValue());
            record.setInvoiceSent(invoiceSent.getValue());
    
            try {
                caseRecordService.updateUpcomingCaseDetails(record);
                refreshUpcomingGrid();
                dialog.close();
            } catch (InvalidWorkflowTransitionException ex) {
                showError(ex.getMessage());
            }
        });
    
        dialog.add(formLayout);
        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }


    private void openAddPatientDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add Patient");
        dialog.setWidth("900px");

        TextField lastName = new TextField("Patient Last");
        TextField firstName = new TextField("Patient First");
        TextField patientId = new TextField("Patient ID");
        
        ComboBox<SiteEntity> siteName = new ComboBox<>("Site Name");
        siteName.setItems(siteService.getAllSites());
        siteName.setItemLabelGenerator(site ->
                site.getId() == null ? "+ Add new site" : site.getFacilityName()
        );
        siteName.setWidthFull();

        SiteEntity addNewSiteOption = new SiteEntity();
        addNewSiteOption.setFacilityName("+ Add new site");

        siteName.setItems(
                java.util.stream.Stream.concat(
                        siteService.getAllSites().stream(),
                        java.util.stream.Stream.of(addNewSiteOption)
                ).toList()
        );

        siteName.addValueChangeListener(event -> {
            SiteEntity selected = event.getValue();
            if (selected != null && selected.getId() == null) {
                siteName.clear();

                new SiteDialog(
                        siteService,
                        newSite -> {
                            siteName.setItems(
                                    java.util.stream.Stream.concat(
                                            siteService.getAllSites().stream(),
                                            java.util.stream.Stream.of(addNewSiteOption)
                                    ).toList()
                            );
                            siteName.setValue(newSite);
                        }
                ).open();
            }
        });

        TextField funder = new TextField("Funder");

        DatePicker dateOfBirth = new DatePicker("Date of Birth");
        DatePicker dateScanned = new DatePicker("Date Scanned");

        Checkbox intakeSheetDone = new Checkbox("Intake Sheet Done");
        Checkbox intakeSheetSent = new Checkbox("Intake Sheet Sent");
        Checkbox invoiceSent = new Checkbox("Invoice Sent");

        FormLayout formLayout = new FormLayout();
        formLayout.setWidthFull();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );

        formLayout.add(
                lastName, firstName,
                patientId, siteName,
                dateOfBirth, dateScanned,
                funder, intakeSheetDone,
                intakeSheetSent, invoiceSent
        );

        Button saveButton = new Button("Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", e -> dialog.close());

        saveButton.addClickListener(event -> {
            try {
                if (lastName.getValue().trim().isEmpty()) {
                    showError("Last name is required.");
                    return;
                }

                CaseRecordEntity record = new CaseRecordEntity();
                record.setPatientLastName(lastName.getValue().trim());
                record.setPatientFirstName(firstName.getValue().trim());
                record.setPatientId(patientId.getValue().trim());
                record.setSiteName(
                        siteName.getValue() != null
                                ? siteName.getValue().getFacilityName()
                                : ""
                );
                record.setOwnerGroup("UNASSIGNED");
                record.setDateOfBirth(dateOfBirth.getValue());
                record.setDateScanned(dateScanned.getValue());
                record.setFunder(funder.getValue().trim());
                record.setIntakeSheetDone(intakeSheetDone.getValue());
                record.setIntakeSheetSent(intakeSheetSent.getValue());
                record.setInvoiceSent(invoiceSent.getValue());
                record.setPatientStatus(PatientStatus.UPCOMING);

                caseRecordService.createUpcomingCase(record);

                refreshUpcomingGrid();
                dialog.close();
            } catch (InvalidWorkflowTransitionException ex) {
                showError(ex.getMessage());
            }
        });

        dialog.add(formLayout);
        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    private void showError(String message) {
        com.vaadin.flow.component.notification.Notification.show(
                message,
                3500,
                com.vaadin.flow.component.notification.Notification.Position.MIDDLE
        );
    }

    private boolean containsIgnoreCase(String value, String filter) {
        return value != null && value.toLowerCase().contains(filter);
    }
}