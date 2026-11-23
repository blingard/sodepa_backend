package com.sodepa.erp.notification.presentation.rest;

import com.sodepa.erp.notification.application.NotificationService;
import com.sodepa.erp.notification.application.governance.NotificationPreferenceService;
import com.sodepa.erp.notification.infrastructure.entities.NotificationEntity;
import com.sodepa.erp.notification.infrastructure.entities.NotificationUserPreferenceEntity;
import com.sodepa.erp.notification.presentation.dto.DispatchNotificationRequest;
import com.sodepa.erp.notification.presentation.dto.UpdateNotificationPreferencesRequest;
import com.sodepa.erp.utils.Permissions;
import com.sodepa.erp.share.UserData;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.share.erreurs.AccesRefuseException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Contrôleur REST exposant les services de notifications (Email, SMS, WhatsApp, In-App) et préférences utilisateur.
 */
@RestController
@RequestMapping({"/api/notifications", "/api/v1/notifications"})
@RequiredArgsConstructor
@Tag(name = "Notifications (Multi-Canal)", description = "API d'envoi, consultation et gestion des préférences de notifications ERP SODEPA")
public class NotificationRestController {

    private final NotificationService notificationService;
    private final NotificationPreferenceService preferenceService;
    private final UtilsService utilsService;

    /**
     * Récupère l'historique des notifications d'un utilisateur avec cloisonnement anti-IDOR.
     */
    @GetMapping
    @Operation(summary = "Lister les notifications (Email, SMS, WhatsApp, In-App) d'un utilisateur")
    public ResponseEntity<List<NotificationEntity>> listerNotifications(
            @RequestParam(required = false) UUID utilisateurId) {
        UUID targetId = resolveAuthorizedTargetUserId(utilisateurId);
        return ResponseEntity.ok(notificationService.listerNotificationsUtilisateur(targetId));
    }

    /**
     * Récupère l'historique paginé des notifications d'un utilisateur avec cloisonnement anti-IDOR.
     */
    @GetMapping("/page")
    @Operation(summary = "Lister de façon paginée les notifications d'un utilisateur")
    public ResponseEntity<Page<NotificationEntity>> listerNotificationsPaginees(
            @RequestParam(required = false) UUID utilisateurId,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID targetId = resolveAuthorizedTargetUserId(utilisateurId);
        return ResponseEntity.ok(notificationService.listerNotificationsUtilisateur(targetId, pageable));
    }

    /**
     * Récupère uniquement les notifications In-App non lues pour la cloche d'alertes UI.
     */
    @GetMapping("/en-attente")
    @Operation(summary = "Lister les notifications In-App non lues (cloche UI) de l'utilisateur connecté")
    public ResponseEntity<List<NotificationEntity>> listerNotificationsEnAttente() {
        UserData userData = utilsService.getCurrentUserData();
        UUID currentUserId = parseUuidSafe(userData.userId());
        return ResponseEntity.ok(notificationService.listerNotificationsEnAttenteUtilisateur(currentUserId));
    }

