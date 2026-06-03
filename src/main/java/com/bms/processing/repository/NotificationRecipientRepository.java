package com.bms.processing.repository;

import com.bms.processing.entity.NotificationRecipientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRecipientRepository
        extends JpaRepository<NotificationRecipientEntity, Long> {

    List<NotificationRecipientEntity> findByGroupNameAndEnabledTrue(
            String groupName
    );
}