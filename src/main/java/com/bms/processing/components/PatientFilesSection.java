package com.bms.processing.components;

import com.bms.processing.entity.CaseRecordEntity;
import com.bms.processing.entity.PatientFileEntity;
import com.bms.processing.service.PatientFileService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

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

                add(buildUpload(), grid);
                refresh();
        }

        private void buildGrid() {
                grid.setWidthFull();
                grid.setAllRowsVisible(true);

                grid.addColumn(PatientFileEntity::getOriginalFileName)
                        .setHeader("File Name")
                        .setAutoWidth(true)
                        .setFlexGrow(1);

                grid.addColumn(PatientFileEntity::getFileType)
                        .setHeader("File Type")
                        .setAutoWidth(true);

                grid.addColumn(file ->
                                file.getFileDate() != null
                                        ? file.getFileDate().toString()
                                        : "")
                        .setHeader("File Date")
                        .setAutoWidth(true);

                grid.addColumn(PatientFileEntity::getSource)
                        .setHeader("Source")
                        .setAutoWidth(true);

                grid.addComponentColumn(file -> {
                        Anchor view = new Anchor(
                                "/patient-files/" + file.getId() + "/view",
                                "View"
                        );
                        view.setTarget("_blank");
                        return view;
                }).setHeader("View").setAutoWidth(true);

                grid.addComponentColumn(file -> {
                        Anchor download = new Anchor(
                                "/patient-files/" + file.getId() + "/download",
                                "Download"
                        );
                        download.getElement().setAttribute("download", true);
                        return download;
                }).setHeader("Download").setAutoWidth(true);
        }

        private void refresh() {
                if (record == null || record.getId() == null) {
                        grid.setItems(java.util.List.of());
                        return;
                }

                grid.setItems(patientFileService.findFilesForCase(record.getId()));
        }

        private Upload buildUpload() {
                MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
                Upload upload = new Upload(buffer);

                upload.setAcceptedFileTypes("application/pdf", ".pdf");
                upload.setMaxFiles(1);
                upload.setDropAllowed(true);

                upload.addSucceededListener(event -> {
                        try {
                        patientFileService.saveManualPdf(
                                record.getId(),
                                event.getFileName(),
                                event.getMIMEType(),
                                event.getContentLength(),
                                buffer.getInputStream(event.getFileName()),
                                baseStoragePath
                        );

                        showSuccess("PDF uploaded.");
                        refresh();

                        } catch (Exception ex) {
                        showError(ex.getMessage());
                        }
                });

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
}