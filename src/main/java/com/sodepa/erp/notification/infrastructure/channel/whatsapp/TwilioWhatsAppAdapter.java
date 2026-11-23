package com.sodepa.erp.notification.infrastructure.channel.whatsapp;

import com.sodepa.erp.notification.infrastructure.channel.ChannelProvider;
import com.sodepa.erp.notification.infrastructure.channel.NotificationChannelResult;
import com.sodepa.erp.notification.infrastructure.channel.TwilioHttpSupport;
import com.sodepa.erp.notification.infrastructure.config.NotificationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Adaptateur WhatsApp secondaire s'appuyant sur l'API Twilio WhatsApp.
 */
@Component
public class TwilioWhatsAppAdapter implements WhatsAppProvider {

    private final NotificationProperties properties;
    private final RestClient restClient;

    @Autowired
    public TwilioWhatsAppAdapter(NotificationProperties properties) {
        this(properties, ChannelProvider.buildTimeoutRestClient());
    }

    public TwilioWhatsAppAdapter(NotificationProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public String providerName() {
        return "twilio_whatsapp";
    }

    @Override
    public boolean isConfigured() {
        var cfg = properties.getWhatsapp().getTwilio();
        return cfg.isEnabled() && notBlank(cfg.getBaseUrl()) && notBlank(cfg.getAccountSid())
                && notBlank(cfg.getAuthToken()) && notBlank(cfg.getFromNumber());
    }

    @Override
    public NotificationChannelResult send(WhatsAppDispatchRequest request) {
        var cfg = properties.getWhatsapp().getTwilio();
        String fromWa = cfg.getFromNumber().startsWith("whatsapp:") ? cfg.getFromNumber() : "whatsapp:" + cfg.getFromNumber();
        String toWa = request.phoneE164().startsWith("whatsapp:") ? request.phoneE164() : "whatsapp:" + request.phoneE164();
        return TwilioHttpSupport.sendMessage(
                restClient,
                providerName(),
                cfg.getBaseUrl(),
                cfg.getAccountSid(),
                cfg.getAuthToken(),
                fromWa,
                toWa,
                request.fallbackText() != null ? request.fallbackText() : ""
        );
    }

    private boolean notBlank(String v) {
        return v != null && !v.isBlank();
    }
}
