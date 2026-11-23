package com.sodepa.erp.notification.infrastructure.channel;

import com.sodepa.erp.notification.infrastructure.util.LogMasker;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Moteur générique de routage ordonné avec failover automatique entre fournisseurs configurés.
 */
@Slf4j
public class OrderedProviderRegistry<P extends ChannelProvider<R>, R> {

    private final String channelLabel;
    private final Map<String, P> providersByName = new LinkedHashMap<>();

    public OrderedProviderRegistry(String channelLabel, List<P> providers) {
        this.channelLabel = channelLabel;
        if (providers != null) {
            for (P provider : providers) {
                if (provider != null && provider.providerName() != null) {
                    providersByName.put(provider.providerName().toLowerCase(Locale.ROOT), provider);
                }
            }
        }
    }

    /**
     * Tente les fournisseurs dans l'ordre spécifié jusqu'au premier succès.
     */
    public NotificationChannelResult sendWithFailover(List<String> providerOrder, R request) {
        List<String> order = (providerOrder != null && !providerOrder.isEmpty())
                ? providerOrder
                : new ArrayList<>(providersByName.keySet());

        List<String> errors = new ArrayList<>();
        int attempted = 0;
        String lastProvider = "none";

        for (String name : order) {
            if (name == null || name.isBlank()) {
                continue;
            }
            P provider = providersByName.get(name.trim().toLowerCase(Locale.ROOT));
            if (provider == null || !provider.isConfigured()) {
                continue;
            }
            lastProvider = provider.providerName();
            try {
                NotificationChannelResult result = provider.send(request);
                if (result != null && result.success()) {
                    return NotificationChannelResult.okWithFallback(
                            provider.providerName(),
                            result.providerMessageId(),
                            attempted > 0 || result.fallbackUsed()
                    );
                }
                String err = result != null ? result.errorMessage() : "Réponse nulle du fournisseur";
                errors.add(provider.providerName() + ": " + LogMasker.sanitizeError(err));
            } catch (Exception e) {
                String safeErr = LogMasker.sanitizeError(e.getMessage());
                log.warn("[NOTIF-{}] Échec du fournisseur {} (tentative #{}): {}",
                        channelLabel, provider.providerName(), attempted + 1, safeErr);
                errors.add(provider.providerName() + ": " + safeErr);
            }
            attempted++;
        }

        if (attempted == 0) {
            return NotificationChannelResult.failure(
                    lastProvider,
                    "Aucun fournisseur " + channelLabel + " actif ou configuré.",
                    false
            );
        }
        return NotificationChannelResult.failure(
                lastProvider,
                String.join(" | ", errors),
                attempted > 1
        );
    }
}
