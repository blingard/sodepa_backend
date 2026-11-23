package com.sodepa.erp.notification.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Propriétés de configuration du socle multi-canal de notifications ERP SODEPA
 * (Outbox, MailHog/SMTP, passerelles SMS CEMAC, WhatsApp Business et mode Sandbox).
 */
@Component
@ConfigurationProperties(prefix = "sodepa.notifications")
@Getter
@Setter
public class NotificationProperties {

    /** Active l'expédition asynchrone via Outbox PostgreSQL + RabbitMQ. */
    private boolean asyncEnabled = true;

    /** Nombre maximal de tentatives techniques avant passage en Dead-Letter Queue. */
    private int maxRetries = 5;

    /** Délai initial du backoff exponentiel en secondes entre deux tentatives. */
    private long initialBackoffSeconds = 10;

    /** Taille maximale d'un lot lors du rejeu ShedLock. */
    private int outboxBatchSize = 50;

    private Mail mail = new Mail();
    private Sandbox sandbox = new Sandbox();
    private Sms sms = new Sms();
    private Whatsapp whatsapp = new Whatsapp();

    @Getter
    @Setter
    public static class Mail {
        private boolean enabled = true;
        private String fromAddress = "noreply@sodepa.cm";
        private String fromName = "ERP SODEPA";
    }

    @Getter
    @Setter
    public static class Sandbox {
        /** Activé par défaut hors production pour éviter tout envoi vers des contacts réels importés. */
        private boolean enabled = true;
        /** Si renseigné en sandbox, redirige les e-mails sortants vers cette boîte unique (ex. qa-notif@sodepa.local). */
        private String redirectEmail = "";
        /** Si true en sandbox, simule l'envoi SMS sans appel HTTP réel. */
        private boolean smsDryRun = true;
        /** Si true en sandbox, simule l'envoi WhatsApp sans appel HTTP réel. */
        private boolean whatsappDryRun = true;
        /** Numéros explicitement autorisés même en mode sandbox (tests contrôlés). */
        private List<String> allowedPhones = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Sms {
        private boolean enabled = true;
        private String defaultCountryCode = "+237";
        private List<String> providerOrder = new ArrayList<>(List.of("africa_talking", "nexah", "infobip", "twilio", "sandbox"));
        private AfricasTalking africaTalking = new AfricasTalking();
        private Nexah nexah = new Nexah();
        private Infobip infobip = new Infobip();
        private TwilioSms twilio = new TwilioSms();

        @Getter
        @Setter
        public static class AfricasTalking {
            private boolean enabled = false;
            private String baseUrl = "https://api.africastalking.com/version1/messaging";
            private String username = "";
            private String apiKey = "";
            private String senderId = "SODEPA";
        }

        @Getter
        @Setter
        public static class Nexah {
            private boolean enabled = false;
            private String baseUrl = "https://api.nexah.net/api/v1/sms/send";
            private String apiKey = "";
            private String user = "";
            private String password = "";
            private String senderId = "SODEPA";
        }

        @Getter
        @Setter
        public static class Infobip {
            private boolean enabled = false;
            private String baseUrl = "https://api.infobip.com/sms/2/text/advanced";
            private String apiKey = "";
            private String senderId = "SODEPA";
        }

        @Getter
        @Setter
        public static class TwilioSms {
            private boolean enabled = false;
            private String baseUrl = "https://api.twilio.com/2010-04-01";
            private String accountSid = "";
            private String authToken = "";
            private String fromNumber = "";
        }
    }

    @Getter
    @Setter
    public static class Whatsapp {
        private boolean enabled = true;
        /** En cas d'échec WhatsApp (hors sandbox/désactivation), bascule automatiquement sur un SMS enfant lié. */
        private boolean fallbackToSmsOnFailure = true;
        private List<String> providerOrder = new ArrayList<>(List.of("meta_whatsapp", "twilio_whatsapp", "sandbox"));
        private Meta meta = new Meta();
        private TwilioWhatsapp twilio = new TwilioWhatsapp();

        @Getter
        @Setter
        public static class Meta {
            private boolean enabled = false;
            private String apiUrl = "https://graph.facebook.com/v21.0";
            private String phoneNumberId = "";
            private String accessToken = "";
            private String defaultLanguage = "fr";
        }

        @Getter
        @Setter
        public static class TwilioWhatsapp {
            private boolean enabled = false;
            private String baseUrl = "https://api.twilio.com/2010-04-01";
            private String accountSid = "";
            private String authToken = "";
            private String fromNumber = "";
        }
    }
}
