package com.sodepa.erp.notification.presentation.rest;

import com.sodepa.erp.notification.application.governance.NotificationRuleService;
import com.sodepa.erp.notification.application.template.NotificationTemplateService;
import com.sodepa.erp.notification.application.template.RenderedNotificationContent;
import com.sodepa.erp.notification.infrastructure.entities.NotificationOutboxEntity;
import com.sodepa.erp.notification.infrastructure.entities.NotificationRuleEntity;
import com.sodepa.erp.notification.infrastructure.entities.NotificationTemplateEntity;
import com.sodepa.erp.notification.infrastructure.entities.OutboxStatus;
import com.sodepa.erp.notification.infrastructure.outbox.NotificationOutboxService;
import com.sodepa.erp.notification.infrastructure.repo.NotificationOutboxRepository;
import com.sodepa.erp.notification.infrastructure.repo.NotificationRepository;
import com.sodepa.erp.notification.presentation.dto.PreviewNotificationTemplateRequest;
import com.sodepa.erp.notification.presentation.dto.UpsertNotificationRuleRequest;
import com.sodepa.erp.notification.presentation.dto.UpsertNotificationTemplateRequest;
import com.sodepa.erp.share.UserData;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.share.erreurs.RessourceIntrouvableException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Contrôleur d'administration des règles de routage multi-canal, des templates HTML sûrs et du suivi Outbox/DLQ.
 */
@RestController
@RequestMapping({"/api/admin/notifications", "/api/v1/admin/notifications"})
@PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('WORKFLOW_CONFIG', 'WORKFLOW_SUPERVISION', 'VALIDATE_NOTIFICATION')")
@RequiredArgsConstructor
@Tag(name = "Administration Notifications", description = "Gestion des règles événement × canal, modèles HTML sécurisés et supervision Outbox/DLQ")
public class NotificationAdminController {

    private final NotificationRuleService ruleService;
    private final NotificationTemplateService templateService;
    private final NotificationOutboxRepository outboxRepository;
    private final NotificationOutboxService outboxService;
    private final NotificationRepository notificationRepository;
    private final UtilsService utilsService;

    @GetMapping("/rules")
    @Operation(summary = "Lister toutes les règles de notification événement × canal")
    public ResponseEntity<List<NotificationRuleEntity>> listRules() {
        return ResponseEntity.ok(ruleService.listAllRules());
    }

    @GetMapping("/rules/{typeEvenement}")
    @Operation(summary = "Lister les règles associées à un type d'événement")
    public ResponseEntity<List<NotificationRuleEntity>> getRulesForEvent(@PathVariable String typeEvenement) {
        return ResponseEntity.ok(ruleService.getRulesForEvent(typeEvenement));
    }

    @PutMapping("/rules")
    @Operation(summary = "Créer ou mettre à jour une règle événement × canal (règles critiques non désactivables)")
    public ResponseEntity<NotificationRuleEntity> upsertRule(@Valid @RequestBody UpsertNotificationRuleRequest request) {
        String actor = currentUsername();
        return ResponseEntity.ok(ruleService.upsertRule(request, actor));
    }

    @GetMapping("/templates")
    @Operation(summary = "Lister les modèles de notification enregistrés en base")
    public ResponseEntity<List<NotificationTemplateEntity>> listTemplates() {
        return ResponseEntity.ok(templateService.listAllTemplates());
    }

    @PutMapping("/templates")
    @Operation(summary = "Créer ou mettre à jour un modèle HTML/texte sécurisé (validation Anti-SSTI / Anti-XSS)")
    public ResponseEntity<NotificationTemplateEntity> upsertTemplate(
            @Valid @RequestBody UpsertNotificationTemplateRequest request) {
        return ResponseEntity.ok(templateService.upsertTemplate(request, currentUsername()));
    }

    @PostMapping("/templates/preview")
    @Operation(summary = "Prévisualiser le rendu sécurisé d'un modèle avec variables de test")
    public ResponseEntity<RenderedNotificationContent> previewTemplate(
            @Valid @RequestBody PreviewNotificationTemplateRequest request) {
        return ResponseEntity.ok(templateService.preview(request));
    }

    @GetMapping("/outbox/stats")
    @Operation(summary = "Consulter les compteurs par statut de l'Outbox des notifications")
    public ResponseEntity<Map<String, Long>> outboxStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (OutboxStatus status : OutboxStatus.values()) {
            stats.put(status.name(), outboxRepository.countByStatusIn(List.of(status)));
        }
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/outbox/{outboxId}/retry")
    @Operation(summary = "Réinjecter manuellement une entrée Outbox échouée ou en DLQ")
    public ResponseEntity<NotificationOutboxEntity> retryOutbox(@PathVariable UUID outboxId) {
        NotificationOutboxEntity entry = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new RessourceIntrouvableException("Entrée Outbox introuvable : " + outboxId));
        entry.setStatus(OutboxStatus.PENDING);
        entry.setRetryCount(0);
        entry.setNextRetryAt(LocalDateTime.now());
        entry.setLastError(null);
        entry.setPublishedAt(null);
        entry.setProcessedAt(null);
        NotificationOutboxEntity saved = outboxRepository.save(entry);

        notificationRepository.findById(saved.getNotificationId()).ifPresent(notif -> {
            notif.setRetryCount(0);
            notif.setNextRetryAt(null);
            notif.setErreurTechnique(null);
            notificationRepository.save(notif);
        });
        outboxService.publishToBroker(saved.getId());
        return ResponseEntity.ok(saved);
    }

    private String currentUsername() {
        UserData userData = utilsService != null ? utilsService.getCurrentUserData() : null;
        return userData != null && userData.username() != null ? userData.username() : "admin";
    }
}
