package com.sodepa.erp.notification.infrastructure.channel;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Contrat générique d'un adaptateur de canal ou fournisseur externe (SMS / WhatsApp).
 */
public interface ChannelProvider<T> {

    String providerName();

    boolean isConfigured();

    NotificationChannelResult send(T request);

    /**
     * Fabrique un {@link RestClient} configuré avec timeouts explicites (3s connexion, 5s lecture).
     */
    static RestClient buildTimeoutRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder().requestFactory(factory).build();
    }
}
