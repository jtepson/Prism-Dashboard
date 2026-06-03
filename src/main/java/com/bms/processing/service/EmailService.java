package com.bms.processing.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendTestEmail(String to) {
        sendEmail(
                to,
                "Prism Dashboard Test Email",
                "Hello from Prism Dashboard SMTP test."
        );
    }

    public void sendEmail(
            String to,
            String subject,
            String body
    ) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setFrom("support@prismclinicalimaging.com");
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }
}