package com.bms.processing.service;

import com.bms.processing.entity.EmailTemplateEntity;
import com.bms.processing.repository.EmailTemplateRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EmailTemplateService {

    private final EmailTemplateRepository repository;

    public EmailTemplateService(
            EmailTemplateRepository repository
    ) {
        this.repository = repository;
    }

    // used by notification editor
    public Optional<EmailTemplateEntity> findByKey(
            String templateKey
    ) {
        return repository.findByTemplateKey(templateKey);
    }

    public EmailTemplateEntity save(
            EmailTemplateEntity template
    ) {
        return repository.save(template);
    }
}