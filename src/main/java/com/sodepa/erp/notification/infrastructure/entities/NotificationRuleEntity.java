package com.sodepa.erp.notification.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Règle de routage par couple (type d'événement métier × canal de diffusion).
 * Les règles marquées {@code critique = true} ne peuvent être désactivées ni par l'administrateur, ni par l'utilisateur.
 */
@Entity
@Table(
        name = "sys_notification_rules",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_notif_rule_event_canal", columnNames = {"type_evenement", "canal"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRuleEntity {

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

    @Column(name = "type_evenement", nullable = false, length = 60)
    private String typeEvenement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CanalNotification canal;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean critique = false;

    @Column(name = "ordre_priorite", nullable = false)
    @Builder.Default
    private int ordrePriorite = 10;

    @Enumerated(EnumType.STRING)
    @Column(name = "fallback_canal", length = 30)
    private CanalNotification fallbackCanal;

    @Column(length = 255)
    private String description;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
