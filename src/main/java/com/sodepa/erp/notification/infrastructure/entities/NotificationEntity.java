package com.sodepa.erp.notification.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité JPA journalisant toutes les notifications sortantes (Email, SMS et alertes internes)
 * émises par le Référentiel et le Moteur de Workflow SODEPA (CT-REF-05, F-REF-023, F-REF-030).
 * <p>
 * Garantit la traçabilité complète des notifications d'assignation de tâches, de rejets motivés,
 * de relances sur dépassement de délai SLA et d'alertes d'expiration de pièces de conformité.
 * </p>
 */
@Entity
@Table(name = "sys_notifications", indexes = {
    @Index(name = "idx_notif_dest_user", columnList = "destinataire_utilisateur_id"),
    @Index(name = "idx_notif_statut", columnList = "statut"),
    @Index(name = "idx_notif_type_evt", columnList = "type_evenement")
})
/**
 * Entité de persistance représentant `NotificationEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "NTF";

    public String getPrefixeCode() {
        return this.codePrefix;
    }

    public void setPrefixeCode(String prefixeCode) {
        this.codePrefix = prefixeCode;
    }


    /** Identifiant unique universel (UUID) de la notification. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Canal de diffusion utilisé (EMAIL, SMS, EMAIL_ET_SMS, IN_APP). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CanalNotification canal;

    /** Identifiant du compte utilisateur destinataire dans l'ERP (si applicable). */
    @Column(name = "destinataire_utilisateur_id")
    private UUID destinataireUtilisateurId;

    /** Nom ou matricule lisible du destinataire. */
    @Column(name = "destinataire_nom", length = 150)
    private String destinataireNom;

    /** Adresse électronique du destinataire pour l'envoi Email. */
    @Column(name = "destinataire_email", length = 180)
    private String destinataireEmail;

    /** Numéro de téléphone mobile du destinataire pour l'envoi SMS (format national ou international +237). */
    @Column(name = "destinataire_telephone_sms", length = 40)
    private String destinataireTelephoneSms;

    /** Objet du message ou titre court de l'alerte. */
    @Column(nullable = false, length = 250)
    private String sujet;

    /** Contenu textuel détaillé du message transmis par Email et/ou SMS. */
    @Column(name = "message_corps", nullable = false, columnDefinition = "TEXT")
    private String messageCorps;

    /**
     * Type d'événement décleéencheur :
     * <ul>
     *   <li>{@code WORKFLOW_ASSIGNATION_ETAPE} : Nouvelle tâche de validation assignée à un acteur (CT-REF-05)</li>
     *   <li>{@code WORKFLOW_REJET_DOSSIER} : Dossier rejeté avec motif renvoyé à l'émetteur (CT-REF-05)</li>
     *   <li>{@code WORKFLOW_CLOTURE_APPROUVEE} : Dossier intégralement validé</li>
     *   <li>{@code WORKFLOW_RELANCE_SLA} : Relance automatique pour dépassement du délai SLA (CA-REF-030-03)</li>
     *   <li>{@code CONFORMITE_TIERS_EXPIRATION} : Alerte J-15 d'expiration de pièce fiscale/administrative (CA-REF-023-01)</li>
     *   <li>{@code SECURITE_COMPTE_IAM} : Alerte de sécurité ou verrouillage de compte (CA-REF-028-02)</li>
     * </ul>
     */
    @Column(name = "type_evenement", nullable = false, length = 60)
    private String typeEvenement;

    /** Nom de l'entité métier rattachée (ex: WORKFLOW_INSTANCE, TIERS, UTILISATEUR). */
    @Column(name = "reference_entite_type", length = 60)
    private String referenceEntiteType;

    /** Identifiant technique ou code de l'entité métier rattachée. */
    @Column(name = "reference_entite_id", length = 100)
    private String referenceEntiteId;

    /** Statut courant de la notification (EN_ATTENTE, ENVOYEE, ECHEC, LUE). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutNotification statut = StatutNotification.SEND;

    /** Détail technique en cas d'échec d'envoi SMTP ou passerelle SMS. */
    @Column(name = "erreur_technique", length = 500)
    private String erreurTechnique;

    /** Date et heure d'émission de la notification. */
    @Column(name = "date_envoi", nullable = false)
    @Builder.Default
    private LocalDateTime dateEnvoi = LocalDateTime.now();

    /** Date et heure de prise de connaissance par l'utilisateur. */
    @Column(name = "date_lecture")
    private LocalDateTime dateLecture;

    /** Identifiant de regroupement lorsque plusieurs canaux découlent d'un même événement métier. */
    @Column(name = "group_id")
    private UUID groupId;

    /** Identifiant parent lorsque cette notification est un repli (ex. SMS après échec WhatsApp). */
    @Column(name = "parent_notification_id")
    private UUID parentNotificationId;

    /** Clé d'idempotence empêchant la double création / double expédition. */
    @Column(name = "idempotency_key", unique = true, length = 160)
    private String idempotencyKey;

    /** Nom technique du fournisseur ayant traité le message (ex. smtp-mailhog, africa_talking, meta_whatsapp, in_app). */
    @Column(name = "provider_name", length = 60)
    private String providerName;

    /** Identifiant de traçabilité renvoyé par le fournisseur distant. */
    @Column(name = "provider_message_id", length = 160)
    private String providerMessageId;

    /** Indique si un mécanisme de repli (failover ou repli WhatsApp -> SMS) a été déclenché. */
    @Column(name = "fallback_used", nullable = false)
    @Builder.Default
    private boolean fallbackUsed = false;

    /** Nombre de tentatives techniques d'expédition effectuées. */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    /** Prochaine échéance planifiée en cas de nouvelle tentative différée. */
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    /** Rendu HTML sécurisé pour les courriels multipart/alternative. */
    @Column(name = "message_html", columnDefinition = "TEXT")
    private String messageHtml;

    /** Numéro WhatsApp normalisé au format international E.164. */
    @Column(name = "destinataire_whatsapp", length = 40)
    private String destinataireWhatsapp;

    /** Code du template métier appliqué. */
    @Column(name = "template_code", length = 80)
    private String templateCode;

    /** Paramètres structurés JSON (notamment template WhatsApp HSM et variables résolues). */
    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;
}
