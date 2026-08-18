package com.bms.processing.components;

import com.bms.processing.entity.CaseIssueEntity;
import com.bms.processing.entity.CaseRecordEntity;
import com.bms.processing.model.CaseIssueSource;
import com.bms.processing.model.CaseIssueType;
import com.bms.processing.service.CaseIssueService;
import com.bms.processing.service.CurrentUserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextArea;

public class CaseIssueDialog extends Dialog {

    private final CaseRecordEntity record;
    private final CaseIssueService caseIssueService;
    private final CurrentUserService currentUserService;
    private final Runnable afterSave;

    private final ComboBox<CaseIssueSource> source =
            new ComboBox<>("Issue Source");

    private final ComboBox<CaseIssueType> type =
            new ComboBox<>("Issue Category");

    private final Checkbox blocking =
            new Checkbox("Blocking Issue");

    private final TextArea description =
            new TextArea("Issue Note");

    private final CaseIssueEntity issue;

    public CaseIssueDialog(
            CaseRecordEntity record,
            CaseIssueService caseIssueService,
            CurrentUserService currentUserService,
            Runnable afterSave
    ) {
        this(
                record,
                null,
                caseIssueService,
                currentUserService,
                afterSave
        );
    }
    
    public CaseIssueDialog(
            CaseRecordEntity record,
            CaseIssueEntity issue,
            CaseIssueService caseIssueService,
            CurrentUserService currentUserService,
            Runnable afterSave
    ) {
        this.record = record;
        this.issue = issue;
        this.caseIssueService = caseIssueService;
        this.currentUserService = currentUserService;
        this.afterSave = afterSave;

        setHeaderTitle(
                issue == null
                        ? "Add Issue"
                        : "Issue Details"
        );

        setWidth("600px");

        buildDialog();
    }

    private void buildDialog() {

        source.setItems(CaseIssueSource.values());
        source.setItemLabelGenerator(this::formatEnum);

        type.setItems(CaseIssueType.values());
        type.setItemLabelGenerator(this::formatEnum);

        source.setRequired(true);
        type.setRequired(true);

        blocking.setValue(false);

        description.setRequired(true);
        description.setWidthFull();
        description.setMinHeight("120px");

        if (issue != null) {
            source.setValue(issue.getIssueSource());
            type.setValue(issue.getIssueType());
            blocking.setValue(Boolean.TRUE.equals(issue.getBlocking()));
            description.setValue(
                    issue.getDescription() != null
                            ? issue.getDescription()
                            : ""
            );
        }

        FormLayout form = new FormLayout();
        form.setWidthFull();

        form.add(
                source,
                type,
                blocking,
                description
        );

        form.setColspan(description, 2);

        add(form);

        Button cancel = new Button("Cancel", event -> close());

        if (issue == null) {
            Button save = new Button("Create Issue", event -> createIssue());
            save.addClassName("primary-action");

            getFooter().add(cancel, save);

        } else {
            Button saveChanges = new Button(
                    "Save Changes",
                    event -> updateIssue()
            );

            Button resolve = new Button(
                    "Resolve Issue",
                    event -> openResolveDialog()
            );

            getFooter().add(
                    cancel,
                    saveChanges,
                    resolve
            );
        }
    }

    private void createIssue() {

        if (source.getValue() == null) {
            showError("Issue source is required.");
            return;
        }

        if (type.getValue() == null) {
            showError("Issue category is required.");
            return;
        }

        if (description.getValue() == null
                || description.getValue().isBlank()) {
            showError("Issue note is required.");
            return;
        }

        caseIssueService.createIssue(
                record,
                source.getValue(),
                type.getValue(),
                blocking.getValue(),
                formatEnum(type.getValue()),
                description.getValue().trim(),
                currentUserService.getUsername()
        );

        if (afterSave != null) {
            afterSave.run();
        }

        close();
    }

    private String formatEnum(Enum<?> value) {
        if (value == null) {
            return "";
        }

        String formatted = value.name()
                .replace("_", " ")
                .toLowerCase();

        String[] words = formatted.split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isBlank()) {
                if (!result.isEmpty()) {
                    result.append(" ");
                }

                result.append(
                        Character.toUpperCase(word.charAt(0))
                                + word.substring(1)
                );
            }
        }

        return result.toString();
    }

    private void showError(String message) {
        Notification notification = Notification.show(
                message,
                4000,
                Notification.Position.MIDDLE
        );

        notification.addThemeVariants(
                NotificationVariant.LUMO_ERROR
        );
    }

    private void updateIssue() {

        if (source.getValue() == null) {
            showError("Issue source is required.");
            return;
        }

        if (type.getValue() == null) {
            showError("Issue category is required.");
            return;
        }

        if (description.getValue() == null
                || description.getValue().isBlank()) {
            showError("Issue note is required.");
            return;
        }

        issue.setIssueSource(source.getValue());
        issue.setIssueType(type.getValue());
        issue.setBlocking(blocking.getValue());
        issue.setTitle(formatEnum(type.getValue()));
        issue.setDescription(description.getValue().trim());

        caseIssueService.save(issue);

        if (afterSave != null) {
            afterSave.run();
        }

        close();
    }

    private void openResolveDialog() {

        Dialog resolveDialog = new Dialog();
        resolveDialog.setHeaderTitle("Resolve Issue");
        resolveDialog.setWidth("500px");

        TextArea resolutionNote =
                new TextArea("Resolution Note");

        resolutionNote.setWidthFull();
        resolutionNote.setMinHeight("120px");
        resolutionNote.setRequired(true);

        Button cancel = new Button(
                "Cancel",
                event -> resolveDialog.close()
        );

        Button resolve = new Button(
                "Resolve",
                event -> {

                    if (resolutionNote.getValue() == null
                            || resolutionNote.getValue().isBlank()) {
                        showError("Resolution note is required.");
                        return;
                    }

                    caseIssueService.resolveIssue(
                            issue,
                            currentUserService.getUsername(),
                            resolutionNote.getValue().trim()
                    );

                    if (afterSave != null) {
                        afterSave.run();
                    }

                    resolveDialog.close();
                    close();
                }
        );

        resolveDialog.add(resolutionNote);
        resolveDialog.getFooter().add(cancel, resolve);
        resolveDialog.open();
    }
}