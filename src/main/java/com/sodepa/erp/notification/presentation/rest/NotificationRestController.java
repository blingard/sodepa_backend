package com.sodepa.erp.notification.presentation.rest;

import com.sodepa.erp.notification.application.NotificationService;
import com.sodepa.erp.notification.infrastructure.entities.NotificationEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Contrôleur REST exposant les services de notifications par Email, SMS et centre d'alertes interne (CT-REF-05).
 */
@RestController
@RequestMapping({"/api/notifications", "/api/v1/notifications"})
@RequiredArgsConstructor
@Tag(name = "Notifications (Email & SMS)", description = "API d'envoi et de suivi des notifications par Email et SMS du socleée SODEPA")
public class NotificationRestController {

    private final NotificationService notificationService;

    /**
     * Récupère l'historique des notifications (Email, SMS, In-App) d'un utilisateur ou l'ensemble des notifications.
     *
     * @param utilisateurId Identifiant UUID optionnel de l'utilisateur destinataire.
     * @return Liste chronologique des notifications.
     */
    @GetMapping
    @Operation(summary = "Lister les notifications (Email, SMS, In-App) d'un utilisateur")
    public ResponseEntity<List<NotificationEntity>> listerNotifications(
            @RequestParam(required = false) UUID utilisateurId) {
        return ResponseEntity.ok(notificationService.listerNotificationsUtilisateur(utilisateurId));
    }

    /**
     * Décleéenche l'envoi d'un courrier électronique (Email) avec journalisation dans le référentiel de notifications.
     *
     * @param utilisateurId       Identifiant UUID optionnel du destinataire.
     * @param destinataireNom     Nom complet du destinataire.
     * @param email               Adresse électronique du destinataire.
     * @param sujet               Objet de l'email.
     * @param messageCorps        Contenu textuel du message.
     * @param typeEvenement       Type d'événement métier.
     * @param referenceEntiteType Type d'entité métier associée.
     * @param referenceEntiteId   Identifiant de l'entité métier associée.
     * @return La notification crée et envoyée.
     */
    @PostMapping("/email")
    @Operation(summary = "Envoyer une notification par Email")
    public ResponseEntity<NotificationEntity> envoyerEmail(
            @RequestParam(required = false) UUID utilisateurId,
            @RequestParam String destinataireNom,
            @RequestParam String email,
            @RequestParam String sujet,
            @RequestParam String messageCorps,
            @RequestParam(defaultValue = "NOTIFICATION_EMAIL") String typeEvenement,
            @RequestParam(required = false) String referenceEntiteType,
            @RequestParam(required = false) String referenceEntiteId) {
        NotificationEntity saved = notificationService.envoyerEmail(
                utilisateurId, destinataireNom, email, sujet, messageCorps,
                typeEvenement, referenceEntiteType, referenceEntiteId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Décleéenche l'envoi d'un SMS à un collaborateur ou partenaire avec normalisation au format camerounais (+237).
     *
     * @param utilisateurId       Identifiant UUID optionnel du destinataire.
     * @param destinataireNom     Nom complet du destinataire.
     * @param telephoneSms        Numéro de téléphone mobile du destinataire.
     * @param sujet               Titre court de l'alerte SMS.
     * @param messageCorps        Texte du SMS.
     * @param typeEvenement       Type d'événement métier.
     * @param referenceEntiteType Type d'entité métier associée.
     * @param referenceEntiteId   Identifiant de l'entité métier associée.
     * @return La notification SMS crée et envoyée.
     */
    @PostMapping("/sms")
    @Operation(summary = "Envoyer une notification par SMS")
    public ResponseEntity<NotificationEntity> envoyerSms(
            @RequestParam(required = false) UUID utilisateurId,
            @RequestParam String destinataireNom,
            @RequestParam String telephoneSms,
            @RequestParam(defaultValue = "Alerte SMS SODEPA") String sujet,
            @RequestParam String messageCorps,
            @RequestParam(defaultValue = "NOTIFICATION_SMS") String typeEvenement,
            @RequestParam(required = false) String referenceEntiteType,
            @RequestParam(required = false) String referenceEntiteId) {
        NotificationEntity saved = notificationService.envoyerSms(
                utilisateurId, destinataireNom, telephoneSms, sujet, messageCorps,
                typeEvenement, referenceEntiteType, referenceEntiteId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Marque une notification comme lue et enregistrée la date/heure de lecture.
     *
     * @param id Identifiant UUID de la notification.
     * @return La notification mise à jour.
     */
    @PatchMapping("/{id}/lue")
    @Operation(summary = "Marquer une notification comme lue")
    public ResponseEntity<NotificationEntity> marquerCommeLue(@PathVariable UUID id) {
        return ResponseEntity.ok(notificationService.marquerCommeLue(id));
    }
}
