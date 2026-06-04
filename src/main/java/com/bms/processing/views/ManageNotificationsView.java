package com.bms.processing.views.manage;

import com.bms.processing.entity.NotificationRecipientEntity;
import com.bms.processing.layouts.MainLayout;
import com.bms.processing.service.NotificationRecipientService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.List;

@PageTitle("Manage Notification Groups")
@PermitAll
@Route(value = "manage/notifications", layout = MainLayout.class)
public class ManageNotificationsView extends VerticalLayout {

    private final NotificationRecipientService notificationRecipientService;

    private final Grid<NotificationRuleRow> grid =
            new Grid<>(NotificationRuleRow.class, false);

    private final TextField searchField = new TextField();

    public ManageNotificationsView(NotificationRecipientService notificationRecipientService) {
        this.notificationRecipientService = notificationRecipientService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle().set("background", "#f8fafc");

        add(buildHeader());
        add(buildSearchRow());

        configureGrid();
        refreshGrid();

        add(grid);
        expand(grid);
    }

    private Component buildHeader() {
        H2 title = new H2("Notification Management");
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "2rem")
                .set("font-weight", "700")
                .set("color", "#0f172a");

        Span subtitle = new Span("Configure email recipients for workflow statuses and events.");
        subtitle.getStyle()
                .set("color", "#64748b")
                .set("font-size", "0.95rem");

        VerticalLayout titleBlock = new VerticalLayout(title, subtitle);
        titleBlock.setPadding(false);
        titleBlock.setSpacing(false);

        Button addButton = new Button("+ Add Recipient");
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(event -> openRecipientDialog(null, null));

