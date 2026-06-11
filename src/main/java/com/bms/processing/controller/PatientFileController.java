package com.bms.processing.controller;

import com.bms.processing.entity.PatientFileEntity;
import com.bms.processing.repository.PatientFileRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;

@RestController
@RequestMapping("/patient-files")
public class PatientFileController {

    private final PatientFileRepository repository;

    public PatientFileController(PatientFileRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{id}/view")
    public ResponseEntity<Resource> view(@PathVariable Long id) {

        PatientFileEntity file = repository.findById(id)
                .orElseThrow();

        Resource resource =
                new FileSystemResource(Path.of(file.getStoragePath()));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {

        PatientFileEntity file = repository.findById(id)
                .orElseThrow();

        Resource resource =
                new FileSystemResource(Path.of(file.getStoragePath()));

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(file.getOriginalFileName())
                                .build()
                                .toString()
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}