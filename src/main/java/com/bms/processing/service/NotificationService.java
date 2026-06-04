package com.bms.processing.service;

import com.bms.processing.entity.EmailTemplateEntity;
import com.bms.processing.repository.NotificationRecipientRepository;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final EmailService emailService;
    private final NotificationRecipientRepository notificationRecipientRepository;
    private final EmailTemplateService emailTemplateService;

    // pulls email service, recipients, and saved templates
    public NotificationService(
            EmailService emailService,
            NotificationRecipientRepository notificationRecipientRepository,
            EmailTemplateService emailTemplateService
    ) {
        this.emailService = emailService;
        this.notificationRecipientRepository = notificationRecipientRepository;
        this.emailTemplateService = emailTemplateService;
    }

    public void sendTestNotification(String to) {
        emailService.sendTestEmail(to);
    }

    // group sending method with final subject/body
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

    // template-backed send path
    private void sendTemplateToGroup(
            String groupName,
            String fallbackSubject,
            String fallbackBody,
            String patientName,
            String patientId,
            String status,
            String errorMessage
    ) {
        EmailTemplateEntity template =
                emailTemplateService.findByKey(groupName)
                        .orElse(null);

        // use UI template if present, otherwise old safe default
        String subject = template != null
                ? template.getSubject()
                : fallbackSubject;

        String body = template != null
                ? template.getBody()
                : fallbackBody;

        subject = replaceTemplateVariables(
                subject,
                patientName,
                patientId,
                status,
                errorMessage
        );

        body = replaceTemplateVariables(
                body,
                patientName,
                patientId,
                status,
                errorMessage
        );

        sendToGroup(groupName, subject, body);
    }

    // simple placeholder replacement for now
    private String replaceTemplateVariables(
            String value,
            String patientName,
            String patientId,
            String status,
            String errorMessage
    ) {
        if (value == null) {
            return "";
        }

        return value
                .replace("{{patientName}}", safe(patientName))
                .replace("{{patientId}}", safe(patientId))
                .replace("{{status}}", safe(status))
                .replace("{{errorMessage}}", safe(errorMessage));
    }

    // keeps emails from saying null
    private String safe(String value) {
        return value == null ? "" : value;
    }

    public void notifyCaseCompleted(String patientName) {
        sendTemplateToGroup(
                "CASE_COMPLETED",
                "Prism Dashboard: Case Completed",
                "Case completed for {{patientName}}",
                patientName,
                "",
                "Completed",
                ""
        );
    }

    public void notifyPatientCreated(String patientName) {
        sendTemplateToGroup(
                "PATIENT_CREATED",
                "Prism Dashboard: Patient Created",
                "Patient created: {{patientName}}",
                patientName,
                "",
                "Upcoming",
                ""
        );
    }

    public void notifyCaseFinalized(String patientName) {
        sendTemplateToGroup(
                "CASE_FINALIZED",
                "Prism Dashboard: Patient Processed",
                "Patient processed and ready for review: {{patientName}}",
                patientName,
                "",
                "Processed",
                ""
        );
    }

    public void notifyCaseError(String patientName, String errorMessage) {
        sendTemplateToGroup(
                "CASE_ERROR",
                "Prism Dashboard: Error Reported",
                "Error reported for {{patientName}}\n\n{{errorMessage}}",
                patientName,
                "",
                "Error",
                errorMessage
        );
    }
}