        HorizontalLayout header = new HorizontalLayout(titleBlock, addButton);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.START);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        return header;
    }

    private Component buildSearchRow() {
        searchField.setPlaceholder("Search recipients, groups, or email...");
        searchField.setClearButtonVisible(true);
        searchField.setWidth("520px");
        searchField.addValueChangeListener(event -> refreshGrid());

        HorizontalLayout row = new HorizontalLayout(searchField);
        row.setWidthFull();
        row.setAlignItems(FlexComponent.Alignment.CENTER);

        return row;
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.getStyle()
                .set("background", "white")
                .set("border", "1px solid #e2e8f0")
                .set("border-radius", "10px")
                .set("overflow", "hidden");

        grid.addComponentColumn(this::buildStatusCell)
                .setHeader("Status / Event")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(NotificationRuleRow::description)
                .setHeader("Description")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addComponentColumn(this::buildRecipientsCell)
                .setHeader("Recipients / Groups")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(row -> "Active")
                .setHeader("Status")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addComponentColumn(this::buildActionsCell)
                .setHeader("Actions")
                .setAutoWidth(true)
                .setFlexGrow(0);
    }

    private Component buildStatusCell(NotificationRuleRow row) {
        Icon icon = row.icon().create();
        icon.setSize("18px");

        Div iconBubble = new Div(icon);
        iconBubble.getStyle()
                .set("width", "36px")
                .set("height", "36px")
                .set("border-radius", "999px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("background", row.iconBackground())
                .set("color", row.iconColor());

        Span label = new Span(row.label());
        label.getStyle()
                .set("font-weight", "700")
                .set("color", "#0f172a");

        Span group = new Span(row.groupName());
        group.getStyle()
                .set("font-size", "0.78rem")
                .set("color", "#64748b");

        VerticalLayout text = new VerticalLayout(label, group);
        text.setPadding(false);
        text.setSpacing(false);

        HorizontalLayout layout = new HorizontalLayout(iconBubble, text);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setSpacing(true);

        return layout;
    }

    private Component buildRecipientsCell(NotificationRuleRow row) {
        List<NotificationRecipientEntity> recipients = getRecipientsForGroup(row.groupName());

        if (recipients.isEmpty()) {
            Span empty = new Span("No recipients");
            empty.getStyle().set("color", "#94a3b8");
            return empty;
        }

        String summary = recipients.stream()
                .map(NotificationRecipientEntity::getEmailAddress)
                .limit(2)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

        if (recipients.size() > 2) {
            summary += " +" + (recipients.size() - 2) + " more";
        }

        Span text = new Span(summary);
        text.getStyle().set("color", "#334155");

        return text;
    }

    private Component buildActionsCell(NotificationRuleRow row) {
        Button editButton = new Button("Edit");
        editButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        editButton.addClickListener(event -> openRuleDialog(row));

        return editButton;
    }

    private void refreshGrid() {
        String filter = searchField.getValue() == null
                ? ""
                : searchField.getValue().trim().toLowerCase();

        List<NotificationRuleRow> rows = defaultRows().stream()
                .filter(row ->
                        filter.isEmpty()
                                || row.label().toLowerCase().contains(filter)
                                || row.groupName().toLowerCase().contains(filter)
                                || row.description().toLowerCase().contains(filter)
                                || getRecipientsForGroup(row.groupName()).stream()
                                .anyMatch(recipient ->
                                        contains(recipient.getEmailAddress(), filter)
                                )
                )
                .toList();

        grid.setItems(rows);
    }

    private void openRuleDialog(NotificationRuleRow row) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(row.label() + " Notification Rule");
        dialog.setWidth("780px");

        H3 title = new H3(row.label());
        title.getStyle().set("margin", "0");

        Span description = new Span(row.description());
        description.getStyle().set("color", "#64748b");

        Grid<NotificationRecipientEntity> recipientGrid =
                new Grid<>(NotificationRecipientEntity.class, false);

        recipientGrid.addColumn(NotificationRecipientEntity::getEmailAddress)
                .setHeader("Recipient")
                .setAutoWidth(true)
                .setFlexGrow(1);

        recipientGrid.addColumn(recipient ->
                        Boolean.TRUE.equals(recipient.getEnabled()) ? "Active" : "Inactive"
                )
                .setHeader("Status")
                .setAutoWidth(true);

        recipientGrid.addComponentColumn(recipient -> {
            Button editButton = new Button("Edit");
            editButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
            editButton.addClickListener(event -> {
                dialog.close();
                openRecipientDialog(recipient, row.groupName());
            });

            Button deleteButton = new Button("Delete");
            deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(event -> {
                notificationRecipientService.delete(recipient);
                recipientGrid.setItems(getRecipientsForGroup(row.groupName()));
                refreshGrid();
            });

            return new HorizontalLayout(editButton, deleteButton);
        }).setHeader("Actions").setAutoWidth(true);

        recipientGrid.setItems(getRecipientsForGroup(row.groupName()));
        recipientGrid.setHeight("300px");

        Button addRecipientButton = new Button("+ Add Recipient");
        addRecipientButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addRecipientButton.addClickListener(event -> {
            dialog.close();
            openRecipientDialog(null, row.groupName());
        });

        VerticalLayout content = new VerticalLayout(
                title,
                description,
                addRecipientButton,
                recipientGrid
        );
        content.setPadding(false);
        content.setSpacing(true);

        Button closeButton = new Button("Close", event -> dialog.close());

        dialog.add(content);
        dialog.getFooter().add(closeButton);
        dialog.open();
    }

    private void openRecipientDialog(
            NotificationRecipientEntity existingRecipient,
            String lockedGroupName
    ) {
        boolean editMode = existingRecipient != null;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(editMode ? "Edit Recipient" : "Add Recipient");
        dialog.setWidth("520px");

        TextField groupName = new TextField("Notification Group");
        groupName.setWidthFull();

        EmailField emailAddress = new EmailField("Email Address");
        emailAddress.setWidthFull();

        Checkbox enabled = new Checkbox("Enabled", true);

        if (lockedGroupName != null) {
            groupName.setValue(lockedGroupName);
            groupName.setReadOnly(true);
        }

        if (editMode) {
            groupName.setValue(existingRecipient.getGroupName() == null ? "" : existingRecipient.getGroupName());
            emailAddress.setValue(existingRecipient.getEmailAddress() == null ? "" : existingRecipient.getEmailAddress());
            enabled.setValue(Boolean.TRUE.equals(existingRecipient.getEnabled()));
        }

        VerticalLayout form = new VerticalLayout(groupName, emailAddress, enabled);
        form.setPadding(false);
        form.setSpacing(true);

        Button cancelButton = new Button("Cancel", event -> dialog.close());

        Button saveButton = new Button(editMode ? "Save Changes" : "Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        saveButton.addClickListener(event -> {
            NotificationRecipientEntity recipient = editMode
                    ? existingRecipient
                    : new NotificationRecipientEntity();

            recipient.setGroupName(groupName.getValue().trim());
            recipient.setEmailAddress(emailAddress.getValue().trim());
            recipient.setEnabled(enabled.getValue());

            notificationRecipientService.save(recipient);
            refreshGrid();
            dialog.close();
        });

        dialog.add(form);
        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    private List<NotificationRecipientEntity> getRecipientsForGroup(String groupName) {
        return notificationRecipientService.findAll().stream()
                .filter(recipient -> groupName.equals(recipient.getGroupName()))
                .toList();
    }

    private List<NotificationRuleRow> defaultRows() {
        return List.of(
                new NotificationRuleRow(
                        "Upcoming",
                        "PATIENT_CREATED",
                        "Notifies intake recipients when a patient is created.",
                        VaadinIcon.CALENDAR,
                        "#ede9fe",
                        "#6d28d9"
                ),
                new NotificationRuleRow(
                        "Processed",
                        "CASE_FINALIZED",
                        "Notifies BMS team when patient has been processed and is ready for review.",
                        VaadinIcon.CHECK_CIRCLE,
                        "#dcfce7",
                        "#16a34a"
                ),
                new NotificationRuleRow(
                        "Completed",
                        "CASE_COMPLETED",
                        "Notifies BMS team when patient is marked Completed.",
                        VaadinIcon.CHECK,
                        "#ffedd5",
                        "#ea580c"
                ),
                new NotificationRuleRow(
                        "Errors",
                        "CASE_ERROR",
                        "Notifies BMS team and Prism support when an error occurs.",
                        VaadinIcon.WARNING,
                        "#ffe4e6",
                        "#e11d48"
                )
        );
    }

    private boolean contains(String value, String filter) {
        return value != null && value.toLowerCase().contains(filter);
    }

    private record NotificationRuleRow(
            String label,
            String groupName,
            String description,
            VaadinIcon icon,
            String iconBackground,
            String iconColor
    ) {
    }
}