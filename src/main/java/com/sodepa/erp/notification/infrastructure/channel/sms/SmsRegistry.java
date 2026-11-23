package com.sodepa.erp.notification.infrastructure.channel.sms;

import com.sodepa.erp.notification.infrastructure.channel.NotificationChannelResult;
import com.sodepa.erp.notification.infrastructure.channel.OrderedProviderRegistry;
import com.sodepa.erp.notification.infrastructure.config.NotificationProperties;
import com.sodepa.erp.notification.infrastructure.util.PhoneNumberNormalizer;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Registre SMS multi-fournisseurs CEMAC avec garde-fou Sandbox et failover ordonné.
 */
@Service
public class SmsRegistry {

    private final NotificationProperties properties;
    private final OrderedProviderRegistry<SmsProvider, SmsDispatchRequest> registry;
    private final SandboxSmsAdapter sandboxSmsAdapter;

    public SmsRegistry(NotificationProperties properties, List<SmsProvider> providers, SandboxSmsAdapter sandboxSmsAdapter) {
        this.properties = properties;
        this.registry = new OrderedProviderRegistry<>("SMS", providers);
        this.sandboxSmsAdapter = sandboxSmsAdapter;
    }

    public NotificationChannelResult sendSms(String rawPhone, String messageText, String typeEvenement) {
        if (!properties.getSms().isEnabled()) {
            return NotificationChannelResult.failure("sms", "Canal SMS désactivé par configuration.", false);
        }
        String phoneE164 = PhoneNumberNormalizer.normalize(rawPhone, properties.getSms().getDefaultCountryCode());
        if (phoneE164 == null) {
            return NotificationChannelResult.failure("sms", "Numéro de téléphone mobile absent ou invalide.", false);
        }
        String sanitizedText = messageText != null ? messageText.trim() : "";
        SmsDispatchRequest request = new SmsDispatchRequest(phoneE164, sanitizedText, typeEvenement);

        var sandbox = properties.getSandbox();
        if (sandbox != null && sandbox.isEnabled() && sandbox.isSmsDryRun()
                && (sandbox.getAllowedPhones() == null || !sandbox.getAllowedPhones().contains(phoneE164))) {
            return sandboxSmsAdapter.send(request);
        }
        return registry.sendWithFailover(properties.getSms().getProviderOrder(), request);
    }
}
