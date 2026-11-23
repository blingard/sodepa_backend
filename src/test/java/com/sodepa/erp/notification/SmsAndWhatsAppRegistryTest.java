package com.sodepa.erp.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.notification.infrastructure.channel.NotificationChannelResult;
import com.sodepa.erp.notification.infrastructure.channel.NotificationDispatchService;
import com.sodepa.erp.notification.infrastructure.channel.email.EmailChannelAdapter;
import com.sodepa.erp.notification.infrastructure.channel.sms.AfricasTalkingSmsAdapter;
import com.sodepa.erp.notification.infrastructure.channel.sms.NexahSmsAdapter;
import com.sodepa.erp.notification.infrastructure.channel.sms.SandboxSmsAdapter;
import com.sodepa.erp.notification.infrastructure.channel.sms.SmsRegistry;
import com.sodepa.erp.notification.infrastructure.channel.whatsapp.MetaWhatsAppCloudAdapter;
import com.sodepa.erp.notification.infrastructure.channel.whatsapp.SandboxWhatsAppAdapter;
import com.sodepa.erp.notification.infrastructure.channel.whatsapp.WhatsAppRegistry;
import com.sodepa.erp.notification.infrastructure.config.NotificationProperties;
import com.sodepa.erp.notification.infrastructure.entities.CanalNotification;
import com.sodepa.erp.notification.infrastructure.entities.NotificationEntity;
import com.sodepa.erp.notification.infrastructure.entities.StatutNotification;
import com.sodepa.erp.notification.infrastructure.metrics.NotificationMetrics;
import com.sodepa.erp.notification.infrastructure.outbox.NotificationOutboxService;
import com.sodepa.erp.notification.infrastructure.repo.NotificationRepository;
import com.sodepa.erp.notification.infrastructure.util.LogMasker;
import com.sodepa.erp.notification.infrastructure.util.PhoneNumberNormalizer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class SmsAndWhatsAppRegistryTest {

    @Test
    @DisplayName("Normalisation téléphone CEMAC +237 et masquage PII dans les logs")
    void shouldNormalizeCemacPhonesAndMaskPii() {
        assertEquals("+237699123456", PhoneNumberNormalizer.normalize("699 12 34 56", "+237"));
        assertEquals("+237677001122", PhoneNumberNormalizer.normalize("237677001122", "+237"));
        assertEquals("+237655443322", PhoneNumberNormalizer.normalize("00237655443322", "+237"));
        assertNull(PhoneNumberNormalizer.normalize("invalid", "+237"));

        assertEquals("237699123456", PhoneNumberNormalizer.toMetaWhatsAppNumber("+237699123456"));
        assertEquals("je***@sodepa.cm", LogMasker.maskEmail("jean.dupont@sodepa.cm"));
        assertEquals("+237***3456", LogMasker.maskPhone("+237699123456"));
        assertEquals("401 Unauthorized Bearer ***", LogMasker.sanitizeError("401 Unauthorized Bearer secretToken123"));
    }

    @Test
    @DisplayName("SmsRegistry : bascule automatiquement d'Africa's Talking en panne HTTP vers Nexah (fallbackUsed=true)")
    void shouldFailoverFromAfricasTalkingToNexahWithFormUrlEncoded() {
        NotificationProperties props = new NotificationProperties();
        props.getSandbox().setEnabled(false);
        props.getSms().setProviderOrder(List.of("africa_talking", "nexah"));

        props.getSms().getAfricaTalking().setEnabled(true);
        props.getSms().getAfricaTalking().setBaseUrl("https://at.mock.test/version1/messaging");
        props.getSms().getAfricaTalking().setUsername("sodepa-user");
        props.getSms().getAfricaTalking().setApiKey("at-secret");

        props.getSms().getNexah().setEnabled(true);
        props.getSms().getNexah().setBaseUrl("https://nexah.mock.test/api/v1/sms/send");
        props.getSms().getNexah().setApiKey("nexah-secret");

        RestClient.Builder atBuilder = RestClient.builder();
        MockRestServiceServer atServer = MockRestServiceServer.bindTo(atBuilder).build();
        atServer.expect(requestTo("https://at.mock.test/version1/messaging"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("apiKey", "at-secret"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(containsString("to=%2B237699112233")))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        RestClient.Builder nexahBuilder = RestClient.builder();
        MockRestServiceServer nexahServer = MockRestServiceServer.bindTo(nexahBuilder).build();
        nexahServer.expect(requestTo("https://nexah.mock.test/api/v1/sms/send"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.mobiles").value("237699112233"))
                .andRespond(withSuccess("{\"responsecode\":1,\"messageId\":\"NX-999\"}", MediaType.APPLICATION_JSON));

        SmsRegistry registry = new SmsRegistry(
                props,
                List.of(
                        new AfricasTalkingSmsAdapter(props, atBuilder.build()),
                        new NexahSmsAdapter(props, nexahBuilder.build())
                ),
                new SandboxSmsAdapter(props)
        );

        NotificationChannelResult res = registry.sendSms("699112233", "Validation requise DA-2026-01", "WORKFLOW_ASSIGNATION_ETAPE");

        assertTrue(res.success());
        assertTrue(res.fallbackUsed());
        assertEquals("nexah", res.providerName());
        assertEquals("NX-999", res.providerMessageId());
        atServer.verify();
        nexahServer.verify();
    }

    @Test
    @DisplayName("MetaWhatsAppCloudAdapter : envoie le payload template HSM officiel avec paramètres ordonnés")
    void shouldSendMetaHsmTemplatePayload() {
        NotificationProperties props = new NotificationProperties();
        props.getSandbox().setEnabled(false);
        props.getWhatsapp().setProviderOrder(List.of("meta_whatsapp"));
        props.getWhatsapp().getMeta().setEnabled(true);
        props.getWhatsapp().getMeta().setApiUrl("https://graph.mock.test/v21.0");
        props.getWhatsapp().getMeta().setPhoneNumberId("100200300");
        props.getWhatsapp().getMeta().setAccessToken("meta-token");

        RestClient.Builder waBuilder = RestClient.builder();
        MockRestServiceServer waServer = MockRestServiceServer.bindTo(waBuilder).build();
        waServer.expect(requestTo("https://graph.mock.test/v21.0/100200300/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer meta-token"))
                .andExpect(jsonPath("$.messaging_product").value("whatsapp"))
                .andExpect(jsonPath("$.to").value("237699112233"))
                .andExpect(jsonPath("$.type").value("template"))
                .andExpect(jsonPath("$.template.name").value("sodepa_wf_rejet_dossier"))
                .andExpect(jsonPath("$.template.components[0].parameters[0].text").value("Paul"))
                .andExpect(jsonPath("$.template.components[0].parameters[1].text").value("EXPRESSION_BESOIN"))
                .andRespond(withSuccess("{\"messages\":[{\"id\":\"wamid.HBgM237\"}]}", MediaType.APPLICATION_JSON));

        WhatsAppRegistry waRegistry = new WhatsAppRegistry(
                props,
                List.of(new MetaWhatsAppCloudAdapter(props, waBuilder.build())),
                new SandboxWhatsAppAdapter(props)
        );

        NotificationChannelResult res = waRegistry.sendWhatsApp(
                "+237699112233",
                "sodepa_wf_rejet_dossier",
                "fr",
                List.of("Paul", "EXPRESSION_BESOIN", "ETAPE_DAF", "Budget épuisé"),
                "Texte fallback",
                "WORKFLOW_REJET_DOSSIER"
        );

        assertTrue(res.success());
        assertEquals("meta_whatsapp", res.providerName());
        assertEquals("wamid.HBgM237", res.providerMessageId());
        waServer.verify();
    }

    @Test
    @DisplayName("NotificationDispatchService : en cas d'échec WhatsApp hors sandbox, crée une notification enfant SMS traçable et incrémente les métriques")
    void shouldFallbackWhatsAppFailureToLinkedSmsChildNotification() {
        NotificationProperties props = new NotificationProperties();
        props.getSandbox().setEnabled(false);
        props.getWhatsapp().setFallbackToSmsOnFailure(true);
        props.getWhatsapp().setProviderOrder(List.of("meta_whatsapp")); // non configuré -> échec WhatsApp
        props.getSms().setProviderOrder(List.of("nexah"));
        props.getSms().getNexah().setEnabled(true);
        props.getSms().getNexah().setBaseUrl("https://nexah.mock.test/api/v1/sms/send");
        props.getSms().getNexah().setApiKey("nexah-secret");

        RestClient.Builder nexahBuilder = RestClient.builder();
        MockRestServiceServer nexahServer = MockRestServiceServer.bindTo(nexahBuilder).build();
        nexahServer.expect(requestTo("https://nexah.mock.test/api/v1/sms/send"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"responsecode\":1,\"messageId\":\"NX-FB-1\"}", MediaType.APPLICATION_JSON));

        SmsRegistry smsRegistry = new SmsRegistry(
                props,
                List.of(new NexahSmsAdapter(props, nexahBuilder.build())),
                new SandboxSmsAdapter(props)
        );
        WhatsAppRegistry waRegistry = new WhatsAppRegistry(
                props,
                List.of(new MetaWhatsAppCloudAdapter(props)),
                new SandboxWhatsAppAdapter(props)
        );

        NotificationRepository notifRepo = mock(NotificationRepository.class);
        NotificationOutboxService outboxService = mock(NotificationOutboxService.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        NotificationMetrics metrics = new NotificationMetrics(meterRegistry, null);

        UUID parentId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID outboxId = UUID.randomUUID();

        NotificationEntity parentWa = NotificationEntity.builder()
                .id(parentId)
                .groupId(groupId)
                .canal(CanalNotification.WHATSAPP)
                .idempotencyKey("KEY-1:WHATSAPP")
                .destinataireNom("Paul Biya Test")
                .destinataireWhatsapp("+237699887766")
                .destinataireTelephoneSms("+237699887766")
                .sujet("Alerte SLA")
                .messageCorps("Dossier en retard")
                .typeEvenement("WORKFLOW_RELANCE_SLA")
                .statut(StatutNotification.PENDING)
                .build();

        when(notifRepo.findById(parentId)).thenReturn(Optional.of(parentWa));
        when(notifRepo.findByIdempotencyKey("KEY-1:WHATSAPP:fallback-sms")).thenReturn(Optional.empty());
        when(notifRepo.save(any(NotificationEntity.class))).thenAnswer(inv -> {
            NotificationEntity e = inv.getArgument(0);
            if (e.getId() == null) {
                e.setId(UUID.randomUUID());
            }
            return e;
        });

        NotificationDispatchService dispatcher = new NotificationDispatchService(
                notifRepo,
                outboxService,
                new EmailChannelAdapter(null, props),
                smsRegistry,
                waRegistry,
                props,
                metrics,
                new ObjectMapper()
        );

        dispatcher.dispatch(parentId, outboxId);

        verify(notifRepo).save(argThat(e ->
                e.getCanal() == CanalNotification.SMS
                        && parentId.equals(e.getParentNotificationId())
                        && e.isFallbackUsed()
                        && e.getStatut() == StatutNotification.SENT
                        && "NX-FB-1".equals(e.getProviderMessageId())
        ));
        verify(outboxService).markCompleted(outboxId);
        assertEquals(1.0, meterRegistry.counter("sodepa.notifications.fallback.total", "from", "WHATSAPP", "to", "SMS").count());
        assertEquals(1.0, meterRegistry.counter("sodepa.notifications.sent.total", "canal", "SMS", "provider", "nexah").count());
        nexahServer.verify();
    }
}
