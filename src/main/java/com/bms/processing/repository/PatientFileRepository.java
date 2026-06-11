package com.bms.processing.repository;

import com.bms.processing.entity.PatientFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatientFileRepository extends JpaRepository<PatientFileEntity, Long> {

    List<PatientFileEntity> findByCaseRecordIdOrderByFileDateDesc(Long caseRecordId);
}