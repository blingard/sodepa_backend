package com.sodepa.erp.notification.infrastructure.channel.email;

import com.sodepa.erp.notification.infrastructure.channel.NotificationChannelResult;
import com.sodepa.erp.notification.infrastructure.config.NotificationProperties;
import com.sodepa.erp.notification.infrastructure.util.LogMasker;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Adaptateur d'envoi d'e-mails {@code multipart/alternative} (HTML + texte brut) via SMTP / MailHog.
 */
@Slf4j
@Component
public class EmailChannelAdapter {

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    @Autowired
    public EmailChannelAdapter(
            @Autowired(required = false) JavaMailSender mailSender,
            NotificationProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public NotificationChannelResult sendEmail(String recipientEmail, String recipientName, String subject, String textBody, String htmlBody) {
        if (!properties.getMail().isEnabled()) {
            return NotificationChannelResult.failure("smtp", "Canal Email désactivé par configuration.", false);
        }
        if (recipientEmail == null || recipientEmail.isBlank() || !recipientEmail.contains("@")) {
            return NotificationChannelResult.failure("smtp", "Adresse e-mail destinataire absente ou invalide.", false);
        }

        String effectiveTo = recipientEmail.trim();
        boolean redirected = false;
        NotificationProperties.Sandbox sandbox = properties.getSandbox();
        if (sandbox != null && sandbox.isEnabled() && sandbox.getRedirectEmail() != null && !sandbox.getRedirectEmail().isBlank()) {
            effectiveTo = sandbox.getRedirectEmail().trim();
            redirected = true;
        }

        if (mailSender == null) {
            if (sandbox != null && sandbox.isEnabled()) {
                log.info("[NOTIF-MAIL-SANDBOX] E-mail simulé (JavaMailSender absent) vers {} (sujet='{}')",
                        LogMasker.maskEmail(effectiveTo), subject);
                return NotificationChannelResult.ok("sandbox-mail", "SBX-MAIL-" + UUID.randomUUID());
            }
            return NotificationChannelResult.failure("smtp", "Aucun JavaMailSender Spring configuré.", false);
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            String fromAddr = properties.getMail().getFromAddress();
            String fromName = properties.getMail().getFromName();
            if (fromName != null && !fromName.isBlank()) {
                helper.setFrom(fromAddr, fromName);
            } else {
                helper.setFrom(fromAddr);
            }
            helper.setTo(effectiveTo);
            helper.setSubject(subject != null && !subject.isBlank() ? subject : "Notification ERP SODEPA");

            String safeText = textBody != null ? textBody : "";
            String safeHtml = (htmlBody != null && !htmlBody.isBlank())
                    ? htmlBody
                    : buildFallbackHtml(helper.getMimeMessage().getSubject(), recipientName, safeText);
            helper.setText(safeText, safeHtml);

            mailSender.send(mimeMessage);
            String messageId = mimeMessage.getMessageID() != null ? mimeMessage.getMessageID() : "SMTP-" + UUID.randomUUID();
            String provider = (sandbox != null && sandbox.isEnabled()) ? "smtp-mailhog" : "smtp";
            log.info("[NOTIF-MAIL] E-mail transmis via {} vers {} (redirection={}, msgId={})",
                    provider, LogMasker.maskEmail(effectiveTo), redirected, messageId);
            return NotificationChannelResult.ok(provider, messageId);
        } catch (Exception e) {
            String safeErr = LogMasker.sanitizeError(e.getMessage());
            log.error("[NOTIF-MAIL] Échec envoi SMTP vers {} : {}", LogMasker.maskEmail(effectiveTo), safeErr);
            return NotificationChannelResult.failure("smtp", safeErr, false);
        }
    }

    private String buildFallbackHtml(String subject, String recipientName, String plainText) {
        String escapedTitle = HtmlUtils.htmlEscape(subject != null ? subject : "ERP SODEPA");
        String escapedName = HtmlUtils.htmlEscape(recipientName != null && !recipientName.isBlank() ? recipientName : "Collaborateur");
        String escapedBody = HtmlUtils.htmlEscape(plainText).replace("\n", "<br/>");
        return "<div style=\"font-family:Arial,sans-serif;max-width:600px;margin:0 auto;border:1px solid #e2e8f0;border-radius:8px;\">"
                + "<div style=\"background:#0f5132;color:#ffffff;padding:14px 20px;\"><h2 style=\"margin:0;font-size:17px;\">" + escapedTitle + "</h2></div>"
                + "<div style=\"padding:20px;color:#1e293b;line-height:1.5;\"><p>Bonjour <strong>" + escapedName + "</strong>,</p><p>" + escapedBody + "</p></div>"
                + "</div>";
    }
}
