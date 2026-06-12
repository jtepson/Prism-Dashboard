package com.bms.processing.service;

import com.bms.processing.entity.DicomConfigEntity;
import com.bms.processing.repository.DicomConfigRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DicomConfigService {

    private final DicomConfigRepository repository;

    public DicomConfigService(DicomConfigRepository repository) {
        this.repository = repository;
    }

    public List<DicomConfigEntity> findAll() {
        return repository.findAll();
    }

    public List<DicomConfigEntity> findEnabled() {
        return repository.findByEnabledTrue();
    }

    public Optional<DicomConfigEntity> findDefaultEnabled() {
        return repository.findFirstByEnabledTrueOrderByIdAsc();
    }

    public DicomConfigEntity save(DicomConfigEntity config) {
        validate(config);
        return repository.save(config);
    }

    public void delete(DicomConfigEntity config) {
        repository.delete(config);
    }

    public DicomConfigEntity getActiveConfiguration() {
        return findDefaultEnabled().orElse(null);
    }

    private void validate(DicomConfigEntity config) {
        if (config == null) {
            throw new IllegalArgumentException("DICOM config is required.");
        }

        if (isBlank(config.getConfigName())) {
            throw new IllegalArgumentException("Config name is required.");
        }

        if (isBlank(config.getRemoteAeTitle())) {
            throw new IllegalArgumentException("Remote AE title is required.");
        }

        if (isBlank(config.getRemoteHost())) {
            throw new IllegalArgumentException("Remote host is required.");
        }

        if (config.getRemotePort() == null || config.getRemotePort() <= 0) {
            throw new IllegalArgumentException("Remote port is required.");
        }

        if (isBlank(config.getLocalAeTitle())) {
            throw new IllegalArgumentException("Local AE title is required.");
        }

        if (isBlank(config.getStoragePath())) {
            throw new IllegalArgumentException("Storage path is required.");
        }

        if (config.getEnabled() == null) {
            config.setEnabled(true);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}