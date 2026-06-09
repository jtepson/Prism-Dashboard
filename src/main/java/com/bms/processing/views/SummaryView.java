package com.bms.processing.views;

import com.bms.processing.entity.CaseRecordEntity;
import com.bms.processing.layouts.MainLayout;
import com.bms.processing.model.PatientStatus;
import com.bms.processing.service.CaseRecordService;
import com.bms.processing.components.CaseRecordDialog;
import com.bms.processing.components.DashboardMetricCard;
import com.bms.processing.components.DashboardWidget;
import com.bms.processing.components.PatientQuickView;
import com.bms.processing.service.SiteService;
import com.bms.processing.service.AuditEventService;
import com.bms.processing.entity.AuditEventEntity;
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
import jakarta.annotation.security.PermitAll;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;

import com.vaadin.flow.component.notification.Notification;

@PageTitle("Summary")
@PermitAll
@Route(value = "", layout = MainLayout.class)
public class SummaryView extends VerticalLayout {

    private final CaseRecordService caseRecordService;
    private final SiteService siteService;

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

    //addding for dashboard alignment
    private static final String DASHBOARD_MAX_WIDTH = "1480px";
    private static final String DASHBOARD_GRID_COLUMNS = "repeat(12, minmax(0, 1fr))";

    private final Div quickViewPanel = new Div();
    private final Div quickViewBackdrop = new Div();

    private final Div dashboardGridContainer = new Div();

    private CaseRecordEntity selectedRecord;

    //Updated options menu selections for dash grid 0527
    private final Checkbox showNeedsAttention = new Checkbox("Needs Attention", true);
    private final Checkbox showProcessedWidget = new Checkbox("Processed", true);
    private final Checkbox showRecentActivity = new Checkbox("Recent Activity", true);
    private final Checkbox showProcessingQueue = new Checkbox("Processing", true);
    private final Checkbox showUpcomingWidget = new Checkbox("Upcoming", true);
    private final Checkbox showCompletedWidget = new Checkbox("Completed (30 Days)", true);
    private final Checkbox showErrorsWidget = new Checkbox("Errors", true);

    //Presets for configurable widget placement
    private final Select<String> needsAttentionColumn = new Select<>();
    private final Select<String> processedColumn = new Select<>();
    private final Select<String> recentActivityColumn = new Select<>();
    private final Select<String> processingQueueColumn = new Select<>();
    private final Select<String> upcomingColumn = new Select<>();
    private final Select<String> completedColumn = new Select<>();
    private final Select<String> errorsColumn = new Select<>();

    //Audit logging
    private final AuditEventService auditEventService;

    public SummaryView(
                CaseRecordService caseRecordService,
                SiteService siteService,
                AuditEventService auditEventService
    ) {
        this.caseRecordService = caseRecordService;
        this.siteService = siteService;
        this.auditEventService = auditEventService;

        setSizeFull();
        setPadding(false);
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

        searchField.setPlaceholder("Search last name, ID, or site");
        searchField.setClearButtonVisible(true);
        searchField.setWidth("420px");
        searchField.addValueChangeListener(event -> refreshDashboardGrid());

        dashboardBody.setPadding(false);
        dashboardBody.setSpacing(true);
        dashboardBody.setWidthFull();

        //drawer styling
        quickViewPanel.setWidth("380px");
        quickViewPanel.getStyle()
                .set("position", "fixed")
                .set("top", "0")
                .set("right", "0")
                .set("height", "100vh")
                .set("width", "390px")
                .set("z-index", "1000")
                .set("background", "#ffffff")
                .set("border-left", "1px solid #dbe3ee")
                .set("box-shadow", "-8px 0 24px rgba(15, 23, 42, 0.18)")
                .set("padding", "1rem")
                .set("display", "none")
                .set("overflow-y", "auto")
                .set("transform", "translateX(100%)")
                .set("transition", "transform 0.2s ease-in-out");

        //background behind drawer
        quickViewBackdrop.getStyle()
                .set("position", "fixed")
                .set("top", "0")
                .set("left", "0")
                .set("width", "100vw")
                .set("height", "100vh")
                .set("z-index", "999")
                .set("background", "rgba(15, 23, 42, 0.18)")
                .set("display", "none")
                .set("opacity", "1")
                .set("transition", "opacity 0.2s ease-in-out");

        quickViewBackdrop.addClickListener(event -> hideQuickView());

        add(
                buildDashboardPage(title, subtitle),
                quickViewBackdrop,
                quickViewPanel
        );

    }

