package com.bms.processing.service;

import org.springframework.stereotype.Service;
import com.bms.processing.repository.NotificationRecipientRepository;

@Service
public class NotificationService {

    private final EmailService emailService;
    private final NotificationRecipientRepository notificationRecipientRepository;

    //pulls email service to send to specific recipient
    public NotificationService(
            EmailService emailService,
            NotificationRecipientRepository notificationRecipientRepository
    ) {
        this.emailService = emailService;
        this.notificationRecipientRepository = notificationRecipientRepository;
    }

    public void sendTestNotification(String to) {
        emailService.sendTestEmail(to);
    }

    //test sending method, removed personal email and made it db backed
    public void notifyCaseCompleted(String patientName) {
        sendToGroup(
                "CASE_COMPLETED",
                "Prism Dashboard: Case Completed",
                "Case completed for " + patientName
        );
    }

    //group sending method
    public void sendToGroup(
            String groupName,
            String subject,
            String body
    ) {
        notificationRecipientRepository
                .findByGroupNameAndEnabledTrue(groupName)
                .forEach(recipient ->
                        emailService.sendEmail(
                                recipient.getEmailAddress(),
                                subject,
                                body
                        )
                );
    }
}