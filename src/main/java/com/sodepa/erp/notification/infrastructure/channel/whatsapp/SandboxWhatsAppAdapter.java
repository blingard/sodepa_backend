package com.sodepa.erp.notification.infrastructure.channel.whatsapp;

import com.sodepa.erp.notification.infrastructure.channel.NotificationChannelResult;
import com.sodepa.erp.notification.infrastructure.config.NotificationProperties;
import com.sodepa.erp.notification.infrastructure.util.LogMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adaptateur WhatsApp Sandbox empêchant tout appel externe en développement/test.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SandboxWhatsAppAdapter implements WhatsAppProvider {

    private final NotificationProperties properties;

    @Override
    public String providerName() {
        return "sandbox";
    }

    @Override
    public boolean isConfigured() {
        var sandbox = properties.getSandbox();
        return sandbox != null && sandbox.isEnabled() && sandbox.isWhatsappDryRun();
    }

    @Override
    public NotificationChannelResult send(WhatsAppDispatchRequest request) {
        String id = "SBX-WA-" + UUID.randomUUID();
        log.info("[NOTIF-WA-SANDBOX] WhatsApp simulé vers {} (evt={}, template={}, ref={})",
                LogMasker.maskPhone(request.phoneE164()),
                request.typeEvenement(),
                request.templateName(),
                id);
        return NotificationChannelResult.ok(providerName(), id);
    }
}
