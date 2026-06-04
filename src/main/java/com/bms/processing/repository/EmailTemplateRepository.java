package com.bms.processing.repository;

import com.bms.processing.entity.EmailTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailTemplateRepository
        extends JpaRepository<EmailTemplateEntity, Long> {

    Optional<EmailTemplateEntity> findByTemplateKey(
            String templateKey
    );
}