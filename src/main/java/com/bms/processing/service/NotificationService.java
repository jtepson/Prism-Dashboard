package com.bms.processing.service;

import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final EmailService emailService;

    public NotificationService(EmailService emailService) {
        this.emailService = emailService;
    }

    public void sendTestNotification(String to) {
        emailService.sendTestEmail(to);
    }

    public void notifyCaseCompleted(String patientName) {
        emailService.sendEmail(
                "jepperson@prismclinical.com",
                "Prism Dashboard: Case Completed",
                "Case completed for " + patientName
        );
    }
}