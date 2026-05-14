package com.bms.processing.views;

import com.bms.processing.layouts.MainLayout;
import com.bms.processing.entity.CaseRecordEntity;
import com.bms.processing.model.PatientStatus;
import com.bms.processing.service.CaseRecordService;
import com.bms.processing.service.InvalidWorkflowTransitionException;
import com.bms.processing.model.ThirdPartyStatus;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import javax.swing.GroupLayout.Alignment;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@PageTitle("Upcoming")
@Route(value = "upcoming", layout = MainLayout.class)
public class UpcomingView extends VerticalLayout {

    private final CaseRecordService caseRecordService;
    private final Grid<CaseRecordEntity> grid = new Grid<>(CaseRecordEntity.class, false);
    private final TextField searchField = new TextField();

    public UpcomingView(CaseRecordService caseRecordService) {
        this.caseRecordService = caseRecordService;
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
        headerRow.setAlignItems(Alignment.CENTER);

        configureGrid();

        refreshUpcomingGrid();
        grid.addItemClickListener(event -> openEditPatientDialog(event.getItem()));

        add(headerRow, subtitle, searchField, grid);
        expand(grid);
    }

    private void configureGrid() {
        grid.setSizeFull();
    
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
    
        grid.addColumn(row -> Boolean.TRUE.equals(row.getIntakeSheetSent()) ? "Yes" : "No")
                .setHeader("Intake Sheet Sent")
                .setAutoWidth(true);        

        grid.addComponentColumn(record -> {
            Button markReceivedButton = new Button("Mark Received");
            markReceivedButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
                
            markReceivedButton.addClickListener(event -> {
                try {
                    caseRecordService.markImagesReceived(record);
                    refreshUpcomingGrid();
                } catch (InvalidWorkflowTransitionException ex) {
                    Span message = new Span(ex.getMessage());
                    add(message);
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

    private List<UpcomingPatientRow> createMockUpcomingPatients() {
        List<UpcomingPatientRow> rows = new ArrayList<>();

        rows.add(new UpcomingPatientRow(
                "Adams",
                "John",
                "BMS-1002",
                "Mayo",
                LocalDate.now().minusDays(2),
                "Medicare",
                true,
                true
        ));

        rows.add(new UpcomingPatientRow(
                "Smith",
                "Claire",
                "BMS-1003",
                "Simmons",
                LocalDate.now().minusDays(1),
                "Private",
                false,
                false
        ));

        return rows;
    }

    public static class UpcomingPatientRow {
        private final String patientLast;
        private final String patientFirst;
        private final String patientId;
        private final String siteName;
        private final LocalDate dateScanned;
        private final String funder;
        private final boolean intakeSheetDone;
        private final boolean intakeSheetSent;

        public UpcomingPatientRow(
                String patientLast,
                String patientFirst,
                String patientId,
                String siteName,
                LocalDate dateScanned,
                String funder,
                boolean intakeSheetDone,
                boolean intakeSheetSent
        ) {
            this.patientLast = patientLast;
            this.patientFirst = patientFirst;
            this.patientId = patientId;
            this.siteName = siteName;
            this.dateScanned = dateScanned;
            this.funder = funder;
            this.intakeSheetDone = intakeSheetDone;
            this.intakeSheetSent = intakeSheetSent;
        }

        public String getPatientLast() {
            return patientLast;
        }

        public String getPatientFirst() {
            return patientFirst;
        }

        public String getPatientId() {
            return patientId;
        }

        public String getSiteName() {
            return siteName;
        }

        public LocalDate getDateScanned() {
            return dateScanned;
        }

        public String getFunder() {
            return funder;
        }

        public boolean isIntakeSheetDone() {
            return intakeSheetDone;
        }

        public boolean isIntakeSheetSent() {
            return intakeSheetSent;
        }
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
    
        TextField siteName = new TextField("Site Name");
        siteName.setValue(record.getSiteName() != null ? record.getSiteName() : "");
    
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
            record.setPatientLastName(lastName.getValue().trim());
            record.setPatientFirstName(firstName.getValue().trim());
            record.setPatientId(patientId.getValue().trim());
            record.setSiteName(siteName.getValue().trim());
            record.setDateOfBirth(dateOfBirth.getValue());
            record.setDateScanned(dateScanned.getValue());
            record.setFunder(funder.getValue().trim());
            record.setIntakeSheetDone(intakeSheetDone.getValue());
            record.setIntakeSheetSent(intakeSheetSent.getValue());
            record.setInvoiceSent(invoiceSent.getValue());
    
            caseRecordService.updateUpcomingCaseDetails(record);
            refreshUpcomingGrid();
            dialog.close();
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
        TextField siteName = new TextField("Site Name");
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
                CaseRecordEntity record = new CaseRecordEntity();
                record.setPatientLastName(lastName.getValue().trim());
                record.setPatientFirstName(firstName.getValue().trim());
                record.setPatientId(patientId.getValue().trim());
                record.setSiteName(siteName.getValue().trim());
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
                Span message = new Span(ex.getMessage());
                add(message);
            }
        });

        dialog.add(formLayout);
        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }
    private boolean containsIgnoreCase(String value, String filter) {
        return value != null && value.toLowerCase().contains(filter);
    }
}