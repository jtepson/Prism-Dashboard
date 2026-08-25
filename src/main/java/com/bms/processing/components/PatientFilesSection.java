package com.bms.processing.components;

import com.bms.processing.entity.CaseRecordEntity;
import com.bms.processing.entity.PatientFileEntity;
import com.bms.processing.service.PatientFileService;
import com.bms.processing.service.SecureShareService;
import com.bms.processing.service.CurrentUserService;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

public class PatientFilesSection extends VerticalLayout {

        private final CaseRecordEntity record;
        private final PatientFileService patientFileService;
        private final SecureShareService secureShareService;

        private final String baseStoragePath;

        private final Grid<PatientFileEntity> grid =
                new Grid<>(PatientFileEntity.class, false);

        private final CurrentUserService currentUserService;

        public PatientFilesSection(
                CaseRecordEntity record,
                PatientFileService patientFileService,
                SecureShareService secureShareService,
                CurrentUserService currentUserService,
                String baseStoragePath
        ) {
                this.record = record;
                this.patientFileService = patientFileService;
                this.baseStoragePath = baseStoragePath;
                this.secureShareService = secureShareService;
                this.currentUserService = currentUserService;

                setPadding(false);
                setSpacing(true);
                setWidthFull();

                buildGrid();

                Button secureShareButton = new Button(
                        "Share Files...",
                        event -> openSecureShareDialog()
                );

                secureShareButton.getStyle()
                        .set("color", "var(--lumo-primary-text-color)");

                HorizontalLayout fileActions = new HorizontalLayout(
                        buildUpload(),
                        secureShareButton
                );

                fileActions.setPadding(false);
                fileActions.setSpacing(true);
                fileActions.setAlignItems(FlexComponent.Alignment.CENTER);

                add(
                        buildUploadInstructions(),
                        fileActions,
                        grid
                );

                refresh();

                refresh();
        }

        //minimal grid - updated 08202026
        private void buildGrid() {
                grid.setWidthFull();
                grid.setAllRowsVisible(true);

                grid.addColumn(PatientFileEntity::getOriginalFileName)
                        .setHeader("File Name")
                        .setFlexGrow(1);

                grid.addColumn(file ->
                                file.getCreatedAt() != null
                                        ? file.getCreatedAt().toLocalDate().toString()
                                        : ""
                        )
                        .setHeader("Uploaded")
                        .setWidth("130px")
                        .setFlexGrow(0);

                grid.addColumn(file ->
                                file.getUploadedBy() != null
                                        ? file.getUploadedBy()
                                        : ""
                        )
                        .setHeader("Uploaded By")
                        .setWidth("160px")
                        .setFlexGrow(0);

                grid.addComponentColumn(file -> {
                        HorizontalLayout actions = new HorizontalLayout();
                        actions.setPadding(false);
                        actions.setSpacing(true);
                        actions.setAlignItems(
                                com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER
                        );

                        String contentType = file.getContentType();

                        boolean viewable =
                                "application/pdf".equalsIgnoreCase(contentType)
                                        || "image/jpeg".equalsIgnoreCase(contentType)
                                        || "image/png".equalsIgnoreCase(contentType);

                        if (viewable) {
                                Anchor view = new Anchor(
                                        "/patient-files/" + file.getId() + "/view",
                                        "View"
                                );

                                view.setTarget("_blank");

                                view.getStyle()
                                        .set("text-decoration", "none");
                                        
                                actions.add(view);
                        }

                        Anchor download = new Anchor(
                                "/patient-files/" + file.getId() + "/download",
                                "Download"
                        );

                        download.getElement().setAttribute("download", true);

                        download.getStyle()
                                .set("text-decoration", "none");

                        actions.add(download);

                        return actions;
                })
                .setHeader("")
                .setWidth("180px")
                .setFlexGrow(0);
        }

        public void refresh() {
                if (record == null || record.getId() == null) {
                        grid.setItems(java.util.List.of());
                        return;
                }

                grid.setItems(patientFileService.findFilesForCase(record.getId()));
        }

