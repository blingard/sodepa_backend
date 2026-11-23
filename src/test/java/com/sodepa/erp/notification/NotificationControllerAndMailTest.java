package com.sodepa.erp.notification;

import com.sodepa.erp.notification.application.NotificationService;
import com.sodepa.erp.notification.application.governance.NotificationPreferenceService;
import com.sodepa.erp.notification.application.governance.NotificationRuleService;
import com.sodepa.erp.notification.application.template.NotificationTemplateService;
import com.sodepa.erp.notification.application.template.SafeHtmlTemplateEngine;
import com.sodepa.erp.notification.infrastructure.channel.NotificationChannelResult;
import com.sodepa.erp.notification.infrastructure.channel.email.EmailChannelAdapter;
import com.sodepa.erp.notification.infrastructure.config.NotificationProperties;
import com.sodepa.erp.notification.infrastructure.entities.CanalNotification;
import com.sodepa.erp.notification.infrastructure.entities.NotificationEntity;
import com.sodepa.erp.notification.infrastructure.entities.NotificationRuleEntity;
import com.sodepa.erp.notification.infrastructure.entities.StatutNotification;
import com.sodepa.erp.notification.infrastructure.repo.NotificationOutboxRepository;
import com.sodepa.erp.notification.infrastructure.repo.NotificationRepository;
import com.sodepa.erp.notification.infrastructure.repo.NotificationRuleRepository;
import com.sodepa.erp.notification.infrastructure.repo.NotificationTemplateRepository;
import com.sodepa.erp.notification.presentation.rest.NotificationAdminController;
import com.sodepa.erp.notification.presentation.rest.NotificationRestController;
import com.sodepa.erp.share.UserData;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.share.erreurs.GestionnaireErreursApi;
import com.sodepa.erp.utils.Permissions;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NotificationControllerAndMailTest {

    @Test
    @DisplayName("EmailChannelAdapter : construit un MimeMessage multipart HTML + texte avec redirection sandbox optionnelle")
    void shouldBuildMultipartMimeMessageWithOptionalSandboxRedirect() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationProperties props = new NotificationProperties();
        props.getMail().setEnabled(true);
        props.getMail().setFromAddress("noreply@sodepa.cm");
        props.getMail().setFromName("ERP SODEPA");
        props.getSandbox().setEnabled(true);
        props.getSandbox().setRedirectEmail("qa-inbox@sodepa.local");

        EmailChannelAdapter adapter = new EmailChannelAdapter(mailSender, props);
        NotificationChannelResult res = adapter.sendEmail(
                "real-employee@sodepa.cm",
                "Paul",
                "Validation Expression Besoin",
                "Texte brut",
                "<p>Bonjour <strong>Paul</strong></p>"
        );

        assertTrue(res.success());
        assertEquals("smtp-mailhog", res.providerName());
        verify(mailSender).send(mimeMessage);
        assertEquals("qa-inbox@sodepa.local", mimeMessage.getAllRecipients()[0].toString());
    }

    @Test
    @DisplayName("NotificationRestController : anti-IDOR sur /page & cloche /en-attente réservée IN_APP")
    void shouldEnforceAntiIdorOnPagedEndpointAndFilterBell() throws Exception {
        NotificationService notifService = mock(NotificationService.class);
        NotificationPreferenceService prefService = mock(NotificationPreferenceService.class);
        UtilsService utilsService = mock(UtilsService.class);

        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();
        when(utilsService.getCurrentUserData()).thenReturn(UserData.builder()
                .userId(aliceId.toString())
                .username("alice")
                .permissions(Set.of())
                .isAdmin(false)
                .build());

        when(notifService.listerNotificationsUtilisateur(eq(aliceId), any())).thenReturn(
                new PageImpl<>(List.of(NotificationEntity.builder()
                        .id(UUID.randomUUID())
                        .canal(CanalNotification.IN_APP)
                        .statut(StatutNotification.SENT)
                        .sujet("Test")
                        .messageCorps("Msg")
                        .typeEvenement("WORKFLOW_ASSIGNATION_ETAPE")
                        .build()), PageRequest.of(0, 10), 1)
        );
        when(notifService.listerNotificationsEnAttenteUtilisateur(aliceId)).thenReturn(List.of());

        MockMvc mvc = MockMvcBuilders.standaloneSetup(new NotificationRestController(notifService, prefService, utilsService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GestionnaireErreursApi())
                .build();

        // Anti-IDOR : Alice ne peut pas consulter les notifications paginées de Bob
        mvc.perform(get("/api/notifications/page").param("utilisateurId", bobId.toString()))
                .andExpect(status().isForbidden());

        // Consultation propre paginée OK
        mvc.perform(get("/api/notifications/page").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].canal").value("IN_APP"));

        // Cloche UI
        mvc.perform(get("/api/notifications/en-attente"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("NotificationAdminController : rejette désactivation règle critique et XSS dans template HTML (HTTP 400)")
    void shouldRejectCriticalDisableAndXssTemplateWithBadRequest() throws Exception {
        NotificationRuleRepository ruleRepo = mock(NotificationRuleRepository.class);
        when(ruleRepo.findByTypeEvenementAndCanal("WORKFLOW_REJET_DOSSIER", CanalNotification.EMAIL))
                .thenReturn(Optional.of(NotificationRuleEntity.builder()
                        .typeEvenement("WORKFLOW_REJET_DOSSIER")
                        .canal(CanalNotification.EMAIL)
                        .actif(true)
                        .critique(true)
                        .build()));

        NotificationTemplateRepository tplRepo = mock(NotificationTemplateRepository.class);
        UtilsService utilsService = mock(UtilsService.class);
        when(utilsService.getCurrentUserData()).thenReturn(UserData.builder()
                .userId(UUID.randomUUID().toString())
                .username("admin_maker")
                .permissions(Set.of(Permissions.WORKFLOW_CONFIG))
                .isAdmin(true)
                .build());

        NotificationAdminController adminCtrl = new NotificationAdminController(
                new NotificationRuleService(ruleRepo),
                new NotificationTemplateService(tplRepo, new SafeHtmlTemplateEngine()),
                mock(NotificationOutboxRepository.class),
                null,
                mock(NotificationRepository.class),
                utilsService
        );

        MockMvc mvc = MockMvcBuilders.standaloneSetup(adminCtrl)
                .setControllerAdvice(new GestionnaireErreursApi())
                .build();

        mvc.perform(put("/api/admin/notifications/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "typeEvenement": "WORKFLOW_REJET_DOSSIER",
                                  "canal": "EMAIL",
                                  "actif": false,
                                  "critique": true,
                                  "ordrePriorite": 10
                                }
                                """))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/admin/notifications/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "XSS_TEST_FR",
                                  "typeEvenement": "NOTIFICATION_GENERALE",
                                  "langue": "fr",
                                  "sujetTemplate": "Alerte",
                                  "bodyTextTemplate": "Bonjour {{destinataireNom}}",
                                  "bodyHtmlTemplate": "<div onclick='alert(1)'><script>steal()</script></div>"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
