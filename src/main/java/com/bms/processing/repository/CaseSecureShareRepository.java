package com.bms.processing.repository;

import com.bms.processing.entity.CaseSecureShareEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CaseSecureShareRepository
        extends JpaRepository<CaseSecureShareEntity, Long> {

    Optional<CaseSecureShareEntity> findByTokenHash(String tokenHash);

    List<CaseSecureShareEntity> findByCaseRecordIdOrderByCreatedAtDesc(
            Long caseRecordId
    );
}