package com.bms.processing.views.manage;

import com.bms.processing.entity.NotificationRecipientEntity;
import com.bms.processing.layouts.MainLayout;
import com.bms.processing.service.NotificationRecipientService;
import com.bms.processing.entity.EmailTemplateEntity;
import com.bms.processing.service.EmailTemplateService;
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
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
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
    private final EmailTemplateService emailTemplateService;

    private final Grid<NotificationRuleRow> grid =
            new Grid<>(NotificationRuleRow.class, false);

    private final TextField searchField = new TextField();

    public ManageNotificationsView(
            NotificationRecipientService notificationRecipientService,
            EmailTemplateService emailTemplateService
    ) {
        this.notificationRecipientService = notificationRecipientService;
        this.emailTemplateService = emailTemplateService;

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

        grid.addComponentColumn(row -> {

            int enabledCount = (int) getRecipientsForGroup(row.groupName())
                    .stream()
                    .filter(r -> Boolean.TRUE.equals(r.getEnabled()))
                    .count();

            Span badge = new Span(
                    enabledCount > 0 ? "Active" : "Inactive"
            );

            badge.getStyle()
                    .set("padding", "4px 10px")
                    .set("border-radius", "999px")
                    .set("font-size", "0.75rem")
                    .set("font-weight", "600")
                    .set("background",
                            enabledCount > 0
                                    ? "#dcfce7"
                                    : "#fee2e2")
                    .set("color",
                            enabledCount > 0
                                    ? "#166534"
                                    : "#991b1b");

            return badge;

        }).setHeader("Status")
        .setAutoWidth(true);

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

        List<NotificationRecipientEntity> recipients =
                getRecipientsForGroup(row.groupName());

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);

        Span count = new Span(
                recipients.size() + " recipient"
                        + (recipients.size() == 1 ? "" : "s")
        );

        count.getStyle()
                .set("font-weight", "600")
                .set("color", "#0f172a");

        String preview = recipients.stream()
                .map(NotificationRecipientEntity::getEmailAddress)
                .limit(2)
                .reduce((a, b) -> a + ", " + b)
                .orElse("No recipients configured");

        Span emails = new Span(preview);

        emails.getStyle()
                .set("font-size", "0.8rem")
                .set("color", "#64748b");

        layout.add(count, emails);

        return layout;
    }

    private Component buildActionsCell(NotificationRuleRow row) {
        Button editButton = new Button("Edit");
        editButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        editButton.addClickListener(event -> openRuleDialog(row));

        return editButton;
    }

    private Component buildRuleDetails(NotificationRuleRow row) {

        VerticalLayout details = new VerticalLayout();
        details.setPadding(true);
        details.setSpacing(true);

        details.getStyle()
                .set("border", "1px solid #e2e8f0")
                .set("border-radius", "10px")
                .set("background", "#f8fafc");

        H3 title = new H3("Rule Details");
        title.getStyle().set("margin", "0");

        details.add(
                title,
                createDetailRow("Status / Event", row.label()),
                createDetailRow("Notification Group", row.groupName()),
                createDetailRow("Description", row.description()),
                createDetailRow("Recipients",
                        String.valueOf(
                                getRecipientsForGroup(row.groupName()).size()
                        ))
        );

        return details;
    }

    private Component createDetailRow(String label, String value) {

        VerticalLayout row = new VerticalLayout();
        row.setPadding(false);
        row.setSpacing(false);

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "0.75rem")
                .set("color", "#64748b");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-weight", "600")
                .set("color", "#0f172a");

        row.add(labelSpan, valueSpan);

        return row;
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
        dialog.setWidth("1100px");

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
        recipientGrid.setHeight("360px");
        recipientGrid.setWidthFull();

        Button addRecipientButton = new Button("+ Add Recipient");
        addRecipientButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addRecipientButton.addClickListener(event -> {
            dialog.close();
            openRecipientDialog(null, row.groupName());
        });

        Tab recipientsTab = new Tab("Recipients");
        Tab settingsTab = new Tab("Settings");
        Tab previewTab = new Tab("Email Preview");
        Tab activityTab = new Tab("Activity");

        Tabs tabs = new Tabs(
                recipientsTab,
                settingsTab,
                previewTab,
                activityTab
        );

        tabs.setWidthFull();

        VerticalLayout recipientsContent = new VerticalLayout(
                addRecipientButton,
                recipientGrid
        );

        recipientsContent.setPadding(false);
        recipientsContent.setSpacing(true);

        Span placeholder = new Span("This section will be added in a later step.");
        placeholder.getStyle()
                .set("color", "#64748b")
                .set("font-size", "0.9rem");

        VerticalLayout tabContent = new VerticalLayout(recipientsContent);
        tabContent.setPadding(false);
        tabContent.setSpacing(true);

        tabs.addSelectedChangeListener(event -> {
            tabContent.removeAll();

            if (event.getSelectedTab() == recipientsTab) {
                tabContent.add(recipientsContent);
            } else if (event.getSelectedTab() == settingsTab) {
                tabContent.add(buildSettingsTab(row));
            } else if (event.getSelectedTab() == previewTab) {
                tabContent.add(buildEmailPreviewTab(row));
            } else {
                tabContent.add(buildActivityTab(row));
            }
        });

        VerticalLayout leftSide = new VerticalLayout(
                title,
                description,
                tabs,
                tabContent
        );

        leftSide.setPadding(false);
        leftSide.setSpacing(true);
        leftSide.setWidthFull();

        Component detailsPanel =
                buildRuleDetails(row);

        HorizontalLayout content =
                new HorizontalLayout(
                        leftSide,
                        detailsPanel
                );

        content.setWidthFull();

        leftSide.setWidth("700px");
        detailsPanel.getElement()
                .getStyle()
                .set("min-width", "360px")
                .set("max-width", "360px");

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

    private Component buildSettingsTab(NotificationRuleRow row) {
        // rule-level options will eventually persist
        Checkbox enabled =
                new Checkbox("Enable this notification rule", true);

        enabled.setReadOnly(true);

        // future: immediate vs digest delivery
        Checkbox sendImmediately =
                new Checkbox("Send notifications immediately", true);

        sendImmediately.setReadOnly(true);

        // future: allow turning emails off
        Checkbox emailEnabled =
                new Checkbox("Email delivery enabled", true);

        emailEnabled.setReadOnly(true);

        Span note =
                new Span(
                        "Settings are currently placeholders until notification rule storage is implemented."
                );

        note.getStyle()
                .set("color", "#64748b");

        VerticalLayout layout =
                new VerticalLayout(
                        enabled,
                        sendImmediately,
                        emailEnabled,
                        note
                );

        layout.setPadding(false);
        layout.setSpacing(true);

        return layout;
    }

    // editable email template for this rule
    private Component buildEmailPreviewTab(NotificationRuleRow row) {

        EmailTemplateEntity template =
                emailTemplateService.findByKey(row.groupName())
                        .orElseGet(() -> {
                            EmailTemplateEntity created =
                                    new EmailTemplateEntity();

                            created.setTemplateKey(row.groupName());
                            created.setSubject(getPreviewSubject(row));
                            created.setBody(getPreviewBody(row));
                            created.setEnabled(true);

                            return created;
                        });

        TextField subjectField =
                new TextField("Email Subject");

        subjectField.setValue(template.getSubject());
        subjectField.setWidthFull();

        TextArea bodyField =
                new TextArea("Email Body");

        bodyField.setValue(template.getBody());
        bodyField.setWidthFull();
        bodyField.setHeight("260px");

        // these are the variables we support first
        Span helper =
                new Span("Available variables: {{patientName}}, {{patientId}}, {{status}}, {{errorMessage}}");

        helper.getStyle()
                .set("font-size", "0.8rem")
                .set("color", "#64748b");

        Button saveTemplateButton =
                new Button("Save Template");

        saveTemplateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        saveTemplateButton.addClickListener(event -> {
            template.setSubject(subjectField.getValue().trim());
            template.setBody(bodyField.getValue().trim());
            template.setEnabled(true);

            emailTemplateService.save(template);
        });

        VerticalLayout templateEditor =
                new VerticalLayout(
                        subjectField,
                        bodyField,
                        helper,
                        saveTemplateButton
                );

        templateEditor.setPadding(true);
        templateEditor.setSpacing(true);
        templateEditor.setWidthFull();

        templateEditor.getStyle()
                .set("border", "1px solid #e2e8f0")
                .set("border-radius", "10px")
                .set("background", "#ffffff");

        return templateEditor;
    }

    // placeholder until audit events are wired in
    private Component buildActivityTab(NotificationRuleRow row) {
        Span activity = new Span("No recent activity logged for this notification rule.");

        activity.getStyle()
                .set("color", "#64748b")
                .set("font-size", "0.9rem");

        return activity;
    }

    // temporary subject mapping per workflow event
    private String getPreviewSubject(NotificationRuleRow row) {

        return switch (row.groupName()) {
            case "PATIENT_CREATED" ->
                    "Prism Dashboard: Patient Created";
            case "CASE_FINALIZED" ->
                    "Prism Dashboard: Patient Processed";
            case "CASE_COMPLETED" ->
                    "Prism Dashboard: Case Completed";
            case "CASE_ERROR" ->
                    "Prism Dashboard: Error Reported";
            default ->
                    "Prism Dashboard: Notification";
        };
    }

    // temporary body mapping per workflow event
    private String getPreviewBody(NotificationRuleRow row) {

        return switch (row.groupName()) {
            case "PATIENT_CREATED" ->
                    """
                    A new patient has been created in Prism Dashboard.

                    Patient: {{patientName}}
                    Status: Upcoming

                    Please review intake details when available.
                    """;
            case "CASE_FINALIZED" ->
                    """
                    A patient has been processed and is ready for review.

                    Patient: {{patientName}}
                    Status: Processed

                    Please review the processed case.
                    """;
            case "CASE_COMPLETED" ->
                    """
                    A patient case has been marked completed.

                    Patient: {{patientName}}
                    Status: Completed

                    No further action is required.
                    """;
            case "CASE_ERROR" ->
                    """
                    An error has been reported for a patient case.

                    Patient: {{patientName}}
                    Error: {{errorMessage}}

                    Please review the case and resolve the issue.
                    """;
            default ->
                    "A Prism Dashboard notification event occurred.";
        };
    }
}