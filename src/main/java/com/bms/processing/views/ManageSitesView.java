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

@PageTitle("Manage Sites")
@PermitAll
@Route(value = "manage/sites", layout = MainLayout.class)
public class ManageSitesView extends VerticalLayout {

    private final SiteService siteService;

    private final Grid<SiteEntity> grid =
            new Grid<>(SiteEntity.class, false);

    private final TextField searchField = new TextField();

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
        searchField.addValueChangeListener(event -> refreshGrid());

        configureGrid();
        refreshGrid();

        add(header, searchField, grid);
        expand(grid);
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
}