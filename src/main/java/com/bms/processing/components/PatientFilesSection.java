package com.bms.processing.components;

import com.bms.processing.entity.CaseRecordEntity;
import com.bms.processing.entity.PatientFileEntity;
import com.bms.processing.service.PatientFileService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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

public class PatientFilesSection extends VerticalLayout {

        private final CaseRecordEntity record;
        private final PatientFileService patientFileService;
        private final String baseStoragePath;

        private final Grid<PatientFileEntity> grid =
                new Grid<>(PatientFileEntity.class, false);

        public PatientFilesSection(
                CaseRecordEntity record,
                PatientFileService patientFileService,
                @Value("${prism.files.storage-path}") String baseStoragePath
        ) {
                this.record = record;
                this.patientFileService = patientFileService;
                this.baseStoragePath = baseStoragePath;

                setPadding(false);
                setSpacing(true);
                setWidthFull();

                buildGrid();

                add(buildUploadInstructions(), buildUpload(), grid);
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
}