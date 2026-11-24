package com.sodepa.erp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.audit.application.inputs.WorkflowEntityStateSnapshot;
import com.sodepa.erp.audit.application.usecase.AuditEventPublisher;
import com.sodepa.erp.audit.infrastructure.repo.ClickHouseManager;
import com.sodepa.erp.audit.infrastructure.repo.DuckDbAuditManager;
import com.sodepa.erp.authorisation.application.IamGouvernanceService;
import com.sodepa.erp.authorisation.infrastructure.entities.AffectationProfilUtilisateurEntity;
import com.sodepa.erp.authorisation.infrastructure.entities.ProfilFonctionnelEntity;
import com.sodepa.erp.authorisation.infrastructure.repo.AffectationProfilUtilisateurRepository;
import com.sodepa.erp.authorisation.infrastructure.repo.ProfilFonctionnelRepository;
import com.sodepa.erp.authorisation.infrastructure.repo.RegleSodRepository;
import com.sodepa.erp.paie.application.PaieCalculateurService;
import com.sodepa.erp.paie.application.PaieService;
import com.sodepa.erp.paie.infrastructure.entities.BulletinPaieEntity;
import com.sodepa.erp.paie.infrastructure.repo.*;
import com.sodepa.erp.rh.infrastructure.entities.EmployeEntity;
import com.sodepa.erp.rh.infrastructure.repo.EmployeRepository;
import com.sodepa.erp.share.CodeSequenceService;
import com.sodepa.erp.configuration.RequestTrackingEvent;
import com.sodepa.erp.configuration.ratelimit.EndpointRateLimitInterceptor;
import com.sodepa.erp.configuration.ratelimit.EndpointThrottleProperties;
import com.sodepa.erp.configuration.ratelimit.EndpointThrottleService;
import com.sodepa.erp.configuration.ratelimit.ThrottleFailurePolicy;
import com.sodepa.erp.configuration.ratelimit.ThrottleKeyType;
import com.sodepa.erp.configuration.ratelimit.ThrottleResponseWriter;
import com.sodepa.erp.notification.application.NotificationService;
import com.sodepa.erp.organigramme.infrastructure.repo.AffectationPosteRepository;
import com.sodepa.erp.organigramme.infrastructure.repo.DelegationPouvoirRepository;
import com.sodepa.erp.organigramme.infrastructure.repo.DerogationPermissionRepository;
import com.sodepa.erp.rh.infrastructure.repo.ContratTravailRepository;
import com.sodepa.erp.share.CurrentUserAuthenticationToken;
import com.sodepa.erp.share.FileStorageService;
import com.sodepa.erp.share.UserData;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.share.erreurs.AccesRefuseException;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import com.sodepa.erp.user.infrastructure.repo.UserRepository;
import com.sodepa.erp.utils.Permissions;
import com.sodepa.erp.utils.UserManagementEngine;
import com.sodepa.erp.workflow.application.WorkflowSnapshotMerger;
import com.sodepa.erp.workflow.application.WorkflowStateService;
import com.sodepa.erp.workflow.infrastructure.entities.WorkflowStateOutboxEntity;
import com.sodepa.erp.workflow.infrastructure.repo.WorkflowStateOutboxRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityResilienceHardeningTest {


    @Mock private StringRedisTemplate redisTemplate;
    @Mock private EndpointThrottleService mockThrottleService;

    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private DuckDbAuditManager duckDbAuditManager;

    @Mock private ProfilFonctionnelRepository profilRepository;
    @Mock private AffectationProfilUtilisateurRepository affectationProfilRepository;
    @Mock private RegleSodRepository regleSodRepository;
    @Mock private UserRepository userRepository;
    @Mock private ContratTravailRepository contratTravailRepository;
    @Mock private NotificationService notificationService;
    @Mock private FileStorageService fileStorageService;
    @Mock private AffectationPosteRepository affectationPosteRepository;
    @Mock private DelegationPouvoirRepository delegationPouvoirRepository;
    @Mock private DerogationPermissionRepository derogationPermissionRepository;

    @Mock private ClickHouseManager clickHouseManager;
    @Mock private WorkflowStateOutboxRepository outboxRepository;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("P0 Sécurité : EndpointRateLimitInterceptor distingue les utilisateurs via CurrentUserAuthenticationToken")
    void shouldExtractUserKeyFromCurrentUserAuthenticationTokenAndIgnoreUserAgentOnAuth() throws Exception {
        EndpointThrottleProperties props = new EndpointThrottleProperties();
        props.setEnabled(true);

        EndpointThrottleProperties.Rule apiRule = new EndpointThrottleProperties.Rule();
        apiRule.setName("api-rule");
        apiRule.setPathPatterns(List.of("/api/budget/**"));
        apiRule.setKeyType(ThrottleKeyType.AUTHENTICATED_ENDPOINT);
        apiRule.setIncludePathInKey(true);
        apiRule.setLimit(30);
        apiRule.setDurationSeconds(60);

        EndpointThrottleProperties.Rule loginRule = new EndpointThrottleProperties.Rule();
        loginRule.setName("auth-login");
        loginRule.setPathPatterns(List.of("/api/auth/login"));
        loginRule.setKeyType(ThrottleKeyType.PUBLIC_ENDPOINT);
        loginRule.setIncludePathInKey(true);
        loginRule.setLimit(10);
        loginRule.setDurationSeconds(60);

        props.setRules(List.of(loginRule, apiRule));

        EndpointRateLimitInterceptor interceptor = new EndpointRateLimitInterceptor(
                props, mockThrottleService, new ThrottleResponseWriter(new ObjectMapper()));
        when(mockThrottleService.isAllowed(any(), anyString())).thenReturn(true);

        // 1. Deux utilisateurs sur même IP reçoivent des clés distinctes (pas de collision bucket IP)
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(new CurrentUserAuthenticationToken(
                "alice", "jwt", List.of(),
                UserData.builder().userId(user1.toString()).username("alice").permissions(Set.of()).build()));
        MockHttpServletRequest req1 = new MockHttpServletRequest("GET", "/api/budget/plans");
        req1.setRemoteAddr("10.0.0.5");
        interceptor.preHandle(req1, new MockHttpServletResponse(), new Object());

        SecurityContextHolder.getContext().setAuthentication(new CurrentUserAuthenticationToken(
                "bob", "jwt", List.of(),
                UserData.builder().userId(user2.toString()).username("bob").permissions(Set.of()).build()));
        MockHttpServletRequest req2 = new MockHttpServletRequest("GET", "/api/budget/plans");
        req2.setRemoteAddr("10.0.0.5");
        interceptor.preHandle(req2, new MockHttpServletResponse(), new Object());

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockThrottleService, times(2)).isAllowed(eq(apiRule), keyCaptor.capture());
        List<String> keys = keyCaptor.getAllValues();
        assertNotEquals(keys.get(0), keys.get(1));
        assertTrue(keys.get(0).contains(user1.toString()));
        assertTrue(keys.get(1).contains(user2.toString()));

        // 2. Rotation User-Agent sur /api/auth/login ne change PAS la clé d'IP
        SecurityContextHolder.clearContext();
        MockHttpServletRequest loginA = new MockHttpServletRequest("POST", "/api/auth/login");
        loginA.setRemoteAddr("192.168.1.99");
        loginA.addHeader("User-Agent", "BotAgent/1.0");
        MockHttpServletRequest loginB = new MockHttpServletRequest("POST", "/api/auth/login");
        loginB.setRemoteAddr("192.168.1.99");
        loginB.addHeader("User-Agent", "AnotherBrowser/99.0");
        interceptor.preHandle(loginA, new MockHttpServletResponse(), new Object());
        interceptor.preHandle(loginB, new MockHttpServletResponse(), new Object());

        ArgumentCaptor<String> loginKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockThrottleService, times(2)).isAllowed(eq(loginRule), loginKeyCaptor.capture());
        assertEquals(loginKeyCaptor.getAllValues().get(0), loginKeyCaptor.getAllValues().get(1));
    }

    @Test
    @DisplayName("P0 Résilience : EndpointThrottleService script Lua atomique + repli mémoire borné si Redis KO")
    @SuppressWarnings("unchecked")
    void shouldUseAtomicLuaAndBoundedMemoryFallbackWhenRedisDown() {
        EndpointThrottleService service = new EndpointThrottleService(redisTemplate);
        EndpointThrottleProperties.Rule rule = new EndpointThrottleProperties.Rule();
        rule.setName("test-rule");
        rule.setLimit(2);
        rule.setDurationSeconds(60);
        rule.setFailurePolicy(ThrottleFailurePolicy.FAIL_CLOSED);

        // Mode nominal Redis Lua atomique
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenReturn(1L, 2L, 3L);
        assertTrue(service.isAllowed(rule, "user-lua"));
        assertTrue(service.isAllowed(rule, "user-lua"));
        assertFalse(service.isAllowed(rule, "user-lua"));

        // Mode dégradé Redis KO -> repli mémoire local applique la limite au lieu de bloquer ou ouvrir aveuglément
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenThrow(new RuntimeException("Connection refused to Redis"));
        assertTrue(service.isAllowed(rule, "user-fallback"));
        assertTrue(service.isAllowed(rule, "user-fallback"));
        assertFalse(service.isAllowed(rule, "user-fallback"));
    }

    @Test
    @DisplayName("P0 Sécurité IAM : résolution dynamique des profils sans copie permanente + blocage auto-attribution")
    void shouldResolveDynamicProfilesWithExpirationAndBlockSelfAssignment() {
        UUID actorId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        UtilsService utilsService = new UtilsService();
        SecurityContextHolder.getContext().setAuthentication(new CurrentUserAuthenticationToken(
                "drh.manager", "jwt", List.of(),
                UserData.builder().userId(actorId.toString()).username("drh.manager")
                        .permissions(Set.of(Permissions.INIT_UPDATE_USER)).build()));

        IamGouvernanceService iamService = new IamGouvernanceService(
                profilRepository, affectationProfilRepository, regleSodRepository,
                userRepository, contratTravailRepository, notificationService);
        ReflectionTestUtils.setField(iamService, "utilsService", utilsService);

        // 1. Auto-attribution interdite (4 yeux)
        assertThrows(AccesRefuseException.class, () ->
                iamService.affecterProfilAUtilisateur(actorId, "PROFIL_DAF", null, null, null, null));

        // 2. Attribution légitime par un tiers -> ne modifie PAS user.getPermissions()
        UtilisateurEntity targetUser = UtilisateurEntity.builder()
                .id(targetUserId).username("agent.cible").actif(true).permissions(new HashSet<>()).build();
        ProfilFonctionnelEntity profilComptable = ProfilFonctionnelEntity.builder()
                .id(UUID.randomUUID()).code("PROFIL_COMPTA").libelle("Comptable")
                .permissions(Set.of(Permissions.INIT_CREATE_ECRITURE, Permissions.VIEW_ECRITURE))
                .mfaRequis(true).actif(true).build();

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(profilRepository.findByCodeIgnoreCase("PROFIL_COMPTA")).thenReturn(Optional.of(profilComptable));
        when(affectationProfilRepository.findByUtilisateurIdAndActifTrue(targetUserId)).thenReturn(List.of());
        when(regleSodRepository.findByActifTrueOrderByCodeRegleAsc()).thenReturn(List.of());
        when(affectationProfilRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AffectationProfilUtilisateurEntity savedAff = iamService.affecterProfilAUtilisateur(
                targetUserId, "PROFIL_COMPTA", null, null, 2026, LocalDate.now().plusDays(30));

        assertTrue(targetUser.getPermissions().isEmpty(), "Les permissions du profil ne doivent PAS être copiées dans user.permissions");
        assertTrue(targetUser.isMfaActive());

        // 3. UserManagementEngine résout dynamiquement si actif+valide, et retire à l'expiration/révocation
        UserManagementEngine engine = new UserManagementEngine(
                userRepository, fileStorageService, affectationPosteRepository,
                delegationPouvoirRepository, derogationPermissionRepository);
        ReflectionTestUtils.setField(engine, "affectationProfilRepository", affectationProfilRepository);

        when(affectationPosteRepository.findByUtilisateurId(targetUserId)).thenReturn(List.of());
        when(delegationPouvoirRepository.findByDelegataireIdAndStatut(eq(targetUserId), any())).thenReturn(List.of());
        when(derogationPermissionRepository.findByUtilisateurIdAndStatut(eq(targetUserId), any())).thenReturn(List.of());
        when(affectationProfilRepository.findByUtilisateurIdAndActifTrue(targetUserId)).thenReturn(List.of(savedAff));

        Set<Permissions> activePerms = engine.resolveEffectivePermissions(targetUserId);
        assertTrue(activePerms.contains(Permissions.INIT_CREATE_ECRITURE));

        // Expiration de l'affectation -> permissions dynamiquement retirées sans modification de utilisateur.permissions
        savedAff.setDateFin(LocalDate.now().minusDays(1));
        Set<Permissions> expiredPerms = engine.resolveEffectivePermissions(targetUserId);
        assertFalse(expiredPerms.contains(Permissions.INIT_CREATE_ECRITURE));
    }

    @Test
    @DisplayName("P0 Résilience : AuditEventPublisher bascule sur DuckDB sans exception si RabbitMQ est coupé")
    void shouldFallbackToDuckDbWhenRabbitMqIsDown() {
        AuditEventPublisher publisher = new AuditEventPublisher(rabbitTemplate);
        ReflectionTestUtils.setField(publisher, "duckDbAuditManager", duckDbAuditManager);

        doThrow(new AmqpConnectException(new RuntimeException("Broker down")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        RequestTrackingEvent event = new RequestTrackingEvent(
                "cid-1", "sid-1", Instant.now(), "POST", "/api/budget/plans",
                null, "127.0.0.1", "Alice", "u-1", "", null, 201, "", null, 12L);

        assertDoesNotThrow(() -> publisher.publishActivity(event));
        verify(duckDbAuditManager).saveUserActivity(
                eq("u-1"), eq("Alice"), eq("sid-1"), eq("HTTP_201"),
                eq("POST"), eq("/api/budget/plans"), eq("cid-1"), eq("127.0.0.1"));
    }

    @Test
    @DisplayName("P1 Résilience : WorkflowStateService isole les messages poison-pill après MAX_OUTBOX_RETRIES")
    void shouldSkipPoisonPillOutboxRowAndReplayHealthyRows() throws Exception {
        WorkflowStateService stateService = new WorkflowStateService(
                clickHouseManager, outboxRepository, new WorkflowSnapshotMerger(new ObjectMapper()));

        WorkflowStateOutboxEntity poison = WorkflowStateOutboxEntity.builder()
                .id(UUID.randomUUID()).instanceId(UUID.randomUUID()).version(1)
                .eventType("INIT").entityName("EB").operationType("CREATE")
                .fullSnapshotJson("{}").retryCount(10).synced(false).createdAt(Instant.now()).build();
        WorkflowStateOutboxEntity healthy = WorkflowStateOutboxEntity.builder()
                .id(UUID.randomUUID()).instanceId(UUID.randomUUID()).version(1)
                .eventType("INIT").entityName("EB").operationType("CREATE")
                .fullSnapshotJson("{}").retryCount(0).synced(false).createdAt(Instant.now()).build();

        when(outboxRepository.findBySyncedFalseOrderByCreatedAtAscVersionAsc()).thenReturn(List.of(poison, healthy));
        when(clickHouseManager.isAvailable()).thenReturn(true);
        when(clickHouseManager.existsWorkflowEntityState(healthy.getId())).thenReturn(false);

        stateService.replayPendingOutbox();

        verify(clickHouseManager, never()).existsWorkflowEntityState(poison.getId());
        verify(clickHouseManager, times(1)).saveWorkflowEntityState(any(WorkflowEntityStateSnapshot.class));
        assertTrue(healthy.isSynced());
        assertFalse(poison.isSynced());
    }

    @Test
    @DisplayName("P0 Sécurité : X-Forwarded-For forgé ne contourne pas le rate-limiting sur /api/auth/login")
    void shouldIgnoreSpoofedXForwardedForHeaderOnLoginRateLimit() throws Exception {
        EndpointThrottleProperties props = new EndpointThrottleProperties();
        props.setEnabled(true);
        EndpointThrottleProperties.Rule loginRule = new EndpointThrottleProperties.Rule();
        loginRule.setName("auth-login");
        loginRule.setPathPatterns(List.of("/api/auth/login"));
        loginRule.setKeyType(ThrottleKeyType.PUBLIC_ENDPOINT);
        loginRule.setIncludePathInKey(true);
        loginRule.setLimit(10);
        loginRule.setDurationSeconds(60);
        props.setRules(List.of(loginRule));

        EndpointRateLimitInterceptor interceptor = new EndpointRateLimitInterceptor(
                props, mockThrottleService, new ThrottleResponseWriter(new ObjectMapper()));
        when(mockThrottleService.isAllowed(any(), anyString())).thenReturn(true);

        MockHttpServletRequest r1 = new MockHttpServletRequest("POST", "/api/auth/login");
        r1.setRemoteAddr("203.0.113.10");
        r1.addHeader("X-Forwarded-For", "10.1.1.1");

        MockHttpServletRequest r2 = new MockHttpServletRequest("POST", "/api/auth/login");
        r2.setRemoteAddr("203.0.113.10");
        r2.addHeader("X-Forwarded-For", "198.51.100.99");

        interceptor.preHandle(r1, new MockHttpServletResponse(), new Object());
        interceptor.preHandle(r2, new MockHttpServletResponse(), new Object());

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockThrottleService, times(2)).isAllowed(eq(loginRule), captor.capture());
        assertEquals(captor.getAllValues().get(0), captor.getAllValues().get(1));
        assertFalse(captor.getAllValues().get(0).contains("10.1.1.1"));
        assertFalse(captor.getAllValues().get(0).contains("198.51.100.99"));
    }

    @Test
    @DisplayName("P0 Sécurité IDOR Paie : PaieService bloque la lecture des bulletins d'un collègue et filtre sur soi-même")
    void shouldEnforcePayrollConfidentialityAndBlockIdor() {
        PeriodePaieRepository periodeRepo = mock(PeriodePaieRepository.class);
        PrimeConfigRepository primeRepo = mock(PrimeConfigRepository.class);
        VariablePaieRepository varRepo = mock(VariablePaieRepository.class);
        BulletinPaieRepository bulletinRepo = mock(BulletinPaieRepository.class);
        BulletinLigneRepository ligneRepo = mock(BulletinLigneRepository.class);
        EmployeRepository employeRepo = mock(EmployeRepository.class);

        UtilsService utilsService = new UtilsService();
        PaieService paieService = new PaieService(
                periodeRepo, primeRepo, varRepo, bulletinRepo, ligneRepo,
                employeRepo, mock(PaieCalculateurService.class), null, utilsService, new ObjectMapper(), clickHouseManager);

        UUID aliceUserId = UUID.randomUUID();
        UUID bobUserId = UUID.randomUUID();
        UUID aliceEmpId = UUID.randomUUID();
        UUID bulletinBobId = UUID.randomUUID();

        UtilisateurEntity bobUser = UtilisateurEntity.builder().id(bobUserId).username("bob").build();
        EmployeEntity bobEmp = EmployeEntity.builder().id(UUID.randomUUID()).utilisateur(bobUser).build();
        BulletinPaieEntity bulletinBob = BulletinPaieEntity.builder().id(bulletinBobId).employe(bobEmp).build();
        when(bulletinRepo.findById(bulletinBobId)).thenReturn(Optional.of(bulletinBob));

        SecurityContextHolder.getContext().setAuthentication(new CurrentUserAuthenticationToken(
                "alice", "jwt", List.of(),
                UserData.builder().userId(aliceUserId.toString()).username("alice").permissions(Set.of()).build()));

        // Tentative d'accès au bulletin d'un collègue -> 403 AccesRefuseException
        assertThrows(AccesRefuseException.class, () -> paieService.getBulletinById(bulletinBobId));

        // Listing des bulletins sans privilège Paie -> limité strictement à son propre employeId
        UUID periodeId = UUID.randomUUID();
        EmployeEntity aliceEmp = EmployeEntity.builder().id(aliceEmpId).build();
        when(employeRepo.findByUtilisateurId(aliceUserId)).thenReturn(Optional.of(aliceEmp));
        when(bulletinRepo.findByPeriodeIdAndEmployeId(eq(periodeId), eq(aliceEmpId), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        paieService.getBulletins(periodeId, org.springframework.data.domain.PageRequest.of(0, 20));
        verify(bulletinRepo).findByPeriodeIdAndEmployeId(eq(periodeId), eq(aliceEmpId), any());
        verify(bulletinRepo, never()).findByPeriodeId(eq(periodeId), any());
    }

    @Test
    @DisplayName("P1 Concurrence : CodeSequenceService sans collision")
    void shouldAllocateUniqueCodesWithoutCollision() throws Exception {
        // Même séquence partagée sur 16 threads concurrents : aucune collision
        Set<Long> sharedAllocated = Collections.synchronizedSet(new HashSet<>());
        List<Thread> racing = new ArrayList<>();
        String seqName = "seq_code_concurrent_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        for (int i = 0; i < 16; i++) {
            racing.add(new Thread(() -> sharedAllocated.add(
                    CodeSequenceService.nextOrCount(null, seqName, () -> 50L))));
        }
        for (Thread t : racing) t.start();
        for (Thread t : racing) t.join();
        assertEquals(16, sharedAllocated.size());
        assertTrue(sharedAllocated.stream().allMatch(v -> v >= 51L));
    }

    @Test
    @DisplayName("Lot R1 Sécurité : RH PII anti-IDOR, NotificationService sans findAll(null) & Workflow Need-to-Know par entité")
    void shouldEnforceRhPiiConfidentialityAndNotificationGuardAndWorkflowNeedToKnow() {
        com.sodepa.erp.rh.infrastructure.repo.EmployeRepository empRepo = mock(com.sodepa.erp.rh.infrastructure.repo.EmployeRepository.class);
        com.sodepa.erp.rh.infrastructure.repo.GrilleSalarialeRepository grilleRepo = mock(com.sodepa.erp.rh.infrastructure.repo.GrilleSalarialeRepository.class);
        com.sodepa.erp.organigramme.infrastructure.repo.UniteOrganisationnelleRepository uniteRepo = mock(com.sodepa.erp.organigramme.infrastructure.repo.UniteOrganisationnelleRepository.class);
        com.sodepa.erp.organigramme.infrastructure.repo.PosteRepository posteRepo = mock(com.sodepa.erp.organigramme.infrastructure.repo.PosteRepository.class);
        com.sodepa.erp.workflow.application.WorkflowService wfService = mock(com.sodepa.erp.workflow.application.WorkflowService.class);
        WorkflowStateService wfStateService = mock(WorkflowStateService.class);

        UtilsService utilsService = new UtilsService();
        com.sodepa.erp.rh.application.RhService rhService = new com.sodepa.erp.rh.application.RhService(
                empRepo, grilleRepo, uniteRepo, posteRepo, userRepository, derogationPermissionRepository,
                clickHouseManager, utilsService, new ObjectMapper(), contratTravailRepository, wfService, wfStateService);

        UUID aliceUserId = UUID.randomUUID();
        UUID bobEmpId = UUID.randomUUID();
        UUID bobUserId = UUID.randomUUID();
        UtilisateurEntity bobUser = UtilisateurEntity.builder().id(bobUserId).username("bob").build();
        EmployeEntity bobEmp = EmployeEntity.builder().id(bobEmpId).matricule("EMP-BOB").nom("Bob").prenom("Test")
                .compteBancaireRib("CM21-SECRET-RIB").numeroCnps("CNPS-SECRET").utilisateur(bobUser).build();

        SecurityContextHolder.getContext().setAuthentication(new CurrentUserAuthenticationToken(
                "alice", "jwt", List.of(),
                UserData.builder().userId(aliceUserId.toString()).username("alice").permissions(Set.of()).build()));

        when(empRepo.findById(bobEmpId)).thenReturn(Optional.of(bobEmp));
        when(wfStateService.findActiveInstanceForEntity(anyList(), eq(bobEmpId))).thenReturn(Optional.empty());
        assertThrows(AccesRefuseException.class, () -> rhService.getEmployeById(bobEmpId));

        when(empRepo.findByUtilisateurId(aliceUserId)).thenReturn(Optional.empty());
        when(wfStateService.findActiveInstancesByEntityUuid(anyList())).thenReturn(Collections.emptyMap());
        assertTrue(rhService.listerEmployes(null, null, null).isEmpty());
        verify(empRepo, never()).findAll();

        // NotificationService ne déclenche plus findAll() si utilisateurId == null
        com.sodepa.erp.notification.infrastructure.repo.NotificationRepository notifRepo =
                mock(com.sodepa.erp.notification.infrastructure.repo.NotificationRepository.class);
        NotificationService realNotif = new NotificationService(notifRepo);
        assertTrue(realNotif.listerNotificationsUtilisateur(null).isEmpty());
        verify(notifRepo, never()).findAll();
    }

    @Test
    @DisplayName("Lots R1-R3 Résilience & Dispo : GED fail-closed prod secret + Cheptel SQL aggregates")
    void shouldEnforceGedProdSecretRequirementAndCheptelSqlAggregates() {
        com.sodepa.erp.ged.infrastructure.repo.DocumentGedRepository gedRepo =
                mock(com.sodepa.erp.ged.infrastructure.repo.DocumentGedRepository.class);
        com.sodepa.erp.share.MinioService minioService = mock(com.sodepa.erp.share.MinioService.class);
        com.sodepa.erp.ged.application.GedService gedService = new com.sodepa.erp.ged.application.GedService(gedRepo, minioService);

        org.springframework.mock.env.MockEnvironment prodEnv = new org.springframework.mock.env.MockEnvironment();
        prodEnv.setActiveProfiles("prod");
        ReflectionTestUtils.setField(gedService, "environment", prodEnv);
        ReflectionTestUtils.setField(gedService, "hmacSecret", "");

        UUID docId = UUID.randomUUID();
        when(gedRepo.findById(docId)).thenReturn(Optional.of(
                com.sodepa.erp.ged.infrastructure.entities.DocumentGedEntity.builder()
                        .id(docId).hashSha256("abc").nomFichier("test.pdf").build()));
        assertThrows(IllegalStateException.class, () -> gedService.signerDocumentElectroniquement(docId, "tester"));

        // CheptelService.obtenirTableauBord sans bovinRepository.findAll()
        com.sodepa.erp.pastoral.infrastructure.repo.PastoralRanchRepository ranchRepo =
                mock(com.sodepa.erp.pastoral.infrastructure.repo.PastoralRanchRepository.class);
        com.sodepa.erp.pastoral.infrastructure.repo.PastoralCategorieCheptelRepository catRepo =
                mock(com.sodepa.erp.pastoral.infrastructure.repo.PastoralCategorieCheptelRepository.class);
        com.sodepa.erp.pastoral.infrastructure.repo.PastoralBovinRepository bovinRepo =
                mock(com.sodepa.erp.pastoral.infrastructure.repo.PastoralBovinRepository.class);
        com.sodepa.erp.pastoral.infrastructure.repo.PastoralMouvementCheptelRepository mvtRepo =
                mock(com.sodepa.erp.pastoral.infrastructure.repo.PastoralMouvementCheptelRepository.class);
        com.sodepa.erp.pastoral.infrastructure.repo.PastoralHerdCompositionRepository herdRepo =
                mock(com.sodepa.erp.pastoral.infrastructure.repo.PastoralHerdCompositionRepository.class);
        com.sodepa.erp.configuration.parametres.application.ParametreConfigurationService cfgSvc =
                mock(com.sodepa.erp.configuration.parametres.application.ParametreConfigurationService.class);
        com.sodepa.erp.workflow.application.WorkflowService wfSvc =
                mock(com.sodepa.erp.workflow.application.WorkflowService.class);

        com.sodepa.erp.pastoral.application.CheptelService cheptel = new com.sodepa.erp.pastoral.application.CheptelService(
                ranchRepo, catRepo, bovinRepo, mvtRepo, herdRepo, cfgSvc, wfSvc, new UtilsService(), new ObjectMapper(), clickHouseManager);

        when(bovinRepo.countByStatut(com.sodepa.erp.pastoral.infrastructure.entities.StatutBovin.VIVANT_AU_RANCH)).thenReturn(25L);
        when(bovinRepo.sumValeurComptableByStatut(com.sodepa.erp.pastoral.infrastructure.entities.StatutBovin.VIVANT_AU_RANCH))
                .thenReturn(new BigDecimal("12500000"));
        when(bovinRepo.countGroupedByCategorieAndStatut(com.sodepa.erp.pastoral.infrastructure.entities.StatutBovin.VIVANT_AU_RANCH))
                .thenReturn(List.<Object[]>of(new Object[]{"VEAUX", 10L}, new Object[]{"REPRODUCTEURS", 15L}));
        when(bovinRepo.countGroupedByRanchAndStatut(any())).thenReturn(Collections.emptyList());
        when(ranchRepo.findAll()).thenReturn(Collections.emptyList());

        var tb = cheptel.obtenirTableauBord();
        assertEquals(25L, tb.getTotalTetesVivantes());
        assertEquals(0, new BigDecimal("12500000").compareTo(tb.getValeurEstimeeTotaleCheptel()));
        assertEquals(10L, tb.getRepartitionParCategorie().get("VEAUX"));
        verify(bovinRepo, never()).findAll();
    }

    @Test
    @DisplayName("Lot R2-Diag : @Scheduled + @SchedulerLock répète fixedDelay avec SchedulingConfig")
    void shouldRepeatScheduledTaskWithShedLockAdvisor() throws InterruptedException {
        java.util.concurrent.atomic.AtomicInteger lockCalls = new java.util.concurrent.atomic.AtomicInteger(0);
        net.javacrumbs.shedlock.core.LockProvider mockLockProvider = lockCfg -> {
            lockCalls.incrementAndGet();
            return Optional.of(() -> {
            });
        };
        org.springframework.context.annotation.AnnotationConfigApplicationContext ctx =
                new org.springframework.context.annotation.AnnotationConfigApplicationContext();
        ctx.getBeanFactory().registerSingleton("lockProvider", mockLockProvider);
        ctx.register(TestShedLockSchedulingConfig.class, DummyScheduledJob.class);
        ctx.refresh();
        try {
            DummyScheduledJob job = ctx.getBean(DummyScheduledJob.class);
            long deadline = System.currentTimeMillis() + 3000L;
            while (job.getCount() < 3 && System.currentTimeMillis() < deadline) {
                Thread.sleep(50L);
            }
            assertTrue(job.getCount() >= 3, "Attendu >= 3 exécutions @Scheduled + @SchedulerLock, obtenu=" + job.getCount());
            assertTrue(lockCalls.get() >= 3, "Attendu >= 3 acquisitions LockProvider, obtenu=" + lockCalls.get());
        } finally {
            ctx.close();
        }
    }

    @org.springframework.context.annotation.Configuration
    @org.springframework.scheduling.annotation.EnableScheduling
    @net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock(defaultLockAtMostFor = "PT1M")
    static class TestShedLockSchedulingConfig {
        @org.springframework.context.annotation.Bean
        public org.springframework.scheduling.TaskScheduler taskScheduler() {
            org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler scheduler =
                    new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();
            scheduler.setPoolSize(2);
            scheduler.setThreadNamePrefix("test-shedlock-");
            return scheduler;
        }
    }

    static class DummyScheduledJob {
        private final java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(0);

        @org.springframework.scheduling.annotation.Scheduled(fixedDelayString = "${test.delay:60}")
        @net.javacrumbs.shedlock.spring.annotation.SchedulerLock(name = "dummyJob", lockAtMostFor = "PT30S", lockAtLeastFor = "PT0.01S")
        public void tick() {
            counter.incrementAndGet();
        }

        public int getCount() {
            return counter.get();
        }
    }

    @Test
    @DisplayName("Lot R2-Diag : ClickHouseManager n'étend pas indéfiniment la fenêtre du circuit breaker")
    void shouldNotExtendClickHouseCircuitCooldownWhenAlreadyOpen() {
        ClickHouseManager manager = new ClickHouseManager();
        ReflectionTestUtils.invokeMethod(manager, "openCircuitBreaker", "err-1", new java.sql.SQLException("down-1"));
        assertTrue(manager.isCircuitOpen());
        long firstDeadline = (Long) ReflectionTestUtils.getField(manager, "circuitOpenUntilMillis");

        // Un second appel concurrent ne doit pas repousser circuitOpenUntilMillis
        ReflectionTestUtils.setField(manager, "circuitOpenUntilMillis", firstDeadline - 5_000L);
        long adjustedDeadline = (Long) ReflectionTestUtils.getField(manager, "circuitOpenUntilMillis");
        ReflectionTestUtils.invokeMethod(manager, "openCircuitBreaker", "err-2", new java.sql.SQLException("down-2"));
        long afterSecondError = (Long) ReflectionTestUtils.getField(manager, "circuitOpenUntilMillis");

        assertEquals(adjustedDeadline, afterSecondError,
                "Le circuit breaker déjà ouvert ne doit pas prolonger sa fenêtre de cooldown.");
        assertFalse(manager.probeAvailability(), "probeAvailability() sans URL doit retourner false sans lever d'exception.");
        assertEquals(adjustedDeadline, ReflectionTestUtils.getField(manager, "circuitOpenUntilMillis"),
                "probeAvailability() ne doit pas prolonger le cooldown existant.");
    }

    @Test
    @DisplayName("Lot R2 : JwtAuthenticationFilter authentifie /actuator/prometheus sans X-Correlation-Id tout en l'exigeant sur /api/**")
    void shouldAuthenticatePrometheusActuatorWithoutCorrelationIdHeader() throws Exception {
        com.sodepa.erp.configuration.FilterBaseMethod filterBaseMethod = mock(com.sodepa.erp.configuration.FilterBaseMethod.class);
        com.sodepa.erp.configuration.JwtAuthenticationFilter filter =
                new com.sodepa.erp.configuration.JwtAuthenticationFilter(filterBaseMethod);

        // 1. /actuator/health reste public sans JWT
        var reqHealth = new org.springframework.mock.web.MockHttpServletRequest("GET", "/actuator/health");
        var respHealth = new org.springframework.mock.web.MockHttpServletResponse();
        var chainHealth = new org.springframework.mock.web.MockFilterChain();
        filter.doFilter(reqHealth, respHealth, chainHealth);
        assertEquals(200, respHealth.getStatus());
        verify(filterBaseMethod, never()).authenticateWithJwt(anyString());

        // 2. /actuator/prometheus avec Bearer JWT authentifie sans exiger X-Correlation-Id
        var reqProm = new org.springframework.mock.web.MockHttpServletRequest("GET", "/actuator/prometheus");
        reqProm.addHeader("Authorization", "Bearer prom-jwt-token");
        var respProm = new org.springframework.mock.web.MockHttpServletResponse();
        var chainProm = new org.springframework.mock.web.MockFilterChain();
        filter.doFilter(reqProm, respProm, chainProm);
        assertEquals(200, respProm.getStatus());
        verify(filterBaseMethod).authenticateWithJwt("prom-jwt-token");

        // 3. Route API métier sans X-Correlation-Id reste refusée en 400
        var reqApi = new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/v1/rh/employes");
        reqApi.addHeader("Authorization", "Bearer api-jwt-token");
        var respApi = new org.springframework.mock.web.MockHttpServletResponse();
        filter.doFilter(reqApi, respApi, new org.springframework.mock.web.MockFilterChain());
        assertEquals(400, respApi.getStatus());
    }
}
