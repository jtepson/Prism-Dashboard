package com.bms.processing.views.manage;

import com.bms.processing.entity.NotificationRecipientEntity;
import com.bms.processing.layouts.MainLayout;
import com.bms.processing.service.NotificationRecipientService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@PageTitle("Manage Notification Groups")
@PermitAll
@Route(value = "manage/notifications", layout = MainLayout.class)
public class ManageNotificationsView extends VerticalLayout {

    private final NotificationRecipientService notificationRecipientService;

    private final Grid<NotificationRecipientEntity> grid =
            new Grid<>(NotificationRecipientEntity.class, false);

    private final TextField searchField = new TextField();

    public ManageNotificationsView(NotificationRecipientService notificationRecipientService) {
        this.notificationRecipientService = notificationRecipientService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Notification Management");
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "2rem")
                .set("font-weight", "700")
                .set("color", "#1e293b");

        Span subtitle = new Span("Configure email recipients for workflow statuses and events.");
        subtitle.getStyle()
                .set("color", "#64748b")
                .set("font-size", "0.98rem");

        VerticalLayout titleBlock = new VerticalLayout(title, subtitle);
        titleBlock.setPadding(false);
        titleBlock.setSpacing(false);

        Button addButton = new Button("+ Add Recipient");
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(event -> openRecipientDialog(null));

        HorizontalLayout header = new HorizontalLayout(titleBlock, addButton);
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);

        searchField.setPlaceholder("Search recipients, groups, or email...");
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

        grid.addColumn(NotificationRecipientEntity::getGroupName)
                .setHeader("Status / Event")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(NotificationRecipientEntity::getEmailAddress)
                .setHeader("Recipient")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(recipient ->
                        Boolean.TRUE.equals(recipient.getEnabled())
                                ? "Active"
                                : "Inactive"
                )
                .setHeader("Status")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addComponentColumn(recipient -> {
            Button editButton = new Button("Edit");
            editButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
            editButton.addClickListener(event -> openRecipientDialog(recipient));

            Button deleteButton = new Button("Delete");
            deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(event -> {
                notificationRecipientService.delete(recipient);
                refreshGrid();
            });

            return new HorizontalLayout(editButton, deleteButton);
        }).setHeader("Actions").setAutoWidth(true);
    }

    private void refreshGrid() {
        String filter = searchField.getValue() == null
                ? ""
                : searchField.getValue().trim().toLowerCase();

        grid.setItems(
                notificationRecipientService.findAll().stream()
                        .filter(recipient ->
                                filter.isEmpty()
                                        || contains(recipient.getGroupName(), filter)
                                        || contains(recipient.getEmailAddress(), filter)
                        )
                        .toList()
        );
    }

    private void openRecipientDialog(NotificationRecipientEntity existingRecipient) {
        boolean editMode = existingRecipient != null;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(editMode ? "Edit Recipient" : "Add Recipient");
        dialog.setWidth("520px");

        TextField groupName = new TextField("Notification Group");
        groupName.setWidthFull();
        groupName.setPlaceholder("CASE_COMPLETED, CASE_ERROR, PATIENT_CREATED");

        EmailField emailAddress = new EmailField("Email Address");
        emailAddress.setWidthFull();

        Checkbox enabled = new Checkbox("Enabled", true);

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

    private boolean contains(String value, String filter) {
        return value != null && value.toLowerCase().contains(filter);
    }
}