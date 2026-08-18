package com.bms.processing.service;

import com.bms.processing.entity.PatientFileEntity;
import com.bms.processing.repository.PatientFileRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class PatientFileService {

    private final PatientFileRepository repository;
    private final CaseRecordService caseRecordService;
    private final List<String> allowedExtensions;
    private final long maxSizeBytes;
    private final CurrentUserService currentUserService;

    //updated constructer for new centralization for uploading mechanics - 08182026
    public PatientFileService(
            PatientFileRepository repository,
            CaseRecordService caseRecordService,
            CurrentUserService currentUserService,
            @Value("${prism.files.allowed-extensions}") String allowedExtensions,
            @Value("${prism.files.max-size-mb}") long maxSizeMb
    ) {
        this.repository = repository;
        this.caseRecordService = caseRecordService;
        this.currentUserService = currentUserService;

        this.allowedExtensions = Arrays.stream(allowedExtensions.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(value -> !value.isBlank())
                .toList();

        this.maxSizeBytes = maxSizeMb * 1024L * 1024L;
    }

    public List<PatientFileEntity> findFilesForCase(Long caseRecordId) {
        return repository.findByCaseRecordIdOrderByFileDateDesc(caseRecordId);
    }

    // removing previous pdf-restricted uploading component for this new one - updated 08182026
    public PatientFileEntity saveManualFile(
            Long caseRecordId,
            String originalFilename,
            String contentType,
            long fileSize,
            InputStream inputStream,
            String baseStoragePath
    ) throws IOException {

        if (caseRecordId == null) {
            throw new IllegalArgumentException("Case record ID is required.");
        }

        if (inputStream == null) {
            throw new IllegalArgumentException("A file is required.");
        }

        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Original filename is required.");
        }

        if (fileSize <= 0) {
            throw new IllegalArgumentException("File is empty.");
        }

        if (fileSize > maxSizeBytes) {
            throw new IllegalArgumentException(
                    "File exceeds the maximum allowed size."
            );
        }

        String extension = getExtension(originalFilename);

        if (extension == null || !allowedExtensions.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Unsupported file type: " + originalFilename
            );
        }

        Path caseDirectory = Path.of(
                baseStoragePath,
                "patient-files",
                caseRecordId.toString()
        );

        Files.createDirectories(caseDirectory);

        String storedFileName =
                UUID.randomUUID() + "." + extension.toLowerCase();

        Path targetPath = caseDirectory.resolve(storedFileName);

        Files.copy(
                inputStream,
                targetPath,
                StandardCopyOption.REPLACE_EXISTING
        );

        if (!Files.exists(targetPath)) {
            throw new IOException("File storage failed.");
        }

        PatientFileEntity entity = new PatientFileEntity();
        entity.setCaseRecordId(caseRecordId);
        entity.setFileName(storedFileName);
        entity.setOriginalFileName(originalFilename);
        entity.setFileType(determineFileType(extension));
        entity.setSource("Manual Upload");
        entity.setFileDate(LocalDate.now());
        entity.setContentType(
                contentType == null || contentType.isBlank()
                        ? "application/octet-stream"
                        : contentType
        );
        entity.setFileSize(fileSize);
        entity.setStoragePath(targetPath.toString());
        entity.setUploadedBy(currentUserService.getUsername());

        return repository.save(entity);
    }

    public PatientFileEntity saveManualPdf(
            Long caseRecordId,
            String originalFilename,
            String contentType,
            long fileSize,
            InputStream inputStream,
            String baseStoragePath
    ) throws IOException {

        if (caseRecordId == null) {
            throw new IllegalArgumentException("Case record ID is required.");
        }

        if (inputStream == null) {
            throw new IllegalArgumentException("A PDF file is required.");
        }

        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed.");
        }

        Path caseDirectory = Path.of(baseStoragePath, "patient-files", caseRecordId.toString());
        Files.createDirectories(caseDirectory);

        String storedFileName = UUID.randomUUID() + ".pdf";
        Path targetPath = caseDirectory.resolve(storedFileName);

        Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

        PatientFileEntity entity = new PatientFileEntity();
        entity.setCaseRecordId(caseRecordId);
        entity.setFileName(storedFileName);
        entity.setOriginalFileName(originalFilename);
        entity.setFileType("Report");
        entity.setSource("Manual Upload");
        entity.setFileDate(LocalDate.now());
        entity.setContentType(contentType == null || contentType.isBlank() ? "application/pdf" : contentType);
        entity.setFileSize(fileSize);
        entity.setStoragePath(targetPath.toString());

        return repository.save(entity);
    }

    public PatientFileEntity registerRetrievedReport(
            Long caseRecordId,
            Path pdfPath,
            String originalFileName,
            String source
    ) throws IOException {

        if (caseRecordId == null) {
            throw new IllegalArgumentException("Case record ID is required.");
        }

        if (pdfPath == null || !Files.exists(pdfPath)) {
            throw new IllegalArgumentException("PDF path does not exist.");
        }

        PatientFileEntity entity = new PatientFileEntity();
        entity.setCaseRecordId(caseRecordId);
        entity.setFileName(pdfPath.getFileName().toString());
        entity.setOriginalFileName(originalFileName);
        entity.setFileType("Report");
        entity.setSource(source == null || source.isBlank() ? "DICOM" : source);
        entity.setFileDate(LocalDate.now());
        entity.setContentType("application/pdf");
        entity.setFileSize(Files.size(pdfPath));
        entity.setStoragePath(pdfPath.toString());

        PatientFileEntity savedFile = repository.save(entity);
        autoUpdateThirdPartyStatus(savedFile);
        return savedFile;
    }

    public List<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    private void autoUpdateThirdPartyStatus(PatientFileEntity file) {
        if (file == null) {
            return;
        }

        if ("IMEKA".equalsIgnoreCase(file.getSource())
                && "Report".equalsIgnoreCase(file.getFileType())) {

            // IMEKA report landed in Patient Files so status should follow it and get updated - updated 6252026
            caseRecordService.markImekaUploadedFromReport(file.getCaseRecordId());
        }
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');

        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return null;
        }

        return filename.substring(dotIndex + 1);
    }

    private String determineFileType(String extension) {
        return switch (extension.toLowerCase()) {
            case "pdf" -> "PDF";
            case "docx" -> "Document";
            case "xlsx" -> "Spreadsheet";
            case "csv" -> "CSV";
            case "jpg", "jpeg", "png" -> "Image";
            default -> "Other";
        };
    }
}