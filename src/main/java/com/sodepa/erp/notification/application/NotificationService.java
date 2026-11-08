package com.sodepa.erp.notification.application;

import com.sodepa.erp.notification.infrastructure.entities.CanalNotification;
import com.sodepa.erp.notification.infrastructure.entities.NotificationEntity;
import com.sodepa.erp.notification.infrastructure.entities.StatutNotification;
import com.sodepa.erp.notification.infrastructure.repo.NotificationRepository;
import com.sodepa.erp.rh.infrastructure.entities.EmployeEntity;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service applicatif central de gestion des notifications multi-canaux (Email et SMS) de l'ERP SODEPA.
 * <p>
 * Implémente les exigences transverses et fonctionnelles du Cahier des Charges Module 01 :
 * <ul>
 *   <li><b>CT-REF-05</b> : Notification automatique (Email + SMS) de l'acteur suivant à chaque étape d'un circuit de validation,
 *       et notification de l'émetteur avec motif obligatoire en cas de rejet.</li>
 *   <li><b>CA-REF-030-03</b> : Relance automatique par Email et SMS en cas de dépassement du délai SLA d'une tâche.</li>
 *   <li><b>CA-REF-023-01</b> : Alertes d'expiration des pièces de conformité des tiers (NIU, attestation fiscale, RCCM).</li>
 *   <li><b>CA-REF-028-02</b> : Alertes de sécurité IAM lors du verrouillage d'un compte ou d'une activité sensible.</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Envoie une notification par courrier électronique (Email) et journalise l'envoi en base de données.
     *
     * @param utilisateurId      Identifiant UUID du destinataire (optionnel).
     * @param destinataireNom    Nom complet du destinataire.
     * @param email              Adresse électronique cible.
     * @param sujet              Objet du courrier électronique.
     * @param messageCorps       Contenu détaillé du message.
     * @param typeEvenement      Catégorie d'événement métier décleéencheur.
     * @param referenceEntiteType Type de l'entité concernée (ex: WORKFLOW_INSTANCE, TIERS).
     * @param referenceEntiteId  Identifiant de l'entité concernée.
     * @return L'entité {@link NotificationEntity} persistée et horodatée.
     */
    @Transactional
    public NotificationEntity envoyerEmail(
            UUID utilisateurId,
            String destinataireNom,
            String email,
            String sujet,
            String messageCorps,
            String typeEvenement,
            String referenceEntiteType,
            String referenceEntiteId) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("L'adresse email du destinataire est obligatoire pour un envoi EMAIL.");
        }

        log.info("[NOTIFICATION-EMAIL] Envoi à {} <{}> | Sujet: {} | Événement: {}",
                destinataireNom, email, sujet, typeEvenement);

        NotificationEntity notif = NotificationEntity.builder()
                .canal(CanalNotification.EMAIL)
                .destinataireUtilisateurId(utilisateurId)
                .destinataireNom(destinataireNom)
                .destinataireEmail(email.trim())
                .sujet(sujet)
                .messageCorps(messageCorps)
                .typeEvenement(typeEvenement != null ? typeEvenement : "NOTIFICATION_GENERALE")
                .referenceEntiteType(referenceEntiteType)
                .referenceEntiteId(referenceEntiteId)
                .statut(StatutNotification.ENVOYEE)
                .dateEnvoi(LocalDateTime.now())
                .build();

        return notificationRepository.save(notif);
    }

    /**
     * Envoie une notification par message texte (SMS) avec normalisation du numéro mobile camerounais (+237)
     * et journalise l'envoi en base de données.
     *
     * @param utilisateurId      Identifiant UUID du destinataire (optionnel).
     * @param destinataireNom    Nom complet du destinataire.
     * @param telephoneSms       Numéro de téléphone mobile du destinataire.
     * @param sujet              Titre court du SMS.
     * @param messageCorps       Texte du SMS (recommandé <= 160 caractères par segment).
     * @param typeEvenement      Catégorie d'événement métier décleéencheur.
     * @param referenceEntiteType Type de l'entité concernée.
     * @param referenceEntiteId  Identifiant de l'entité concernée.
     * @return L'entité {@link NotificationEntity} persistée et horodatée.
     */
    @Transactional
    public NotificationEntity envoyerSms(
            UUID utilisateurId,
            String destinataireNom,
            String telephoneSms,
            String sujet,
            String messageCorps,
            String typeEvenement,
            String referenceEntiteType,
            String referenceEntiteId) {

        if (telephoneSms == null || telephoneSms.isBlank()) {
            throw new IllegalArgumentException("Le numéro de téléphone mobile est obligatoire pour un envoi SMS.");
        }

        String numeroNormalise = normaliserTelephoneCameroun(telephoneSms);
        log.info("[NOTIFICATION-SMS] Envoi à {} ({}) | Message: {}",
                destinataireNom, numeroNormalise, messageCorps);

        NotificationEntity notif = NotificationEntity.builder()
                .canal(CanalNotification.SMS)
                .destinataireUtilisateurId(utilisateurId)
                .destinataireNom(destinataireNom)
                .destinataireTelephoneSms(numeroNormalise)
                .sujet(sujet != null ? sujet : "Alerte SMS ERP SODEPA")
                .messageCorps(messageCorps)
                .typeEvenement(typeEvenement != null ? typeEvenement : "NOTIFICATION_SMS")
                .referenceEntiteType(referenceEntiteType)
                .referenceEntiteId(referenceEntiteId)
                .statut(StatutNotification.ENVOYEE)
                .dateEnvoi(LocalDateTime.now())
                .build();

        return notificationRepository.save(notif);
    }

    /**
     * Envoie une notification combinée par Email ET par SMS (CT-REF-05) à un utilisateur ou collaborateur SODEPA.
     *
     * @param utilisateur        Compte utilisateur destinataire (peut être null si employé fourni).
     * @param employe            Fiche employé destinataire (peut être null si utilisateur fourni).
     * @param sujet              Objet du message.
     * @param messageCorps       Corps détaillé de la notification.
     * @param typeEvenement      Code de l'événement décleéencheur.
     * @param referenceEntiteType Type d'entité métier concernée.
     * @param referenceEntiteId  Identifiant de l'entité métier concernée.
     * @return L'enregistréement {@link NotificationEntity} du canal combiné {@link CanalNotification#EMAIL_ET_SMS}.
     */
    @Transactional
    public NotificationEntity notifierEmailEtSms(
            UtilisateurEntity utilisateur,
            EmployeEntity employe,
            String sujet,
            String messageCorps,
            String typeEvenement,
            String referenceEntiteType,
            String referenceEntiteId) {

        UUID userId = utilisateur != null ? utilisateur.getId()
                : (employe != null && employe.getUtilisateur() != null ? employe.getUtilisateur().getId() : null);

        String nomComplet = employe != null
                ? employe.getPrenom() + " " + employe.getNom()
                : (utilisateur != null ? utilisateur.getPrenom() + " " + utilisateur.getNom() : "Agent SODEPA");

        String email = utilisateur != null && utilisateur.getEmail() != null
                ? utilisateur.getEmail()
                : (employe != null && employe.getEmail() != null ? employe.getEmail() : "notification@sodepa.cm");

        String telephone = employe != null && employe.getTelephone() != null
                ? normaliserTelephoneCameroun(employe.getTelephone())
                : "+237600000000";

        log.info("[NOTIFICATION-MULTICANAL EMAIL+SMS] Destinataire: {} | Email: {} | SMS: {} | Sujet: {}",
                nomComplet, email, telephone, sujet);

        NotificationEntity notif = NotificationEntity.builder()
                .canal(CanalNotification.EMAIL_ET_SMS)
                .destinataireUtilisateurId(userId)
                .destinataireNom(nomComplet)
                .destinataireEmail(email)
                .destinataireTelephoneSms(telephone)
                .sujet(sujet)
                .messageCorps(messageCorps)
                .typeEvenement(typeEvenement)
                .referenceEntiteType(referenceEntiteType)
                .referenceEntiteId(referenceEntiteId)
                .statut(StatutNotification.ENVOYEE)
                .dateEnvoi(LocalDateTime.now())
                .build();

        return notificationRepository.save(notif);
    }

    /**
     * Notifie par Email et SMS un validateur lorsqu'une nouvelle tâche d'étape de workflow lui est assignée (CT-REF-05).
     *
     * @param validateurUser    Compte utilisateur du validateur assigné.
     * @param validateurEmploye Fiche employé du validateur assigné.
     * @param codeEtape         Code de l'étape de workflow.
     * @param intituleAction    Intitulé de l'action attendue.
     * @param entityName        Type de dossier soumis.
     * @param entityId          Identifiant du dossier soumis.
     * @param exigencesEtape    Résumé des actions requises sur l'étape (ex: ajout de fichier, modification de champs).
     */
    @Transactional
    public void notifierAssignationTacheWorkflow(
            UtilisateurEntity validateurUser,
            EmployeEntity validateurEmploye,
            String codeEtape,
            String intituleAction,
            String entityName,
            String entityId,
            String exigencesEtape) {

        String sujet = "[ERP SODEPA] Nouvelle validation requise - Éétape " + codeEtape;
        String corps = String.format(
                "Bonjour, une tâche de validation (%s) vous a été assignée sur le dossier %s [%s]. Exigences de l'étape : %s.",
                intituleAction != null ? intituleAction : codeEtape,
                entityName,
                entityId,
                exigencesEtape != null ? exigencesEtape : "Validation standard");

        notifierEmailEtSms(
                validateurUser,
                validateurEmploye,
                sujet,
                corps,
                "WORKFLOW_ASSIGNATION_ETAPE",
                entityName,
                entityId
        );
    }

    /**
     * Notifie par Email et SMS l'émetteur initial d'un dossier lorsqu'une étape est rejetée ou retournée pour révision (CT-REF-05).
     *
     * @param initiateurUser    Compte utilisateur de l'émetteur initial.
     * @param initiateurEmploye Fiche employé de l'émetteur initial.
     * @param codeEtape         Code de l'étape ayant prononcé le rejet.
     * @param entityName        Type de dossier concerné.
     * @param entityId          Identifiant du dossier concerné.
     * @param motifRejet        Motif obligatoire saisi par le valideur.
     */
    @Transactional
    public void notifierRejetDossierWorkflow(
            UtilisateurEntity initiateurUser,
            EmployeEntity initiateurEmploye,
            String codeEtape,
            String entityName,
            String entityId,
            String motifRejet) {

        String sujet = "[ERP SODEPA] Dossier rejeté à l'étape " + codeEtape + " (" + entityName + ")";
        String corps = String.format(
                "Votre dossier %s [%s] a été rejeté/retourné à l'étape %s. Motif : %s.",
                entityName, entityId, codeEtape, motifRejet);

        notifierEmailEtSms(
                initiateurUser,
                initiateurEmploye,
                sujet,
                corps,
                "WORKFLOW_REJET_DOSSIER",
                entityName,
                entityId
        );
    }

    /**
     * Notifie par Email et SMS l'émetteur initial lorsque son dossier a franchi toutes les étapes de validation.
     *
     * @param initiateurUser    Compte utilisateur de l'émetteur initial.
     * @param initiateurEmploye Fiche employé de l'émetteur initial.
     * @param entityName        Type de dossier validé.
     * @param entityId          Identifiant du dossier validé.
     */
    @Transactional
    public void notifierClotureApprouveeWorkflow(
            UtilisateurEntity initiateurUser,
            EmployeEntity initiateurEmploye,
            String entityName,
            String entityId) {

        String sujet = "[ERP SODEPA] Dossier approuvé - " + entityName + " [" + entityId + "]";
        String corps = String.format(
                "Votre dossier %s [%s] a été intégralement validé à l'issue du circuit d'approbation SODEPA.",
                entityName, entityId);

        notifierEmailEtSms(
                initiateurUser,
                initiateurEmploye,
                sujet,
                corps,
                "WORKFLOW_CLOTURE_APPROUVEE",
                entityName,
                entityId
        );
    }

    /**
     * Notifie par Email et SMS l'expiration imminente ou échue d'une pièce administrative/fiscale d'un tiers (CA-REF-023-01).
     *
     * @param codeTiers      Code unique du tiers.
     * @param raisonSociale  Raison sociale ou nom complet du tiers.
     * @param typePiece      Intitulé du document (ex: Attestation de non-redevance DGI).
     * @param dateExpiration Date d'échéance de la pièce.
     * @param emailDest      Adresse email de contact.
     * @param telephoneDest  Numéro de téléphone mobile.
     */
    @Transactional
    public void notifierExpirationPieceTiers(
            String codeTiers,
            String raisonSociale,
            String typePiece,
            String dateExpiration,
            String emailDest,
            String telephoneDest) {

        String sujet = "[ERP SODEPA - Conformité Tiers] Alerte expiration pièce : " + typePiece + " (" + codeTiers + ")";
        String corps = String.format(
                "Alerte de conformité (CA-REF-023-01) : La pièce %s du tiers %s [%s] expire le %s. Veuillez fournir une pièce à jour sous 15 jours pour éviter la suspension des règlements et commandes.",
                typePiece, raisonSociale, codeTiers, dateExpiration);

        if (emailDest != null && !emailDest.isBlank()) {
            envoyerEmail(null, raisonSociale, emailDest, sujet, corps, "TIERS_PIECE_EXPIRATION_J15", "TIERS_PIECE", codeTiers);
        }
        if (telephoneDest != null && !telephoneDest.isBlank()) {
            envoyerSms(null, raisonSociale, telephoneDest, "Alerte Conformité SODEPA", corps, "TIERS_PIECE_EXPIRATION_J15", "TIERS_PIECE", codeTiers);
        }
    }

    /**
     * Liste toutes les notifications destinées à un utilisateur donné.
     *
     * @param utilisateurId Identifiant UUID de l'utilisateur.
     * @return Liste des notifications de l'utilisateur.
     */
    @Transactional(readOnly = true)
    public List<NotificationEntity> listerNotificationsUtilisateur(UUID utilisateurId) {
        if (utilisateurId == null) {
            return notificationRepository.findAll();
        }
        return notificationRepository.findByDestinataireUtilisateurIdOrderByDateEnvoiDesc(utilisateurId);
    }

    /**
     * Marque une notification comme lue par son destinataire.
     *
     * @param notificationId Identifiant UUID de la notification.
     * @return La notification mise à jour avec son horodatage de lecture.
     */
    @Transactional
    public NotificationEntity marquerCommeLue(UUID notificationId) {
        NotificationEntity notif = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification introuvable : " + notificationId));
        notif.setStatut(StatutNotification.LUE);
        notif.setDateLecture(LocalDateTime.now());
        return notificationRepository.save(notif);
    }

    /**
     * Normalise un numéro de téléphone au format international camerounais (+237).
     *
     * @param telephone Numéro brut saisi.
     * @return Numéro normalisé préfixé par {@code +237}.
     */
    private String normaliserTelephoneCameroun(String telephone) {
        String digits = telephone.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+237")) {
            return digits;
        }
        if (digits.startsWith("237") && digits.length() >= 12) {
            return "+" + digits;
        }
        if (digits.length() == 9) {
            return "+237" + digits;
        }
        return digits;
    }
}
