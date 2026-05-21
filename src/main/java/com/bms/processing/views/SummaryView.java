package com.bms.processing.views;

import com.bms.processing.entity.CaseRecordEntity;
import com.bms.processing.layouts.MainLayout;
import com.bms.processing.model.PatientStatus;
import com.bms.processing.service.CaseRecordService;
import com.bms.processing.components.CaseRecordDialog;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.GroupLayout.Alignment;

@PageTitle("Summary")
@Route(value = "", layout = MainLayout.class)
public class SummaryView extends VerticalLayout {

    private final CaseRecordService caseRecordService;

    private final VerticalLayout dashboardBody = new VerticalLayout();
    private final Select<String> layoutModeSelect = new Select<>();

    private final Select<String> order1Select = new Select<>();
    private final Select<String> order2Select = new Select<>();private final Select<String> order3Select = new Select<>();
    private final Select<String> order4Select = new Select<>();
    private final Select<String> order5Select = new Select<>();

    private final Checkbox showProcessed = new Checkbox("Processed", true);
    private final Checkbox showProcessing = new Checkbox("Processing", true);
    private final Checkbox showErrors = new Checkbox("Errors", true);
    private final Checkbox showUpcoming = new Checkbox("Upcoming", true);
    private final Checkbox showCompleted = new Checkbox("Completed, Last 30 Days", true);

    private final TextField searchField = new TextField();

    private static final String PROCESSED = "Processed";
    private static final String PROCESSING = "Processing";
    private static final String ERRORS = "Errors";
    private static final String UPCOMING = "Upcoming";
    private static final String COMPLETED_30 = "Completed, Last 30 Days";

    private static final String LAYOUT_ROWS = "Rows View";
    private static final String LAYOUT_GRID = "Grid View";

    public SummaryView(CaseRecordService caseRecordService) {
        this.caseRecordService = caseRecordService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("BMS Dashboard");
                title.getStyle()
                        .set("margin-bottom", "0")
                        .set("font-size", "2rem")
                        .set("font-weight", "700")
                        .set("color", "#1e293b");

                Span subtitle = new Span("Operations dashboard for workflow status, active queues, exceptions, and recent completions.");
                subtitle.getStyle()
                        .set("color", "#64748b")
                        .set("margin-top", "0")
                        .set("font-size", "0.98rem");

        Div toolbarCard = buildToolbarCard();

        searchField.setPlaceholder("Search last name, ID, or site");
        searchField.setClearButtonVisible(true);
        searchField.setWidth("420px");
        searchField.addValueChangeListener(event -> rebuildDashboard());

        dashboardBody.setPadding(false);
        dashboardBody.setSpacing(true);
        dashboardBody.setWidthFull();

        add(title, subtitle, searchField, toolbarCard, dashboardBody);
        expand(dashboardBody);

        normalizeSectionOrderSelections();
        rebuildDashboard();
    }

