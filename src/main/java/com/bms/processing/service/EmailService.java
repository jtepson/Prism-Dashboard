package com.bms.processing.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDateTime;

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

    // single email with TO + CC recipients
    public void sendEmail(
            List<String> toRecipients,
            List<String> ccRecipients,
            String subject,
            String body
    ) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(
                toRecipients.toArray(new String[0])
        );

        if (!ccRecipients.isEmpty()) {
            message.setCc(
                    ccRecipients.toArray(new String[0])
            );
        }

        message.setFrom("support@prismclinicalimaging.com");
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }

    // sends secure share verification code for temp share - 08252026
    public void sendSecureShareAccessCode(
            String to,
            String recipientName,
            String accessCode
    ) {
        String subject = "Prism Secure File Access - Verification Code";

        String body =
                "Hello " + recipientName + ",\n\n"
                + "Your Prism secure file access verification code is:\n\n"
                + accessCode + "\n\n"
                + "This code expires in 10 minutes.\n\n"
                + "If you did not request this code, you can ignore this email.";

        sendEmail(to, subject, body);
    }

    // sends secure share link
    public void sendSecureShareLink(
            String to,
            String recipientName,
            String shareUrl,
            LocalDateTime expiresAt
    ) {
        String subject = "Prism Secure File Access";

        String body =
                "Hello " + recipientName + ",\n\n"
                + "Prism Clinical Imaging has securely shared files with you.\n\n"
                + "Access the files here:\n"
                + shareUrl + "\n\n"
                + "This secure link expires on "
                + expiresAt.toLocalDate()
                + ".\n\n"
                + "You will be asked to verify your email address with a one-time code before accessing the files.\n\n"
                + "If you were not expecting these files, please disregard this message.";

        sendEmail(
                to,
                subject,
                body
        );
    }
}