    private Component buildFixedDashboardLayout() {
        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setWidthFull();
        wrapper.setPadding(false);
        wrapper.setSpacing(true);

        Div topGrid = new Div();
        topGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", DASHBOARD_GRID_COLUMNS)
                .set("gap", "1rem")
                .set("width", "100%");

        Component needsAttention = new DashboardWidget("Needs Attention", buildNeedsAttentionWidget());
        Component processing = new DashboardWidget("Processing", buildProcessingQueueWidget());
        Component recentActivity = new DashboardWidget("Recent Activity", buildRecentActivityWidget());

        needsAttention.getElement().getStyle().set("grid-column", "span 4");
        processing.getElement().getStyle().set("grid-column", "span 4");
        recentActivity.getElement().getStyle().set("grid-column", "span 4");

        topGrid.add(needsAttention, processing, recentActivity);

        Div separator = new Div();
        separator.getStyle()
                .set("height", "1px")
                .set("background", "#dbe3ee")
                .set("margin", "0.5rem 0 0.25rem 0")
                .set("width", "100%");

        VerticalLayout lowerStack = new VerticalLayout();
        lowerStack.setWidthFull();
        lowerStack.setPadding(false);
        lowerStack.setSpacing(true);

        lowerStack.add(
                new DashboardWidget("Upcoming", buildUpcomingWidget()),
                new DashboardWidget("Errors", buildErrorsWidget()),
                new DashboardWidget("Processed", buildProcessedWidget()),
                new DashboardWidget("Completed (Last 30 Days)", buildCompletedWidget())
        );

        getStyle()
                .set("overflow-x", "auto")
                .set("background", "#f5f7fb");

        wrapper.add(topGrid, separator, lowerStack);
        return wrapper;
    }

    //adding this in order to connect widgets and metrics together for alignment - updated 6092026
    private Component buildDashboardPage(H2 title, Span subtitle) {
        VerticalLayout shell = new VerticalLayout();
        shell.setPadding(true);
        shell.setSpacing(true);
        shell.setWidthFull();

        shell.getStyle()
                .set("padding", "1.5rem")
                .set("max-width", DASHBOARD_MAX_WIDTH)
                .set("min-width", "980px")
                .set("margin", "0")
                .set("box-sizing", "border-box");

        shell.add(
                buildDashboardHeader(title, subtitle),
                buildMetricSection(),
                buildFixedDashboardLayout()
        );

        return shell;
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
                new CaseRecordDialog(
                        event.getItem(),
                        caseRecordService,
                        CaseRecordDialog.Mode.SUMMARY,
                        this::rebuildDashboard,
                        null,
                        auditEventService
                ).open()
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

        grid.getStyle()
                .set("--lumo-primary-color-10pct", "rgba(37, 99, 235, 0.08)");
        grid.getElement().getStyle().set("cursor", "pointer");
        grid.addClassName("interactive-grid");
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

    //refining widget trackers here, at first they were too thin so trying to tweak it to get it uniform to widgets below 6092025
    private Component buildMetricSection() {
        Div metrics = new Div();
        metrics.setWidthFull();

        metrics.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", DASHBOARD_GRID_COLUMNS)
                .set("gap", "1rem")
                .set("width", "100%")
                .set("margin-bottom", "1rem");

        Component upcoming = new DashboardMetricCard(
                "Upcoming",
                getUpcomingRecords().size(),
                "Awaiting intake",
                "#7c3aed",
                VaadinIcon.CALENDAR
        );

        Component processing = new DashboardMetricCard(
                "Processing",
                getProcessingRecords().size(),
                "In progress",
                "#2563eb",
                VaadinIcon.REFRESH
        );

        Component errors = new DashboardMetricCard(
                "Errors",
                getErrorRecords().size(),
                "Needs attention",
                "#dc2626",
                VaadinIcon.WARNING
        );

        Component completed = new DashboardMetricCard(
                "Completed",
                getCompletedLast30Records().size(),
                "Last 30 days",
                "#16a34a",
                VaadinIcon.CHECK_CIRCLE
        );

        upcoming.getElement().getStyle().set("grid-column", "span 3");
        processing.getElement().getStyle().set("grid-column", "span 3");
        errors.getElement().getStyle().set("grid-column", "span 3");
        completed.getElement().getStyle().set("grid-column", "span 3");

        metrics.add(upcoming, processing, errors, completed);

        return metrics;
    }

    private HorizontalLayout buildDashboardHeader(H2 title, Span subtitle) {
        VerticalLayout titleBlock = new VerticalLayout(title, subtitle);
        titleBlock.setPadding(false);
        titleBlock.setSpacing(false);

        HorizontalLayout header = new HorizontalLayout(titleBlock);
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);

        return header;
    }

