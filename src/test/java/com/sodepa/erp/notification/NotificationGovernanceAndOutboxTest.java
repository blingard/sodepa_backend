package com.sodepa.erp.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.configuration.RabbitMqConfig;
import com.sodepa.erp.notification.application.NotificationService;
import com.sodepa.erp.notification.application.governance.NotificationPreferenceService;
import com.sodepa.erp.notification.application.governance.NotificationRuleService;
import com.sodepa.erp.notification.application.template.NotificationTemplateService;
import com.sodepa.erp.notification.application.template.SafeHtmlTemplateEngine;
import com.sodepa.erp.notification.infrastructure.config.NotificationProperties;
import com.sodepa.erp.notification.infrastructure.entities.*;
import com.sodepa.erp.notification.infrastructure.metrics.NotificationMetrics;
import com.sodepa.erp.notification.infrastructure.outbox.NotificationDispatchMessage;
import com.sodepa.erp.notification.infrastructure.outbox.NotificationOutboxService;
import com.sodepa.erp.notification.infrastructure.repo.NotificationOutboxRepository;
import com.sodepa.erp.notification.infrastructure.repo.NotificationRepository;
import com.sodepa.erp.notification.infrastructure.repo.NotificationRuleRepository;
import com.sodepa.erp.notification.infrastructure.repo.NotificationTemplateRepository;
import com.sodepa.erp.notification.infrastructure.repo.NotificationUserPreferenceRepository;
import com.sodepa.erp.notification.presentation.dto.UpsertNotificationRuleRequest;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationGovernanceAndOutboxTest {

    @Test
    @DisplayName("Règles critiques non désactivables & opt-out utilisateur ignoré sur événement critique")
    void shouldProtectCriticalRulesAndBypassUserOptOutForCriticalEvents() {
        NotificationRuleRepository ruleRepo = mock(NotificationRuleRepository.class);
        NotificationRuleEntity criticalRule = NotificationRuleEntity.builder()
                .typeEvenement("IAM_DESACTIVATION_AUTOMATIQUE")
                .canal(CanalNotification.EMAIL)
                .actif(true)
                .critique(true)
                .ordrePriorite(10)
                .build();
        NotificationRuleEntity optionalSmsRule = NotificationRuleEntity.builder()
                .typeEvenement("WORKFLOW_CLOTURE_APPROUVEE")
                .canal(CanalNotification.EMAIL)
                .actif(true)
                .critique(false)
                .ordrePriorite(10)
                .build();

        when(ruleRepo.findByTypeEvenementAndCanal("IAM_DESACTIVATION_AUTOMATIQUE", CanalNotification.EMAIL))
                .thenReturn(Optional.of(criticalRule));
        when(ruleRepo.findByTypeEvenementOrderByOrdrePrioriteAsc("IAM_DESACTIVATION_AUTOMATIQUE"))
                .thenReturn(List.of(criticalRule));
        when(ruleRepo.findByTypeEvenementOrderByOrdrePrioriteAsc("WORKFLOW_CLOTURE_APPROUVEE"))
                .thenReturn(List.of(optionalSmsRule));

        NotificationRuleService service = new NotificationRuleService(ruleRepo);

        // Un admin ne peut pas désactiver une règle critique
        assertThrows(IllegalArgumentException.class, () -> service.upsertRule(
                new UpsertNotificationRuleRequest("IAM_DESACTIVATION_AUTOMATIQUE", CanalNotification.EMAIL,
                        false, true, 10, null, "Tentative désactivation"),
                "admin_maker"
        ));

        // L'utilisateur a désactivé EMAIL dans ses préférences
        NotificationUserPreferenceEntity prefsOptOut = NotificationUserPreferenceEntity.builder()
                .utilisateurId(UUID.randomUUID())
                .emailEnabled(false)
                .smsEnabled(false)
                .inAppEnabled(true)
                .build();

        // Événement non critique -> opt-out respecté (EMAIL désactivé)
        var nonCriticalDecisions = service.resolveChannelDecisions(
                "WORKFLOW_CLOTURE_APPROUVEE", Set.of(CanalNotification.EMAIL), prefsOptOut, true, true);
        assertFalse(nonCriticalDecisions.get(0).enabled());
        assertEquals("USER_OPT_OUT", nonCriticalDecisions.get(0).skipCode());

        // Événement critique IAM -> opt-out contourné (EMAIL actif!)
        var criticalDecisions = service.resolveChannelDecisions(
                "IAM_DESACTIVATION_AUTOMATIQUE", Set.of(CanalNotification.EMAIL), prefsOptOut, true, true);
        assertTrue(criticalDecisions.get(0).enabled());
        assertTrue(criticalDecisions.get(0).critical());
    }

    @Test
    @DisplayName("NotificationService : 1 ligne par canal via groupId, aucun faux numéro +237600000000 et cloche IN_APP stricte")
    void shouldSplitChannelsWithoutFakeFallbacksAndFilterBellToInApp() {
        NotificationRepository notifRepo = mock(NotificationRepository.class);
        NotificationOutboxService outboxService = mock(NotificationOutboxService.class);
        NotificationRuleRepository ruleRepo = mock(NotificationRuleRepository.class);
        NotificationUserPreferenceRepository prefRepo = mock(NotificationUserPreferenceRepository.class);
        NotificationTemplateRepository tplRepo = mock(NotificationTemplateRepository.class);

        when(ruleRepo.findByTypeEvenementOrderByOrdrePrioriteAsc("WORKFLOW_ASSIGNATION_ETAPE")).thenReturn(List.of(
                NotificationRuleEntity.builder().typeEvenement("WORKFLOW_ASSIGNATION_ETAPE").canal(CanalNotification.EMAIL).actif(true).critique(true).ordrePriorite(10).build(),
                NotificationRuleEntity.builder().typeEvenement("WORKFLOW_ASSIGNATION_ETAPE").canal(CanalNotification.IN_APP).actif(true).critique(true).ordrePriorite(10).build(),
                NotificationRuleEntity.builder().typeEvenement("WORKFLOW_ASSIGNATION_ETAPE").canal(CanalNotification.SMS).actif(true).critique(true).ordrePriorite(20).build()
        ));
        UUID userId = UUID.randomUUID();
        when(prefRepo.findByUtilisateurId(userId)).thenReturn(Optional.empty());
        when(tplRepo.findByTypeEvenementAndLangueAndActifTrue(anyString(), anyString())).thenReturn(Optional.empty());

        List<NotificationEntity> savedEntities = new ArrayList<>();
        when(notifRepo.save(any(NotificationEntity.class))).thenAnswer(inv -> {
            NotificationEntity e = inv.getArgument(0);
            if (e.getId() == null) {
                e.setId(UUID.randomUUID());
            }
            savedEntities.add(e);
            return e;
        });

        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        NotificationService service = new NotificationService(
                notifRepo,
                outboxService,
                new NotificationRuleService(ruleRepo),
                new NotificationPreferenceService(prefRepo),
                new NotificationTemplateService(tplRepo, new SafeHtmlTemplateEngine()),
                new NotificationMetrics(meterRegistry, null),
                new ObjectMapper()
        );

        UtilisateurEntity userSansTel = UtilisateurEntity.builder()
                .id(userId)
                .username("marie.mbarga")
                .nom("MBARGA")
                .prenom("Marie")
                .email("marie.mbarga@sodepa.cm")
                .telephones(Set.of()) // Aucun numéro !
                .build();

        service.notifierAssignationTacheWorkflow(
                userSansTel, null, "CHEF_SERVICE", "Chef de Service", "EXPRESSION_BESOIN", "EB-42", "Visa technique");

        // Vérifier : EMAIL=PENDING+Outbox, IN_APP=SENT, SMS=SKIPPED (jamais de faux +237600000000!), groupId partagé
        assertEquals(3, savedEntities.size());
        UUID sharedGroup = savedEntities.get(0).getGroupId();
        assertNotNull(sharedGroup);
        assertTrue(savedEntities.stream().allMatch(e -> sharedGroup.equals(e.getGroupId())));

        NotificationEntity emailRow = savedEntities.stream().filter(e -> e.getCanal() == CanalNotification.EMAIL).findFirst().orElseThrow();
        NotificationEntity inAppRow = savedEntities.stream().filter(e -> e.getCanal() == CanalNotification.IN_APP).findFirst().orElseThrow();
        NotificationEntity smsRow = savedEntities.stream().filter(e -> e.getCanal() == CanalNotification.SMS).findFirst().orElseThrow();

        assertEquals(StatutNotification.PENDING, emailRow.getStatut());
        assertEquals(StatutNotification.SENT, inAppRow.getStatut());
        assertEquals(StatutNotification.SKIPPED, smsRow.getStatut());
        assertNull(smsRow.getDestinataireTelephoneSms());
        assertEquals("MISSING_CONTACT", smsRow.getProviderName());

        // Seules les entrées externes (ici EMAIL) entrent dans l'Outbox
        verify(outboxService, times(1)).enqueue(emailRow);
        verify(outboxService, never()).enqueue(smsRow);
        verify(outboxService, never()).enqueue(inAppRow);

        // Vérifier la cloche UI : appel strict avec CanalNotification.IN_APP
        service.listerNotificationsEnAttenteUtilisateur(userId);
        verify(notifRepo).findByDestinataireUtilisateurIdAndCanalAndStatutInOrderByDateEnvoiDesc(
                eq(userId),
                eq(CanalNotification.IN_APP),
                argThat(list -> list.contains(StatutNotification.PENDING) && list.contains(StatutNotification.SENT))
        );
    }

    @Test
    @DisplayName("Outbox : backoff exponentiel borné et bascule vers RabbitMQ DLQ après épuisement des tentatives")
    void shouldMoveExhaustedOutboxEntryToDeadLetterQueue() {
        NotificationOutboxRepository outboxRepo = mock(NotificationOutboxRepository.class);
        NotificationRepository notifRepo = mock(NotificationRepository.class);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        NotificationProperties props = new NotificationProperties();
        props.setMaxRetries(3);
        props.setInitialBackoffSeconds(10);

        UUID notifId = UUID.randomUUID();
        UUID outboxId = UUID.randomUUID();

        NotificationOutboxEntity outbox = NotificationOutboxEntity.builder()
                .id(outboxId)
                .notificationId(notifId)
                .canal(CanalNotification.SMS)
                .typeEvenement("WORKFLOW_REJET_DOSSIER")
                .payloadJson("{}")
                .status(OutboxStatus.FAILED)
                .retryCount(2) // Prochaine tentative = 3 == maxRetries
                .build();

        NotificationEntity notif = NotificationEntity.builder()
                .id(notifId)
                .canal(CanalNotification.SMS)
                .statut(StatutNotification.PENDING)
                .build();

        when(outboxRepo.findById(outboxId)).thenReturn(Optional.of(outbox));
        when(notifRepo.findById(notifId)).thenReturn(Optional.of(notif));

        NotificationOutboxService service = new NotificationOutboxService(
                outboxRepo, notifRepo, props, new NotificationMetrics(null, null),
                new ObjectMapper(), eventPublisher, rabbitTemplate
        );

        assertEquals(10L, service.computeBackoffSeconds(1));
        assertEquals(20L, service.computeBackoffSeconds(2));
        assertEquals(40L, service.computeBackoffSeconds(3));

        service.recordFailure(outboxId, notifId, "HTTP 503 Gateway Timeout", true);

        assertEquals(OutboxStatus.DEAD_LETTER, outbox.getStatus());
        assertEquals(3, outbox.getRetryCount());
        assertEquals(StatutNotification.FAILED, notif.getStatut());
        assertNull(notif.getNextRetryAt());
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMqConfig.NOTIF_DLX_EXCHANGE),
                eq(RabbitMqConfig.ROUTING_NOTIF_DLQ),
                any(NotificationDispatchMessage.class)
        );
    }
}
