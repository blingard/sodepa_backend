package com.sodepa.erp.notification.application.governance;

import com.sodepa.erp.notification.infrastructure.entities.NotificationUserPreferenceEntity;
import com.sodepa.erp.notification.infrastructure.repo.NotificationUserPreferenceRepository;
import com.sodepa.erp.notification.infrastructure.util.PhoneNumberNormalizer;
import com.sodepa.erp.notification.presentation.dto.UpdateNotificationPreferencesRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

/**
 * Service de gestion des préférences de notification par utilisateur (canaux actifs, WhatsApp, langue).
 */
@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationUserPreferenceRepository preferenceRepository;

    public NotificationUserPreferenceEntity getPreferences(UUID utilisateurId) {
        if (utilisateurId == null) {
            return defaultPreferences(null);
        }
        return preferenceRepository.findByUtilisateurId(utilisateurId)
                .orElseGet(() -> defaultPreferences(utilisateurId));
    }

    @Transactional
    public NotificationUserPreferenceEntity updatePreferences(UUID utilisateurId, UpdateNotificationPreferencesRequest req) {
        if (utilisateurId == null) {
            throw new IllegalArgumentException("Utilisateur non authentifié.");
        }
        NotificationUserPreferenceEntity entity = preferenceRepository.findByUtilisateurId(utilisateurId)
                .orElseGet(() -> NotificationUserPreferenceEntity.builder()
                        .utilisateurId(utilisateurId)
                        .emailEnabled(true)
                        .smsEnabled(true)
                        .whatsappEnabled(true)
                        .inAppEnabled(true)
                        .langue("fr")
                        .build());

        if (req.emailEnabled() != null) {
            entity.setEmailEnabled(req.emailEnabled());
        }
        if (req.smsEnabled() != null) {
            entity.setSmsEnabled(req.smsEnabled());
        }
        if (req.whatsappEnabled() != null) {
            entity.setWhatsappEnabled(req.whatsappEnabled());
        }
        if (req.inAppEnabled() != null) {
            entity.setInAppEnabled(req.inAppEnabled());
        }
        if (req.whatsappPhone() != null) {
            if (req.whatsappPhone().isBlank()) {
                entity.setWhatsappPhone(null);
            } else {
                String normalized = PhoneNumberNormalizer.normalize(req.whatsappPhone(), "+237");
                if (normalized == null) {
                    throw new IllegalArgumentException("Numéro WhatsApp invalide (format E.164 ou local CEMAC attendu).");
                }
                entity.setWhatsappPhone(normalized);
            }
        }
        if (req.langue() != null && !req.langue().isBlank()) {
            entity.setLangue(req.langue().trim().toLowerCase(Locale.ROOT));
        }
        return preferenceRepository.save(entity);
    }

    private NotificationUserPreferenceEntity defaultPreferences(UUID utilisateurId) {
        return NotificationUserPreferenceEntity.builder()
                .utilisateurId(utilisateurId)
                .emailEnabled(true)
                .smsEnabled(true)
                .whatsappEnabled(true)
                .inAppEnabled(true)
                .langue("fr")
                .build();
    }
}