    private Div buildToolbarCard() {
        Div toolbarCard = new Div();
        toolbarCard.setWidthFull();
        toolbarCard.getStyle()
                .set("display", "flex")
                .set("justify-content", "flex-end")
                .set("align-items", "center");

    Button optionsButton = new Button("Options", new Icon(VaadinIcon.COG));
    optionsButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    optionsButton.getElement().setProperty("title", "Dashboard Options");
    optionsButton.getStyle()
        .set("border", "1px solid #dbe3ee")
        .set("border-radius", "999px")
        .set("padding", "0.45rem 0.9rem")
        .set("background", "#ffffff")
        .set("box-shadow", "0 2px 8px rgba(15, 23, 42, 0.05)")
        .set("font-weight", "600")
        .set("color", "#334155");

        optionsButton.addClickListener(event -> {
                List<String> allSections = List.of(PROCESSED, PROCESSING, ERRORS, UPCOMING, COMPLETED_30);

                Select<String> tempLayoutModeSelect = new Select<>();
                tempLayoutModeSelect.setLabel("Layout");
                tempLayoutModeSelect.setItems(LAYOUT_ROWS, LAYOUT_GRID);
                tempLayoutModeSelect.setValue(layoutModeSelect.getValue());
                tempLayoutModeSelect.setWidthFull();

                Select<String> tempOrder1Select = new Select<>();
                tempOrder1Select.setLabel("Slot 1");
                tempOrder1Select.setItems(allSections);
                tempOrder1Select.setValue(order1Select.getValue());
                tempOrder1Select.setWidthFull();

                Select<String> tempOrder2Select = new Select<>();
                tempOrder2Select.setLabel("Slot 2");
                tempOrder2Select.setItems(allSections);
                tempOrder2Select.setValue(order2Select.getValue());
                tempOrder2Select.setWidthFull();

                Select<String> tempOrder3Select = new Select<>();
                tempOrder3Select.setLabel("Slot 3");
                tempOrder3Select.setItems(allSections);
                tempOrder3Select.setValue(order3Select.getValue());
                tempOrder3Select.setWidthFull();

                Select<String> tempOrder4Select = new Select<>();
                tempOrder4Select.setLabel("Slot 4");
                tempOrder4Select.setItems(allSections);
                tempOrder4Select.setValue(order4Select.getValue());
                tempOrder4Select.setWidthFull();

                Select<String> tempOrder5Select = new Select<>();
                tempOrder5Select.setLabel("Slot 5");
                tempOrder5Select.setItems(allSections);
                tempOrder5Select.setValue(order5Select.getValue());
                tempOrder5Select.setWidthFull();

                Checkbox tempShowProcessed = new Checkbox("Processed", showProcessed.getValue());
                Checkbox tempShowProcessing = new Checkbox("Processing", showProcessing.getValue());
                Checkbox tempShowErrors = new Checkbox("Errors", showErrors.getValue());
                Checkbox tempShowUpcoming = new Checkbox("Upcoming", showUpcoming.getValue());
                Checkbox tempShowCompleted = new Checkbox("Completed, Last 30 Days", showCompleted.getValue());

                Span orderLabel = new Span("Section Order");
                orderLabel.getStyle()
                        .set("font-weight", "600")
                        .set("font-size", "0.9rem");

                VerticalLayout orderColumn = new VerticalLayout(
                        orderLabel,
                        tempOrder1Select,
                        tempOrder2Select,
                        tempOrder3Select,
                        tempOrder4Select,
                        tempOrder5Select
                );
                orderColumn.setPadding(false);
                orderColumn.setSpacing(false);

                Span toggleLabel = new Span("Show Sections");
                toggleLabel.getStyle()
                        .set("font-weight", "600")
                        .set("font-size", "0.9rem");

                VerticalLayout toggleColumn = new VerticalLayout(
                        toggleLabel,
                        tempShowProcessed,
                        tempShowProcessing,
                        tempShowErrors,
                        tempShowUpcoming,
                        tempShowCompleted
                );
                toggleColumn.setPadding(false);
                toggleColumn.setSpacing(false);

                Dialog optionsDialog = new Dialog();
                optionsDialog.setHeaderTitle("Dashboard Options");
                optionsDialog.setModal(false);
                optionsDialog.setResizable(false);
                optionsDialog.setDraggable(false);
                optionsDialog.setCloseOnOutsideClick(true);
                optionsDialog.setCloseOnEsc(true);
                optionsDialog.setWidth("420px");

                Button cancelButton = new Button("Cancel");
                cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                cancelButton.addClickListener(e -> optionsDialog.close());

                Button saveButton = new Button("Save");
                saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                saveButton.addClickListener(e -> {
                layoutModeSelect.setValue(tempLayoutModeSelect.getValue());

                order1Select.setValue(tempOrder1Select.getValue());
                order2Select.setValue(tempOrder2Select.getValue());
                order3Select.setValue(tempOrder3Select.getValue());
                order4Select.setValue(tempOrder4Select.getValue());
                order5Select.setValue(tempOrder5Select.getValue());

                showProcessed.setValue(tempShowProcessed.getValue());
                showProcessing.setValue(tempShowProcessing.getValue());
                showErrors.setValue(tempShowErrors.getValue());
                showUpcoming.setValue(tempShowUpcoming.getValue());
                showCompleted.setValue(tempShowCompleted.getValue());

                normalizeSectionOrderSelections();
                rebuildDashboard();
                optionsDialog.close();
                });

                VerticalLayout content = new VerticalLayout(
                        tempLayoutModeSelect,
                        orderColumn,
                        toggleColumn
                );
                content.setPadding(false);
                content.setSpacing(true);
                content.setWidthFull();

                optionsDialog.add(content);
                optionsDialog.getFooter().add(cancelButton, saveButton);
                optionsDialog.open();
        });

        toolbarCard.add(optionsButton);
        return toolbarCard;
        }