    //Here is the initial layout grid builder for the dash, updated for option functionality 0527
    private Component buildDashboardGrid() {
        
        VerticalLayout leftColumn = new VerticalLayout();
        VerticalLayout rightColumn = new VerticalLayout();

        leftColumn.setPadding(false);
        rightColumn.setPadding(false);
        
        addWidgetToColumn(
                leftColumn,
                rightColumn,
                showProcessedWidget,
                processedColumn,
                new DashboardWidget(
                        "Processed",
                        buildProcessedWidget()
                )
        );

        addWidgetToColumn(
                leftColumn,
                rightColumn,
                showUpcomingWidget,
                upcomingColumn,
                new DashboardWidget(
                        "Upcoming",
                        buildUpcomingWidget()
                )
        );

        addWidgetToColumn(
                leftColumn,
                rightColumn,
                showCompletedWidget,
                completedColumn,
                new DashboardWidget(
                        "Completed (30 Days)",
                        buildCompletedWidget()
                )
        );

        addWidgetToColumn(
                leftColumn,
                rightColumn,
                showErrorsWidget,
                errorsColumn,
                new DashboardWidget(
                        "Errors",
                        buildErrorsWidget()
                )
        );

        if (showNeedsAttention.getValue()) {
                leftColumn.add(
                        new DashboardWidget(
                                "Needs Attention",
                                buildNeedsAttentionWidget()
                        )
                );
        }

                if (showProcessingQueue.getValue()) {
                rightColumn.add(
                        new DashboardWidget(
                                "Processing Queue",
                                buildProcessingQueueWidget()
                        )
                );
        }

                if (showRecentActivity.getValue()) {
                leftColumn.add(
                        new DashboardWidget(
                                "Recent Activity",
                                buildRecentActivityWidget()
                        )
                );
        }

        HorizontalLayout dashboardGrid =
                new HorizontalLayout(leftColumn, rightColumn);

        dashboardGrid.setWidthFull();
        dashboardGrid.setAlignItems(Alignment.START);

        leftColumn.setWidth("50%");
        rightColumn.setWidth("50%");

        return dashboardGrid;
    }

    //Needs attention section for dash grid
    private Component buildNeedsAttentionWidget() {
        Grid<CaseRecordEntity> grid = new Grid<>(CaseRecordEntity.class, false);
        applyStandardGridStyle(grid, true);

        grid.setPartNameGenerator(record ->
                selectedRecord != null
                        && record.getId() != null
                        && record.getId().equals(selectedRecord.getId())
                        ? "summary-selected-row-cell"
                        : null
        );

        grid.addClassName("interactive-grid");

        grid.addColumn(CaseRecordEntity::getPatientLastName)
                .setHeader("Last")
                .setAutoWidth(true);

        grid.addColumn(CaseRecordEntity::getSiteName)
                .setHeader("Site")
                .setAutoWidth(true);

        grid.addComponentColumn(record -> buildStatusChip(formatEnum(record.getPatientStatus())))
                .setHeader("Status")
                .setAutoWidth(true);

        grid.setItems(getErrorRecords());

        grid.addItemClickListener(event ->
                showQuickView(event.getItem())
        );

        return grid;
    }

    //Proccesing grid for the dashboard
    private Component buildProcessingQueueWidget() {
        Grid<CaseRecordEntity> grid = new Grid<>(CaseRecordEntity.class, false);

        grid.setColumnReorderingAllowed(false);
        grid.setWidthFull();

        applyStandardGridStyle(grid, true);

        grid.setPartNameGenerator(record ->
                selectedRecord != null
                        && record.getId() != null
                        && record.getId().equals(selectedRecord.getId())
                        ? "summary-selected-row-cell"
                        : null
        );

        grid.addClassName("interactive-grid");

        grid.addColumn(CaseRecordEntity::getPatientLastName)
                .setHeader("Last")
                .setFlexGrow(1);

        grid.addColumn(CaseRecordEntity::getPatientFirstName)
                .setHeader("First")
                .setFlexGrow(1);

        grid.addColumn(CaseRecordEntity::getSiteName)
                .setHeader("Site")
                .setFlexGrow(2);

        grid.addComponentColumn(record ->
                buildStatusChip(formatEnum(record.getPatientStatus())))
                .setHeader("Status")
                .setFlexGrow(1);

        grid.setItems(getProcessingRecords());

        grid.addItemClickListener(event ->
                showQuickView(event.getItem())
        );

        return grid;
}

