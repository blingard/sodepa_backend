package com.sodepa.erp.notification.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Préférences individuelles de réception des notifications par canal pour un utilisateur ERP SODEPA.
 * Les notifications critiques (sécurité IAM, rejets, relances SLA) contournent l'opt-out.
 */
@Entity
@Table(
        name = "sys_notification_user_prefs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_notif_user_prefs_user", columnNames = "utilisateur_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationUserPreferenceEntity {

    @Id
    private UUID id;

    @PrePersist
    @PreUpdate
    void touch() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        this.updatedAt = LocalDateTime.now();
    }

    @Column(name = "utilisateur_id", nullable = false, unique = true)
    private UUID utilisateurId;

    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private boolean emailEnabled = true;

    @Column(name = "sms_enabled", nullable = false)
    @Builder.Default
    private boolean smsEnabled = true;

    @Column(name = "whatsapp_enabled", nullable = false)
    @Builder.Default
    private boolean whatsappEnabled = true;

    @Column(name = "in_app_enabled", nullable = false)
    @Builder.Default
    private boolean inAppEnabled = true;

    @Column(name = "whatsapp_phone", length = 40)
    private String whatsappPhone;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String langue = "fr";

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
