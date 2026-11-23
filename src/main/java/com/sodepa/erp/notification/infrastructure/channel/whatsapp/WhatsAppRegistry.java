package com.sodepa.erp.notification.infrastructure.channel.whatsapp;

import com.sodepa.erp.notification.infrastructure.channel.NotificationChannelResult;
import com.sodepa.erp.notification.infrastructure.channel.OrderedProviderRegistry;
import com.sodepa.erp.notification.infrastructure.config.NotificationProperties;
import com.sodepa.erp.notification.infrastructure.util.PhoneNumberNormalizer;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Registre WhatsApp Business avec garde-fou Sandbox et failover ordonné Meta Cloud / Twilio.
 */
@Service
public class WhatsAppRegistry {

    private final NotificationProperties properties;
    private final OrderedProviderRegistry<WhatsAppProvider, WhatsAppDispatchRequest> registry;
    private final SandboxWhatsAppAdapter sandboxAdapter;

    public WhatsAppRegistry(
            NotificationProperties properties,
            List<WhatsAppProvider> providers,
            SandboxWhatsAppAdapter sandboxAdapter) {
        this.properties = properties;
        this.registry = new OrderedProviderRegistry<>("WHATSAPP", providers);
        this.sandboxAdapter = sandboxAdapter;
    }

    public NotificationChannelResult sendWhatsApp(
            String rawPhone,
            String templateName,
            String languageCode,
            List<String> templateParams,
            String fallbackText,
            String typeEvenement) {
        if (!properties.getWhatsapp().isEnabled()) {
            return NotificationChannelResult.failure("whatsapp", "Canal WhatsApp désactivé par configuration.", false);
        }
        String phoneE164 = PhoneNumberNormalizer.normalize(rawPhone, properties.getSms().getDefaultCountryCode());
        if (phoneE164 == null) {
            return NotificationChannelResult.failure("whatsapp", "Numéro WhatsApp absent ou invalide.", false);
        }

        WhatsAppDispatchRequest request = new WhatsAppDispatchRequest(
                phoneE164, templateName, languageCode, templateParams, fallbackText, typeEvenement
        );

        var sandbox = properties.getSandbox();
        if (sandbox != null && sandbox.isEnabled() && sandbox.isWhatsappDryRun()
                && (sandbox.getAllowedPhones() == null || !sandbox.getAllowedPhones().contains(phoneE164))) {
            return sandboxAdapter.send(request);
        }
        return registry.sendWithFailover(properties.getWhatsapp().getProviderOrder(), request);
    }
}
