package com.bms.processing.views.manage;

import com.bms.processing.components.SiteDialog;
import com.bms.processing.entity.SiteEntity;
import com.bms.processing.layouts.MainLayout;
import com.bms.processing.service.SiteService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.notification.Notification;

@PageTitle("Manage Sites")
@PermitAll
@Route(value = "manage/sites", layout = MainLayout.class)
public class ManageSitesView extends VerticalLayout {

    private final SiteService siteService;

    private final Grid<SiteEntity> grid =
            new Grid<>(SiteEntity.class, false);

    private final TextField searchField = new TextField();

    //formatting for site management table
    private final VerticalLayout siteList = new VerticalLayout();
    private final VerticalLayout siteDetails = new VerticalLayout();
    private SiteEntity selectedSite;

    public ManageSitesView(SiteService siteService) {
        this.siteService = siteService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Site Management");
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "2rem")
                .set("font-weight", "700")
                .set("color", "#1e293b");

        Span subtitle = new Span("Manage and maintain all imaging sites in the system.");
        subtitle.getStyle()
                .set("color", "#64748b")
                .set("font-size", "0.98rem");

        VerticalLayout titleBlock = new VerticalLayout(title, subtitle);
        titleBlock.setPadding(false);
        titleBlock.setSpacing(false);

        Button addSiteButton = new Button("+ Add Site");
        addSiteButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addSiteButton.addClickListener(event ->
                new SiteDialog(
                        siteService,
                        site -> refreshGrid()
                ).open()
        );

