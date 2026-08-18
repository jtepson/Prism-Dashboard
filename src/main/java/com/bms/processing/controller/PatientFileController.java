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

                MediaType mediaType = resolveMediaType(file);

                return ResponseEntity.ok()
                        .header(
                                HttpHeaders.CONTENT_DISPOSITION,
                                ContentDisposition.inline()
                                        .filename(file.getOriginalFileName())
                                        .build()
                                        .toString()
                        )
                        .contentType(mediaType)
                        .body(resource);
        }

        @GetMapping("/{id}/download")
        public ResponseEntity<Resource> download(@PathVariable Long id) {

                PatientFileEntity file = repository.findById(id)
                        .orElseThrow();

                Resource resource =
                        new FileSystemResource(Path.of(file.getStoragePath()));

                MediaType mediaType = resolveMediaType(file);

                return ResponseEntity.ok()
                        .header(
                                HttpHeaders.CONTENT_DISPOSITION,
                                ContentDisposition.attachment()
                                        .filename(file.getOriginalFileName())
                                        .build()
                                        .toString()
                        )
                        .contentType(mediaType)
                        .body(resource);
        }

        private MediaType resolveMediaType(PatientFileEntity file) {

                if (file.getContentType() == null || file.getContentType().isBlank()) {
                return MediaType.APPLICATION_OCTET_STREAM;
                }

                try {
                return MediaType.parseMediaType(file.getContentType());
                } catch (IllegalArgumentException ex) {
                return MediaType.APPLICATION_OCTET_STREAM;
                }
        }
}