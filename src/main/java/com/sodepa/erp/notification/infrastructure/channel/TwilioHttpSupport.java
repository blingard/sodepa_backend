package com.sodepa.erp.notification.infrastructure.channel;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Helper HTTP partagé pour l'API Twilio Programmable Messaging (SMS et WhatsApp).
 */
public final class TwilioHttpSupport {

    private TwilioHttpSupport() {
    }

    public static NotificationChannelResult sendMessage(
            RestClient restClient,
            String providerName,
            String baseUrl,
            String accountSid,
            String authToken,
            String from,
            String to,
            String body) {
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String url = normalizedBase + "/Accounts/" + accountSid + "/Messages.json";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", to);
        form.add("From", from);
        form.add("Body", body);

        String credentials = accountSid + ":" + authToken;
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        Map<?, ?> response = restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, basicAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        String sid = response != null && response.get("sid") != null ? response.get("sid").toString() : null;
        if (sid == null || sid.isBlank()) {
            return NotificationChannelResult.failure(providerName, "Twilio n'a retourné aucun SID de message.", false);
        }
        return NotificationChannelResult.ok(providerName, sid);
    }
}
