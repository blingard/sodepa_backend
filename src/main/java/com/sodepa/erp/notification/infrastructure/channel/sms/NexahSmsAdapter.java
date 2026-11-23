package com.sodepa.erp.notification.infrastructure.channel.sms;

import com.sodepa.erp.notification.infrastructure.channel.ChannelProvider;
import com.sodepa.erp.notification.infrastructure.channel.NotificationChannelResult;
import com.sodepa.erp.notification.infrastructure.config.NotificationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Adaptateur SMS Nexah (passerelle locale Cameroun / CEMAC).
 */
@Component
public class NexahSmsAdapter implements SmsProvider {

    private final NotificationProperties properties;
    private final RestClient restClient;

    @Autowired
    public NexahSmsAdapter(NotificationProperties properties) {
        this(properties, ChannelProvider.buildTimeoutRestClient());
    }

    public NexahSmsAdapter(NotificationProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public String providerName() {
        return "nexah";
    }

    @Override
    public boolean isConfigured() {
        var cfg = properties.getSms().getNexah();
        return cfg.isEnabled() && notBlank(cfg.getBaseUrl()) && (notBlank(cfg.getApiKey()) || notBlank(cfg.getUser()));
    }

    @Override
    public NotificationChannelResult send(SmsDispatchRequest request) {
        var cfg = properties.getSms().getNexah();
        Map<String, Object> body = new LinkedHashMap<>();
        if (notBlank(cfg.getUser())) {
            body.put("user", cfg.getUser());
        }
        if (notBlank(cfg.getPassword())) {
            body.put("password", cfg.getPassword());
        }
        if (notBlank(cfg.getApiKey())) {
            body.put("apiKey", cfg.getApiKey());
        }
        body.put("senderid", notBlank(cfg.getSenderId()) ? cfg.getSenderId() : "SODEPA");
        body.put("mobiles", request.phoneE164().replace("+", ""));
        body.put("sms", request.messageText());

        Map<?, ?> response = restClient.post()
                .uri(cfg.getBaseUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response != null && response.get("responsecode") instanceof Number code && code.intValue() != 1) {
            return NotificationChannelResult.failure(providerName(), "Nexah responsecode=" + code.intValue(), false);
        }
        Object msgId = response != null ? response.get("messageId") : null;
        return NotificationChannelResult.ok(providerName(), msgId != null ? msgId.toString() : "NEXAH-" + UUID.randomUUID());
    }

    private boolean notBlank(String v) {
        return v != null && !v.isBlank();
    }
}
