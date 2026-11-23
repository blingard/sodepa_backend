package com.sodepa.erp.notification.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modèle de notification administrable en base de données (sujet, texte brut, HTML sécurisé et template WhatsApp HSM).
 * Aucun moteur de script/SpEL n'est exécuté sur ces modèles : seuls des placeholders {@code {{cle}}} échappés HTML sont substitués.
 */
@Entity
@Table(
        name = "sys_notification_templates",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_notif_templates_code", columnNames = "code"),
                @UniqueConstraint(name = "uk_notif_templates_event_lang", columnNames = {"type_evenement", "langue"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplateEntity {

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

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(name = "type_evenement", nullable = false, length = 60)
    private String typeEvenement;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String langue = "fr";

    @Column(name = "sujet_template", nullable = false, length = 250)
    private String sujetTemplate;

    @Column(name = "body_text_template", nullable = false, columnDefinition = "TEXT")
    private String bodyTextTemplate;

    @Column(name = "body_html_template", columnDefinition = "TEXT")
    private String bodyHtmlTemplate;

    @Column(name = "whatsapp_template_name", length = 120)
    private String whatsappTemplateName;

    @Column(name = "whatsapp_param_order", length = 500)
    private String whatsappParamOrder;

    @Column(name = "allowed_placeholders", length = 500)
    private String allowedPlaceholders;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