        // new method for expanded uploading logic - updated 08182026
        private Upload buildUpload() {
                MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
                Upload upload = new Upload(buffer);

                String[] acceptedTypes = patientFileService.getAllowedExtensions().stream()
                        .map(extension -> "." + extension)
                        .toArray(String[]::new);

                upload.setAcceptedFileTypes(acceptedTypes);
                upload.setMaxFiles(10);
                upload.setDropAllowed(true);

                long maxSizeBytes = patientFileService.getMaxSizeBytes();
                int maxSizeMb = (int) (maxSizeBytes / (1024L * 1024L));

                upload.setMaxFileSize((int) Math.min(maxSizeBytes, Integer.MAX_VALUE));

                upload.setDropLabel(new Span("Drop patient files here"));

                String allowedTypes = patientFileService.getAllowedExtensions().stream()
                        .map(String::toUpperCase)
                        .collect(Collectors.joining(", "));

                Div instructions = new Div();

                Paragraph allowed = new Paragraph(
                        "Allowed file types: " + allowedTypes
                );

                Paragraph maxSize = new Paragraph(
                        "Maximum file size: " + maxSizeMb + " MB"
                );

                instructions.add(allowed, maxSize);

                allowed.getStyle().set("margin", "0");
                maxSize.getStyle().set("margin", "0");

                upload.addSucceededListener(event -> {
                        try {
                        patientFileService.saveManualFile(
                                record.getId(),
                                event.getFileName(),
                                event.getMIMEType(),
                                event.getContentLength(),
                                buffer.getInputStream(event.getFileName()),
                                baseStoragePath
                        );

                        showSuccess(event.getFileName() + " uploaded.");
                        refresh();

                        } catch (Exception ex) {
                        showError(ex.getMessage());
                        }
                });

                upload.addFileRejectedListener(event ->
                        showError(
                                event.getErrorMessage() != null
                                        ? event.getErrorMessage()
                                        : "File upload was rejected."
                        )
                );

                VerticalLayout wrapper = new VerticalLayout(
                        upload,
                        instructions
                );

                wrapper.setPadding(false);
                wrapper.setSpacing(false);
                wrapper.setWidthFull();

                return upload;
        }

