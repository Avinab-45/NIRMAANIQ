package com.construction.service;

import com.construction.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@nirmaaniq.com}")
    private String mailFrom;

    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (MailException e) {
            log.error("Failed to deliver email to {}: {}", to, e.getMessage());
            throw new ApiException("Failed to send email. Please check your SMTP configuration or try again later.",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Unexpected error delivering email to {}: {}", to, e.getMessage());
            throw new ApiException("An unexpected error occurred while sending email.",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void sendOtpEmail(String to, String otp) {
        String subject = "NirmaanIQ - Contractor Verification OTP";
        String body = String.format(
                "Hello,\n\n" +
                "Your one-time verification code for NirmaanIQ Contractor access is:\n\n" +
                "  %s\n\n" +
                "This code will expire in 5 minutes.\n" +
                "Please do not share this OTP with anyone.\n\n" +
                "Regards,\n" +
                "The NirmaanIQ Construction Intelligence Team",
                otp
        );
        // Do not log the OTP value in logs for security compliance
        log.info("Initiating contractor OTP email delivery to: {}", to);
        sendEmail(to, subject, body);
    }
}
