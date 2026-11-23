package com.sodepa.erp.notification.infrastructure.channel.whatsapp;

import com.sodepa.erp.notification.infrastructure.channel.ChannelProvider;
import com.sodepa.erp.notification.infrastructure.channel.NotificationChannelResult;
import com.sodepa.erp.notification.infrastructure.config.NotificationProperties;
import com.sodepa.erp.notification.infrastructure.util.PhoneNumberNormalizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Adaptateur officiel Meta WhatsApp Cloud API supportant les modèles pré-approuvés (HSM) et messages texte.
 */
@Component
public class MetaWhatsAppCloudAdapter implements WhatsAppProvider {

    private final NotificationProperties properties;
    private final RestClient restClient;

    @Autowired
    public MetaWhatsAppCloudAdapter(NotificationProperties properties) {
        this(properties, ChannelProvider.buildTimeoutRestClient());
    }

    public MetaWhatsAppCloudAdapter(NotificationProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public String providerName() {
        return "meta_whatsapp";
    }

    @Override
    public boolean isConfigured() {
        var cfg = properties.getWhatsapp().getMeta();
        return cfg.isEnabled() && notBlank(cfg.getApiUrl()) && notBlank(cfg.getPhoneNumberId()) && notBlank(cfg.getAccessToken());
    }

    @Override
    public NotificationChannelResult send(WhatsAppDispatchRequest request) {
        var cfg = properties.getWhatsapp().getMeta();
        String toMeta = PhoneNumberNormalizer.toMetaWhatsAppNumber(request.phoneE164());
        if (toMeta == null) {
            return NotificationChannelResult.failure(providerName(), "Numéro WhatsApp invalide.", false);
        }

        String baseUrl = cfg.getApiUrl().endsWith("/") ? cfg.getApiUrl().substring(0, cfg.getApiUrl().length() - 1) : cfg.getApiUrl();
        String url = baseUrl + "/" + cfg.getPhoneNumberId() + "/messages";
        Map<String, Object> payload = buildMetaPayload(toMeta, request, cfg.getDefaultLanguage());

        Map<?, ?> response = restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + cfg.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Map.class);

        String messageId = null;
        if (response != null && response.get("messages") instanceof List<?> messages && !messages.isEmpty()
                && messages.get(0) instanceof Map<?, ?> first && first.get("id") != null) {
            messageId = first.get("id").toString();
        }
        return NotificationChannelResult.ok(providerName(), messageId != null ? messageId : "WA-META-" + UUID.randomUUID());
    }

    private Map<String, Object> buildMetaPayload(String toMeta, WhatsAppDispatchRequest req, String defaultLang) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("messaging_product", "whatsapp");
        root.put("to", toMeta);

        if (notBlank(req.templateName())) {
            String lang = notBlank(req.languageCode()) ? req.languageCode() : (notBlank(defaultLang) ? defaultLang : "fr");
            List<Map<String, String>> bodyParams = (req.templateParams() == null ? List.<String>of() : req.templateParams())
                    .stream()
                    .map(val -> Map.of("type", "text", "text", val != null ? val : ""))
                    .toList();

            Map<String, Object> template = new LinkedHashMap<>();
            template.put("name", req.templateName());
            template.put("language", Map.of("code", lang));
            if (!bodyParams.isEmpty()) {
                template.put("components", List.of(Map.of(
                        "type", "body",
                        "parameters", bodyParams
                )));
            }
            root.put("type", "template");
            root.put("template", template);
        } else {
            root.put("type", "text");
            root.put("text", Map.of("body", req.fallbackText() != null ? req.fallbackText() : ""));
        }
        return root;
    }

    private boolean notBlank(String v) {
        return v != null && !v.isBlank();
    }
}