    private void rebuildDashboard() {
        dashboardBody.removeAll();

        List<SectionDefinition> sections = getOrderedVisibleSections();

        if (sections.isEmpty()) {
            dashboardBody.add(buildEmptyState());
            return;
        }

        if (LAYOUT_GRID.equals(layoutModeSelect.getValue())) {
            dashboardBody.add(buildGridDashboard(sections));
        } else {
            for (SectionDefinition section : sections) {
                dashboardBody.add(buildSectionCard(section, false));
            }
        }
    }

    private List<String> getSelectedSectionOrder() {
        return List.of(
                order1Select.getValue(),
                order2Select.getValue(),
                order3Select.getValue(),
                order4Select.getValue(),
                order5Select.getValue()
        );
        }

    private void normalizeSectionOrderSelections() {
        List<String> allSections = List.of(PROCESSED, PROCESSING, ERRORS, UPCOMING, COMPLETED_30);
        List<Select<String>> selects = List.of(order1Select, order2Select, order3Select, order4Select, order5Select);

        List<String> used = new ArrayList<>();

        for (Select<String> select : selects) {
                String value = select.getValue();

                if (value == null || used.contains(value)) {
                for (String candidate : allSections) {
                        if (!used.contains(candidate)) {
                        select.setValue(candidate);
                        value = candidate;
                        break;
                        }
                }
                }

                used.add(value);
        }
        }    

    private List<SectionDefinition> getOrderedVisibleSections() {
        List<SectionDefinition> visibleSections = new ArrayList<>();

        if (showProcessed.getValue()) {
                visibleSections.add(new SectionDefinition(PROCESSED, getProcessedRecords(), "processed"));
        }
        if (showProcessing.getValue()) {
                visibleSections.add(new SectionDefinition(PROCESSING, getProcessingRecords(), "processing"));
        }
        if (showErrors.getValue()) {
                visibleSections.add(new SectionDefinition(ERRORS, getErrorRecords(), "errors"));
        }
        if (showUpcoming.getValue()) {
                visibleSections.add(new SectionDefinition(UPCOMING, getUpcomingRecords(), "upcoming"));
        }
        if (showCompleted.getValue()) {
                visibleSections.add(new SectionDefinition(COMPLETED_30, getCompletedLast30Records(), "completed"));
        }

        List<String> desiredOrder = getSelectedSectionOrder();
        List<SectionDefinition> orderedSections = new ArrayList<>();

        for (String title : desiredOrder) {
                for (SectionDefinition section : visibleSections) {
                if (section.title().equals(title) && orderedSections.stream().noneMatch(existing -> existing.title().equals(title))) {
                        orderedSections.add(section);
                }
                }
        }

        for (SectionDefinition section : visibleSections) {
                if (orderedSections.stream().noneMatch(existing -> existing.title().equals(section.title()))) {
                orderedSections.add(section);
                }
        }

        return orderedSections;
        }

    private Component buildGridDashboard(List<SectionDefinition> sections) {
        Div gridLayout = new Div();
        gridLayout.setWidthFull();
        gridLayout.addClassName("summary-grid-layout");
        gridLayout.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(700px, 1fr))")
                .set("gap", "2.5rem")
                .set("align-items", "start");

        for (SectionDefinition section : sections) {
                Component card = buildSectionCard(section, true);
                card.getElement().getStyle()
                        .set("min-width", "0")
                        .set("width", "100%")
                        .set("max-width", "100%");
                gridLayout.add(card);
        }

