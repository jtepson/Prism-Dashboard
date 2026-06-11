package com.bms.processing.service;

import com.bms.processing.entity.PatientFileEntity;
import com.bms.processing.repository.PatientFileRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PatientFileService {

    private final PatientFileRepository repository;

    public PatientFileService(PatientFileRepository repository) {
        this.repository = repository;
    }

    public List<PatientFileEntity> findFilesForCase(Long caseRecordId) {
        return repository.findByCaseRecordIdOrderByFileDateDesc(caseRecordId);
    }

    public PatientFileEntity saveManualPdf(
            Long caseRecordId,
            MultipartFile file,
            String baseStoragePath
    ) throws IOException {

        if (caseRecordId == null) {
            throw new IllegalArgumentException("Case record ID is required.");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A PDF file is required.");
        }

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed.");
        }

        Path caseDirectory = Path.of(baseStoragePath, "patient-files", caseRecordId.toString());
        Files.createDirectories(caseDirectory);

        String storedFileName = UUID.randomUUID() + ".pdf";
        Path targetPath = caseDirectory.resolve(storedFileName);

        file.transferTo(targetPath);

        PatientFileEntity entity = new PatientFileEntity();
        entity.setCaseRecordId(caseRecordId);
        entity.setFileName(storedFileName);
        entity.setOriginalFileName(originalFilename);
        entity.setFileType("Report");
        entity.setSource("Manual Upload");
        entity.setFileDate(LocalDate.now());
        entity.setContentType(file.getContentType());
        entity.setFileSize(file.getSize());
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

        return repository.save(entity);
    }
}