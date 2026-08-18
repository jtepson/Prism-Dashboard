package com.bms.processing.service;

import com.bms.processing.entity.EmailTemplateEntity;
import com.bms.processing.repository.NotificationRecipientRepository;
import com.bms.processing.entity.CaseRecordEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final EmailService emailService;
    private final NotificationRecipientRepository notificationRecipientRepository;
    private final EmailTemplateService emailTemplateService;
    private final CaseIssueService caseIssueService;

    // pulls email service, recipients, and saved templates
    public NotificationService(
            EmailService emailService,
            NotificationRecipientRepository notificationRecipientRepository,
            EmailTemplateService emailTemplateService,
            CaseIssueService caseIssueService
    ) {
        this.emailService = emailService;
        this.notificationRecipientRepository = notificationRecipientRepository;
        this.emailTemplateService = emailTemplateService;
        this.caseIssueService = caseIssueService;
    }

    public void sendTestNotification(String to) {
        emailService.sendTestEmail(to);
    }

    // group sending method with final subject/body. Sends one email per workflow event updated 6082026
    public void sendToGroup(
                String groupName,
                String subject,
                String body
        ) {
        List<String> toRecipients =
                notificationRecipientRepository
                        .findByGroupNameAndEnabledTrue(groupName)
                        .stream()
                        .filter(recipient ->
                                !"CC".equalsIgnoreCase(recipient.getRecipientType())
                        )
                        .map(recipient -> recipient.getEmailAddress())
                        .toList();

        List<String> ccRecipients =
                notificationRecipientRepository
                        .findByGroupNameAndEnabledTrue(groupName)
                        .stream()
                        .filter(recipient ->
                                "CC".equalsIgnoreCase(recipient.getRecipientType())
                        )
                        .map(recipient -> recipient.getEmailAddress())
                        .toList();

        // do not send if nobody is in TO
        if (toRecipients.isEmpty()) {
                return;
        }

        emailService.sendEmail(
                toRecipients,
                ccRecipients,
                subject,
                body
        );
    }

    public void notifyCaseFinalized(CaseRecordEntity record) {
        String issueSummary = caseIssueService.buildActiveIssueSummary(record);

        sendTemplateToGroup(
                "CASE_FINALIZED",
                "Prism Dashboard: Patient Processed",
                "Patient processed and ready for review: {{patientName}}\n\n{{issueSummary}}",
                record.getPatientFirstName() + " " + record.getPatientLastName(),
                record.getPatientId(),
                record.getSiteName(),
                "Processed",
                "",
                issueSummary
        );
    }

    public void notifyCaseCompleted(CaseRecordEntity record) {
        String issueSummary = caseIssueService.buildActiveIssueSummary(record);

        sendTemplateToGroup(
                "CASE_COMPLETED",
                "Prism Dashboard: Case Completed",
                "Case completed for {{patientName}}\n\n{{issueSummary}}",
                record.getPatientFirstName() + " " + record.getPatientLastName(),
                record.getPatientId(),
                record.getSiteName(),
                "Completed",
                "",
                issueSummary
        );
    }

    // template-backed send path
    private void sendTemplateToGroup(
            String groupName,
            String fallbackSubject,
            String fallbackBody,
            String patientName,
            String patientId,
            String sitename,
            String status,
            String errorMessage,
            String issueSummary
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
                errorMessage,
                sitename,
                issueSummary
        );

        body = replaceTemplateVariables(
                body,
                patientName,
                patientId,
                status,
                sitename,
                errorMessage,
                issueSummary
        );

        sendToGroup(groupName, subject, body);
    }

    // placeholder replacement for now
    private String replaceTemplateVariables(
            String value,
            String patientName,
            String patientId,
            String status,
            String errorMessage,
            String siteName,
            String issueSummary
    ) {
        if (value == null) {
            return "";
        }

        return value
                .replace("{{patientName}}", safe(patientName))
                .replace("{{patientId}}", safe(patientId))
                .replace("{{status}}", safe(status))
                .replace("{{errorMessage}}", safe(errorMessage))
                .replace("{{siteName}}", safe(siteName))
                .replace("{{issueSummary}}", safe(issueSummary));
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
                "",
                "Completed",
                "",
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
                "",
                "Upcoming",
                "",
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
                "",
                "Processed",
                "",
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
                "",
                "Error",
                errorMessage,
                ""
        );
    }
}