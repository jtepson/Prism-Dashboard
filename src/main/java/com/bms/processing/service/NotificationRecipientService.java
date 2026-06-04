package com.bms.processing.service;

import com.bms.processing.entity.NotificationRecipientEntity;
import com.bms.processing.repository.NotificationRecipientRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationRecipientService {

    private final NotificationRecipientRepository repository;

    public NotificationRecipientService(NotificationRecipientRepository repository) {
        this.repository = repository;
    }

    public List<NotificationRecipientEntity> findAll() {
        return repository.findAll();
    }

    public NotificationRecipientEntity save(NotificationRecipientEntity recipient) {
        return repository.save(recipient);
    }

    public void delete(NotificationRecipientEntity recipient) {
        repository.delete(recipient);
    }
}