    //Here is the upcoming grid obj
    private Component buildUpcomingWidget() {
        Grid<CaseRecordEntity> grid = new Grid<>(CaseRecordEntity.class, false);
        applyStandardGridStyle(grid, true);

        grid.setPartNameGenerator(record ->
                selectedRecord != null
                        && record.getId() != null
                        && record.getId().equals(selectedRecord.getId())
                        ? "summary-selected-row-cell"
                        : null
        );

        grid.addClassName("interactive-grid");

        grid.addColumn(CaseRecordEntity::getPatientLastName)
                .setHeader("Last")
                .setAutoWidth(true);

        grid.addColumn(CaseRecordEntity::getPatientFirstName)
                .setHeader("First")
                .setAutoWidth(true);

        grid.addColumn(CaseRecordEntity::getSiteName)
                .setHeader("Site")
                .setAutoWidth(true);

        grid.addColumn(record -> formatDate(record.getDateScanned()))
                .setHeader("Scanned")
                .setAutoWidth(true);

        grid.setItems(getUpcomingRecords());

        grid.addItemClickListener(event ->
                showQuickView(event.getItem())
        );

        return grid;
    }

    //Completed in the last 30 days obj for the dash grid
    private Component buildCompletedWidget() {
        Grid<CaseRecordEntity> grid = new Grid<>(CaseRecordEntity.class, false);
        applyStandardGridStyle(grid, true);

        grid.setPartNameGenerator(record ->
                selectedRecord != null
                        && record.getId() != null
                        && record.getId().equals(selectedRecord.getId())
                        ? "summary-selected-row-cell"
                        : null
        );

        grid.addClassName("interactive-grid");

        grid.addColumn(CaseRecordEntity::getPatientLastName)
                .setHeader("Last")
                .setAutoWidth(true);

        grid.addColumn(CaseRecordEntity::getPatientFirstName)
                .setHeader("First")
                .setAutoWidth(true);

        grid.addColumn(CaseRecordEntity::getSiteName)
                .setHeader("Site")
                .setAutoWidth(true);

        grid.addColumn(record -> formatDateTimeCompact(record.getCompletedDate()))
                .setHeader("Completed")
                .setAutoWidth(true);

        grid.setItems(getCompletedLast30Records());

        grid.addItemClickListener(event ->
                showQuickView(event.getItem())
        );

        return grid;
    }

    //Errors section for dash
    private Component buildErrorsWidget() {
        Grid<CaseRecordEntity> grid = new Grid<>(CaseRecordEntity.class, false);
        applyStandardGridStyle(grid, true);

        grid.setPartNameGenerator(record ->
                selectedRecord != null
                        && record.getId() != null
                        && record.getId().equals(selectedRecord.getId())
                        ? "summary-selected-row-cell"
                        : null
        );

        grid.addClassName("interactive-grid");

        grid.addColumn(CaseRecordEntity::getPatientLastName)
                .setHeader("Last")
                .setAutoWidth(true);

        grid.addColumn(CaseRecordEntity::getPatientFirstName)
                .setHeader("First")
                .setAutoWidth(true);

        grid.addColumn(CaseRecordEntity::getSiteName)
                .setHeader("Site")
                .setAutoWidth(true);

        grid.addComponentColumn(record ->
                buildStatusChip(formatEnum(record.getPatientStatus())))
                .setHeader("Status")
                .setAutoWidth(true);

        grid.addColumn(record -> {
                        if (record.getImekaErrorNote() != null && !record.getImekaErrorNote().isBlank()) {
                        return "IMEKA";
                        }
                        if (record.getDuramapErrorNote() != null && !record.getDuramapErrorNote().isBlank()) {
                        return "DuraMap";
                        }
                        if (record.getNeuroreaderErrorNote() != null && !record.getNeuroreaderErrorNote().isBlank()) {
                        return "Neuroreader";
                        }
                        return "General";
                })
                .setHeader("Source")
                .setAutoWidth(true);

        grid.setItems(getErrorRecords());

        grid.addItemClickListener(event ->
                showQuickView(event.getItem())
        );

        return grid;
    }

