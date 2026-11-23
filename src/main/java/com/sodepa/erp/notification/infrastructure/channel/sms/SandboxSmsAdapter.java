package com.sodepa.erp.notification.infrastructure.channel.sms;

import com.sodepa.erp.notification.infrastructure.channel.NotificationChannelResult;
import com.sodepa.erp.notification.infrastructure.config.NotificationProperties;
import com.sodepa.erp.notification.infrastructure.util.LogMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adaptateur SMS Sandbox empêchant tout appel payant vers des numéros réels hors production.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SandboxSmsAdapter implements SmsProvider {

    private final NotificationProperties properties;

    @Override
    public String providerName() {
        return "sandbox";
    }

    @Override
    public boolean isConfigured() {
        var sandbox = properties.getSandbox();
        return sandbox != null && sandbox.isEnabled() && sandbox.isSmsDryRun();
    }

    @Override
    public NotificationChannelResult send(SmsDispatchRequest request) {
        String id = "SBX-SMS-" + UUID.randomUUID();
        log.info("[NOTIF-SMS-SANDBOX] Envoi SMS simulé vers {} (evt={}, chars={}, ref={})",
                LogMasker.maskPhone(request.phoneE164()),
                request.typeEvenement(),
                request.messageText() != null ? request.messageText().length() : 0,
                id);
        return NotificationChannelResult.ok(providerName(), id);
    }
}
