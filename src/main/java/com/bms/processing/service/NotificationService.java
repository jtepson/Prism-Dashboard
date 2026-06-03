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
}