package com.sodepa.erp.notification.infrastructure.channel.sms;

import com.sodepa.erp.notification.infrastructure.channel.ChannelProvider;
import com.sodepa.erp.notification.infrastructure.channel.NotificationChannelResult;
import com.sodepa.erp.notification.infrastructure.config.NotificationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Adaptateur SMS Africa's Talking utilisant un formulaire encodé {@code x-www-form-urlencoded}.
 */
@Component
public class AfricasTalkingSmsAdapter implements SmsProvider {

    private final NotificationProperties properties;
    private final RestClient restClient;

    @Autowired
    public AfricasTalkingSmsAdapter(NotificationProperties properties) {
        this(properties, ChannelProvider.buildTimeoutRestClient());
    }

    public AfricasTalkingSmsAdapter(NotificationProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public String providerName() {
        return "africa_talking";
    }

    @Override
    public boolean isConfigured() {
        var cfg = properties.getSms().getAfricaTalking();
        return cfg.isEnabled() && notBlank(cfg.getBaseUrl()) && notBlank(cfg.getUsername()) && notBlank(cfg.getApiKey());
    }

    @Override
    public NotificationChannelResult send(SmsDispatchRequest request) {
        var cfg = properties.getSms().getAfricaTalking();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", cfg.getUsername());
        form.add("to", request.phoneE164());
        form.add("message", request.messageText());
        if (notBlank(cfg.getSenderId())) {
            form.add("from", cfg.getSenderId());
        }

        Map<?, ?> response = restClient.post()
                .uri(cfg.getBaseUrl())
                .header("apiKey", cfg.getApiKey())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        String messageId = extractMessageId(response);
        if (messageId == null) {
            return NotificationChannelResult.failure(providerName(), "Réponse Africa's Talking sans identifiant de message valide.", false);
        }
        return NotificationChannelResult.ok(providerName(), messageId);
    }

    private String extractMessageId(Map<?, ?> response) {
        if (response == null) {
            return null;
        }
        Object smsDataObj = response.get("SMSMessageData");
        if (!(smsDataObj instanceof Map<?, ?> smsData)) {
            return null;
        }
        Object recObj = smsData.get("Recipients");
        if (!(recObj instanceof List<?> recipients) || recipients.isEmpty()) {
            return null;
        }
        Object first = recipients.get(0);
        if (!(first instanceof Map<?, ?> recipient)) {
            return null;
        }
        Object statusCodeObj = recipient.get("statusCode");
        if (statusCodeObj instanceof Number n) {
            int code = n.intValue();
            if (code >= 400) {
                throw new IllegalStateException("Africa's Talking reject statusCode=" + code);
            }
        }
        Object msgId = recipient.get("messageId");
        return msgId != null ? msgId.toString() : "AT-ACCEPTED";
    }

    private boolean notBlank(String v) {
        return v != null && !v.isBlank();
    }
}