        HorizontalLayout header = new HorizontalLayout(titleBlock, addSiteButton);
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);

        searchField.setPlaceholder("Search sites...");
        searchField.setClearButtonVisible(true);
        searchField.setWidth("420px");
        searchField.addValueChangeListener(event -> refreshSiteList());

        configureGrid();
        refreshGrid();

        //Expanding grid formatting here
        HorizontalLayout mainLayout = new HorizontalLayout();
        mainLayout.setSizeFull();
        mainLayout.setSpacing(true);

        VerticalLayout leftPanel = new VerticalLayout(searchField, siteList);
        leftPanel.setWidth("320px");
        leftPanel.setPadding(false);
        leftPanel.setSpacing(true);

        siteDetails.setWidthFull();
        siteDetails.setPadding(false);
        siteDetails.setSpacing(true);

        mainLayout.add(leftPanel, siteDetails);
        mainLayout.expand(siteDetails);

        add(header, mainLayout);
        expand(mainLayout);

        refreshSiteList();
    }

    private void configureGrid() {
        grid.setSizeFull();

        grid.addColumn(SiteEntity::getFacilityName)
                .setHeader("Facility Name")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(SiteEntity::getAddress)
                .setHeader("Address")
                .setAutoWidth(true);

        grid.addColumn(SiteEntity::getTransferMethod)
                .setHeader("Transfer Method")
                .setAutoWidth(true);

        grid.addColumn(site ->
                        Boolean.TRUE.equals(site.getImekaCertified())
                                ? "Yes"
                                : "No"
                )
                .setHeader("IMEKA Certified")
                .setAutoWidth(true);

        grid.addColumn(SiteEntity::getScannerBrand)
                .setHeader("Scanner Brand")
                .setAutoWidth(true);

        grid.addColumn(SiteEntity::getMagnetStrength)
                .setHeader("Magnet Strength")
                .setAutoWidth(true);

        grid.addItemClickListener(event ->
                com.vaadin.flow.component.notification.Notification.show(
                        "Site edit dialog coming next.",
                        3000,
                        com.vaadin.flow.component.notification.Notification.Position.BOTTOM_END
                )
        );
    }

    private void refreshGrid() {
        String filter = searchField.getValue() == null
                ? ""
                : searchField.getValue().trim().toLowerCase();

        grid.setItems(
                siteService.getAllSites().stream()
                        .filter(site ->
                                filter.isEmpty()
                                        || contains(site.getFacilityName(), filter)
                                        || contains(site.getAddress(), filter)
                                        || contains(site.getTransferMethod(), filter)
                                        || contains(site.getScannerBrand(), filter)
                                        || contains(site.getMagnetStrength(), filter)
                        )
                        .toList()
        );
    }

    private boolean contains(String value, String filter) {
        return value != null && value.toLowerCase().contains(filter);
    }

    //
    private void refreshSiteList() {
        siteList.removeAll();

        String filter = searchField.getValue() == null
                ? ""
                : searchField.getValue().trim().toLowerCase();

        siteService.getAllSites().stream()
                .filter(site ->
                        filter.isEmpty()
                                || contains(site.getFacilityName(), filter)
                                || contains(site.getAddress(), filter)
                                || contains(site.getTransferMethod(), filter)
                                || contains(site.getScannerBrand(), filter)
                                || contains(site.getMagnetStrength(), filter)
                )
                .forEach(site -> {
                    Button siteButton = new Button(site.getFacilityName());
                    siteButton.setWidthFull();
                    siteButton.getStyle()
                            .set("justify-content", "flex-start")
                            .set("border-radius", "10px")
                            .set("font-weight", "600");

                    siteButton.addClickListener(event -> {
                        selectedSite = site;
                        renderSiteDetails(site);
                    });

                    siteList.add(siteButton);
                });

        if (selectedSite == null && siteList.getComponentCount() > 0) {
            SiteEntity firstSite = siteService.getAllSites().get(0);
            selectedSite = firstSite;
            renderSiteDetails(firstSite);
        }
    }

    private void renderSiteDetails(SiteEntity site) {
        siteDetails.removeAll();

        if (site == null) {
            siteDetails.add(new Span("Select a site to view details."));
            return;
        }

        H2 siteTitle = new H2(site.getFacilityName());
        siteTitle.getStyle()
                .set("margin", "0")
                .set("font-size", "1.6rem")
                .set("font-weight", "700");

        Button editButton = new Button("Edit Site");
        editButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        editButton.addClickListener(event ->
                Notification.show(
                        "Edit Site coming next.",
                        3000,
                        Notification.Position.BOTTOM_END
                )
        );

        Button archiveButton = new Button("Archive Site");
        archiveButton.addThemeVariants(ButtonVariant.LUMO_ERROR);

        HorizontalLayout headerRow = new HorizontalLayout(siteTitle, editButton, archiveButton);
        headerRow.setWidthFull();
        headerRow.setAlignItems(Alignment.CENTER);
        headerRow.setJustifyContentMode(JustifyContentMode.BETWEEN);

        HorizontalLayout cardRow = new HorizontalLayout();
        cardRow.setWidthFull();
        cardRow.setSpacing(true);

        cardRow.add(
                buildInfoCard(
                        "Facility Information",
                        "Facility Name", site.getFacilityName(),
                        "Address", site.getAddress()
                ),
                buildInfoCard(
                        "Scanner Information",
                        "Vendor", site.getScannerBrand(),
                        "Magnet Strength", site.getMagnetStrength(),
                        "IMEKA Certified", Boolean.TRUE.equals(site.getImekaCertified()) ? "Yes" : "No"
                ),
                buildInfoCard(
                        "Transfer Information",
                        "Transfer Method", site.getTransferMethod()
                )
        );

        siteDetails.add(headerRow, cardRow);
    }

    private Component buildInfoCard(
            String title,
            String... labelValues
    ) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.setWidthFull();

        card.getStyle()
                .set("background", "#ffffff")
                .set("border", "1px solid #dbe3ee")
                .set("border-radius", "14px")
                .set("box-shadow", "0 2px 8px rgba(15, 23, 42, 0.04)");

        Span titleSpan = new Span(title);
        titleSpan.getStyle()
                .set("font-weight", "700")
                .set("font-size", "0.95rem")
                .set("margin-bottom", "0.75rem");

        card.add(titleSpan);

        for (int i = 0; i < labelValues.length; i += 2) {
            String label = labelValues[i];
            String value = i + 1 < labelValues.length ? labelValues[i + 1] : "";

            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setJustifyContentMode(JustifyContentMode.BETWEEN);

            Span labelSpan = new Span(label);
            labelSpan.getStyle()
                    .set("color", "#64748b")
                    .set("font-size", "0.82rem")
                    .set("font-weight", "600");

            Span valueSpan = new Span(value == null || value.isBlank() ? "-" : value);
            valueSpan.getStyle()
                    .set("color", "#1e293b")
                    .set("font-size", "0.86rem")
                    .set("font-weight", "600");

            row.add(labelSpan, valueSpan);
            card.add(row);
        }

        return card;
    }
}