        return gridLayout;
    }

    private Component buildSectionCard(SectionDefinition section, boolean compactMode) {
        Div card = new Div();
        card.addClassName("summary-card");
        card.addClassName("summary-card-" + section.title().toLowerCase().replace(", last 30 days", "").replace(" ", "-"));
        card.setWidthFull();
        card.getStyle()
                .set("border-radius", "16px")
                .set("background", "#ffffff")
                .set("box-shadow", "0 4px 12px rgba(15, 23, 42, 0.06)")
                .set("border", "1px solid rgba(148, 163, 184, 0.18)")
                .set("padding", "0.7rem 0.75rem 0.65rem 0.75rem")
                .set("border-top", "4px solid " + getSectionColor(section.title()))
                .set("overflow", "hidden")
                .set("position", "relative")
                .set("z-index", "0");

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setSpacing(true);
        header.setPadding(false);
        header.setMargin(false);
        header.getStyle().set("margin-bottom", "0.45rem");

        HorizontalLayout left = new HorizontalLayout();
        left.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        left.setSpacing(true);
        left.setPadding(false);
        left.setMargin(false);

        Icon sectionIcon = getSectionIcon(section.title());
        sectionIcon.setSize("16px");
        sectionIcon.getStyle()
                .set("color", getSectionColor(section.title()))
                .set("flex-shrink", "0");

        Span title = new Span(section.title());
        title.getStyle()
                .set("font-weight", "700")
                .set("font-size", "1rem")
                .set("letter-spacing", "-0.01em")
                .set("color", "#1e293b")
                .set("line-height", "1.1");

        Span countBadge = new Span(String.valueOf(section.records().size()));
        countBadge.getStyle()
                .set("background", getSectionColor(section.title()))
                .set("color", "white")
                .set("border-radius", "999px")
                .set("padding", "3px 10px")
                .set("font-size", "0.74rem")
                .set("font-weight", "700")
                .set("line-height", "1.1")
                .set("min-width", "26px")
                .set("text-align", "center");

        left.add(sectionIcon, title, countBadge);

        Button openPageButton = new Button("Open");
        openPageButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        openPageButton.getStyle()
                .set("font-weight", "600")
                .set("font-size", "0.78rem")
                .set("color", getSectionColor(section.title()))
                .set("padding", "0")
                .set("margin", "0")
                .set("line-height", "1");
        openPageButton.addClickListener(event -> UI.getCurrent().navigate(section.route()));

        header.add(left, openPageButton);

        Grid<CaseRecordEntity> grid = buildGridForSection(section.title(), section.records(), compactMode);

        card.add(header, grid);
        return card;
    }
    
    private Icon getSectionIcon(String section) {
        return switch (section) {
                case PROCESSED -> VaadinIcon.CHECK_CIRCLE_O.create();
                case PROCESSING -> VaadinIcon.CLOCK.create();
                case ERRORS -> VaadinIcon.WARNING.create();
                case UPCOMING -> VaadinIcon.CALENDAR.create();
                case COMPLETED_30 -> VaadinIcon.CHECK.create();
                default -> VaadinIcon.RECORDS.create();
        };
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
        valueBox.setText(value == null || value.isBlank() ? "—" : value);
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

    private void addRowsViewSlotOne(Grid<CaseRecordEntity> grid, String header, com.vaadin.flow.function.ValueProvider<CaseRecordEntity, String> provider) {
        grid.addColumn(provider)
                .setHeader(header)
                .setWidth("150px")
                .setFlexGrow(0)
                .setResizable(false);
    }

    private void addRowsViewSlotTwo(Grid<CaseRecordEntity> grid, String header, com.vaadin.flow.function.ValueProvider<CaseRecordEntity, String> provider) {
        grid.addColumn(provider)
                .setHeader(header)
                .setWidth("150px")
                .setFlexGrow(0)
                .setResizable(false);
    }

    private void addRowsViewBlankSlot(Grid<CaseRecordEntity> grid) {
        grid.addColumn(record -> "")
                .setHeader("")
                .setWidth("150px")
                .setFlexGrow(0)
                .setResizable(false);
    }

    private void addRowsViewThirdPartyColumn(Grid<CaseRecordEntity> grid) {
        grid.addComponentColumn(this::buildThirdPartyStatusStack)
                .setHeader("Third-Party Status")
                .setWidth("230px")
                .setFlexGrow(0)
                .setResizable(false);
    }

    private void addRowsViewNotesColumn(Grid<CaseRecordEntity> grid) {
        grid.addComponentColumn(this::buildNotesIndicator)
                .setHeader("Notes")
                .setWidth("80px")
                .setFlexGrow(0)
                .setResizable(false)
                .setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.CENTER);
    }

    private void addStandardSummaryColumns(Grid<CaseRecordEntity> grid) {
        grid.addColumn(CaseRecordEntity::getPatientLastName)
                .setHeader("Last")
                .setWidth("145px")
                .setFlexGrow(0)
                .setResizable(false);

        grid.addColumn(CaseRecordEntity::getPatientFirstName)
                .setHeader("First")
                .setWidth("145px")
                .setFlexGrow(0)
                .setResizable(false);

        grid.addColumn(CaseRecordEntity::getPatientId)
                .setHeader("Patient ID")
                .setWidth("135px")
                .setFlexGrow(0)
                .setResizable(false);

        grid.addColumn(CaseRecordEntity::getSiteName)
                .setHeader("Site")
                .setWidth("125px")
                .setFlexGrow(0)
                .setResizable(false);
        }
    
    private Grid<CaseRecordEntity> buildGridForSection(String sectionTitle, List<CaseRecordEntity> records, boolean compactMode) {
        Grid<CaseRecordEntity> grid = new Grid<>(CaseRecordEntity.class, false);
        grid.addClassName("summary-grid");
        applyStandardGridStyle(grid, compactMode);
        grid.getElement().getStyle().set("cursor", "pointer");

        boolean rowsView = !compactMode;

        if (rowsView) {
                addRowsViewBaseColumns(grid);
        } else {
                addGridViewBaseColumns(grid);
        }

        switch (sectionTitle) {
                case PROCESSED -> {
                if (rowsView) {
                        addRowsViewSlotOne(grid, "Processed Time", record -> formatDateTimeCompact(record.getProcessedDate()));
                        addRowsViewBlankSlot(grid);
                        addRowsViewThirdPartyColumn(grid);
                        addRowsViewNotesColumn(grid);
                } else {
                        grid.addColumn(record -> formatDateTimeCompact(record.getProcessedDate()))
                                .setHeader("Processed")
                                .setWidth("190px")
                                .setFlexGrow(0)
                                .setResizable(false);

                        grid.addComponentColumn(this::buildNotesIndicator)
                                .setHeader("Notes")
                                .setWidth("80px")
                                .setFlexGrow(0)
                                .setResizable(false)
                                .setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.CENTER);
                }
                }

                case PROCESSING -> {
                if (rowsView) {
                        grid.addComponentColumn(record -> buildStatusChip(formatEnum(record.getPatientStatus())))
                                .setHeader("Status")
                                .setWidth("150px")
                                .setFlexGrow(0)
                                .setResizable(false);
                } else {
                        grid.addComponentColumn(record -> buildStatusChip(formatEnum(record.getPatientStatus())))
                                .setHeader("Status")
                                .setWidth("150px")
                                .setFlexGrow(0)
                                .setResizable(false);

                        grid.addComponentColumn(this::buildNotesIndicator)
                                .setHeader("Notes")
                                .setWidth("80px")
                                .setFlexGrow(0)
                                .setResizable(false)
                                .setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.CENTER);
                }
                }

                case ERRORS -> {
                if (rowsView) {
                        addRowsViewSlotOne(grid, "Acquired Time", record -> formatDate(record.getImagesReceivedDate()));
                        addRowsViewBlankSlot(grid);
                        addRowsViewThirdPartyColumn(grid);
                        addRowsViewNotesColumn(grid);
                } else {
                        grid.addComponentColumn(this::buildNotesIndicator)
                                .setHeader("Notes")
                                .setWidth("80px")
                                .setFlexGrow(0)
                                .setResizable(false)
                                .setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.CENTER);
                }
                }

                case UPCOMING -> {
                if (rowsView) {
                        addRowsViewSlotOne(grid, "Date Scanned", record -> formatDate(record.getDateScanned()));
                        addRowsViewSlotTwo(grid, "Funder", record -> nullSafe(record.getFunder()));

                        grid.addComponentColumn(record -> buildReadOnlyCheckbox(Boolean.TRUE.equals(record.getIntakeSheetDone())))
                                .setHeader("Intake Done")
                                .setWidth("115px")
                                .setFlexGrow(0)
                                .setResizable(false)
                                .setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.CENTER);

                        grid.addComponentColumn(record -> buildReadOnlyCheckbox(Boolean.TRUE.equals(record.getIntakeSheetSent())))
                                .setHeader("Intake Sent")
                                .setWidth("115px")
                                .setFlexGrow(0)
                                .setResizable(false)
                                .setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.CENTER);

                        grid.addColumn(record -> formatEnum(record.getPatientStatus()))
                                .setHeader("Status")
                                .setWidth("160px")
                                .setFlexGrow(0)
                                .setResizable(false);

                        addRowsViewNotesColumn(grid);
                } else {
                        grid.addColumn(record -> formatDate(record.getDateScanned()))
                                .setHeader("Date Scanned")
                                .setWidth("140px")
                                .setFlexGrow(0)
                                .setResizable(false);

                        grid.addComponentColumn(this::buildNotesIndicator)
                                .setHeader("Notes")
                                .setWidth("80px")
                                .setFlexGrow(0)
                                .setResizable(false)
                                .setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.CENTER);
                }
                }

                case COMPLETED_30 -> {
                if (rowsView) {
                        addRowsViewSlotOne(grid, "Acquired Time", record -> formatDate(record.getImagesReceivedDate()));
                        addRowsViewSlotTwo(grid, "Completed Time", record -> formatDateTimeCompact(record.getCompletedDate()));
                        addRowsViewThirdPartyColumn(grid);
                        addRowsViewNotesColumn(grid);
                } else {
                        grid.addColumn(record -> formatDateTimeCompact(record.getCompletedDate()))
                                .setHeader("Completed")
                                .setWidth("190px")
                                .setFlexGrow(0)
                                .setResizable(false);

                        grid.addComponentColumn(this::buildNotesIndicator)
                                .setHeader("Notes")
                                .setWidth("80px")
                                .setFlexGrow(0)
                                .setResizable(false)
                                .setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.CENTER);
                }
                }
        }

        grid.setItems(records);
        grid.addItemClickListener(event ->
                new CaseRecordDialog(event.getItem(), caseRecordService, this::rebuildDashboard).open()
        );
        return grid;
    }

    private Span buildNotesIndicator(CaseRecordEntity record) {
        boolean hasNotes =
                hasText(record.getNotes()) ||
                hasText(record.getImekaErrorNote()) ||
                hasText(record.getDuramapErrorNote()) ||
                hasText(record.getNeuroreaderErrorNote());

        Span badge = new Span(hasNotes ? "!" : "");
        badge.getStyle()
                .set("font-weight", "800")
                .set("color", hasNotes ? "#dc2626" : "transparent")
                .set("display", "inline-flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("width", "24px")
                .set("height", "24px")
                .set("border-radius", "999px")
                .set("font-size", "0.95rem")
                .set("background", hasNotes ? "rgba(220, 38, 38, 0.08)" : "transparent")
                .set("border", hasNotes ? "1px solid rgba(220, 38, 38, 0.18)" : "1px solid transparent");

        return badge;
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

    private String getSectionColor(String section) {
        return switch (section) {
                case PROCESSED -> "#4CAF50";       // green
                case PROCESSING -> "#3B82F6";      // blue
                case ERRORS -> "#EF4444";          // red
                case UPCOMING -> "#8B5CF6";        // purple
                case COMPLETED_30 -> "#14B8A6";    // teal
                default -> "#9CA3AF";              // fallback gray
        };
    }

    private Component buildEmptyState() {
        Div empty = new Div();
        empty.setText("No dashboard sections are selected.");
        empty.getStyle()
                .set("border", "1px dashed var(--lumo-contrast-20pct)")
                .set("border-radius", "14px")
                .set("padding", "1rem")
                .set("color", "var(--lumo-secondary-text-color)");
        return empty;
    }

    private Component buildThirdPartyStatusStack(CaseRecordEntity record) {
        VerticalLayout stack = new VerticalLayout();
        stack.setPadding(false);
        stack.setSpacing(false);
        stack.setMargin(false);
        stack.getStyle()
                .set("gap", "0.2rem")
                .set("font-size", "0.82rem")
                .set("line-height", "1.15");

        if (record.isMinorAtScan()) {
                stack.add(buildThirdPartyLine("DuraMap", record.getDuramapStatus()));
        } else {
                stack.add(buildThirdPartyLine("IMEKA", record.getImekaStatus()));

                if (record.getImekaStatus() == com.bms.processing.model.ThirdPartyStatus.ERROR) {
                stack.add(buildThirdPartyLine("DuraMap", record.getDuramapStatus()));
                }

                stack.add(buildThirdPartyLine("Neuroreader", record.getNeuroreaderStatus()));
        }

        return stack;
    }

    private Component buildThirdPartyLine(String label, Enum<?> status) {
        HorizontalLayout row = new HorizontalLayout();
        row.setPadding(false);
        row.setSpacing(true);
        row.setMargin(false);
        row.setAlignItems(Alignment.CENTER);
        row.getStyle().set("gap", "0.4rem");

        Span labelSpan = new Span(label + ":");
        labelSpan.getStyle()
                .set("font-weight", "600")
                .set("color", "#64748b")
                .set("min-width", "78px");

        Span valueSpan = new Span(formatEnum(status));
        valueSpan.getStyle()
                .set("color", "#1e293b");

        row.add(labelSpan, valueSpan);
        return row;
    }

    private Component buildReadOnlyCheckbox(boolean checked) {
        Checkbox checkbox = new Checkbox();
        checkbox.setValue(checked);
        checkbox.setReadOnly(true);

        checkbox.getStyle()
                .set("pointer-events", "none")
                .set("margin", "0 auto"); // centers horizontally

        return checkbox;
    }

    private List<CaseRecordEntity> getProcessedRecords() {
        return caseRecordService.findAll().stream()
                .filter(record ->
                        record.getPatientStatus() == PatientStatus.PROCESSED
                                || record.getPatientStatus() == PatientStatus.PROCESSED_WITH_ERRORS
                                || record.getPatientStatus() == PatientStatus.PROCESSED_WITH_THIRD_PARTY_ERRORS
                )
                .sorted(Comparator.comparing(
                        CaseRecordEntity::getProcessedDate,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .filter(this::matchesSummaryFilter)
                .toList();
    }

    private List<CaseRecordEntity> getProcessingRecords() {
        return caseRecordService.findAll().stream()
                .filter(record ->
                        record.getPatientStatus() == PatientStatus.ACQUIRED
                                || record.getPatientStatus() == PatientStatus.PROCESSING
                )
                .sorted(Comparator.comparing(
                        CaseRecordEntity::getPatientLastName,
                        Comparator.nullsLast(String::compareToIgnoreCase)
                ))
                .filter(this::matchesSummaryFilter)
                .toList();
    }

    private List<CaseRecordEntity> getErrorRecords() {
        return caseRecordService.findAll().stream()
                .filter(record -> record.getPatientStatus() == PatientStatus.ERROR)
                .sorted(Comparator.comparing(
                        CaseRecordEntity::getPatientLastName,
                        Comparator.nullsLast(String::compareToIgnoreCase)
                ))
                .filter(this::matchesSummaryFilter)
                .toList();
    }

    private List<CaseRecordEntity> getUpcomingRecords() {
        return caseRecordService.findAll().stream()
                .filter(record ->
                        record.getPatientStatus() == PatientStatus.UPCOMING
                                || record.getPatientStatus() == PatientStatus.VERIFYING
                )
                .sorted(Comparator.comparing(
                        CaseRecordEntity::getPatientLastName,
                        Comparator.nullsLast(String::compareToIgnoreCase)
                ))
                .filter(this::matchesSummaryFilter)
                .toList();
    }

    private List<CaseRecordEntity> getCompletedLast30Records() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);

        return caseRecordService.findAll().stream()
                .filter(record -> record.getPatientStatus() == PatientStatus.COMPLETED)
                .filter(record -> record.getCompletedDate() != null && !record.getCompletedDate().isBefore(cutoff))
                .sorted(Comparator.comparing(
                        CaseRecordEntity::getCompletedDate,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .filter(this::matchesSummaryFilter)
                .toList();
    }

    private String getSectionDescription(String sectionTitle) {
        return switch (sectionTitle) {
            case PROCESSED -> "Recently finalized cases, including processed cases with recoverable issues.";
            case PROCESSING -> "Cases actively being worked through acquisition and third-party processing.";
            case ERRORS -> "Hard-stop workflow failures that need review or return to processing.";
            case UPCOMING -> "Scheduled or verifying cases that have not entered active processing yet.";
            case COMPLETED_30 -> "Recently completed cases from the last 30 days for quick operational review.";
            default -> "";
        };
    }
    
    private void addRowsViewBaseColumns(Grid<CaseRecordEntity> grid) {
        grid.addColumn(CaseRecordEntity::getPatientLastName)
                .setHeader("Last")
                .setWidth("150px")
                .setFlexGrow(0)
                .setResizable(false);

        grid.addColumn(CaseRecordEntity::getPatientFirstName)
                .setHeader("First")
                .setWidth("150px")
                .setFlexGrow(0)
                .setResizable(false);

        grid.addColumn(CaseRecordEntity::getPatientId)
                .setHeader("Patient ID")
                .setWidth("140px")
                .setFlexGrow(0)
                .setResizable(false);

        grid.addColumn(CaseRecordEntity::getSiteName)
                .setHeader("Site")
                .setWidth("140px")
                .setFlexGrow(0)
                .setResizable(false);
    }

    private void addGridViewBaseColumns(Grid<CaseRecordEntity> grid) {
        grid.addColumn(CaseRecordEntity::getPatientLastName)
                .setHeader("Last")
                .setWidth("145px")
                .setFlexGrow(0)
                .setResizable(false);

        grid.addColumn(CaseRecordEntity::getPatientFirstName)
                .setHeader("First")
                .setWidth("145px")
                .setFlexGrow(0)
                .setResizable(false);

        grid.addColumn(CaseRecordEntity::getPatientId)
                .setHeader("Patient ID")
                .setWidth("135px")
                .setFlexGrow(0)
                .setResizable(false);

        grid.addColumn(CaseRecordEntity::getSiteName)
                .setHeader("Site")
                .setWidth("125px")
                .setFlexGrow(0)
                .setResizable(false);
    }

    private void applyStandardGridStyle(Grid<CaseRecordEntity> grid, boolean compactMode) {
        grid.setWidthFull();
        grid.addClassName("summary-grid");
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        grid.getStyle()
                .set("font-size", "0.87rem")
                .set("background", "transparent")
                .set("border-radius", "12px")
                .set("overflow", "hidden");

        grid.setAllRowsVisible(true);
        grid.setMinHeight("0");
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean matchesSummaryFilter(CaseRecordEntity record) {
        String filter = searchField.getValue() == null ? "" : searchField.getValue().trim().toLowerCase();

        if (filter.isEmpty()) {
                return true;
        }

        return containsIgnoreCase(record.getPatientLastName(), filter)
                || containsIgnoreCase(record.getPatientId(), filter)
                || containsIgnoreCase(record.getSiteName(), filter);
        }

    private boolean containsIgnoreCase(String value, String filter) {
        return value != null && value.toLowerCase().contains(filter);
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

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.toString().replace("T", " ");
    }

    private String formatDateTimeCompact(LocalDateTime value) {
        if (value == null) {
                return "";
        }

        int month = value.getMonthValue();
        int day = value.getDayOfMonth();
        int year = value.getYear();

        int hour = value.getHour();
        int minute = value.getMinute();

        int displayHour = hour % 12;
        if (displayHour == 0) {
                displayHour = 12;
        }

        String amPm = hour >= 12 ? "PM" : "AM";

        return String.format("%02d-%02d-%04d %02d:%02d %s", month, day, year, displayHour, minute, amPm);
    }

    private record SectionDefinition(String title, List<CaseRecordEntity> records, String route) {
    }
}