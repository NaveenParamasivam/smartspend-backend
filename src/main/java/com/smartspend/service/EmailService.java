package com.smartspend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Async
    public void sendVerificationEmail(String toEmail, String firstName, String token) {
        String subject = "Verify your SmartSpend account";
        String verifyUrl = frontendUrl + "/verify-email?token=" + token;
        String html = buildVerificationEmail(firstName, verifyUrl);
        sendHtmlEmail(toEmail, subject, html);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String firstName, String token) {
        String subject = "Reset your SmartSpend password";
        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        String html = buildPasswordResetEmail(firstName, resetUrl);
        sendHtmlEmail(toEmail, subject, html);
    }

    @Async
    public void sendBudgetAlertEmail(String toEmail, String firstName, String category,
                                      double percentage, String month) {
        String subject = "Budget Alert - " + category + " spending at " + (int) percentage + "%";
        String html = buildBudgetAlertEmail(firstName, category, percentage, month);
        sendHtmlEmail(toEmail, subject, html);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String buildVerificationEmail(String name, String verifyUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: 'Helvetica Neue', Arial, sans-serif; background:#f4f6f9; margin:0; padding:20px;">
              <div style="max-width:600px; margin:0 auto; background:#fff; border-radius:12px; overflow:hidden; box-shadow:0 4px 20px rgba(0,0,0,0.1);">
                <div style="background:linear-gradient(135deg, #1a1a2e 0%%, #16213e 100%%); padding:40px 30px; text-align:center;">
                  <h1 style="color:#00d4ff; margin:0; font-size:28px; font-weight:700; letter-spacing:2px;">SmartSpend</h1>
                  <p style="color:#94a3b8; margin:8px 0 0; font-size:14px;">Your Financial Intelligence Hub</p>
                </div>
                <div style="padding:40px 30px;">
                  <h2 style="color:#1a1a2e; margin:0 0 16px;">Welcome, %s! 👋</h2>
                  <p style="color:#64748b; line-height:1.6; margin:0 0 24px;">Thanks for signing up for SmartSpend. Please verify your email address to activate your account and start tracking your expenses.</p>
                  <div style="text-align:center; margin:32px 0;">
                    <a href="%s" style="background:linear-gradient(135deg, #00d4ff, #0099ff); color:#fff; padding:14px 36px; border-radius:8px; text-decoration:none; font-weight:600; font-size:16px; display:inline-block;">Verify Email Address</a>
                  </div>
                  <p style="color:#94a3b8; font-size:13px; margin:24px 0 0;">This link expires in 24 hours. If you didn't create an account, you can safely ignore this email.</p>
                  <p style="color:#94a3b8; font-size:12px; margin:8px 0 0; word-break:break-all;">Or copy this link: %s</p>
                </div>
                <div style="background:#f8fafc; padding:20px 30px; text-align:center; border-top:1px solid #e2e8f0;">
                  <p style="color:#94a3b8; margin:0; font-size:12px;">© 2024 SmartSpend. All rights reserved.</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(name, verifyUrl, verifyUrl);
    }

    private String buildPasswordResetEmail(String name, String resetUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: 'Helvetica Neue', Arial, sans-serif; background:#f4f6f9; margin:0; padding:20px;">
              <div style="max-width:600px; margin:0 auto; background:#fff; border-radius:12px; overflow:hidden; box-shadow:0 4px 20px rgba(0,0,0,0.1);">
                <div style="background:linear-gradient(135deg, #1a1a2e 0%%, #16213e 100%%); padding:40px 30px; text-align:center;">
                  <h1 style="color:#00d4ff; margin:0; font-size:28px; font-weight:700; letter-spacing:2px;">SmartSpend</h1>
                </div>
                <div style="padding:40px 30px;">
                  <h2 style="color:#1a1a2e; margin:0 0 16px;">Password Reset Request 🔐</h2>
                  <p style="color:#64748b; line-height:1.6; margin:0 0 24px;">Hi %s, we received a request to reset your SmartSpend password. Click the button below to create a new password.</p>
                  <div style="text-align:center; margin:32px 0;">
                    <a href="%s" style="background:linear-gradient(135deg, #f59e0b, #ef4444); color:#fff; padding:14px 36px; border-radius:8px; text-decoration:none; font-weight:600; font-size:16px; display:inline-block;">Reset Password</a>
                  </div>
                  <p style="color:#94a3b8; font-size:13px; margin:24px 0 0;">This link expires in 1 hour. If you did not request a password reset, ignore this email - your account is safe.</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(name, resetUrl);
    }

    private String buildBudgetAlertEmail(String name, String category, double percentage, String month) {
        String color = percentage >= 100 ? "#ef4444" : "#f59e0b";
        String emoji = percentage >= 100 ? "🚨" : "⚠️";
        String msg = percentage >= 100
            ? "You have exceeded your budget limit"
            : "You are nearing your budget limit";

        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: 'Helvetica Neue', Arial, sans-serif; background:#f4f6f9; margin:0; padding:20px;">
              <div style="max-width:600px; margin:0 auto; background:#fff; border-radius:12px; overflow:hidden; box-shadow:0 4px 20px rgba(0,0,0,0.1);">
                <div style="background:linear-gradient(135deg, #1a1a2e 0%%, #16213e 100%%); padding:40px 30px; text-align:center;">
                  <h1 style="color:#00d4ff; margin:0; font-size:28px; font-weight:700; letter-spacing:2px;">SmartSpend</h1>
                </div>
                <div style="padding:40px 30px;">
                  <h2 style="color:%s; margin:0 0 16px;">%s Budget Alert!</h2>
                  <p style="color:#64748b; line-height:1.6; margin:0 0 16px;">Hi %s, %s for <strong>%s</strong> in %s.</p>
                  <div style="background:#f8fafc; border-left:4px solid %s; padding:16px 20px; border-radius:0 8px 8px 0; margin:24px 0;">
                    <p style="margin:0; color:#1a1a2e; font-size:18px; font-weight:700;">%.0f%% of budget used</p>
                  </div>
                  <p style="color:#94a3b8; font-size:13px;">Log in to SmartSpend to review your spending and adjust your budget if needed.</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(color, emoji, name, msg, category, month, color, percentage);
    }
}