    /**
     * Déclenche l'envoi asynchrone d'un courrier électronique (Email).
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
        verifierHabilitationEnvoi();
        NotificationEntity saved = notificationService.envoyerEmail(
                utilisateurId, destinataireNom, email, sujet, messageCorps,
                typeEvenement, referenceEntiteType, referenceEntiteId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Déclenche l'envoi asynchrone d'un SMS avec normalisation E.164 CEMAC (+237).
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
        verifierHabilitationEnvoi();
        NotificationEntity saved = notificationService.envoyerSms(
                utilisateurId, destinataireNom, telephoneSms, sujet, messageCorps,
                typeEvenement, referenceEntiteType, referenceEntiteId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Déclenche l'envoi asynchrone d'une notification WhatsApp Business (avec repli SMS automatique si configuré).
     */
    @PostMapping("/whatsapp")
    @Operation(summary = "Envoyer une notification par WhatsApp Business")
    public ResponseEntity<NotificationEntity> envoyerWhatsApp(
            @RequestParam(required = false) UUID utilisateurId,
            @RequestParam String destinataireNom,
            @RequestParam String telephoneWhatsapp,
            @RequestParam(defaultValue = "Alerte WhatsApp SODEPA") String sujet,
            @RequestParam String messageCorps,
            @RequestParam(defaultValue = "NOTIFICATION_WHATSAPP") String typeEvenement,
            @RequestParam(required = false) String referenceEntiteType,
            @RequestParam(required = false) String referenceEntiteId) {
        verifierHabilitationEnvoi();
        NotificationEntity saved = notificationService.envoyerWhatsApp(
                utilisateurId, destinataireNom, telephoneWhatsapp, sujet, messageCorps,
                typeEvenement, referenceEntiteType, referenceEntiteId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Émet une notification multi-canal pilotée par les règles et modèles en base de données.
     */
    @PostMapping("/dispatch")
    @Operation(summary = "Émettre une notification multi-canal gouvernée par règles et templates")
    public ResponseEntity<List<NotificationEntity>> dispatchMultiChannel(
            @Valid @RequestBody DispatchNotificationRequest request) {
        verifierHabilitationEnvoi();
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.dispatchMultiChannel(request));
    }

    /**
     * Marque une notification comme lue ({@code READ}) et enregistre la date/heure de lecture.
     */
    @PatchMapping("/{id}/lue")
    @Operation(summary = "Marquer une notification comme lue")
    public ResponseEntity<NotificationEntity> marquerCommeLue(@PathVariable UUID id) {
        UserData userData = utilsService.getCurrentUserData();
        return ResponseEntity.ok(notificationService.marquerCommeLue(
                id, parseUuidSafe(userData.userId()), isAdminOrSupervisor(userData)));
    }

    /**
     * Consulte les préférences de notification de l'utilisateur authentifié.
     */
    @GetMapping("/preferences")
    @Operation(summary = "Consulter mes préférences de réception des notifications")
    public ResponseEntity<NotificationUserPreferenceEntity> getMyPreferences() {
        UserData userData = utilsService.getCurrentUserData();
        UUID currentUserId = parseUuidSafe(userData != null ? userData.userId() : null);
        return ResponseEntity.ok(preferenceService.getPreferences(currentUserId));
    }

    /**
     * Met à jour les préférences de notification de l'utilisateur authentifié (les alertes critiques restent envoyées).
     */
    @PutMapping("/preferences")
    @Operation(summary = "Mettre à jour mes préférences de réception des notifications")
    public ResponseEntity<NotificationUserPreferenceEntity> updateMyPreferences(
            @Valid @RequestBody UpdateNotificationPreferencesRequest request) {
        UserData userData = utilsService.getCurrentUserData();
        UUID currentUserId = parseUuidSafe(userData != null ? userData.userId() : null);
        if (currentUserId == null) {
            throw new AccesRefuseException("Utilisateur non authentifié.");
        }
        return ResponseEntity.ok(preferenceService.updatePreferences(currentUserId, request));
    }

    private UUID resolveAuthorizedTargetUserId(UUID requestedUserId) {
        UserData userData = utilsService.getCurrentUserData();
        UUID currentUserId = parseUuidSafe(userData != null ? userData.userId() : null);
        boolean isAdmin = isAdminOrSupervisor(userData);
        if (!isAdmin) {
            if (requestedUserId != null && currentUserId != null && !requestedUserId.equals(currentUserId)) {
                throw new AccesRefuseException("Vous ne pouvez consulter que vos propres notifications.");
            }
            return currentUserId;
        }
        return requestedUserId != null ? requestedUserId : currentUserId;
    }

    private boolean isAdminOrSupervisor(UserData userData) {
        if (userData == null) {
            return false;
        }
        Set<Permissions> perms = userData.permissions() != null ? userData.permissions() : Collections.emptySet();
        return userData.isAdmin()
                || "admin_maker".equalsIgnoreCase(userData.username())
                || perms.contains(Permissions.GET_FULL_USER_INFO)
                || perms.contains(Permissions.WORKFLOW_SUPERVISION)
                || perms.contains(Permissions.WORKFLOW_CONFIG)
                || perms.contains(Permissions.VALIDATE_NOTIFICATION);
    }

    private void verifierHabilitationEnvoi() {
        UserData userData = utilsService.getCurrentUserData();
        if (!isAdminOrSupervisor(userData)) {
            throw new AccesRefuseException("Vous ne disposez pas des droits nécessaires pour émettre une notification externe.");
        }
    }

    private UUID parseUuidSafe(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