        private void showSuccess(String message) {
                Notification notification = Notification.show(message, 3000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        }

        private void showError(String message) {
                Notification notification = Notification.show(message, 4000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }

        //helper for new upload logic - updated 081822026
        private Component buildUploadInstructions() {
                String allowedTypes = patientFileService.getAllowedExtensions().stream()
                        .map(String::toUpperCase)
                        .collect(Collectors.joining(", "));

                long maxSizeMb =
                        patientFileService.getMaxSizeBytes() / (1024L * 1024L);

                VerticalLayout instructions = new VerticalLayout();
                instructions.setPadding(false);
                instructions.setSpacing(false);

                Span allowed = new Span(
                        "Allowed file types: " + allowedTypes
                );

                Span maxSize = new Span(
                        "Maximum file size: " + maxSizeMb + " MB"
                );

                allowed.getStyle().set("color", "var(--lumo-secondary-text-color)");
                maxSize.getStyle().set("color", "var(--lumo-secondary-text-color)");

                instructions.add(allowed, maxSize);

                return instructions;
        }

        //helper for new columns - updated 08182026
        private String formatFileSize(long bytes) {
                if (bytes < 1024) {
                        return bytes + " B";
                }

                double kb = bytes / 1024.0;

                if (kb < 1024) {
                        return String.format("%.1f KB", kb);
                }

                double mb = kb / 1024.0;

                return String.format("%.1f MB", mb);
        }

        //helper for secure sharing button - 08252026
        private void openSecureShareDialog() {
                List<PatientFileEntity> files =
                        patientFileService.findFilesForCase(record.getId());

                if (files.isEmpty()) {
                        showError("There are no files available to share.");
                        return;
                }

                Dialog dialog = new Dialog();
                dialog.setHeaderTitle("Create Secure Share");
                dialog.setWidth("600px");

                TextField recipientName = new TextField("Recipient Name");
                recipientName.setWidthFull();

                TextField recipientEmail = new TextField("Recipient Email");
                recipientEmail.setWidthFull();

                DatePicker expirationDate = new DatePicker("Expires");
                expirationDate.setValue(
                        java.time.LocalDate.now().plusDays(7)
                );
                expirationDate.setMin(java.time.LocalDate.now());
                expirationDate.setWidthFull();

                Checkbox allowView = new Checkbox("Allow View", true);
                Checkbox allowDownload = new Checkbox("Allow Download", true);

                Grid<PatientFileEntity> fileGrid =
                        new Grid<>(PatientFileEntity.class, false);

                fileGrid.setSelectionMode(
                        Grid.SelectionMode.MULTI
                );

                fileGrid.addColumn(
                        PatientFileEntity::getOriginalFileName
                ).setHeader("File");

                fileGrid.addColumn(
                        PatientFileEntity::getFileType
                ).setHeader("Type")
                        .setWidth("120px")
                        .setFlexGrow(0);

                fileGrid.setItems(files);
                fileGrid.setAllRowsVisible(true);
                fileGrid.setWidthFull();

                Span fileLabel = new Span("Select Files");
                fileLabel.getStyle()
                        .set("font-weight", "600");

                Button createButton = new Button("Create Share");

                createButton.addClickListener(event -> {
                        if (recipientName.isEmpty()) {
                        showError("Recipient name is required.");
                        return;
                        }

                        if (recipientEmail.isEmpty()) {
                        showError("Recipient email is required.");
                        return;
                        }

                        if (expirationDate.getValue() == null) {
                        showError("Expiration date is required.");
                        return;
                        }

                        var selectedFiles =
                                new HashSet<>(fileGrid.getSelectedItems());

                        if (selectedFiles.isEmpty()) {
                        showError("Select at least one file.");
                        return;
                        }

                        try {
                        var created = secureShareService.createShare(
                                record,
                                recipientName.getValue(),
                                recipientEmail.getValue(),
                                selectedFiles,
                                expirationDate.getValue()
                                        .plusDays(1)
                                        .atStartOfDay(),
                                allowView.getValue(),
                                allowDownload.getValue(),
                                null,
                                currentUserService.getUsername()
                        );

                        secureShareService.sendShareLink(created);

                        dialog.close();

                        showShareCreatedDialog(
                                secureShareService.buildShareUrl(
                                        created.rawToken()
                                ),
                                created.share().getRecipientEmail()
                        );

                        } catch (Exception ex) {
                        showError(ex.getMessage());
                        }
                });

                Button cancelButton = new Button(
                        "Cancel",
                        event -> dialog.close()
                );

                HorizontalLayout permissions =
                        new HorizontalLayout(
                                allowView,
                                allowDownload
                        );

                VerticalLayout content = new VerticalLayout(
                        recipientName,
                        recipientEmail,
                        expirationDate,
                        permissions,
                        fileLabel,
                        fileGrid
                );

                content.setPadding(false);
                content.setWidthFull();

                dialog.add(content);

                dialog.getFooter().add(
                        cancelButton,
                        createButton
                );

                dialog.open();
        }

        //result dialog - 08252026
        private void showShareCreatedDialog(
                String shareUrl,
                String recipientEmail
        ) {
                Dialog dialog = new Dialog();
                dialog.setHeaderTitle("Secure Share Created");
                dialog.setWidth("550px");

                Span confirmation = new Span(
                        "Secure access instructions were sent to "
                                + recipientEmail
                                + "."
                );

                confirmation.getStyle()
                        .set("font-weight", "600");

                Span fallback = new Span(
                        "If the recipient does not receive the email, you can copy the secure link below."
                );

                fallback.getStyle()
                        .set("color", "var(--lumo-secondary-text-color)")
                        .set("font-size", "0.9rem");

                TextField shareLink =
                        new TextField("Secure Share Link");

                shareLink.setValue(shareUrl);
                shareLink.setReadOnly(true);
                shareLink.setWidthFull();

                Button copyButton = new Button("Copy Link");

                copyButton.addClickListener(event -> {
                        shareLink.getElement().executeJs(
                                "navigator.clipboard.writeText($0)",
                                shareLink.getValue()
                        );

                        showSuccess("Share link copied.");
                });

                Button closeButton = new Button(
                        "Close",
                        event -> dialog.close()
                );

                VerticalLayout content =
                        new VerticalLayout(
                                confirmation,
                                fallback,
                                shareLink,
                                copyButton
                        );

                content.setPadding(false);
                content.setSpacing(true);
                content.setWidthFull();

                dialog.add(content);
                dialog.getFooter().add(closeButton);

                dialog.open();
        }
}