package com.bms.processing.repository;

import com.bms.processing.entity.DicomConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DicomConfigRepository extends JpaRepository<DicomConfigEntity, Long> {

    List<DicomConfigEntity> findByEnabledTrue();

    Optional<DicomConfigEntity> findFirstByEnabledTrueOrderByIdAsc();

    Optional<DicomConfigEntity> findByConfigNameIgnoreCase(String configName);
}