    //Added processed grid item 0527
    private Component buildProcessedWidget() {
        Grid<CaseRecordEntity> grid = new Grid<>(CaseRecordEntity.class, false);
        applyStandardGridStyle(grid, true);

        grid.setPartNameGenerator(record ->
                selectedRecord != null
                        && record.getId() != null
                        && record.getId().equals(selectedRecord.getId())
                        ? "summary-selected-row-cell"
                        : null
        );

        grid.addColumn(CaseRecordEntity::getPatientLastName)
                .setHeader("Last")
                .setAutoWidth(true);

        grid.addColumn(CaseRecordEntity::getPatientFirstName)
                .setHeader("First")
                .setAutoWidth(true);

        grid.addColumn(CaseRecordEntity::getSiteName)
                .setHeader("Site")
                .setAutoWidth(true);

        grid.addColumn(record -> formatDateTimeCompact(record.getProcessedDate()))
                .setHeader("Processed")
                .setAutoWidth(true);

        grid.setItems(getProcessedRecords());

        grid.addItemClickListener(event ->
                showQuickView(event.getItem())
        );

        return grid;
    }

    //Updated to account for audit logging for recent activity 06012026
    private Component buildRecentActivityWidget() {
        VerticalLayout activity = new VerticalLayout();
        activity.setPadding(false);
        activity.setSpacing(false);
        activity.setWidthFull();

        var events = auditEventService.getRecentEvents();

        if (events.isEmpty()) {
                Span empty = new Span("No recent activity yet.");
                empty.getStyle()
                        .set("color", "#64748b")
                        .set("font-size", "0.88rem")
                        .set("font-weight", "500");

                activity.add(empty);
                return activity;
        }

        events.stream()
                .limit(8)
                .forEach(event ->
                        activity.add(
                                buildActivityItem(
                                        formatActivityTime(event.getCreatedAt()),
                                        event.getMessage(),
                                        event.getEventType()
                                )
                        )
                );

        return activity;
    }

    //Drawer should show now
    private void showQuickView(CaseRecordEntity record) {

        selectedRecord = record;

        refreshDashboardGrid();
        
        quickViewPanel.removeAll();

        quickViewPanel.add(
                new PatientQuickView(
                        record,
                        this::hideQuickView,
                        () -> {

                        quickViewBackdrop.getStyle().set("display", "none");

                        CaseRecordDialog.Mode dialogMode = getDialogModeForRecord(record);

                        CaseRecordDialog dialog = new CaseRecordDialog(
                                record,
                                caseRecordService,
                                dialogMode,
                                this::rebuildDashboard,
                                (dialogMode == CaseRecordDialog.Mode.UPCOMING
                                        || dialogMode == CaseRecordDialog.Mode.PROCESSING)
                                        ? siteService
                                        : null,
                                auditEventService
                        );

                        dialog.addDetachListener(event -> {
                        if ("block".equals(quickViewPanel.getStyle().get("display"))) {
                                quickViewBackdrop.getStyle()
                                        .set("display", "block")
                                        .set("opacity", "1");
                        }
                        });

                        dialog.open();
                        }
                )
        );

        quickViewPanel.getStyle().set("display", "block");
        //drawer should start off screen
        quickViewPanel.getStyle().set("transform", "translateX(0)");
        quickViewBackdrop.getStyle().set("display", "block");
    }

    //should allow clicking outside of dialog to close drawer now
    private void hideQuickView() {
        //slide in transiton
        quickViewPanel.getStyle().set("transform", "translateX(100%)");
        quickViewBackdrop.getStyle().set("opacity", "1");
        quickViewBackdrop.getStyle().set("display", "none");

        selectedRecord = null;

        refreshDashboardGrid();
    }

