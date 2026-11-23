package com.sodepa.erp.notification.infrastructure.channel.sms;

import com.sodepa.erp.notification.infrastructure.channel.ChannelProvider;
import com.sodepa.erp.notification.infrastructure.channel.NotificationChannelResult;
import com.sodepa.erp.notification.infrastructure.config.NotificationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Adaptateur SMS Infobip (/sms/2/text/advanced).
 */
@Component
public class InfobipSmsAdapter implements SmsProvider {

    private final NotificationProperties properties;
    private final RestClient restClient;

    @Autowired
    public InfobipSmsAdapter(NotificationProperties properties) {
        this(properties, ChannelProvider.buildTimeoutRestClient());
    }

    public InfobipSmsAdapter(NotificationProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public String providerName() {
        return "infobip";
    }

    @Override
    public boolean isConfigured() {
        var cfg = properties.getSms().getInfobip();
        return cfg.isEnabled() && cfg.getBaseUrl() != null && !cfg.getBaseUrl().isBlank()
                && cfg.getApiKey() != null && !cfg.getApiKey().isBlank();
    }

    @Override
    public NotificationChannelResult send(SmsDispatchRequest request) {
        var cfg = properties.getSms().getInfobip();
        String sender = (cfg.getSenderId() != null && !cfg.getSenderId().isBlank()) ? cfg.getSenderId() : "SODEPA";
        Map<String, Object> payload = Map.of(
                "messages", List.of(Map.of(
                        "from", sender,
                        "destinations", List.of(Map.of("to", request.phoneE164())),
                        "text", request.messageText()
                ))
        );

        Map<?, ?> response = restClient.post()
                .uri(cfg.getBaseUrl())
                .header(HttpHeaders.AUTHORIZATION, "App " + cfg.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Map.class);

        String messageId = null;
        if (response != null && response.get("messages") instanceof List<?> messages && !messages.isEmpty()
                && messages.get(0) instanceof Map<?, ?> firstMsg && firstMsg.get("messageId") != null) {
            messageId = firstMsg.get("messageId").toString();
        }
        return NotificationChannelResult.ok(providerName(), messageId != null ? messageId : "INFOBIP-" + UUID.randomUUID());
    }
}
