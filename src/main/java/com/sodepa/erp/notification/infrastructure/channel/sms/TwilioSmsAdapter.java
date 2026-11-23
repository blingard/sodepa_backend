package com.sodepa.erp.notification.infrastructure.channel.sms;

import com.sodepa.erp.notification.infrastructure.channel.ChannelProvider;
import com.sodepa.erp.notification.infrastructure.channel.NotificationChannelResult;
import com.sodepa.erp.notification.infrastructure.channel.TwilioHttpSupport;
import com.sodepa.erp.notification.infrastructure.config.NotificationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Adaptateur SMS Twilio Programmable Messaging.
 */
@Component
public class TwilioSmsAdapter implements SmsProvider {

    private final NotificationProperties properties;
    private final RestClient restClient;

    @Autowired
    public TwilioSmsAdapter(NotificationProperties properties) {
        this(properties, ChannelProvider.buildTimeoutRestClient());
    }

    public TwilioSmsAdapter(NotificationProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public String providerName() {
        return "twilio";
    }

    @Override
    public boolean isConfigured() {
        var cfg = properties.getSms().getTwilio();
        return cfg.isEnabled() && notBlank(cfg.getBaseUrl()) && notBlank(cfg.getAccountSid())
                && notBlank(cfg.getAuthToken()) && notBlank(cfg.getFromNumber());
    }

    @Override
    public NotificationChannelResult send(SmsDispatchRequest request) {
        var cfg = properties.getSms().getTwilio();
        return TwilioHttpSupport.sendMessage(
                restClient,
                providerName(),
                cfg.getBaseUrl(),
                cfg.getAccountSid(),
                cfg.getAuthToken(),
                cfg.getFromNumber(),
                request.phoneE164(),
                request.messageText()
        );
    }

    private boolean notBlank(String v) {
        return v != null && !v.isBlank();
    }
}
