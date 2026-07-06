package com.bms.processing.views.manage;

import com.bms.processing.components.SiteDialog;
import com.bms.processing.components.SiteContactDialog;
import com.bms.processing.entity.SiteEntity;
import com.bms.processing.layouts.MainLayout;
import com.bms.processing.service.SiteService;
import com.bms.processing.service.SiteContactService;
import com.bms.processing.entity.SiteContactEntity;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
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

    //Site contacts
    private final SiteContactService siteContactService;

    public ManageSitesView(
            SiteService siteService,
            SiteContactService siteContactService
    ){
        this.siteService = siteService;
        this.siteContactService = siteContactService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        getStyle()
                .set("background", "#f5f7fb");

        Button addSiteButton = new Button("+ Add Site");
        addSiteButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addSiteButton.addClickListener(event ->
                new SiteDialog(
                        siteService,
                        site -> {
                            selectedSite = site;
                            refreshSiteList();
                            renderSiteDetails(site);
                        }
                ).open()
        );

        HorizontalLayout header = new HorizontalLayout(
                MainLayout.pageTitle("Site Manager"),
                addSiteButton
        );
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);

        searchField.setPlaceholder("Search sites...");
        searchField.setClearButtonVisible(true);
        searchField.setWidthFull();
        searchField.addValueChangeListener(event -> refreshSiteList());

        configureGrid();
        refreshGrid();

        //Expanding grid formatting here, updated 6032026
        HorizontalLayout mainLayout = new HorizontalLayout();
        mainLayout.setWidthFull();
        mainLayout.setHeight("620px");
        mainLayout.setSpacing(true);
        mainLayout.getStyle()
            .set("gap", "1rem")
            .set("align-items", "stretch");

        Span allSitesLabel = new Span("All Sites");
        allSitesLabel.getStyle()
                .set("font-weight", "700")
                .set("font-size", "0.85rem")
                .set("color", "#1e293b");

        // added some styling to allow for scroll after the massive amounts of sites that have been added - updated 7022026
        VerticalLayout leftPanel = new VerticalLayout(searchField, allSitesLabel, siteList);
        leftPanel.setWidth("320px");
        leftPanel.setMinWidth("300px");
        leftPanel.setMaxWidth("360px");

        siteList.setWidthFull();
        siteList.setHeightFull();
        siteList.setPadding(false);
        siteList.setSpacing(true);

        siteList.getStyle()
                .set("overflow-y", "auto")
                .set("overflow-x", "hidden")
                .set("padding-right", "0.75rem");

        leftPanel.getStyle()
                .set("background", "#ffffff")
                .set("border", "1px solid #dbe3ee")
                .set("border-radius", "14px")
                .set("padding", "1rem")
                .set("padding-right", "0.5rem")
                .set("box-shadow", "0 2px 8px rgba(15, 23, 42, 0.04)");

        leftPanel.setPadding(true);
        leftPanel.setSpacing(true);
        leftPanel.setHeightFull();
        leftPanel.expand(siteList);

        siteDetails.setWidthFull();
        siteDetails.setPadding(true);
        siteDetails.setSpacing(true);
        siteDetails.getStyle()
                .set("background", "#ffffff")
                .set("border", "1px solid #dbe3ee")
                .set("border-radius", "14px")
                .set("box-shadow", "0 2px 8px rgba(15, 23, 42, 0.04)")
                .set("min-height", "420px")
                .set("padding", "1.25rem");

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

        grid.addColumn(site ->
                String.join(", ",
                        java.util.stream.Stream.of(
                                site.getAddressLine1(),
                                site.getCity(),
                                site.getState()
                        )
                        .filter(value -> value != null && !value.isBlank())
                        .toList()
                )
        )
        .setHeader("Location")
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
                                        || contains(site.getAddressLine1(), filter)
                                        || contains(site.getAddressLine2(), filter)
                                        || contains(site.getCity(), filter)
                                        || contains(site.getState(), filter)
                                        || contains(site.getZipCode(), filter)
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
                                || contains(site.getAddressLine1(), filter)
                                || contains(site.getAddressLine2(), filter)
                                || contains(site.getCity(), filter)
                                || contains(site.getState(), filter)
                                || contains(site.getZipCode(), filter)
                                || contains(site.getTransferMethod(), filter)
                                || contains(site.getScannerBrand(), filter)
                                || contains(site.getMagnetStrength(), filter)
                )
                .forEach(site -> {
                    //Changing button style for site list, didn't like the "button" look, wanted a seemless list look 6042026
                    Component siteCard = buildSiteListCard(site);

                    siteCard.getElement().addEventListener("click", event -> {
                        selectedSite = site;
                        refreshSiteList();
                        renderSiteDetails(site);
                    });

                    siteList.add(siteCard);
                });

        if (selectedSite == null && siteList.getComponentCount() > 0) {
            SiteEntity firstSite = siteService.getAllSites().get(0);
            selectedSite = firstSite;
            renderSiteDetails(firstSite);
        }
    }

    private Component buildSiteListCard(SiteEntity site) {
        boolean selected = selectedSite != null
                && selectedSite.getId() != null
                && selectedSite.getId().equals(site.getId());

        boolean active = !Boolean.FALSE.equals(site.getActive());

        VerticalLayout card = new VerticalLayout();
        card.setWidthFull();
        card.setPadding(false);
        card.setSpacing(false);

        card.getStyle()
                .set("background", selected ? "#eef6ff" : "#ffffff")
                .set("border", selected ? "1px solid #3b82f6" : "1px solid #e2e8f0")
                .set("border-radius", "14px")
                .set("padding", "0.85rem")
                .set("cursor", "pointer")
                .set("box-shadow", selected
                        ? "0 6px 16px rgba(59, 130, 246, 0.16)"
                        : "0 2px 6px rgba(15, 23, 42, 0.04)");

        Span name = new Span(site.getFacilityName() == null ? "Unnamed Site" : site.getFacilityName());
        name.getStyle()
                .set("font-weight", "800")
                .set("font-size", "0.95rem")
                .set("color", "#0f172a");

        Span badge = new Span(active ? "ACTIVE" : "INACTIVE");
        badge.getStyle()
                .set("background", active ? "#dcfce7" : "#e2e8f0")
                .set("color", active ? "#15803d" : "#475569")
                .set("border-radius", "999px")
                .set("padding", "0.15rem 0.5rem")
                .set("font-size", "0.68rem")
                .set("font-weight", "800");

        HorizontalLayout topRow = new HorizontalLayout(name, badge);
        topRow.setWidthFull();
        topRow.setAlignItems(Alignment.CENTER);
        topRow.setJustifyContentMode(JustifyContentMode.BETWEEN);

        Span location = new Span(
                buildLocation(site)
        );
        location.getStyle()
                .set("font-size", "0.78rem")
                .set("color", "#64748b")
                .set("font-weight", "600")
                .set("margin-top", "0.35rem");

        Span transfer = new Span(site.getTransferMethod() == null || site.getTransferMethod().isBlank()
                ? "Transfer method not set"
                : site.getTransferMethod());
        transfer.getStyle()
                .set("font-size", "0.78rem")
                .set("color", "#334155")
                .set("margin-top", "0.15rem");

        card.add(topRow, location, transfer);

        return card;
    }

    private String buildLocation(SiteEntity site) {
        String city = site.getCity();
        String state = site.getState();

        if (city == null || city.isBlank()) {
            return "Location not set";
        }

        if (state == null || state.isBlank()) {
            return city;
        }

        return city + ", " + state;
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
                new SiteDialog(
                        siteService,
                        site,
                        savedSite -> {
                            selectedSite = savedSite;
                            refreshSiteList();
                            renderSiteDetails(savedSite);
                        }
                ).open()
        );

        Button archiveButton = new Button("Archive Site");
        archiveButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        archiveButton.addClickListener(event -> {
            site.setActive(false);
            SiteEntity saved = siteService.save(site);
            selectedSite = saved;
            refreshSiteList();
            renderSiteDetails(saved);
        });

        boolean active = !Boolean.FALSE.equals(site.getActive());

        Span activeBadge = new Span(active ? "ACTIVE" : "INACTIVE");
        activeBadge.getStyle()
                .set("background", active ? "#dcfce7" : "#e2e8f0")
                .set("color", active ? "#15803d" : "#475569")
                .set("border-radius", "999px")
                .set("padding", "0.2rem 0.55rem")
                .set("font-size", "0.72rem")
                .set("font-weight", "800");

        HorizontalLayout titleGroup = new HorizontalLayout(siteTitle, activeBadge);
        titleGroup.setAlignItems(Alignment.CENTER);

        HorizontalLayout actionGroup = new HorizontalLayout(editButton, archiveButton);
        actionGroup.setSpacing(true);

        HorizontalLayout headerRow = new HorizontalLayout(titleGroup, actionGroup);
        headerRow.setWidthFull();
        headerRow.setAlignItems(Alignment.CENTER);
        headerRow.setJustifyContentMode(JustifyContentMode.BETWEEN);

        //notes tab details
        Tab overviewTab = new Tab("Overview");
        Tab contactsTab = new Tab("Contacts");
        Tab notesTab = new Tab("Notes");

        Tabs tabs = new Tabs(
                overviewTab,
                contactsTab,
                notesTab
        );

        tabs.getStyle()
                .set("border-bottom", "1px solid #e2e8f0");

        Div tabContent = new Div();
        tabContent.setWidthFull();

        tabs.addSelectedChangeListener(event -> {
            tabContent.removeAll();

            if (event.getSelectedTab() == overviewTab) {
                tabContent.add(buildOverviewContent(site));
            } else if (event.getSelectedTab() == contactsTab) {
                tabContent.add(buildContactsContent(site));
            } else {
                tabContent.add(buildNotesContent(site));
            }
        });

        tabContent.add(buildOverviewContent(site));

        siteDetails.add(headerRow, tabs, tabContent);
    }

    //Moving overview card
    private Component buildOverviewContent(SiteEntity site) {
        Div cardRow = new Div();
        cardRow.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(4, minmax(240px, 1fr))")
                .set("gap", "1rem")
                .set("width", "100%");
        cardRow.setWidthFull();

        cardRow.add(
                buildInfoCard(
                        "Facility Information",
                        "Facility Name", site.getFacilityName(),
                        "Address", site.getAddressLine1(),
                        "City", site.getCity(),
                        "State", site.getState(),
                        "ZIP", site.getZipCode()
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
                ),
                buildInfoCard(
                        "Status / Dates",
                        "Status", Boolean.FALSE.equals(site.getActive()) ? "Inactive" : "Active",
                        "Date Added", "-",
                        "Last Updated", "-"
                )
        );

        return cardRow;
    }

    private Component buildInfoCard(
            String title,
            String... labelValues
    ) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        //This needs tweaked
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

    //Notes response and save feedback
    private Component buildNotesContent(SiteEntity site) {
        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setPadding(false);
        wrapper.setSpacing(true);
        wrapper.setWidthFull();

        TextArea notesArea = new TextArea("Site Notes");

        notesArea.setWidthFull();
        notesArea.setMinHeight("220px");
        notesArea.setValue(site.getNotes() == null ? "" : site.getNotes());

        Button saveNotesButton = new Button("Save Notes");
        saveNotesButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        saveNotesButton.addClickListener(event -> {
            site.setNotes(notesArea.getValue());
            SiteEntity saved = siteService.save(site);
            selectedSite = saved;
            Notification.show("Site notes saved.", 2500, Notification.Position.BOTTOM_END);
        });

        wrapper.add(notesArea, saveNotesButton);
        return wrapper;
    }

    //Contact information for sites
    private Component buildContactsContent(SiteEntity site) {

        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setPadding(false);
        wrapper.setSpacing(true);
        wrapper.setWidthFull();

        Button addContactButton = new Button("+ Add Contact");

        addContactButton.addClickListener(event ->
                new SiteContactDialog(
                        siteContactService,
                        site,
                        null,
                        savedContact -> {
                            siteDetails.removeAll();
                            renderSiteDetails(site);
                        }
                ).open()
        );

        wrapper.add(addContactButton);

        for (SiteContactEntity contact :
                siteContactService.getContactsForSite(site)) {

            VerticalLayout card = new VerticalLayout();
            card.setPadding(true);
            card.setSpacing(false);

            card.getStyle()
                    .set("border", "1px solid #dbe3ee")
                    .set("border-radius", "12px")
                    .set("background", "#ffffff");

            Span name = new Span(
                    contact.getContactName() == null
                            ? "Unnamed Contact"
                            : contact.getContactName()
            );

            name.getStyle()
                    .set("font-weight", "700");

            Span title = new Span(
                    contact.getTitle() == null
                            ? "-"
                            : contact.getTitle()
            );

            title.getStyle()
                    .set("color", "#64748b")
                    .set("font-size", "0.82rem");

            Span email = new Span(
                    contact.getEmail() == null
                            ? "-"
                            : contact.getEmail()
            );

            Span phone = new Span(
                    contact.getPhone() == null
                            ? "-"
                            : contact.getPhone()
            );

            Button editButton = new Button("Edit");
            editButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

            editButton.addClickListener(event ->
                    new SiteContactDialog(
                            siteContactService,
                            site,
                            contact,
                            savedContact -> {
                                siteDetails.removeAll();
                                renderSiteDetails(site);
                            }
                    ).open()
            );

            Button deleteButton = new Button("Delete");
            deleteButton.addThemeVariants(
                    ButtonVariant.LUMO_SMALL,
                    ButtonVariant.LUMO_ERROR
            );

            deleteButton.addClickListener(event -> {
                siteContactService.delete(contact);
                siteDetails.removeAll();
                renderSiteDetails(site);
            });

            HorizontalLayout buttonRow = new HorizontalLayout(editButton, deleteButton);
            buttonRow.setSpacing(true);

            card.add(name, title, email, phone, buttonRow);

            wrapper.add(card);
        }

        return wrapper;
    }
}