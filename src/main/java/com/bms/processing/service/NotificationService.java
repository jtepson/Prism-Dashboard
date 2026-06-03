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

    //test sending method, removed personal email and made it db backed
    public void notifyCaseCompleted(String patientName) {
        sendToGroup(
                "CASE_COMPLETED",
                "Prism Dashboard: Case Completed",
                "Case completed for " + patientName
        );
    }

    public void notifyPatientCreated(String patientName) {
        sendToGroup(
                "PATIENT_CREATED",
                "Prism Dashboard: Patient Created",
                "Patient created: " + patientName
        );
    }

    public void notifyCaseFinalized(String patientName) {
        sendToGroup(
                "CASE_FINALIZED",
                "Prism Dashboard: Patient Processed",
                "Patient processed and ready for review: " + patientName
        );
    }

    public void notifyCaseError(String patientName, String errorMessage) {
        sendToGroup(
                "CASE_ERROR",
                "Prism Dashboard: Error Reported",
                "Error reported for " + patientName + "\n\n" + errorMessage
        );
    }
}