    //Helps the full patient dialog when selected from the
    private CaseRecordDialog.Mode getDialogModeForRecord(CaseRecordEntity record) {
        if (record.getPatientStatus() == null) {
                return CaseRecordDialog.Mode.SUMMARY;
        }

        return switch (record.getPatientStatus()) {
                case UPCOMING, VERIFYING -> CaseRecordDialog.Mode.UPCOMING;
                case ACQUIRED, PROCESSING -> CaseRecordDialog.Mode.PROCESSING;
                case PROCESSED, PROCESSED_WITH_ERRORS, PROCESSED_WITH_THIRD_PARTY_ERRORS -> CaseRecordDialog.Mode.PROCESSED;
                case COMPLETED -> CaseRecordDialog.Mode.COMPLETED;
                case ERROR -> CaseRecordDialog.Mode.ERRORS;
        };
    }

    //persistent row hovering on grids, rebuilt for new dashboard revision - updated 6082026, updated again 6092026, trying to nail down this alignment
    private void refreshDashboardGrid() {
        removeAll();

        add(
                        buildDashboardPage(
                                new H2("BMS Dashboard"),
                                new Span("Operational overview of workflow status and patient queues.")
                        ),
                        quickViewBackdrop,
                        quickViewPanel
        );
    }
    
    //Will allow configrable widget placement regardless of side now
    private void addWidgetToColumn(
        VerticalLayout leftColumn,
        VerticalLayout rightColumn,
        Checkbox visibilityCheckbox,
        Select<String> columnSelect,
        Component widget
    ) {

        if (!visibilityCheckbox.getValue()) {
                return;
        }

        if ("Right".equals(columnSelect.getValue())) {
                rightColumn.add(widget);
        } else {
                leftColumn.add(widget);
        }
    }

    //audit log helper, replaced placeholder on 6012026, formatting text from audit codes
    private Component buildActivityItem(String time, String message, String eventType) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setAlignItems(Alignment.CENTER);
        row.setSpacing(true);

        row.getStyle()
                .set("padding", "0.55rem 0")
                .set("border-bottom", "1px solid #eef2f7");

        Icon icon = getActivityIcon(eventType);
        icon.setSize("20px");
        icon.getStyle()
                .set("color", getActivityColor(eventType))
                .set("flex-shrink", "0");

        Span timeText = new Span(time);
        timeText.getStyle()
                .set("font-size", "0.78rem")
                .set("font-weight", "700")
                .set("color", "#64748b")
                .set("min-width", "70px");

        Span messageText = new Span(message);
        messageText.getStyle()
                .set("font-size", "0.88rem")
                .set("font-weight", isActivityEmphasized(eventType) ? "700" : "500")
                .set("color", getActivityTextColor(eventType));

        row.add(icon, timeText, messageText);

        return row;
    }

    private Icon getActivityIcon(String eventType) {
        return switch (eventType) {
                case "PATIENT_CREATED" -> VaadinIcon.USER_CARD.create();
                case "IMAGES_RECEIVED" -> VaadinIcon.DOWNLOAD_ALT.create();
                case "PROCESSING_STARTED" -> VaadinIcon.REFRESH.create();
                case "RETURNED_TO_PROCESSING" -> VaadinIcon.REFRESH.create();
                case "CASE_ERROR" -> VaadinIcon.WARNING.create();
                case "CASE_FINALIZED" -> VaadinIcon.CHECK.create();
                case "CASE_COMPLETED" -> VaadinIcon.CHECK_CIRCLE_O.create();
                default -> VaadinIcon.INFO_CIRCLE_O.create();
        };
    }

    private String getActivityColor(String eventType) {
        return switch (eventType) {
                case "CASE_ERROR" -> "#dc2626";
                case "CASE_FINALIZED", "CASE_COMPLETED" -> "#16a34a";
                case "PROCESSING_STARTED", "RETURNED_TO_PROCESSING" -> "#2563eb";
                case "IMAGES_RECEIVED" -> "#0891b2";
                case "PATIENT_CREATED" -> "#64748b";
                default -> "#64748b";
        };
    }

    private String getActivityTextColor(String eventType) {
        return switch (eventType) {
                case "CASE_ERROR" -> "#dc2626";
                case "CASE_FINALIZED", "CASE_COMPLETED" -> "#16a34a";
                default -> "#334155";
        };
    }

    private boolean isActivityEmphasized(String eventType) {
        return eventType != null
                && (
                eventType.equals("CASE_ERROR")
                        || eventType.equals("CASE_FINALIZED")
                        || eventType.equals("CASE_COMPLETED")
        );
    }

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

        return String.format("%d:%02d %s", displayHour, minute, amPm);
    }

}

