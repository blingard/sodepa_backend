package com.sodepa.erp.workflow.application;

import com.sodepa.erp.audit.application.inputs.MakerCheckerMessageInput;
import com.sodepa.erp.audit.application.usecase.AuditEventPublisher;
import com.sodepa.erp.share.MakerCheckerMapper;
import com.sodepa.erp.share.MakerCheckerOutput;
import com.sodepa.erp.share.MakerCheckerRequestEntity;
import com.sodepa.erp.share.MakerCheckerRequestJpaRepo;
import com.sodepa.erp.share.MakerCheckerSmartOutput;
import com.sodepa.erp.user.application.outputs.UserRecordSmartOutput;
import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.MakerCheckerOperationType;
import com.sodepa.erp.utils.MakerCheckerStatus;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.utils.PageableRecord;
import org.springframework.beans.factory.annotation.Value;

import com.sodepa.erp.notification.infrastructure.entities.CanalNotification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.notification.application.NotificationService;
import com.sodepa.erp.share.erreurs.AccesRefuseException;
import com.sodepa.erp.share.erreurs.ConflitMetierException;
import com.sodepa.erp.share.erreurs.RequeteInvalideException;
import com.sodepa.erp.share.erreurs.RessourceIntrouvableException;
import com.sodepa.erp.workflow.application.handler.WorkflowEntityHandler;
import com.sodepa.erp.workflow.application.handler.WorkflowEntityHandlerRegistry;
import com.sodepa.erp.share.MinioService;
import com.sodepa.erp.organigramme.infrastructure.repo.DelegationPouvoirRepository;
import com.sodepa.erp.organigramme.infrastructure.entities.StatutDelegation;
import com.sodepa.erp.organigramme.infrastructure.entities.DelegationPouvoirEntity;
import com.sodepa.erp.notification.application.SendNotificationInput;
import com.sodepa.erp.organigramme.infrastructure.entities.PosteEntity;
import com.sodepa.erp.organigramme.infrastructure.entities.UniteOrganisationnelleEntity;
import com.sodepa.erp.organigramme.infrastructure.repo.PosteRepository;
import com.sodepa.erp.rh.infrastructure.entities.EmployeEntity;
import com.sodepa.erp.rh.infrastructure.repo.EmployeRepository;
import com.sodepa.erp.share.CodeSequenceService;
import com.sodepa.erp.share.UserData;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import com.sodepa.erp.user.infrastructure.repo.UserRepository;
import com.sodepa.erp.utils.Permissions;
import com.sodepa.erp.utils.CodeGenerator;
import com.sodepa.erp.workflow.infrastructure.entities.*;
import com.sodepa.erp.workflow.infrastructure.repo.*;
import com.sodepa.erp.audit.application.inputs.WorkflowEventMessageInput;
import com.sodepa.erp.audit.infrastructure.repo.ClickHouseManager;
import com.sodepa.erp.organigramme.infrastructure.repo.UniteOrganisationnelleRepository;
import com.sodepa.erp.workflow.presentation.requests.*;
import com.sodepa.erp.workflow.presentation.responses.WorkflowDefinitionResponse;
import com.sodepa.erp.workflow.presentation.responses.WorkflowInstanceResponse;
import com.sodepa.erp.workflow.presentation.responses.WorkflowMetierResponse;
import com.sodepa.erp.workflow.presentation.responses.WorkflowTacheResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service applicatif principal du Moteur de Workflow Universel SODEPA.
 * <p>
 * Ce service orchestre l'ensemble du cycle de vie des circuits d'approbation transverses :
 * <ul>
 *   <li>Référentiel dynamique des opérations métiers ({@link WorkflowMetierEntity}) évitant tout code en dur</li>
 *   <li>Centrage sur l'employé ({@link EmployeEntity}) et son unité organisationnelle</li>
 *   <li>Obligation stricte de paramétrage de workflow par unité opérationnelle</li>
 *   <li>Bypass de sécurité pour la gouvernance et les superviseurs (DG, DAF, PCA, Administrateurs)</li>
 *   <li>Contrôle d'éligibilité du Maker en amont (rôles et postes autorisés)</li>
 *   <li>Génération en parallèle des tâches pour les acteurs multiples d'une étape</li>
 *   <li>Évaluation dynamique de complétion d'étape (validateurs obligatoires 'REQUIRED' vs optionnels)</li>
 *   <li>Protection étanche contre la modification croisée et respect du principe 'Need-to-Know'</li>
 *   <li>Enregistrement systématique dans la piste d'audit infalsifiable</li>
 *   <li>Publication d'événements Spring découplés lors de la clôture complète d'un circuit</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    /** Référentiel des requêtes Maker-Checker de transition / portage direct. */
    private final MakerCheckerRequestJpaRepo makerCheckerRequestRepo;

    /** Mapper Maker-Checker pour les réponses DTO. */
    private final MakerCheckerMapper makerCheckerMapper;

    /** Service de publication des événements d'audit Maker-Checker vers RabbitMQ. */
    private final AuditEventPublisher auditEventPublisher;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private com.sodepa.erp.comptabilite.generale.infrastructure.adapter.user.UserAdapterInterface userAdapterInterface;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CodeSequenceService codeSequenceService;

    @Value("${app.duration:30}")
    private Long makerCheckerDurationDays;

    /** Référentiel des opérations / métiers du workflow. */
    private final WorkflowMetierRepository workflowMetierRepository;

    /** Référentiel des définitions et modèles de circuits de validation. */
    private final WorkflowDefinitionRepository workflowDefinitionRepository;

    /** Référentiel des étapes configurées pour chaque circuit. */
    private final WorkflowEtapeDefinitionRepository workflowEtapeRepository;

    /** Référentiel des acteurs configurés sur les étapes. */
    private final WorkflowEtapeActeurDefinitionRepository workflowEtapeActeurRepository;

    /** Référentiel des instances concrètes de circuits en cours ou terminés. */
    private final WorkflowInstanceRepository workflowInstanceRepository;

    /** Référentiel des tâches individuelles attribuées aux valideurs. */
    private final WorkflowTacheRepository workflowTacheRepository;

    /** Référentiel de la piste d'audit et d'immuabilité des actions. */
    private final WorkflowHistoriqueAuditRepository workflowHistoriqueAuditRepository;

    /** Référentiel des utilisateurs et comptes techniques du système. */
    private final UserRepository userRepository;

    /** Référentiel des employés SODEPA (clé métier centrale). */
    private final EmployeRepository employeRepository;

    /** Moteur Jackson pour la sérialisation des métadonnées de contexte en JSON. */
    private final ObjectMapper objectMapper;

    /** Émetteur d'événements applicatifs Spring pour le découplage événementiel. */
    private final ApplicationEventPublisher eventPublisher;

    /** Référentiel des unités organisationnelles SODEPA. */
    private final UniteOrganisationnelleRepository uniteRepository;

    /** Référentiel des postes organisationnels SODEPA. */
    private final PosteRepository posteRepository;

    /** Gestionnaire d'audit analytique ClickHouse (OLAP). */
    private final ClickHouseManager clickHouseManager;

    private final UtilsService utilsService;

    private final NotificationService notificationService;

    private final MinioService minioService;

    private final DelegationPouvoirRepository delegationPouvoirRepository;

    private final WorkflowEntityHandlerRegistry entityHandlerRegistry;

    private final WorkflowStateService workflowStateService;

    private final WorkflowSnapshotMerger workflowSnapshotMerger;

    private String resolveActorPoste(EmployeEntity emp, WorkflowTaskEntity task) {
        if (emp != null && emp.getPoste() != null) {
            return emp.getPoste().getCodePoste() != null ? emp.getPoste().getCodePoste() : emp.getPoste().getIntitule();
        }
        if (task != null && task.getAssignePoste() != null) {
            return task.getAssignePoste().getCodePoste() != null ? task.getAssignePoste().getCodePoste() : task.getAssignePoste().getIntitule();
        }
        return null;
    }

    private void initierEtatWorkflow(
            WorkflowInstanceEntity instance,
            MakerCheckerOperationType explicitOpType,
            Map<String, Object> initialPayload,
            UtilisateurEntity userInitiateur,
            EmployeEntity empInitiateur
    ) {
        String opType = explicitOpType != null ? explicitOpType.name() : "WORKFLOW";
        String userId = userInitiateur != null ? userInitiateur.getId().toString() : null;
        String username = userInitiateur != null ? userInitiateur.getUsername() : null;
        String poste = resolveActorPoste(empInitiateur, null);
        workflowStateService.recordInitiation(instance, opType, initialPayload, userId, username, poste);
        workflowInstanceRepository.save(instance);
    }

    private void auditClickHouseAsync(WorkflowEventMessageInput input) {
        if (clickHouseManager == null) return;
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                clickHouseManager.saveWorkflowEvent(input);
            } catch (Exception e) {
                log.warn("Impossible d'enregistrer l'événement de workflow dans ClickHouse: {}", e.getMessage());
            }
        });
    }

    /**
     * Démarre une instance de workflow à partir du code de l'OPÉRATION MÉTIER (ex: "RECRUITMENT", "SERVICE_MEMO")
     * et de l'EMPLOYÉ initiateur, sans aucun code de workflow écrit en dur.
     *
     * @param request Requête contenant le code métier, l'employé initiateur et l'entité cible.
     * @return DTO représentant l'instance nouvellement instanciée.
     * @throws IllegalArgumentException Si l'opération métier ou l'employé est introuvable.
     * @throws SecurityException        Si l'opération est réservée à une autre unité ou si l'employé n'est pas éligible Maker.
     * @throws IllegalStateException    Si l'unité de l'employé ne possède aucun circuit configuré pour ce métier (Obligation stricte).
     */
    @Transactional
    public WorkflowInstanceResponse demarrerWorkflowMetier(DemarrerWorkflowMetierRequest request) {
        // 1. Résolution de l'employé initiateur
        EmployeEntity employe = employeRepository.findById(request.getEmployeId())
                .orElseThrow(() -> new RessourceIntrouvableException("Employé initiateur introuvable avec l'ID: " + request.getEmployeId()));

        UniteOrganisationnelleEntity unite = employe.getUniteOrganisationnelle();
        if (unite == null) {
            throw new ConflitMetierException("L'employé " + employe.getMatricule() + " n'est rattaché à aucune unité organisationnelle");
        }

        // 2. Résolution de l'opération métier
        WorkflowMetierEntity metier = workflowMetierRepository.findByCode(request.getCodeMetier().trim())
                .orElseThrow(() -> new RessourceIntrouvableException("Opération métier introuvable avec le code: " + request.getCodeMetier()));

        if (!metier.isActive()) {
            throw new ConflitMetierException("L'opération métier [" + metier.getTitle() + "] est actuellement désactivée");
        }

        // 3. Vérification d'exclusivité métier (si non transversal)
        if (!metier.isTransversal() && metier.getAuthorizedUnit() != null) {
            if (!unite.getId().equals(metier.getAuthorizedUnit().getId())) {
                throw new AccesRefuseException("L'opération métier [" + metier.getTitle()
                        + "] est strictement réservée à l'unité [" + metier.getAuthorizedUnit().getIntitule()
                        + "]. Votre unité actuelle est [" + unite.getIntitule() + "].");
            }
        }

        // 4. Résolution du workflow paramétré pour cette unité spécifique (Obligation stricte - Choix A2)
        WorkflowDefinitionEntity def = workflowDefinitionRepository
                .findByMetierIdAndUniteOrganisationnelleIdAndActifTrue(metier.getId(), unite.getId())
                .or(() -> workflowDefinitionRepository.findByMetierCodeAndUniteId(metier.getCode(), unite.getId()))
                .orElseThrow(() -> new ConflitMetierException("Aucun circuit de validation n'est configuré pour l'opération ["
                        + metier.getTitle() + "] dans l'unité [" + unite.getIntitule()
                        + "]. Veuillez paramétrer le workflow de votre unité avant de continuer."));

        // 5. Contrôle d'éligibilité du Maker
        UtilisateurEntity userInitiateur = employe.getUtilisateur();
        if (userInitiateur == null) {
            throw new ConflitMetierException("L'employé initiateur " + employe.getMatricule() + " ne possède pas de compte utilisateur.");
        }
        verifierEligibiliteMaker(def.getCode());

        // 6. Extraction de la première étape active
        WorkflowEtapeDefinitionEntity premiereEtape = workflowEtapeRepository
                .findByWorkflowDefinitionIdOrderByOrdreAsc(def.getId()).stream()
                .filter(WorkflowEtapeDefinitionEntity::isActif)
                .findFirst()
                .orElseThrow(() -> new ConflitMetierException("Le circuit de validation [" + def.getCode() + "] ne possède aucune étape active configurée"));

        // 7. Sérialisation du contexte / snapshot initial en JSON
        Map<String, Object> initialPayload = workflowSnapshotMerger.combineInitialPayload(
                request.getContexte(), request.getInitialData());
        String contexteJson = workflowSnapshotMerger.toJson(initialPayload);

        // 8. Création de l'instance liée à l'employé et à son unité
        WorkflowInstanceEntity instance = WorkflowInstanceEntity.builder()
                .id(UUID.randomUUID())
                .workflowDefinition(def)
                .entityName(request.getEntityName())
                .entityId(request.getEntityId())
                .initiateurEmploye(employe)
                .initiateur(userInitiateur)
                .uniteOrganisationnelle(unite)
                .statut(StatutWorkflowInstance.EN_COURS)
                .etapeCourante(premiereEtape)
                .donneesContexte(contexteJson)
                .dateInitiation(Instant.now())
                .build();

        instance = workflowInstanceRepository.save(instance);
        initierEtatWorkflow(instance, request.getOperationType(), initialPayload, userInitiateur, employe);

        // 9. Génération en parallèle des tâches pour les acteurs de la première étape
        createNextStepTask(instance, premiereEtape);
        sendNotificationToNextStep(Collections.singleton(premiereEtape), instance);

        // 10. Audit immuable
        UUID acteurAuditId = userInitiateur != null ? userInitiateur.getId() : employe.getId();
        enregistrerAudit(instance, premiereEtape.getCodeEtape(), acteurAuditId, ActionWorkflowAudit.INITIATION, null,
                "Initiation de l'opération [" + metier.getTitle() + "] par l'employé " + employe.getMatricule() + " (" + unite.getIntitule() + ")");

        auditClickHouseAsync(WorkflowEventMessageInput.builder()
                .id(UUID.randomUUID())
                .instanceId(instance.getId())
                .metierCode(metier.getCode())
                .definitionId(def.getId())
                .etapeId(premiereEtape.getId())
                .employeId(employe.getId())
                .matricule(employe.getMatricule())
                .userId(userInitiateur != null ? userInitiateur.getId().toString() : null)
                .username(userInitiateur != null ? userInitiateur.getUsername() : null)
                .uniteId(unite.getId())
                .actionType("DEMARRAGE_WORKFLOW")
                .decision("INITIE")
                .commentaire("Initiation par " + employe.getMatricule() + " (" + unite.getIntitule() + ")")
                .payload(contexteJson)
                .timestamp(java.time.LocalDateTime.now())
                .build());

        log.info("Workflow opération [{}] démarré pour l'employé {} ({}) - Instance: {}",
                metier.getCode(), employe.getMatricule(), unite.getIntitule(), instance.getId());

        return mapToInstanceResponse(instance);
    }

    /**
     * Démarre une instance de workflow pour une entité métier quelconque (Convergence universelle).
     * Si aucun circuit spécifique n'est paramétré, utilise ou provisionne le circuit standard de validation.
     *
     * @param entityName             Nom fonctionnel de l'entité (ex: "BANQUE", "COMPTE", "TIERS").
     * @param entityId               Identifiant technique de l'entité ciblée.
     * @param contexte               Dictionnaire des données ou modifications associées.
     * @param makerCheckerRequestId Identifiant éventuel de la requête métier liée.
     * @return L'instance de workflow créée et active.
     */
    @Transactional
    public WorkflowInstanceEntity demarrerWorkflowPourEntite(
            String entityName,
            String entityId,
            Map<String, Object> contexte,
            UUID makerCheckerRequestId
    ) {
        return demarrerWorkflowPourEntite(entityName, entityId, contexte, makerCheckerRequestId, null);
    }

    @Transactional
    public WorkflowInstanceEntity demarrerWorkflowPourEntite(
            String entityName,
            String entityId,
            Map<String, Object> contexte,
            UUID makerCheckerRequestId,
            MakerCheckerOperationType operationType
    ) {
        return demarrerWorkflowPourEntite(entityName, entityId, contexte, makerCheckerRequestId, operationType, null);
    }

    @Transactional
    public WorkflowInstanceEntity demarrerWorkflowPourEntite(
            String entityName,
            String entityId,
            Map<String, Object> contexte,
            UUID makerCheckerRequestId,
            MakerCheckerOperationType operationType,
            String preferredDefinitionCode
    ) {
        UserData userData = utilsService.getCurrentUserData();
        UUID currentUserId = UUID.fromString(userData.userId());
        UtilisateurEntity userInitiateur = userRepository.findById(currentUserId).orElse(null);
        EmployeEntity empInitiateur = employeRepository.findByUtilisateurId(currentUserId).orElse(null);
        UniteOrganisationnelleEntity unite = empInitiateur != null ? empInitiateur.getUniteOrganisationnelle() : null;

        // 1. Résolution du circuit adéquat (explicite, spécifique ou standard universel)
        WorkflowDefinitionEntity def = resoudreCircuitPourEntite(entityName, unite, preferredDefinitionCode);

        // 2. Première étape obligatoire
        WorkflowEtapeDefinitionEntity premiereEtape = workflowEtapeRepository
                .findFirstByWorkflowDefinitionIdOrderByOrdreAsc(def.getId())
                .orElseThrow(() -> new IllegalStateException("Le circuit [" + def.getCode() + "] ne possède aucune étape configurée"));

        // 3. Contexte JSON
        Map<String, Object> ctx = contexte != null ? new LinkedHashMap<>(contexte) : new LinkedHashMap<>();
        if (makerCheckerRequestId != null) {
            ctx.put("makerCheckerRequestId", makerCheckerRequestId.toString());
        }
        String contexteJson = workflowSnapshotMerger.toJson(ctx);

        // 4. Instanciation du workflow
        WorkflowInstanceEntity instance = WorkflowInstanceEntity.builder()
                .id(UUID.randomUUID())
                .workflowDefinition(def)
                .entityName(entityName)
                .entityId(entityId)
                .initiateur(userInitiateur)
                .initiateurEmploye(empInitiateur)
                .uniteOrganisationnelle(unite != null ? unite : def.getUniteOrganisationnelle())
                .statut(StatutWorkflowInstance.EN_COURS)
                .etapeCourante(premiereEtape)
                .donneesContexte(contexteJson)
                .dateInitiation(Instant.now())
                .build();

        instance = workflowInstanceRepository.save(instance);
        initierEtatWorkflow(instance, operationType, ctx, userInitiateur, empInitiateur);

        // 5. Création des tâches de validation
        createNextStepTask(instance, premiereEtape);
        sendNotificationToNextStep(Collections.singleton(premiereEtape), instance);

        enregistrerAudit(instance, premiereEtape.getCodeEtape(), currentUserId, ActionWorkflowAudit.INITIATION, null,
                "Démarrage automatique du circuit de workflow pour [" + entityName + " / " + entityId + "]");

        log.info("[WORKFLOW-ENGINE] Instance {} créée et active pour entité {}/{} via circuit {}",
                instance.getId(), entityName, entityId, def.getCode());

        return instance;
    }

    private WorkflowDefinitionEntity resoudreCircuitPourEntite(String entityName, UniteOrganisationnelleEntity unite) {
        return resoudreCircuitPourEntite(entityName, unite, null);
    }

    private WorkflowDefinitionEntity resoudreCircuitPourEntite(
            String entityName, UniteOrganisationnelleEntity unite, String preferredDefinitionCode) {
        if (preferredDefinitionCode != null && !preferredDefinitionCode.isBlank()) {
            Optional<WorkflowDefinitionEntity> preferred = workflowDefinitionRepository.findByCode(preferredDefinitionCode.trim());
            if (preferred.isPresent() && preferred.get().isActif()) {
                return preferred.get();
            }
        }
        String cleanName = entityName != null ? entityName.trim().toUpperCase() : "";

        // a. Recherche par code direct
        Optional<WorkflowDefinitionEntity> specific = workflowDefinitionRepository.findByCode("WF-METIER-" + cleanName)
                .or(() -> workflowDefinitionRepository.findByCode("WF-" + cleanName))
                .or(() -> workflowDefinitionRepository.findByCode(cleanName));

        if (specific.isPresent() && specific.get().isActif()) {
            return specific.get();
        }

        if ("EXPRESSION_BESOIN".equalsIgnoreCase(cleanName)) {
            return ensureMultiStepCircuitExpressionBesoin();
        }

        // b. Recherche par métier lié
        Optional<WorkflowMetierEntity> metierOpt = workflowMetierRepository.findByCode(cleanName);
        if (metierOpt.isPresent()) {
            Optional<WorkflowDefinitionEntity> byMetier = workflowDefinitionRepository.findByMetierId(metierOpt.get().getId())
                    .stream().filter(WorkflowDefinitionEntity::isActif).findFirst();
            if (byMetier.isPresent()) {
                return byMetier.get();
            }
        }

        // c. Recherche du circuit standard universel
        Optional<WorkflowDefinitionEntity> standardOpt = workflowDefinitionRepository.findByCode("CIRCUIT_VALIDATION_STANDARD");
        if (standardOpt.isPresent()) {
            return standardOpt.get();
        }

        // d. Auto-provisioning du circuit standard universel si non encore semé
        return autoProvisionCircuitStandard();
    }

    private WorkflowDefinitionEntity autoProvisionCircuitStandard() {
        WorkflowDefinitionEntity def = WorkflowDefinitionEntity.builder()
                .id(UUID.randomUUID())
                .code("CIRCUIT_VALIDATION_STANDARD")
                .intitule("Circuit Universel de Validation Standard SODEPA")
                .description("Circuit par défaut pour toute entité nécessitant une approbation hiérarchique / directionnelle")
                .domaine(WorkflowDomaine.GOUVERNANCE)
                .version(1)
                .actif(true)
                .dateCreation(Instant.now())
                .etapes(new ArrayList<>())
                .build();

        WorkflowEtapeDefinitionEntity etape = WorkflowEtapeDefinitionEntity.builder()
                .id(UUID.randomUUID())
                .workflowDefinition(def)
                .ordre(1)
                .codeEtape("WF-STD-E1")
                .intitule("Validation et Contrôle Hiérarchique")
                .typeEtape(TypeEtapeWorkflow.VALIDATION_HUMAINE)
                .politiqueValidation(PolitiqueValidation.ONLY_ONE_CAN_VALIDATE)
                .strategieAssignation(StrategieAssignation.POSTE_SPECIFIQUE)
                .delaiSlaHeures(48)
                .actionDepassementSla(ActionDepassementSla.RELANCE_EMAIL)
                .actif(true)
                .acteurs(new ArrayList<>())
                .build();

        WorkflowEtapeActeurDefinitionEntity acteur = WorkflowEtapeActeurDefinitionEntity.builder()
                .id(UUID.randomUUID())
                .etapeDefinition(etape)
                .intituleAction("Validation et Contrôle Hiérarchique")
                .strategieAssignation(StrategieAssignation.POSTE_SPECIFIQUE)
                .valeurAssignation("POSTE_DIRECTEUR_DAF")
                .obligatoire(true)
                .actif(true)
                .delaiSlaHeures(48)
                .actionDepassementSla(ActionDepassementSla.RELANCE_EMAIL)
                .build();

        etape.addActeur(acteur);
        def.addEtape(etape);

        return workflowDefinitionRepository.save(def);
    }

    private WorkflowDefinitionEntity ensureMultiStepCircuitExpressionBesoin() {
        Optional<WorkflowDefinitionEntity> existing = workflowDefinitionRepository.findByCode("WF-EXPRESSION_BESOIN");
        if (existing.isPresent() && existing.get().isActif()) {
            return existing.get();
        }
        WorkflowDefinitionEntity def = WorkflowDefinitionEntity.builder()
                .id(UUID.randomUUID())
                .code("WF-EXPRESSION_BESOIN")
                .intitule("Circuit Multi-Postes de Validation des Expressions de Besoin SODEPA")
                .description("Étape 1 : Visa Budget (SET_ATTRIBUTE) + Conformité Marchés -> Étape 2 : Visa Financier DAF -> Étape 3 : Approbation Finale DG")
                .domaine(WorkflowDomaine.ACHATS)
                .version(1)
                .actif(true)
                .dateCreation(Instant.now())
                .etapes(new ArrayList<>())
                .build();

        WorkflowEtapeDefinitionEntity e1 = WorkflowEtapeDefinitionEntity.builder()
                .id(UUID.randomUUID())
                .workflowDefinition(def)
                .ordre(1)
                .codeEtape("WF-EXPRESSION_BESOIN-E1")
                .intitule("Instruction Budgétaire & Conformité Marchés")
                .typeEtape(TypeEtapeWorkflow.VALIDATION_HUMAINE)
                .politiqueValidation(PolitiqueValidation.EVERY_BODY_CAN_VALIDATE)
                .strategieAssignation(StrategieAssignation.POSTE_SPECIFIQUE)
                .delaiSlaHeures(24)
                .actionDepassementSla(ActionDepassementSla.RELANCE_EMAIL)
                .modeActionValidation(ValidationStepActionMode.DO_NOTHING)
                .actif(true)
                .acteurs(new ArrayList<>())
                .build();

        PosteEntity posteBudget = posteRepository.findByCodePoste("POSTE_CHEF_BUDGET").orElse(null);
        WorkflowEtapeActeurDefinitionEntity a1Budget = WorkflowEtapeActeurDefinitionEntity.builder()
                .id(UUID.randomUUID())
                .etapeDefinition(e1)
                .intituleAction("Visa Imputation Budgétaire")
                .poste(posteBudget)
                .strategieAssignation(StrategieAssignation.POSTE_SPECIFIQUE)
                .valeurAssignation("POSTE_CHEF_BUDGET")
                .modeActionValidation(ValidationStepActionMode.SET_ATTRIBUTE)
                .donnees(Map.of("ligneBudgetaireImputation", "string", "montantEstimeTotal", "number"))
                .obligatoire(true)
                .actif(true)
                .delaiSlaHeures(24)
                .actionDepassementSla(ActionDepassementSla.RELANCE_EMAIL)
                .build();
        e1.addActeur(a1Budget);

        PosteEntity posteMarches = posteRepository.findByCodePoste("POSTE_CHEF_MARCHES").orElse(null);
        WorkflowEtapeActeurDefinitionEntity a1Marches = WorkflowEtapeActeurDefinitionEntity.builder()
                .id(UUID.randomUUID())
                .etapeDefinition(e1)
                .intituleAction("Contrôle Conformité Marchés")
                .poste(posteMarches)
                .strategieAssignation(StrategieAssignation.POSTE_SPECIFIQUE)
                .valeurAssignation("POSTE_CHEF_MARCHES")
                .modeActionValidation(ValidationStepActionMode.DO_NOTHING)
                .obligatoire(true)
                .actif(true)
                .delaiSlaHeures(24)
                .actionDepassementSla(ActionDepassementSla.RELANCE_EMAIL)
                .build();
        e1.addActeur(a1Marches);
        def.addEtape(e1);

        WorkflowEtapeDefinitionEntity e2 = WorkflowEtapeDefinitionEntity.builder()
                .id(UUID.randomUUID())
                .workflowDefinition(def)
                .ordre(2)
                .codeEtape("WF-EXPRESSION_BESOIN-E2")
                .intitule("Visa Financier Direction DAF")
                .typeEtape(TypeEtapeWorkflow.VALIDATION_HUMAINE)
                .politiqueValidation(PolitiqueValidation.ONLY_ONE_CAN_VALIDATE)
                .strategieAssignation(StrategieAssignation.POSTE_SPECIFIQUE)
                .delaiSlaHeures(24)
                .actionDepassementSla(ActionDepassementSla.RELANCE_EMAIL)
                .modeActionValidation(ValidationStepActionMode.DO_NOTHING)
                .actif(true)
                .acteurs(new ArrayList<>())
                .build();

        PosteEntity posteDaf = posteRepository.findByCodePoste("POSTE_DIRECTEUR_DAF").orElse(null);
        WorkflowEtapeActeurDefinitionEntity a2Daf = WorkflowEtapeActeurDefinitionEntity.builder()
                .id(UUID.randomUUID())
                .etapeDefinition(e2)
                .intituleAction("Visa Financier DAF")
                .poste(posteDaf)
                .strategieAssignation(StrategieAssignation.POSTE_SPECIFIQUE)
                .valeurAssignation("POSTE_DIRECTEUR_DAF")
                .modeActionValidation(ValidationStepActionMode.DO_NOTHING)
                .obligatoire(true)
                .actif(true)
                .delaiSlaHeures(24)
                .actionDepassementSla(ActionDepassementSla.RELANCE_EMAIL)
                .build();
        e2.addActeur(a2Daf);
        def.addEtape(e2);

        WorkflowEtapeDefinitionEntity e3 = WorkflowEtapeDefinitionEntity.builder()
                .id(UUID.randomUUID())
                .workflowDefinition(def)
                .ordre(3)
                .codeEtape("WF-EXPRESSION_BESOIN-E3")
                .intitule("Approbation Finale Direction Générale")
                .typeEtape(TypeEtapeWorkflow.VALIDATION_HUMAINE)
                .politiqueValidation(PolitiqueValidation.ONLY_ONE_CAN_VALIDATE)
                .strategieAssignation(StrategieAssignation.POSTE_SPECIFIQUE)
                .delaiSlaHeures(24)
                .actionDepassementSla(ActionDepassementSla.RELANCE_EMAIL)
                .modeActionValidation(ValidationStepActionMode.DO_NOTHING)
                .actif(true)
                .acteurs(new ArrayList<>())
                .build();

        PosteEntity posteDg = posteRepository.findByCodePoste("POSTE_DG").orElse(null);
        WorkflowEtapeActeurDefinitionEntity a3Dg = WorkflowEtapeActeurDefinitionEntity.builder()
                .id(UUID.randomUUID())
                .etapeDefinition(e3)
                .intituleAction("Approbation Finale DG")
                .poste(posteDg)
                .strategieAssignation(StrategieAssignation.POSTE_SPECIFIQUE)
                .valeurAssignation("POSTE_DG")
                .modeActionValidation(ValidationStepActionMode.DO_NOTHING)
                .obligatoire(true)
                .actif(true)
                .delaiSlaHeures(24)
                .actionDepassementSla(ActionDepassementSla.RELANCE_EMAIL)
                .build();
        e3.addActeur(a3Dg);
        def.addEtape(e3);

        return workflowDefinitionRepository.save(def);
    }

    /**
     * Vérifie si un circuit actif existe et si l'utilisateur est habilité à l'initier en tant que Maker.
     */
    @Transactional(readOnly = true)
    public void verifierEligibiliteMaker(String workflowCode) {
        if (workflowCode == null || workflowCode.isBlank()) {
            throw new IllegalArgumentException("Le code de workflow est obligatoire pour la vérification du Maker");
        }

        WorkflowDefinitionEntity def = workflowDefinitionRepository.findByCode(workflowCode.trim())
                .orElseThrow(() -> new RessourceIntrouvableException("Aucun circuit de validation configuré pour le code : " + workflowCode));

        if (!def.isActif()) {
            throw new ConflitMetierException("Le circuit de validation [" + workflowCode + "] est actuellement désactivé");
        }

        UserData userData = utilsService.getCurrentUserData();
        EmployeEntity employe = employeRepository.findByUtilisateurId(UUID.fromString(userData.userId())).orElse(null);

        // Les administrateurs et superviseurs sont exemptés
        if (utilsService.isSuperviseurOuAdmin(employe, userData)) {
            return;
        }

        UtilisateurEntity utilisateur = userRepository.findById(UUID.fromString(userData.userId()))
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur non trouvé : " + userData.userId()));

        if (!utilisateur.isActif()) {
            throw new AccesRefuseException("Le compte utilisateur " + utilisateur.getUsername() + " est inactif");
        }

        boolean hasRolesRestriction = def.getRolesInitiateurs() != null && !def.getRolesInitiateurs().isBlank();
        boolean hasPostesRestriction = def.getPostesInitiateurs() != null && !def.getPostesInitiateurs().isBlank();

        if (!hasRolesRestriction && !hasPostesRestriction) {
            return;
        }

        boolean roleAutorise = false;
        if (hasRolesRestriction) {
            List<String> rolesAttendus = Arrays.stream(def.getRolesInitiateurs().split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            roleAutorise = rolesAttendus.stream().anyMatch(role ->
                    utilisateur.getUsername().toLowerCase().contains(role)
                            || utilisateur.getPermissions().stream().anyMatch(p -> p.name().equalsIgnoreCase(role))
            );
        }

        boolean posteAutorise = false;
        if (hasPostesRestriction) {
            List<String> postesAttendus = Arrays.stream(def.getPostesInitiateurs().split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            String monPoste = (employe != null && employe.getPoste() != null)
                    ? (employe.getPoste().getCodePoste() + " " + employe.getPoste().getIntitule()).toLowerCase()
                    : "";

            posteAutorise = postesAttendus.stream().anyMatch(poste ->
                    monPoste.contains(poste) || utilisateur.getUsername().toLowerCase().contains(poste)
            );
        }

        if (!roleAutorise && !posteAutorise) {
            throw new AccesRefuseException("L'employé / utilisateur " + utilisateur.getUsername()
                    + " n'est pas habilité à initier le circuit [" + workflowCode + "]. Rôles/postes requis : "
                    + (hasRolesRestriction ? def.getRolesInitiateurs() : "") + " "
                    + (hasPostesRestriction ? def.getPostesInitiateurs() : ""));
        }
    }

    /**
     * Teste si un utilisateur peut initier un workflow sans lever d'exception.
     */
    @Transactional(readOnly = true)
    public boolean canInitiateWorkflow(String workflowCode) {
        try {
            verifierEligibiliteMaker(workflowCode);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Récupère la liste de toutes les opérations métiers configurées dans le système.
     */
    @Transactional(readOnly = true)
    public List<WorkflowMetierResponse> listerMetiers() {
        return workflowMetierRepository.findAll().stream()
                .map(this::mapToMetierResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère la liste de toutes les opérations métiers configurées dans le système.
     */
    @Transactional(readOnly = true)
    public Page<WorkflowMetierResponse> listerActiveMetiers(Pageable pageable) {
        Page<WorkflowMetierEntity> page = workflowMetierRepository.findAllByIsActiveTrue(pageable);
        return new PageImpl<>(
                page.getContent().stream().map(this::mapToMetierResponse).collect(Collectors.toList()),
                pageable,
                page.getTotalElements()
        );
    }


    @Transactional(readOnly = true)
    public Page<WorkflowDefinitionResponse> listerPageDefinitions(Pageable pageable, Boolean active) {
        if (utilsService.isAdmin()) {
            Page<WorkflowDefinitionEntity> page = (active == null)
                    ? workflowDefinitionRepository.findAll(pageable)
                    : workflowDefinitionRepository.findAllByActif(active, pageable);
            return new PageImpl<>(
                    page.getContent().stream().map(this::mapToDefinitionResponse).collect(Collectors.toList()),
                    pageable,
                    page.getTotalElements()
            );
        } else {
            boolean enableFilter = active == null || active;
            UserData userData = utilsService.getCurrentUserData();
            List<UUID> posteIdList = userData.posteData().stream().map(p -> UUID.fromString(p.id())).collect(Collectors.toList());
            Page<WorkflowDefinitionEntity> pageIds = workflowDefinitionRepository.findPageByPoste(posteIdList, enableFilter, pageable);
            if (pageIds.isEmpty()) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
            List<UUID> idList = pageIds.getContent().stream().map(WorkflowDefinitionEntity::getId).toList();
            Map<UUID, WorkflowDefinitionEntity> byId = workflowDefinitionRepository.findDetailsByIds(idList, posteIdList, enableFilter).stream()
                    .collect(Collectors.toMap(WorkflowDefinitionEntity::getId, Function.identity()));
            List<WorkflowDefinitionResponse> ordered = idList.stream()
                    .map(byId::get).filter(Objects::nonNull).map(this::mapToDefinitionResponse).toList();
            return new PageImpl<>(ordered, pageable, pageIds.getTotalElements());
        }
    }

    /**
     * Démarre une instance de workflow via son code ou ID de définition (rétrocompatibilité).
     */
    @Transactional
    public WorkflowInstanceResponse demarrerWorkflow(DemarrerWorkflowRequest request) {
        WorkflowDefinitionEntity def;
        if (request.getWorkflowDefinitionId() != null) {
            def = workflowDefinitionRepository.findById(request.getWorkflowDefinitionId())
                    .orElseThrow(() -> new RessourceIntrouvableException("Définition introuvable avec l'ID: " + request.getWorkflowDefinitionId()));
        } else if (request.getWorkflowDefinitionCode() != null && !request.getWorkflowDefinitionCode().isBlank()) {
            def = workflowDefinitionRepository.findByCode(request.getWorkflowDefinitionCode())
                    .orElseThrow(() -> new RessourceIntrouvableException("Définition introuvable avec le code: " + request.getWorkflowDefinitionCode()));
        } else {
            throw new RequeteInvalideException("L'identifiant ou le code du circuit est obligatoire");
        }

        verifierEligibiliteMaker(def.getCode());

        UUID requestedInitiateurId = request.getInitiateurId();
        UUID currentUserId = utilsService != null ? utilsService.getCurrentUserIdOrNull() : null;
        if (currentUserId != null) {
            UserData ud = utilsService.getCurrentUserData();
            EmployeEntity currentEmp = employeRepository.findByUtilisateurId(currentUserId).orElse(null);
            if (!utilsService.isSuperviseurOuAdmin(currentEmp, ud)) {
                requestedInitiateurId = currentUserId;
            }
        }
        final UUID effectiveInitiateurId = requestedInitiateurId;
        if (effectiveInitiateurId == null) {
            throw new RequeteInvalideException("L'identifiant de l'initiateur est obligatoire");
        }

        UtilisateurEntity initiateur = userRepository.findById(effectiveInitiateurId).orElse(null);
        EmployeEntity employeInitiateur;
        if (initiateur != null) {
            employeInitiateur = employeRepository.findByUtilisateurId(initiateur.getId()).orElse(null);
        } else {
            employeInitiateur = employeRepository.findById(effectiveInitiateurId)
                    .orElseThrow(() -> new RessourceIntrouvableException("Initiateur introuvable: " + effectiveInitiateurId));
            initiateur = employeInitiateur.getUtilisateur();
            if (initiateur == null) {
                throw new ConflitMetierException("L'employé initiateur " + employeInitiateur.getMatricule() + " ne possède pas de compte utilisateur.");
            }
        }

        WorkflowEtapeDefinitionEntity premiereEtape = workflowEtapeRepository
                .findByWorkflowDefinitionIdOrderByOrdreAsc(def.getId()).stream()
                .filter(WorkflowEtapeDefinitionEntity::isActif)
                .findFirst()
                .orElseThrow(() -> new ConflitMetierException("Le circuit ne possède aucune étape active"));

        Map<String, Object> initialPayload = workflowSnapshotMerger.combineInitialPayload(
                request.getContexte(), request.getInitialData());
        String contexteJson = initialPayload.isEmpty() ? null : workflowSnapshotMerger.toJson(initialPayload);

        WorkflowInstanceEntity instance = WorkflowInstanceEntity.builder()
                .id(UUID.randomUUID())
                .workflowDefinition(def)
                .entityName(request.getEntityName())
                .entityId(request.getEntityId())
                .initiateur(initiateur)
                .initiateurEmploye(employeInitiateur)
                .uniteOrganisationnelle(employeInitiateur != null ? employeInitiateur.getUniteOrganisationnelle() : def.getUniteOrganisationnelle())
                .statut(StatutWorkflowInstance.EN_COURS)
                .etapeCourante(premiereEtape)
                .donneesContexte(contexteJson)
                .dateInitiation(Instant.now())
                .build();

        instance = workflowInstanceRepository.save(instance);
        initierEtatWorkflow(instance, request.getOperationType(), initialPayload, initiateur, employeInitiateur);
        createNextStepTask(instance, premiereEtape);
        sendNotificationToNextStep(Collections.singleton(premiereEtape), instance);
        enregistrerAudit(instance, premiereEtape.getCodeEtape(), initiateur.getId(), ActionWorkflowAudit.INITIATION, null, "Initiation du circuit");

        return mapToInstanceResponse(instance);
    }

    private boolean isRevisionTask(WorkflowTaskEntity tache) {
        return tache != null
                && tache.getIntituleAction() != null
                && tache.getIntituleAction().toLowerCase(Locale.ROOT).contains("révision");
    }

    private boolean isInitiateurRevisionTask(WorkflowTaskEntity tache, UserData userData) {
        if (!isRevisionTask(tache) || userData == null || userData.userId() == null) {
            return false;
        }
        UUID connectedUserId = UUID.fromString(userData.userId());
        WorkflowInstanceEntity inst = tache.getWorkflowInstance();
        return (inst != null && inst.getInitiateur() != null && connectedUserId.equals(inst.getInitiateur().getId()))
                || (tache.getAssigneA() != null && connectedUserId.equals(tache.getAssigneA().getId()));
    }

    /**
     * Traite une tâche de validation avec vérification d'assignation étanche (Verrou 1 anti-usurpation).
     */
    @Transactional
    public void traiterTache(TraitTaskWorkflowRequest request) {
        UserData userData = utilsService.getCurrentUserData();
        WorkflowTaskEntity tache = workflowTacheRepository.findById(request.getWorkflowTaskId()).orElse(null);
        if (tache == null || !isInitiateurRevisionTask(tache, userData)) {
            utilsService.hasPermission(Permissions.WORKFLOW_DECISION);
        }
        if (tache == null) {
            throw new RessourceIntrouvableException("La tâche de workflow spécifiée est introuvable : " + request.getWorkflowTaskId());
        }

        if (tache.getStatut() != StatutWorkflowTache.PENDING) {
            throw new ConflitMetierException("Cette tâche a déjà été traitée ou n'est plus active (statut: " + tache.getStatut() + ").");
        }

        if (request.getDecision() == null || request.getDecision() == WorkflowDecision.PENDING) {
            throw new RequeteInvalideException("Une décision explicite (APPROUVED, REJETE, ou ASK_REVISION) est requise pour traiter une tâche.");
        }
        if ((request.getDecision() == WorkflowDecision.REJETE || request.getDecision() == WorkflowDecision.ASK_REVISION)
                && (request.getCommentaire() == null || request.getCommentaire().isBlank())) {
            throw new RequeteInvalideException("Un motif ou commentaire justificatif est obligatoire en cas de rejet ou de demande de révision.");
        }
        if (tache.getEtapeDefinition() == null) {
            throw new RessourceIntrouvableException("Définition d'étape introuvable pour cette tâche.");
        }
        if (!tache.getEtapeDefinition().isActif()) {
            throw new ConflitMetierException("L'étape de workflow associée à cette tâche n'est plus active.");
        }

        UUID connectedUserId = UUID.fromString(userData.userId());
        EmployeEntity empActeur = employeRepository.findByUtilisateurId(connectedUserId).orElse(null);

        // 1. Contrôle selon la typologie de l'étape
        switch (tache.getEtapeDefinition().getTypeEtape()) {
            case VALIDATION_HUMAINE -> processHumanValidation(tache, request, userData, empActeur);
            case CONTROLE_AUTO -> processAutoValidation(tache, request);
            case SIGNATURE_ELECTRONIQUE -> {
                processHumanValidation(tache, request, userData, empActeur);
                processElectronicSign(tache, request, userData);
            }
            default -> throw new IllegalStateException("WORKFLOW_STEP_TYPE_NOT_SUPPORTED");
        }

        WorkflowInstanceEntity instance = tache.getWorkflowInstance();

        // 2. Téléversement MinIO ou rattachement direct de fichier
        String uploadedFileUrl = null;
        boolean fileAllowed = Objects.equals(tache.getModeActionValidation(), ValidationStepActionMode.FILE_AND_ATTRIBUTE)
                || Objects.equals(tache.getModeActionValidation(), ValidationStepActionMode.ADD_FILE)
                || Objects.equals(tache.getModeActionValidation(), ValidationStepActionMode.OPTIONAL_FILE_OR_ATTRIBUTE);
        boolean fieldsAllowed = Objects.equals(tache.getModeActionValidation(), ValidationStepActionMode.FILE_AND_ATTRIBUTE)
                || Objects.equals(tache.getModeActionValidation(), ValidationStepActionMode.SET_ATTRIBUTE)
                || Objects.equals(tache.getModeActionValidation(), ValidationStepActionMode.OPTIONAL_FILE_OR_ATTRIBUTE);

        if (request.getFile() != null && !request.getFile().isEmpty() && fileAllowed) {
            try {
                String subFolder = "workflows/" + instance.getId() + "/etapes/" + tache.getEtapeDefinition().getCodeEtape();
                String fileName = "justif_" + System.currentTimeMillis() + "_" + request.getFile().getOriginalFilename();
                uploadedFileUrl = minioService.uploadFile(request.getFile(), subFolder + "/" + fileName);
                ensureCurrentData(tache).put("fileUrl", uploadedFileUrl);
            } catch (Exception e) {
                log.error("Échec de l'upload vers MinIO pour la tâche {}: {}", tache.getId(), e.getMessage());
                throw new RuntimeException("Erreur de téléversement MinIO: " + e.getMessage(), e);
            }
        }

        if (request.getChampsModifiesJson() != null && !request.getChampsModifiesJson().isEmpty() && fieldsAllowed) {
            ensureCurrentData(tache).put(tache.getId().toString(), convertToString(request.getChampsModifiesJson()));
        }

        // 3. Contrôle de conformité de l'action requise par le validateur à cette étape
        if (request.getDecision() == WorkflowDecision.APPROUVED
                && tache.getModeActionValidation() != null
                && tache.getModeActionValidation() != ValidationStepActionMode.DO_NOTHING) {
            ValidationStepActionMode mode = tache.getModeActionValidation();
            boolean provideFile = request.getFile() != null && !request.getFile().isEmpty();
            boolean provideFields = request.getChampsModifiesJson() != null && !request.getChampsModifiesJson().isEmpty();

            boolean mustProvideFile = (mode == ValidationStepActionMode.ADD_FILE || mode == ValidationStepActionMode.FILE_AND_ATTRIBUTE);
            boolean mustProvideFields = (mode == ValidationStepActionMode.SET_ATTRIBUTE || mode == ValidationStepActionMode.FILE_AND_ATTRIBUTE);

            if (mustProvideFile && !provideFile) {
                throw new RequeteInvalideException("Action requise non satisfaite : L'étape '" + tache.getEtapeDefinition().getIntitule() + "' impose obligatoirement l'ajout d'une pièce justificative.");
            }
            if (mustProvideFields && !provideFields) {
                throw new RequeteInvalideException("Action requise non satisfaite : L'étape '" + tache.getEtapeDefinition().getIntitule() + "' impose obligatoirement la modification de champs autorisés.");
            }
        }

        // 4. Enregistrement des mutations d'état sur la tâche
        tache.setDecision(request.getDecision());
        tache.setCommentaire(request.getCommentaire());
        tache.setDateDecision(Instant.now());
        if (request.getDecision() == WorkflowDecision.REJETE) {
            tache.setStatut(StatutWorkflowTache.REJETED);
        } else if (request.getDecision() == WorkflowDecision.ASK_REVISION) {
            tache.setStatut(StatutWorkflowTache.EXPIRED);
        } else {
            tache.setStatut(StatutWorkflowTache.APPROUVED);
        }
        workflowTacheRepository.save(tache);

        // 5. Fusion incrémentale de l'état dans ClickHouse (ou Outbox PostgreSQL) et déclenchement des Strategy Handlers
        if (request.getDecision() == WorkflowDecision.APPROUVED) {
            Map<String, Object> allowedPatch = fieldsAllowed ? request.getChampsModifiesJson() : null;
            String stateEventType = isRevisionTask(tache)
                    ? WorkflowStateService.EVENT_REVISION_PATCHED
                    : WorkflowStateService.EVENT_STEP_PATCHED;
            workflowStateService.recordStepDecision(
                    instance,
                    tache,
                    stateEventType,
                    allowedPatch,
                    uploadedFileUrl,
                    connectedUserId.toString(),
                    userData.username(),
                    resolveActorPoste(empActeur, tache),
                    request.getCommentaire()
            );
            workflowInstanceRepository.save(instance);

            String targetEntity = (tache.getEntityPackageReference() != null && !tache.getEntityPackageReference().isBlank())
                    ? tache.getEntityPackageReference()
                    : instance.getEntityName();
            for (WorkflowEntityHandler h : entityHandlerRegistry.getHandlers(targetEntity)) {
                try {
                    h.onStepApproved(tache, request.getChampsModifiesJson());
                } catch (Exception e) {
                    log.error("Erreur dans le handler {} lors de onStepApproved: {}", h.getClass().getSimpleName(), e.getMessage(), e);
                }
            }

            // Publication de l'événement Spring pour tous les modules abonnés (EDA multi-modules)
            eventPublisher.publishEvent(new WorkflowStepApprovedEvent(
                    instance.getId(),
                    instance.getWorkflowDefinition() != null ? instance.getWorkflowDefinition().getCode() : null,
                    instance.getEntityName(),
                    instance.getEntityId(),
                    tache.getEtapeDefinition().getCodeEtape(),
                    tache.getId(),
                    connectedUserId,
                    request.getChampsModifiesJson()
            ));
        }

        // 6. Piste d'audit et ClickHouse
        ActionWorkflowAudit actionAudit = switch (request.getDecision()) {
            case APPROUVED -> ActionWorkflowAudit.APPROBATION;
            case REJETE -> ActionWorkflowAudit.REJET;
            case ASK_REVISION -> ActionWorkflowAudit.DEMANDE_REVISION;
            case PENDING -> throw new IllegalStateException("La décision PENDING ne peut pas être traitée dans l'audit");
        };

        enregistrerAudit(instance, tache.getEtapeDefinition().getCodeEtape(), connectedUserId, actionAudit, request.getDecision(), request.getCommentaire());

        auditClickHouseAsync(WorkflowEventMessageInput.builder()
                .id(UUID.randomUUID())
                .instanceId(instance.getId())
                .metierCode(instance.getWorkflowDefinition() != null && instance.getWorkflowDefinition().getMetier() != null ? instance.getWorkflowDefinition().getMetier().getCode() : null)
                .definitionId(instance.getWorkflowDefinition() != null ? instance.getWorkflowDefinition().getId() : null)
                .etapeId(tache.getEtapeDefinition().getId())
                .tacheId(tache.getId())
                .acteurId(tache.getEtapeActeur() != null ? tache.getEtapeActeur().getId() : null)
                .employeId(empActeur != null ? empActeur.getId() : (tache.getAssigneEmploye() != null ? tache.getAssigneEmploye().getId() : null))
                .matricule(empActeur != null ? empActeur.getMatricule() : (tache.getAssigneEmploye() != null ? tache.getAssigneEmploye().getMatricule() : null))
                .userId(connectedUserId.toString())
                .username(userData.username())
                .uniteId(instance.getUniteOrganisationnelle() != null ? instance.getUniteOrganisationnelle().getId() : null)
                .actionType("DECISION_TACHE")
                .decision(request.getDecision().name())
                .commentaire(request.getCommentaire())
                .timestamp(java.time.LocalDateTime.now())
                .payload(convertToString(mapToTacheResponse(tache)))
                .build());

        // 7. Traitement du cycle de vie selon la décision
        if (request.getDecision() == WorkflowDecision.REJETE) {
            if (tache.isObligatoire()) {
                cloturerInstanceRejetee(instance, tache.getEtapeDefinition(), connectedUserId, request.getCommentaire());
            } else {
                evaluerProgressionEtape(instance, tache.getEtapeDefinition(), connectedUserId, request.getCommentaire());
            }
        } else if (request.getDecision() == WorkflowDecision.ASK_REVISION) {
            instance.setStatut(StatutWorkflowInstance.SUSPENDU);
            workflowInstanceRepository.save(instance);

            creerTacheRevisionInitiateur(instance, tache.getEtapeDefinition(), request.getCommentaire());

            notificationService.notifierRejetDossierWorkflow(
                    instance.getInitiateur(),
                    instance.getInitiateurEmploye(),
                    tache.getEtapeDefinition().getCodeEtape(),
                    instance.getEntityName(),
                    instance.getEntityId(),
                    "Demande de révision / correction : " + request.getCommentaire()
            );

            eventPublisher.publishEvent(new WorkflowRevisionRequestedEvent(
                    instance.getId(),
                    instance.getWorkflowDefinition() != null ? instance.getWorkflowDefinition().getCode() : null,
                    instance.getEntityName(),
                    instance.getEntityId(),
                    tache.getEtapeDefinition().getCodeEtape(),
                    connectedUserId,
                    request.getCommentaire()
            ));

            log.info("Workflow instance {} : Demande de révision formulée à l'étape {}, tâche assignée à l'initiateur",
                    instance.getId(), tache.getEtapeDefinition().getCodeEtape());
        } else if (request.getDecision() == WorkflowDecision.APPROUVED) {
            if (isRevisionTask(tache)) {
                // D5: L'initiateur a soumis ses corrections : réactivation du circuit et de l'étape courante
                instance.setStatut(StatutWorkflowInstance.EN_COURS);
                workflowInstanceRepository.save(instance);

                createNextStepTask(instance, tache.getEtapeDefinition());
                sendNotificationToNextStep(Collections.singleton(tache.getEtapeDefinition()), instance);

                log.info("Workflow instance {} : Corrections soumises par l'initiateur. Étape {} réactivée et notifiée aux validateurs.",
                        instance.getId(), tache.getEtapeDefinition().getCodeEtape());
            } else {
                evaluerProgressionEtape(instance, tache.getEtapeDefinition(), connectedUserId, request.getCommentaire());
            }
        }
    }

    private void processHumanValidation(WorkflowTaskEntity tache, TraitTaskWorkflowRequest request, UserData userData, EmployeEntity empActeur) {
        UUID connectedUserId = UUID.fromString(userData.userId());
        WorkflowInstanceEntity instance = tache.getWorkflowInstance();

        boolean isTacheRevision = isRevisionTask(tache);

        // Règle d'or Maker-Checker 4-yeux (D4) :
        // L'initiateur du dossier ne peut en aucun cas valider ou approuver une étape de son propre dossier,
        // même s'il est superviseur ou administrateur, sauf s'il s'agit expressément d'une tâche de révision/correction qui lui a été renvoyée.
        boolean isInitiateurUser = instance != null && instance.getInitiateur() != null && connectedUserId.equals(instance.getInitiateur().getId());
        boolean isInitiateurEmp = instance != null && instance.getInitiateurEmploye() != null && empActeur != null && empActeur.getId().equals(instance.getInitiateurEmploye().getId());
        if ((isInitiateurUser || isInitiateurEmp) && !isTacheRevision) {
            throw new AccesRefuseException("Ségrégation des tâches (principe Maker-Checker 4-yeux) : " +
                    "vous ne pouvez pas approuver ou valider une étape d'un dossier que vous avez vous-même initié.");
        }

        if (utilsService.isSuperviseurOuAdmin(empActeur, userData)) {
            log.info("L'utilisateur {} est un superviseur ou admin, bypass du verrou d'assignation", userData.username());
            return;
        }

        // 1. Assignation directe utilisateur
        if (tache.getAssigneUser() != null && tache.getAssigneUser().getId().equals(connectedUserId)) {
            return;
        }

        // 2. Assignation directe employé
        if (tache.getAssigneEmploye() != null && empActeur != null && tache.getAssigneEmploye().getId().equals(empActeur.getId())) {
            return;
        }

        // 3. Assignation par poste de l'organigramme
        if (tache.getAssignePoste() != null) {
            Set<UUID> posteIds = (userData.posteData() == null || userData.posteData().isEmpty())
                    ? Collections.emptySet()
                    : userData.posteData().stream().map(p -> UUID.fromString(p.id())).collect(Collectors.toSet());

            if (posteIds.contains(tache.getAssignePoste().getId())) {
                return;
            }
        }

        // 4. Vérification de délégation active
        if (tache.isDelegated() || (tache.getEtapeActeur() != null && tache.getEtapeActeur().isCanBeDelegate())) {
            Instant now = Instant.now();
            List<DelegationPouvoirEntity> delegations = delegationPouvoirRepository.findByDelegataireIdAndStatut(
                    connectedUserId, StatutDelegation.ACTIVE);

            boolean isDelegatedValid = delegations.stream().anyMatch(del ->
                    del.getDateDebut() != null && del.getDateFin() != null &&
                    !now.isBefore(del.getDateDebut()) && !now.isAfter(del.getDateFin()) &&
                    (tache.getAssigneUser() == null || (del.getDelegant() != null && del.getDelegant().getId().equals(tache.getAssigneUser().getId())))
            );

            if (isDelegatedValid) {
                log.info("Accès validé par délégation active pour l'utilisateur {} sur la tâche {}",
                        userData.username(), tache.getId());
                return;
            }
        }

        throw new AccesRefuseException("Action interdite : vous n'êtes pas le validateur assigné à cette tâche ni son délégataire autorisé.");
    }

    private void processAutoValidation(WorkflowTaskEntity tache, TraitTaskWorkflowRequest request) {
        log.info("Traitement automatique SYSTEME de l'étape {}", tache.getEtapeDefinition().getCodeEtape());
        if (request.getDecision() == null) {
            request.setDecision(WorkflowDecision.APPROUVED);
        }
        if (request.getCommentaire() == null || request.getCommentaire().isBlank()) {
            request.setCommentaire("Contrôle automatique SYSTEME validé");
        }
    }

    private void processElectronicSign(WorkflowTaskEntity tache, TraitTaskWorkflowRequest request, UserData userData) {
        log.info("Application de la signature électronique pour la tâche {} par {}", tache.getId(), userData.username());
        String cachet = "Signature certifiée SODEPA - " + userData.username() + " [" + Instant.now() + "]";
        if (request.getCommentaire() == null || request.getCommentaire().isBlank()) {
            request.setCommentaire(cachet);
        } else {
            request.setCommentaire(request.getCommentaire() + " | " + cachet);
        }
    }

    private void creerTacheRevisionInitiateur(WorkflowInstanceEntity instance, WorkflowEtapeDefinitionEntity etapeCourante, String motifRevision) {
        EmployeEntity initiateurEmp = instance.getInitiateurEmploye();
        if (initiateurEmp == null && instance.getInitiateur() != null) {
            initiateurEmp = employeRepository.findByUtilisateurId(instance.getInitiateur().getId()).orElse(null);
        }
        com.sodepa.erp.organigramme.infrastructure.entities.PosteEntity posteInitiateur = initiateurEmp != null ? initiateurEmp.getPoste() : null;

        int delaiHeures = (etapeCourante.getDelaiSlaHeures() != null && etapeCourante.getDelaiSlaHeures() > 0)
                ? etapeCourante.getDelaiSlaHeures()
                : 48;
        Instant echeance = Instant.now().plus(delaiHeures, ChronoUnit.HOURS);

        // Expiration du cycle en cours (PENDING et éventuelles approbations/rejets antérieurs sur cette étape) avant nouvelle boucle
        List<WorkflowTaskEntity> tachesCycleCourant = workflowTacheRepository
                .findByWorkflowInstanceIdAndEtapeDefinitionId(instance.getId(), etapeCourante.getId())
                .stream()
                .filter(t -> t.getStatut() != StatutWorkflowTache.EXPIRED)
                .toList();

        for (WorkflowTaskEntity t : tachesCycleCourant) {
            t.setStatut(StatutWorkflowTache.EXPIRED);
            t.setCommentaire("Tâche clôturée : dossier renvoyé pour révision à l'initiateur.");
            workflowTacheRepository.save(t);
        }

        WorkflowTaskEntity tacheRevision = WorkflowTaskEntity.builder()
                .id(UUID.randomUUID())
                .workflowInstance(instance)
                .etapeDefinition(etapeCourante)
                .intituleAction("Révision / Correction suite aux réserves sur " + etapeCourante.getCodeEtape())
                .obligatoire(true)
                .assigneEmploye(initiateurEmp)
                .assigneUser(instance.getInitiateur())
                .assignePoste(posteInitiateur)
                .statut(StatutWorkflowTache.PENDING)
                .modeActionValidation(ValidationStepActionMode.OPTIONAL_FILE_OR_ATTRIBUTE)
                .commentaire("Motif de renvoi: " + motifRevision)
                .dateCreation(Instant.now())
                .dateEcheance(echeance)
                .isFirstTaskOfStep(true)
                .isLastTaskOfStep(false)
                .build();

        workflowTacheRepository.save(tacheRevision);

        enregistrerAudit(
                instance,
                etapeCourante.getCodeEtape(),
                instance.getInitiateur() != null ? instance.getInitiateur().getId() : null,
                ActionWorkflowAudit.DEMANDE_REVISION,
                WorkflowDecision.ASK_REVISION,
                "Demande de révision transmise à l'initiateur : " + motifRevision
        );
    }

    private String convertToString(Object o){
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            log.error("Erreur de sérialisation JSON: {}", e.getMessage());
            throw new RuntimeException("Erreur de sérialisation JSON", e);
        }
    }

    private void sendNotificationToNextStep(Set<WorkflowEtapeDefinitionEntity> etapes, WorkflowInstanceEntity instance) {
        if (etapes == null || etapes.isEmpty()) return;

        Set<SendNotificationInput> notifInputs = new HashSet<>();
        for (WorkflowEtapeDefinitionEntity etape : etapes) {
            List<WorkflowTaskEntity> taches = workflowTacheRepository.findByWorkflowInstanceIdAndEtapeDefinitionId(instance.getId(), etape.getId());
            for (WorkflowTaskEntity t : taches) {
                UtilisateurEntity user = t.getAssigneUser();
                EmployeEntity emp = t.getAssigneEmploye();
                if (user != null) {
                    notifInputs.add(new SendNotificationInput(
                            "Nouvelle tâche à valider : " + etape.getIntitule(),
                            "Le dossier " + instance.getEntityName() + " [" + instance.getEntityId() + "] requiert votre validation à l'étape " + etape.getCodeEtape(),
                            emp != null ? emp.getTelephone() : null,
                            user.getEmail(),
                            user.getPrenom() + " " + user.getNom(),
                            user.getId(),
                            CanalNotification.EMAIL_ET_SMS,
                            instance.getId(),
                            etape.getCodeEtape()
                    ));
                }
            }
        }
        if (!notifInputs.isEmpty()) {
            notificationService.sendNotificationList(notifInputs);
        }
    }

    private void expirerTachesEnAttente(List<WorkflowTaskEntity> taches, String motif) {
        for (WorkflowTaskEntity t : taches) {
            if (t.getStatut() == StatutWorkflowTache.PENDING) {
                t.setStatut(StatutWorkflowTache.EXPIRED);
                t.setCommentaire(motif);
                workflowTacheRepository.save(t);
            }
        }
    }

    private void cloturerInstanceRejetee(WorkflowInstanceEntity instance, WorkflowEtapeDefinitionEntity etape, UUID acteurId, String commentaire) {
        instance.setStatut(StatutWorkflowInstance.REJETE);
        instance.setDateCloture(Instant.now());
        workflowInstanceRepository.save(instance);

        expirerTachesEnAttente(
                workflowTacheRepository.findByWorkflowInstanceIdAndStatut(instance.getId(), StatutWorkflowTache.PENDING),
                "Tâche annulée suite au rejet définitif de l'instance : " + commentaire
        );

        log.info("Workflow instance {} REJETÉE à l'étape {}", instance.getId(), etape.getCodeEtape());

        eventPublisher.publishEvent(new WorkflowInstanceRejectedEvent(
                instance.getId(),
                instance.getWorkflowDefinition() != null ? instance.getWorkflowDefinition().getCode() : null,
                instance.getEntityName(),
                instance.getEntityId(),
                etape.getCodeEtape(),
                acteurId,
                commentaire
        ));

        for (WorkflowEntityHandler h : entityHandlerRegistry.getHandlers(instance.getEntityName())) {
            try {
                h.onWorkflowRejected(instance, commentaire);
            } catch (Exception e) {
                log.error("Erreur dans le handler {} lors de onWorkflowRejected: {}", h.getClass().getSimpleName(), e.getMessage(), e);
            }
        }

        workflowStateService.recordTerminalEvent(
                instance,
                WorkflowStateService.EVENT_REJECTED,
                etape != null ? etape.getOrdre() : null,
                etape != null ? etape.getCodeEtape() : null,
                acteurId != null ? acteurId.toString() : null,
                resolveActorUsername(acteurId),
                resolveActorPoste(acteurId),
                commentaire
        );
        workflowInstanceRepository.save(instance);

        notificationService.notifierRejetDossierWorkflow(
                instance.getInitiateur(),
                instance.getInitiateurEmploye(),
                etape.getCodeEtape(),
                instance.getEntityName(),
                instance.getEntityId(),
                commentaire
        );
    }

    private void evaluerProgressionEtape(WorkflowInstanceEntity instance, WorkflowEtapeDefinitionEntity etapeCourante, UUID dernierActeurId, String commentaire) {
        List<WorkflowTaskEntity> tachesEtape = workflowTacheRepository
                .findByWorkflowInstanceIdAndEtapeDefinitionId(instance.getId(), etapeCourante.getId());

        if (tachesEtape.isEmpty()) {
            return;
        }

        // D5: Exclure les anciennes tâches expirées/suspendues ainsi que la tâche de révision de l'initiateur
        List<WorkflowTaskEntity> tachesActives = tachesEtape.stream()
                .filter(t -> t.getStatut() != StatutWorkflowTache.EXPIRED)
                .filter(t -> !isRevisionTask(t))
                .collect(Collectors.toList());

        if (tachesActives.isEmpty()) {
            return;
        }

        List<WorkflowTaskEntity> tachesObligatoires = tachesActives.stream()
                .filter(WorkflowTaskEntity::isObligatoire)
                .collect(Collectors.toList());

        boolean toutesObligatoiresValidees = tachesObligatoires.stream()
                .allMatch(t -> t.getStatut() == StatutWorkflowTache.APPROUVED);

        List<WorkflowTaskEntity> tachesOptionnelles = tachesActives.stream()
                .filter(t -> !t.isObligatoire())
                .collect(Collectors.toList());

        boolean optionnelsSatisfaits = true;
        boolean peutEncoreAtteindreQuorum = true;
        if (!tachesOptionnelles.isEmpty()) {
            long approuveesOpt = tachesOptionnelles.stream().filter(t -> t.getStatut() == StatutWorkflowTache.APPROUVED).count();
            long pendingOpt = tachesOptionnelles.stream().filter(t -> t.getStatut() == StatutWorkflowTache.PENDING).count();
            long maxAtteignable = approuveesOpt + pendingOpt;
            PolitiqueValidation pol = etapeCourante.getPolitiqueValidation() != null
                    ? etapeCourante.getPolitiqueValidation()
                    : PolitiqueValidation.ONLY_ONE_CAN_VALIDATE;

            optionnelsSatisfaits = switch (pol) {
                case ONLY_ONE_CAN_VALIDATE -> approuveesOpt >= 1;
                case EVERY_BODY_CAN_VALIDATE -> approuveesOpt == tachesOptionnelles.size();
                case MUST_HAVE_MAJORITY -> approuveesOpt > (tachesOptionnelles.size() / 2);
            };
            peutEncoreAtteindreQuorum = switch (pol) {
                case ONLY_ONE_CAN_VALIDATE -> maxAtteignable >= 1;
                case EVERY_BODY_CAN_VALIDATE -> maxAtteignable == tachesOptionnelles.size();
                case MUST_HAVE_MAJORITY -> maxAtteignable > (tachesOptionnelles.size() / 2);
            };
        }

        if (!tachesOptionnelles.isEmpty() && !optionnelsSatisfaits && !peutEncoreAtteindreQuorum) {
            cloturerInstanceRejetee(instance, etapeCourante, dernierActeurId,
                    "Consensus / quorum devenu impossible à l'étape " + etapeCourante.getCodeEtape()
                            + (commentaire != null ? " : " + commentaire : ""));
            return;
        }

        if (!toutesObligatoiresValidees) {
            log.info("Étape {} : En attente de validation des acteurs obligatoires restants", etapeCourante.getCodeEtape());
            return;
        }

        if (optionnelsSatisfaits) {
            expirerTachesEnAttente(tachesEtape, "Étape franchie : condition de validation atteinte");
            nextStep(instance, etapeCourante, dernierActeurId, commentaire);
        }
    }

    private void nextStep(WorkflowInstanceEntity instance, WorkflowEtapeDefinitionEntity etapeCourante, UUID acteurId, String commentaire) {
        List<WorkflowEtapeDefinitionEntity> etapes = workflowEtapeRepository
                .findByWorkflowDefinitionIdOrderByOrdreAsc(instance.getWorkflowDefinition().getId());

        Optional<WorkflowEtapeDefinitionEntity> suivanteOpt = etapes.stream()
                .filter(WorkflowEtapeDefinitionEntity::isActif)
                .filter(e -> e.getOrdre() > etapeCourante.getOrdre())
                .findFirst();

        if (suivanteOpt.isPresent()) {
            WorkflowEtapeDefinitionEntity suivante = suivanteOpt.get();
            instance.setEtapeCourante(suivante);
            workflowInstanceRepository.save(instance);

            createNextStepTask(instance, suivante);
            sendNotificationToNextStep(Collections.singleton(suivante), instance);
            log.info("Workflow instance {} avance à l'étape: {}", instance.getId(), suivante.getCodeEtape());
        } else {
            instance.setStatut(StatutWorkflowInstance.APPROUVE);
            instance.setDateCloture(Instant.now());
            Map<String, Object> finalData = workflowStateService.getLatestMergedDataMap(instance);
            workflowInstanceRepository.save(instance);

            enregistrerAudit(instance, etapeCourante.getCodeEtape(), acteurId, ActionWorkflowAudit.CLOTURE, WorkflowDecision.APPROUVED, "Circuit approuvé dans sa totalité : " + (commentaire != null ? commentaire : ""));
            log.info("Workflow instance {} TOTALEMENT APPROUVÉE !", instance.getId());

            eventPublisher.publishEvent(new WorkflowInstanceCompletedEvent(
                    instance.getId(),
                    instance.getWorkflowDefinition() != null ? instance.getWorkflowDefinition().getCode() : null,
                    instance.getEntityName(),
                    instance.getEntityId(),
                    etapeCourante.getCodeEtape(),
                    acteurId,
                    commentaire,
                    finalData
            ));

            for (WorkflowEntityHandler h : entityHandlerRegistry.getHandlers(instance.getEntityName())) {
                try {
                    h.onWorkflowCompleted(instance, finalData);
                } catch (ConflitMetierException | RequeteInvalideException | AccesRefuseException e) {
                    throw e;
                } catch (Exception e) {
                    log.error("Erreur dans le handler {} lors de onWorkflowCompleted: {}", h.getClass().getSimpleName(), e.getMessage(), e);
                }
            }

            workflowStateService.recordTerminalEvent(
                    instance,
                    WorkflowStateService.EVENT_COMPLETED,
                    etapeCourante.getOrdre(),
                    etapeCourante.getCodeEtape(),
                    acteurId != null ? acteurId.toString() : null,
                    resolveActorUsername(acteurId),
                    resolveActorPoste(acteurId),
                    commentaire
            );
            workflowInstanceRepository.save(instance);
        }
    }

    /**
     * Résout un employé actif titulaire/rattaché au poste cible (en excluant l'initiateur pour D4).
     */
    private EmployeEntity resoudreEmployeParPoste(WorkflowInstanceEntity instance, PosteEntity poste, UniteOrganisationnelleEntity restrictionUnite) {
        if (poste == null) {
            return null;
        }
        UUID initiateurEmpId = (instance != null && instance.getInitiateurEmploye() != null)
                ? instance.getInitiateurEmploye().getId()
                : null;
        List<EmployeEntity> candidats = employeRepository.findByPosteId(poste.getId()).stream()
                .filter(e -> initiateurEmpId == null || !e.getId().equals(initiateurEmpId))
                .filter(e -> restrictionUnite == null || (e.getUniteOrganisationnelle() != null && e.getUniteOrganisationnelle().getId().equals(restrictionUnite.getId())))
                .toList();
        if (!candidats.isEmpty()) {
            return candidats.get(0);
        }
        return resoudreEmployeAssigne(instance, poste.getCodePoste(), restrictionUnite);
    }

    /**
     * Génère en parallèle les tâches pour les acteurs actifs d'une étape en résolvant l'employé, le poste et l'utilisateur.
     */
    private void createNextStepTask(WorkflowInstanceEntity instance, WorkflowEtapeDefinitionEntity etape) {
        List<WorkflowEtapeActeurDefinitionEntity> acteurs = workflowEtapeActeurRepository.findByEtapeDefinitionId(etape.getId());
        if (acteurs.isEmpty() && etape.getActeurs() != null && !etape.getActeurs().isEmpty()) {
            acteurs = etape.getActeurs();
        }

        List<WorkflowEtapeActeurDefinitionEntity> acteursActifs = acteurs.stream()
                .filter(WorkflowEtapeActeurDefinitionEntity::isActif)
                .toList();

        if (!acteurs.isEmpty() && acteursActifs.isEmpty()) {
            throw new ConflitMetierException("Aucun acteur actif n'est configuré sur l'étape '" + etape.getIntitule() + "'.");
        }

        if (!acteursActifs.isEmpty()) {
            boolean isUniqueActeur = acteursActifs.size() == 1;

            for (WorkflowEtapeActeurDefinitionEntity act : acteursActifs) {
                EmployeEntity assigneEmploye = act.getEmploye();
                UtilisateurEntity assigneUser = act.getUtilisateur();
                PosteEntity ciblePoste = act.getPoste();

                // Résolution dynamique par poste ou valeur d'assignation si non fixé nommément
                if (assigneEmploye == null && assigneUser == null) {
                    if (ciblePoste != null) {
                        assigneEmploye = resoudreEmployeParPoste(instance, ciblePoste, act.getUniteOrganisationnelle());
                    }
                    if (assigneEmploye == null) {
                        assigneEmploye = resoudreEmployeAssigne(instance, act.getValeurAssignation(), act.getUniteOrganisationnelle());
                    }
                    if (assigneEmploye != null) {
                        assigneUser = assigneEmploye.getUtilisateur();
                    } else {
                        String fallbackVal = (act.getValeurAssignation() != null && !act.getValeurAssignation().isBlank())
                                ? act.getValeurAssignation()
                                : (ciblePoste != null ? ciblePoste.getCodePoste() : null);
                        assigneUser = resoudreUtilisateurAssigne(instance, fallbackVal);
                    }
                } else if (assigneEmploye != null && assigneUser == null) {
                    assigneUser = assigneEmploye.getUtilisateur();
                }

                // D4 : Refus formel d'auto-désigner l'initiateur par défaut ou absence de valideur résolu
                if (assigneEmploye == null && assigneUser == null) {
                    log.error("Échec de résolution de validateur pour l'étape {} et l'acteur {}", etape.getCodeEtape(), act.getIntituleAction());
                    throw new ConflitMetierException("Impossible de résoudre un acteur/validateur pour l'étape '"
                            + etape.getIntitule() + "' (règle: " + act.getStrategieAssignation() + ", valeur: " + act.getValeurAssignation()
                            + "). L'initiateur ne peut pas être désigné comme validateur par défaut.");
                }

                Integer slaHeures = act.getDelaiSlaHeures() != null ? act.getDelaiSlaHeures() : etape.getDelaiSlaHeures();
                Instant echeance = (slaHeures != null && slaHeures > 0) ? Instant.now().plus(slaHeures, ChronoUnit.HOURS) : null;
                boolean obligatoire = isUniqueActeur || act.isObligatoire();
                ValidationStepActionMode modeAction = act.getModeActionValidation() != null
                        ? act.getModeActionValidation()
                        : (etape.getModeActionValidation() != null ? etape.getModeActionValidation() : ValidationStepActionMode.OPTIONAL_FILE_OR_ATTRIBUTE);
                PosteEntity posteAssigne = ciblePoste != null
                        ? ciblePoste
                        : (assigneEmploye != null ? assigneEmploye.getPoste() : null);

                WorkflowTaskEntity tache = WorkflowTaskEntity.builder()
                        .id(UUID.randomUUID())
                        .workflowInstance(instance)
                        .etapeDefinition(etape)
                        .etapeActeur(act)
                        .intituleAction(act.getIntituleAction())
                        .obligatoire(obligatoire)
                        .isDelegated(false)
                        .assigneAEmploye(assigneEmploye)
                        .assigneUser(assigneUser)
                        .assignePoste(posteAssigne)
                        .statut(StatutWorkflowTache.PENDING)
                        .dateCreation(Instant.now())
                        .decision(WorkflowDecision.PENDING)
                        .dateEcheance(echeance)
                        .isLastTaskOfStep(etape.isLastTaskOfStep())
                        .isFirstTaskOfStep(etape.isFirstTaskOfStep())
                        .currentData(new HashMap<>())
                        .entityPackageReference(etape.getEntityPackageReference())
                        .modeActionValidation(modeAction)
                        .build();

                workflowTacheRepository.save(tache);
            }
        }
    }

    private java.util.Map<String, Object> ensureCurrentData(WorkflowTaskEntity tache) {
        if (tache.getCurrentData() == null) {
            tache.setCurrentData(new java.util.HashMap<>());
        }
        return tache.getCurrentData();
    }

    private String normaliserTexteRecherche(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return "";
        }
        String withoutAccents = java.text.Normalizer.normalize(valeur, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return withoutAccents.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private boolean tokenMatchesPoste(String token, String posteCombined, java.util.Set<String> posteWords) {
        if (posteWords.contains(token)) {
            return true;
        }
        return switch (token) {
            case "rh" -> posteCombined.contains("ressources humaines");
            case "sd" -> posteCombined.contains("sous directeur");
            case "dg" -> posteCombined.contains("directeur general");
            case "daf" -> posteCombined.contains("administratif") && posteCombined.contains("financier");
            default -> false;
        };
    }

    private int scoreCorrespondanceEmploye(EmployeEntity e, String searchNorm, java.util.Set<String> searchTokens) {
        String codeNorm = normaliserTexteRecherche(e.getPoste() != null ? e.getPoste().getCodePoste() : null);
        String intituleNorm = normaliserTexteRecherche(e.getPoste() != null ? e.getPoste().getIntitule() : null);
        String nomNorm = normaliserTexteRecherche(
                (e.getPrenom() != null ? e.getPrenom() : "") + " " + (e.getNom() != null ? e.getNom() : ""));
        String posteCombined = (codeNorm + " " + intituleNorm).trim();

        if (codeNorm.equals(searchNorm) || intituleNorm.equals(searchNorm) || nomNorm.equals(searchNorm)) {
            return 400;
        }
        if (codeNorm.equals("poste " + searchNorm) || searchNorm.equals("poste " + codeNorm)) {
            return 350;
        }
        if (!posteCombined.isEmpty() && (posteCombined.contains(searchNorm) || codeNorm.contains(searchNorm) || intituleNorm.contains(searchNorm))) {
            return 200 - Math.min(100, Math.abs(intituleNorm.length() - searchNorm.length()));
        }
        if (!nomNorm.isEmpty() && nomNorm.contains(searchNorm)) {
            return 150;
        }
        if (!posteCombined.isEmpty() && !searchTokens.isEmpty()) {
            java.util.Set<String> posteWords = new java.util.HashSet<>(java.util.Arrays.asList(posteCombined.split("\\s+")));
            boolean allMatched = searchTokens.stream().allMatch(token -> tokenMatchesPoste(token, posteCombined, posteWords));
            if (allMatched) {
                return 100 - Math.min(50, posteWords.size());
            }
        }
        return 0;
    }

    private EmployeEntity resoudreEmployeAssigne(WorkflowInstanceEntity instance, String valeurAssignation, UniteOrganisationnelleEntity restrictionUnite) {
        if (valeurAssignation == null || valeurAssignation.isBlank()) {
            return null;
        }

        String searchNorm = normaliserTexteRecherche(valeurAssignation);
        if (searchNorm.isEmpty()) {
            return null;
        }
        java.util.Set<String> searchTokens = new java.util.HashSet<>(java.util.Arrays.asList(searchNorm.split("\\s+")));
        List<EmployeEntity> employes = employeRepository.findAll();
        UUID initiateurEmpId = (instance != null && instance.getInitiateurEmploye() != null) ? instance.getInitiateurEmploye().getId() : null;

        EmployeEntity meilleurCandidat = null;
        int meilleurScore = 0;
        for (EmployeEntity e : employes) {
            if (restrictionUnite != null && (e.getUniteOrganisationnelle() == null || !e.getUniteOrganisationnelle().getId().equals(restrictionUnite.getId()))) {
                continue;
            }
            if (initiateurEmpId != null && e.getId().equals(initiateurEmpId)) {
                continue;
            }
            int score = scoreCorrespondanceEmploye(e, searchNorm, searchTokens);
            if (score > meilleurScore) {
                meilleurScore = score;
                meilleurCandidat = e;
            }
        }
        return meilleurCandidat;
    }

    private UtilisateurEntity resoudreUtilisateurAssigne(WorkflowInstanceEntity instance, String valeurAssignation) {
        if (valeurAssignation != null && !valeurAssignation.isBlank()) {
            String search = valeurAssignation.toLowerCase().trim();
            UUID initiateurUserId = (instance != null && instance.getInitiateur() != null) ? instance.getInitiateur().getId() : null;

            return userRepository.findAll().stream()
                    .filter(UtilisateurEntity::isActif)
                    .filter(u -> initiateurUserId == null || !u.getId().equals(initiateurUserId))
                    .filter(u -> u.getUsername().toLowerCase().contains(search)
                            || (u.getNom() != null && u.getNom().toLowerCase().contains(search)))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private void enregistrerAudit(WorkflowInstanceEntity instance, String etapeCode, UUID acteurId, ActionWorkflowAudit action, WorkflowDecision decision, String commentaire) {
        WorkflowHistoriqueAuditEntity audit = WorkflowHistoriqueAuditEntity.builder()
                .id(UUID.randomUUID())
                .workflowInstance(instance)
                .etapeCode(etapeCode)
                .acteurId(acteurId)
                .action(action)
                .decision(decision)
                .commentaire(commentaire)
                .horodatage(Instant.now())
                .build();

        workflowHistoriqueAuditRepository.save(audit);
    }

    @Transactional(readOnly = true)
    public Page<WorkflowTacheResponse> getMesTaches(StatutWorkflowTache statut, Pageable pageable) {
        StatutWorkflowTache st = statut != null ? statut : StatutWorkflowTache.PENDING;
        UserData userData = utilsService.getCurrentUserData();
        return workflowTacheRepository.findByAssigneAIdAndStatut(UUID.fromString(userData.userId()), st, pageable)
                .map(this::mapToTacheResponse);
    }

    private String resolveActorUsername(UUID acteurId) {
        if (acteurId == null) {
            return null;
        }
        return userRepository.findById(acteurId).map(UtilisateurEntity::getUsername).orElse(null);
    }

    private String resolveActorPoste(UUID acteurId) {
        if (acteurId == null) {
            return null;
        }
        EmployeEntity emp = employeRepository.findByUtilisateurId(acteurId).orElse(null);
        return resolveActorPoste(emp, null);
    }

    private void assertAuthorizedInstance(WorkflowInstanceEntity entity) {
        if (utilsService == null || org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() == null) {
            return;
        }
        UserData userData = utilsService.getCurrentUserData();
        UUID connectedUserId = UUID.fromString(userData.userId());
        EmployeEntity emp = employeRepository.findByUtilisateurId(connectedUserId).orElse(null);
        if (utilsService.isSuperviseurOuAdmin(emp, userData)) {
            return;
        }

        // 2. L'agent est l'initiateur du dossier
        if (entity.getInitiateur() != null && entity.getInitiateur().getId().equals(connectedUserId)) {
            return;
        }
        if (emp != null && entity.getInitiateurEmploye() != null && entity.getInitiateurEmploye().getId().equals(emp.getId())) {
            return;
        }

        // 3. L'instance appartient à la cellule de l'agent
        if (emp != null && emp.getUniteOrganisationnelle() != null && entity.getUniteOrganisationnelle() != null) {
            if (emp.getUniteOrganisationnelle().getId().equals(entity.getUniteOrganisationnelle().getId())) {
                return;
            }
        }

        // 4. L'agent possède une tâche sur cette instance (Need-to-Know)
        boolean hasTask = entity.getTaches().stream().anyMatch(t ->
                (t.getAssigneA() != null && t.getAssigneA().getId().equals(connectedUserId))
                        || (emp != null && t.getAssigneAEmploye() != null && t.getAssigneAEmploye().getId().equals(emp.getId())));

        if (hasTask) {
            return;
        }

        throw new AccesRefuseException("Accès refusé au dossier : vous ne disposez pas des habilitations requises pour consulter cette instance.");
    }

    /**
     * Vérifie le droit de lecture sur une instance de workflow (Superviseur/Admin, Initiateur, Même Unité ou Tâche assignée).
     */
    private WorkflowInstanceEntity getAuthorizedInstance(UUID instanceId) {
        WorkflowInstanceEntity entity = workflowInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new RessourceIntrouvableException("Instance de workflow introuvable: " + instanceId));
        assertAuthorizedInstance(entity);
        return entity;
    }

    /**
     * Consultation d'un dossier avec application stricte du principe Need-to-Know et bypass Superviseurs.
     */
    @Transactional(readOnly = true)
    public WorkflowInstanceResponse getInstanceById(UUID instanceId) {
        return mapToInstanceResponse(getAuthorizedInstance(instanceId));
    }

    /**
     * Vérifie que l'utilisateur connecté dispose d'une habilitation de supervision, de configuration
     * ou d'administration l'autorisant à inspecter directement les états techniques ClickHouse / Outbox.
     */
    private WorkflowInstanceEntity getAuthorizedClickHouseInstance(UUID instanceId) {
        WorkflowInstanceEntity entity = workflowInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new RessourceIntrouvableException("Instance de workflow introuvable: " + instanceId));
        UserData userData = utilsService.getCurrentUserData();

        boolean hasClickHouseAccess = "admin_maker".equalsIgnoreCase(userData.username())
                || (userData.permissions() != null && (
                        userData.permissions().contains(Permissions.WORKFLOW_SUPERVISION)
                        || userData.permissions().contains(Permissions.WORKFLOW_CONFIG)));

        if (!hasClickHouseAccess) {
            throw new AccesRefuseException(
                    "Accès refusé aux états ClickHouse : seules les habilitations WORKFLOW_SUPERVISION, WORKFLOW_CONFIG ou un profil Admin autorisent l'inspection des états.");
        }
        return entity;
    }

    /**
     * Restitue la projection consolidée des données courantes d'une instance (ClickHouse / Outbox, réservé aux profils habilités).
     */
    @Transactional
    public com.sodepa.erp.workflow.presentation.responses.WorkflowCurrentDataResponse getCurrentData(UUID instanceId) {
        WorkflowInstanceEntity entity = getAuthorizedClickHouseInstance(instanceId);
        return workflowStateService.getCurrentData(entity);
    }

    /**
     * Restitue l'historique versionné des états ClickHouse / Outbox d'une instance (réservé aux profils habilités).
     */
    @Transactional
    public List<com.sodepa.erp.workflow.presentation.responses.WorkflowEntityStateResponse> getStateHistory(UUID instanceId) {
        WorkflowInstanceEntity entity = getAuthorizedClickHouseInstance(instanceId);
        return workflowStateService.getStateHistory(entity);
    }



    @Transactional(readOnly = true)
    public Optional<WorkflowInstanceResponse> getInstanceParEntite(String entityName, String entityId) {
        return workflowInstanceRepository.findFirstByEntityNameAndEntityIdOrderByDateInitiationDesc(entityName, entityId)
                .map(entity -> {
                    assertAuthorizedInstance(entity);
                    return mapToInstanceResponse(entity);
                });
    }

    private WorkflowInstanceResponse mapToInstanceResponse(WorkflowInstanceEntity e) {
        List<WorkflowTacheResponse> taches = workflowTacheRepository.findByWorkflowInstanceId(e.getId()).stream()
                .map(this::mapToTacheResponse)
                .collect(Collectors.toList());

        return WorkflowInstanceResponse.builder()
                .id(e.getId())
                .definitionId(e.getWorkflowDefinition() != null ? e.getWorkflowDefinition().getId() : null)
                .definitionCode(e.getWorkflowDefinition() != null ? e.getWorkflowDefinition().getCode() : null)
                .definitionIntitule(e.getWorkflowDefinition() != null ? e.getWorkflowDefinition().getIntitule() : null)
                .uniteOrganisationnelleId(e.getUniteOrganisationnelle() != null ? e.getUniteOrganisationnelle().getId() : null)
                .uniteOrganisationnelleNom(e.getUniteOrganisationnelle() != null ? e.getUniteOrganisationnelle().getIntitule() : null)
                .entityName(e.getEntityName())
                .entityId(e.getEntityId())
                .initiateurEmployeId(e.getInitiateurEmploye() != null ? e.getInitiateurEmploye().getId() : null)
                .initiateurEmployeMatricule(e.getInitiateurEmploye() != null ? e.getInitiateurEmploye().getMatricule() : null)
                .initiateurEmployeNom(e.getInitiateurEmploye() != null ? e.getInitiateurEmploye().getPrenom() + " " + e.getInitiateurEmploye().getNom() : null)
                .initiateurId(e.getInitiateur() != null ? e.getInitiateur().getId() : null)
                .initiateurNom(e.getInitiateur() != null ? e.getInitiateur().getPrenom() + " " + e.getInitiateur().getNom() : null)
                .statut(e.getStatut())
                .etapeCouranteCode(e.getEtapeCourante() != null ? e.getEtapeCourante().getCodeEtape() : null)
                .etapeCouranteIntitule(e.getEtapeCourante() != null ? e.getEtapeCourante().getIntitule() : null)
                .dateInitiation(e.getDateInitiation())
                .dateCloture(e.getDateCloture())
                .taches(taches)
                .build();
    }

    private WorkflowTacheResponse mapToTacheResponse(WorkflowTaskEntity t) {
        String champsModifiables = null;
        if (t.getEtapeActeur() != null && t.getEtapeActeur().getDonnees() != null && !t.getEtapeActeur().getDonnees().isEmpty()) {
            champsModifiables = String.join(",", t.getEtapeActeur().getDonnees().keySet());
        }
        Map<String, Object> taskData = t.getCurrentData();
        if ((taskData == null || taskData.isEmpty()) && t.getWorkflowInstance() != null) {
            try {
                Map<String, Object> cachedOrLive = workflowStateService.getCachedOrLiveDomainData(t.getWorkflowInstance());
                if (cachedOrLive != null && !cachedOrLive.isEmpty()) {
                    taskData = cachedOrLive;
                }
            } catch (Exception ignored) {
            }
        }
        return WorkflowTacheResponse.builder()
                .id(t.getId())
                .instanceId(t.getWorkflowInstance().getId())
                .acteurDefinitionId(t.getEtapeActeur() != null ? t.getEtapeActeur().getId() : null)
                .intituleAction(t.getIntituleAction() != null ? t.getIntituleAction() : (t.getEtapeDefinition() != null ? t.getEtapeDefinition().getIntitule() : null))
                .obligatoire(t.isObligatoire())
                .entityName(t.getWorkflowInstance().getEntityName())
                .entityId(t.getWorkflowInstance().getEntityId())
                .etapeCode(t.getEtapeDefinition() != null ? t.getEtapeDefinition().getCodeEtape() : null)
                .etapeIntitule(t.getEtapeDefinition() != null ? t.getEtapeDefinition().getIntitule() : null)
                .assigneAEmployeId(t.getAssigneAEmploye() != null ? t.getAssigneAEmploye().getId() : null)
                .assigneAEmployeMatricule(t.getAssigneAEmploye() != null ? t.getAssigneAEmploye().getMatricule() : null)
                .assigneAEmployeNom(t.getAssigneAEmploye() != null ? t.getAssigneAEmploye().getPrenom() + " " + t.getAssigneAEmploye().getNom() : null)
                .assigneAEmployePoste((t.getAssigneAEmploye() != null && t.getAssigneAEmploye().getPoste() != null)
                        ? t.getAssigneAEmploye().getPoste().getIntitule()
                        : (t.getAssignePoste() != null ? t.getAssignePoste().getIntitule() : null))
                .assigneAId(t.getAssigneA() != null ? t.getAssigneA().getId() : null)
                .assigneANom(t.getAssigneA() != null ? t.getAssigneA().getPrenom() + " " + t.getAssigneA().getNom() : null)
                .statut(t.getStatut())
                .decision(t.getDecision())
                .commentaire(t.getCommentaire())
                .dateCreation(t.getDateCreation())
                .dateEcheance(t.getDateEcheance())
                .dateDecision(t.getDateDecision())
                .champsModifiablesAutorises(champsModifiables)
                .modeActionValidation(t.getModeActionValidation())
                .pieceJointeUri(t.getPieceJointeUri())
                .champsModifiesJson(t.getChampsModifiesJson())
                .currentData(taskData)
                .build();
    }


    // ═══════════════════════════════════════════════════════════════════════
    // GESTION ET CYCLE DE VIE DES DÉFINITIONS, ÉTAPES ET VALIDATEURS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Crée une nouvelle définition de workflow avec ses étapes et ses validateurs multi-rôles.
     */
    @Transactional
    public WorkflowDefinitionResponse creerDefinition(CreerWorkflowDefinitionRequest request) {
        utilsService.hasPermission(Permissions.WORKFLOW_CONFIG);
        String prefixe = WorkflowDefinitionEntity.getPrefixeCode();
        long seq = CodeSequenceService.nextOrCount(codeSequenceService, "seq_code_workflow_def", workflowDefinitionRepository::count);
        String code = CodeGenerator.generate(prefixe, seq);
        while (workflowDefinitionRepository.existsByCode(code)) {
            seq++;
            code = CodeGenerator.generate(prefixe, seq);
        }

        WorkflowMetierEntity metier = null;
        if (request.getMetierId() != null) {
            metier = workflowMetierRepository.findById(request.getMetierId()).orElse(null);
        } else if (request.getMetierCode() != null) {
            metier = workflowMetierRepository.findByCode(request.getMetierCode()).orElse(null);
        }

        UniteOrganisationnelleEntity unite = null;
        if (request.getUniteOrganisationnelleId() != null) {
            unite = uniteRepository.findById(request.getUniteOrganisationnelleId())
                    .orElseThrow(() -> new IllegalArgumentException("Unité organisationnelle introuvable avec l'ID: " + request.getUniteOrganisationnelleId()));
        }

        WorkflowDefinitionEntity def = WorkflowDefinitionEntity.builder()
                .id(UUID.randomUUID())
                .code(code)
                .intitule(request.getIntitule())
                .description(request.getDescription())
                .domaine(request.getDomaine())
                .metier(metier)
                .uniteOrganisationnelle(unite)
                .rolesInitiateurs(request.getRolesInitiateurs())
                .postesInitiateurs(request.getPostesInitiateurs())
                .version(1)
                .actif(request.isActif())
                .dateCreation(Instant.now())
                .build();

        peuplerEtapesEtActeurs(def, request.getEtapes());

        def = workflowDefinitionRepository.save(def);

        auditClickHouseAsync(WorkflowEventMessageInput.builder()
                .id(UUID.randomUUID())
                .definitionId(def.getId())
                .metierCode(metier != null ? metier.getCode() : null)
                .uniteId(unite != null ? unite.getId() : null)
                .actionType("CREATION_DEFINITION")
                .commentaire("Création du circuit de workflow: " + def.getCode())
                .timestamp(java.time.LocalDateTime.now())
                .build());

        return mapToDefinitionResponse(def);
    }

    /**
     * Modifie un circuit de workflow existant (intitulé, étapes, validateurs, versioning).
     */
    @Transactional
    public WorkflowDefinitionResponse modifierDefinition(UUID id, ModifierWorkflowDefinitionRequest request) {
        utilsService.hasPermission(Permissions.WORKFLOW_CONFIG);
        WorkflowDefinitionEntity def = workflowDefinitionRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Définition introuvable avec l'ID: " + id));

        def.setIntitule(request.getIntitule());
        def.setDescription(request.getDescription());
        def.setDomaine(request.getDomaine());
        def.setRolesInitiateurs(request.getRolesInitiateurs());
        def.setPostesInitiateurs(request.getPostesInitiateurs());
        if (request.getActif() != null) {
            def.setActif(request.getActif());
        }
        def.setVersion(def.getVersion() + 1);

        if (request.getMetierId() != null) {
            def.setMetier(workflowMetierRepository.findById(request.getMetierId()).orElse(null));
        }
        if (request.getUniteOrganisationnelleId() != null) {
            def.setUniteOrganisationnelle(uniteRepository.findById(request.getUniteOrganisationnelleId()).orElse(null));
        }

        if (request.getEtapes() != null) {
            def.getEtapes().clear();
            peuplerEtapesEtActeurs(def, request.getEtapes());
        }

        def = workflowDefinitionRepository.save(def);

        auditClickHouseAsync(WorkflowEventMessageInput.builder()
                .id(UUID.randomUUID())
                .definitionId(def.getId())
                .actionType("MODIFICATION_DEFINITION")
                .commentaire("Mise à jour du circuit de workflow: " + def.getCode() + " (v" + def.getVersion() + ")")
                .timestamp(java.time.LocalDateTime.now())
                .build());

        return mapToDefinitionResponse(def);
    }

    /**
     * Construit et rattache les étapes et leurs validateurs à une définition (création et mise à jour).
     */
    private void peuplerEtapesEtActeurs(
            WorkflowDefinitionEntity def,
            List<CreerWorkflowDefinitionRequest.CreerEtapeDefinitionRequest> etapesReq) {
        if (etapesReq == null) {
            return;
        }
        for (CreerWorkflowDefinitionRequest.CreerEtapeDefinitionRequest etapeReq : etapesReq) {
            String codeEtape = def.getCode() + "-E" + etapeReq.getOrdre();
            WorkflowEtapeDefinitionEntity etape = WorkflowEtapeDefinitionEntity.builder()
                    .id(UUID.randomUUID())
                    .workflowDefinition(def)
                    .ordre(etapeReq.getOrdre())
                    .codeEtape(codeEtape)
                    .intitule(etapeReq.getIntitule())
                    .description(etapeReq.getDescription())
                    .typeEtape(etapeReq.getTypeEtape())
                    .politiqueValidation(etapeReq.getPolitiqueValidation() != null ? etapeReq.getPolitiqueValidation() : PolitiqueValidation.ONLY_ONE_CAN_VALIDATE)
                    .strategieAssignation(etapeReq.getStrategieAssignation())
                    .valeurAssignation(etapeReq.getValeurAssignation())
                    .delaiSlaHeures(etapeReq.getDelaiSlaHeures())
                    .actionDepassementSla(etapeReq.getActionDepassementSla() != null ? etapeReq.getActionDepassementSla() : ActionDepassementSla.RELANCE_EMAIL)
                    .modeActionValidation(etapeReq.getModeActionValidation() != null ? etapeReq.getModeActionValidation() : ValidationStepActionMode.OPTIONAL_FILE_OR_ATTRIBUTE)
                    .actif(true)
                    .build();

            if (etapeReq.getActeurs() != null) {
                for (CreerWorkflowDefinitionRequest.CreerActeurDefinitionRequest actReq : etapeReq.getActeurs()) {
                    EmployeEntity emp = actReq.getEmployeId() != null ? employeRepository.findById(actReq.getEmployeId()).orElse(null) : null;
                    UtilisateurEntity usr = actReq.getUtilisateurId() != null ? userRepository.findById(actReq.getUtilisateurId()).orElse(null) : null;
                    UniteOrganisationnelleEntity actUnite = actReq.getUniteOrganisationnelleId() != null ? uniteRepository.findById(actReq.getUniteOrganisationnelleId()).orElse(null) : null;
                    PosteEntity actPoste = actReq.getPosteId() != null
                            ? posteRepository.findById(actReq.getPosteId())
                                    .orElseThrow(() -> new RessourceIntrouvableException("Poste introuvable avec l'ID: " + actReq.getPosteId()))
                            : null;
                    String cibleVal = (actReq.getValeurAssignation() != null && !actReq.getValeurAssignation().isBlank())
                            ? actReq.getValeurAssignation()
                            : etapeReq.getValeurAssignation();

                    WorkflowEtapeActeurDefinitionEntity acteur = WorkflowEtapeActeurDefinitionEntity.builder()
                            .id(UUID.randomUUID())
                            .etapeDefinition(etape)
                            .intituleAction(actReq.getIntituleAction())
                            .employe(emp)
                            .utilisateur(usr)
                            .uniteOrganisationnelle(actUnite)
                            .poste(actPoste)
                            .strategieAssignation(actReq.getStrategieAssignation())
                            .valeurAssignation(cibleVal)
                            .modeActionValidation(actReq.getModeActionValidation() != null ? actReq.getModeActionValidation() : etape.getModeActionValidation())
                            .donnees(actReq.getFields())
                            .obligatoire(actReq.getObligatoire() != null ? actReq.getObligatoire() : true)
                            .delaiSlaHeures(actReq.getDelaiSlaHeures() != null ? actReq.getDelaiSlaHeures() : etape.getDelaiSlaHeures())
                            .actionDepassementSla(actReq.getActionDepassementSla() != null ? actReq.getActionDepassementSla() : etape.getActionDepassementSla())
                            .actif(actReq.getActif() != null ? actReq.getActif() : true)
                            .build();
                    etape.addActeur(acteur);
                }
            }
            def.addEtape(etape);
        }
    }

    /**
     * Active ou désactive une étape spécifique d'un circuit de validation.
     */
    @Transactional
    public WorkflowDefinitionResponse changerStatutEtape(UUID definitionId, UUID etapeId, boolean actif) {
        utilsService.hasPermission(Permissions.WORKFLOW_CONFIG);
        WorkflowEtapeDefinitionEntity etape = workflowEtapeRepository.findById(etapeId)
                .orElseThrow(() -> new IllegalArgumentException("Étape introuvable avec l'ID: " + etapeId));
        etape.setActif(actif);
        workflowEtapeRepository.save(etape);

        auditClickHouseAsync(WorkflowEventMessageInput.builder()
                .id(UUID.randomUUID())
                .definitionId(definitionId)
                .etapeId(etapeId)
                .actionType("STATUT_ETAPE")
                .commentaire("Statut de l'étape " + etape.getCodeEtape() + " changé à: " + (actif ? "ACTIF" : "INACTIF"))
                .timestamp(java.time.LocalDateTime.now())
                .build());

        WorkflowDefinitionEntity def = workflowDefinitionRepository.findById(definitionId)
                .orElseThrow(() -> new IllegalArgumentException("Définition introuvable: " + definitionId));
        return mapToDefinitionResponse(def);
    }

    /**
     * Active ou désactive un validateur spécifique d'une étape de workflow.
     */
    @Transactional
    public WorkflowDefinitionResponse changerStatutActeur(UUID definitionId, UUID etapeId, UUID acteurId, boolean actif) {
        utilsService.hasPermission(Permissions.WORKFLOW_CONFIG);
        WorkflowEtapeActeurDefinitionEntity acteur = workflowEtapeActeurRepository.findById(acteurId)
                .orElseThrow(() -> new IllegalArgumentException("Validateur introuvable avec l'ID: " + acteurId));
        acteur.setActif(actif);
        workflowEtapeActeurRepository.save(acteur);

        auditClickHouseAsync(WorkflowEventMessageInput.builder()
                .id(UUID.randomUUID())
                .definitionId(definitionId)
                .etapeId(etapeId)
                .acteurId(acteurId)
                .actionType("STATUT_ACTEUR")
                .commentaire("Statut du validateur " + acteur.getIntituleAction() + " changé à: " + (actif ? "ACTIF" : "INACTIF"))
                .timestamp(java.time.LocalDateTime.now())
                .build());

        WorkflowDefinitionEntity def = workflowDefinitionRepository.findById(definitionId)
                .orElseThrow(() -> new IllegalArgumentException("Définition introuvable: " + definitionId));
        return mapToDefinitionResponse(def);
    }

    /**
     * Crée une opération métier dans le registre transversal.
     * Si l'utilisateur n'est pas exempté (Admin, DG, DAF), requiert un workflow de validation.
     */
    @Transactional
    public WorkflowMetierResponse creerMetier(CreerWorkflowMetierRequest request, UUID connectedUserId) {
        if (workflowMetierRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Un métier avec le code [" + request.getCode() + "] existe déjà.");
        }

        UtilisateurEntity user = connectedUserId != null ? userRepository.findById(connectedUserId).orElse(null) : null;
        EmployeEntity emp = connectedUserId != null ? employeRepository.findByUtilisateurId(connectedUserId).orElse(null) : null;

        UserData currentUData = utilsService.getCurrentUserData();
        boolean exempt = utilsService.isSuperviseurOuAdmin(emp, currentUData);

        UniteOrganisationnelleEntity authUnit = null;
        if (request.getAuthorizedUnitId() != null) {
            authUnit = uniteRepository.findById(request.getAuthorizedUnitId()).orElse(null);
        }

        WorkflowMetierEntity metier = WorkflowMetierEntity.builder()
                .id(UUID.randomUUID())
                .code(request.getCode().trim().toUpperCase())
                .title(request.getTitle())
                .description(request.getDescription())
                .domain(request.getDomain())
                .isTransversal(request.getIsTransversal() != null ? request.getIsTransversal() : false)
                .authorizedUnit(authUnit)
                .isActive(exempt ? (request.getIsActive() != null ? request.getIsActive() : true) : false)
                .createdAt(Instant.now())
                .build();

        metier = workflowMetierRepository.save(metier);

        // Si l'utilisateur n'est pas exempté (Admin/DG/DAF), un workflow de création de métier est requis
        if (!exempt && connectedUserId != null) {
            try {
                Map<String, Object> payload = Map.of(
                        "metierId", metier.getId().toString(),
                        "metierCode", metier.getCode(),
                        "title", metier.getTitle()
                );
                demarrerWorkflowMetier(DemarrerWorkflowMetierRequest.builder()
                        .codeMetier("CREATION_METIER")
                        .entityName("WORKFLOW_METIER")
                        .entityId(metier.getId().toString())
                        .employeId(emp != null ? emp.getId() : null)
                        .contexte(payload)
                        .build());
            } catch (Exception e) {
                log.warn("Aucun circuit 'CREATION_METIER' configuré ou erreur de démarrage: {}. Métier enregistré en attente d'activation.", e.getMessage());
            }
        }

        auditClickHouseAsync(WorkflowEventMessageInput.builder()
                .id(UUID.randomUUID())
                .metierCode(metier.getCode())
                .userId(connectedUserId != null ? connectedUserId.toString() : null)
                .actionType("CREATION_METIER")
                .commentaire("Création du processus métier: " + metier.getCode() + " (Exempté: " + exempt + ")")
                .timestamp(java.time.LocalDateTime.now())
                .build());

        return mapToMetierResponse(metier);
    }

    /**
     * Modifie une opération métier existante.
     */
    @Transactional
    public WorkflowMetierResponse modifierMetier(UUID id, ModifierWorkflowMetierRequest request) {
        WorkflowMetierEntity metier = workflowMetierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Métier introuvable avec l'ID: " + id));

        metier.setTitle(request.getTitle());
        metier.setDescription(request.getDescription());
        metier.setDomain(request.getDomain());
        if (request.getIsTransversal() != null) {
            metier.setIsTransversal(request.getIsTransversal());
        }
        if (request.getIsActive() != null) {
            metier.setIsActive(request.getIsActive());
        }
        if (request.getAuthorizedUnitId() != null) {
            metier.setAuthorizedUnit(uniteRepository.findById(request.getAuthorizedUnitId()).orElse(null));
        }

        metier = workflowMetierRepository.save(metier);

        auditClickHouseAsync(WorkflowEventMessageInput.builder()
                .id(UUID.randomUUID())
                .metierCode(metier.getCode())
                .actionType("MODIFICATION_METIER")
                .commentaire("Mise à jour du processus métier: " + metier.getCode())
                .timestamp(java.time.LocalDateTime.now())
                .build());

        return mapToMetierResponse(metier);
    }

    /**
     * Active ou désactive une opération métier.
     */
    @Transactional
    public WorkflowMetierResponse changerStatutMetier(UUID id, boolean isActive) {
        utilsService.hasPermission(Permissions.WORKFLOW_CONFIG);
        WorkflowMetierEntity metier = workflowMetierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Métier introuvable avec l'ID: " + id));
        metier.setIsActive(isActive);
        metier = workflowMetierRepository.save(metier);

        auditClickHouseAsync(WorkflowEventMessageInput.builder()
                .id(UUID.randomUUID())
                .metierCode(metier.getCode())
                .actionType("STATUT_METIER")
                .commentaire("Statut du métier " + metier.getCode() + " changé à: " + (isActive ? "ACTIF" : "INACTIF"))
                .timestamp(java.time.LocalDateTime.now())
                .build());

        return mapToMetierResponse(metier);
    }

    private String resolveEtapeValeurAssignation(WorkflowEtapeDefinitionEntity et) {
        if (et.getValeurAssignation() != null && !et.getValeurAssignation().isBlank()) {
            return et.getValeurAssignation();
        }
        StrategieAssignation strat = et.getStrategieAssignation();
        if ((strat == StrategieAssignation.POSTE_SPECIFIQUE || strat == StrategieAssignation.ROLE_KEYCLOAK)
                && et.getActeurs() != null) {
            return et.getActeurs().stream()
                    .map(WorkflowEtapeActeurDefinitionEntity::getValeurAssignation)
                    .filter(v -> v != null && !v.isBlank())
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private WorkflowDefinitionResponse mapToDefinitionResponse(WorkflowDefinitionEntity def) {
        List<WorkflowDefinitionResponse.WorkflowEtapeResponse> etapes = def.getEtapes().stream()
                .map(et -> {
                    List<WorkflowDefinitionResponse.WorkflowEtapeActeurResponse> acteursDto = et.getActeurs() != null
                            ? et.getActeurs().stream()
                            .map(act -> WorkflowDefinitionResponse.WorkflowEtapeActeurResponse.builder()
                                    .id(act.getId())
                                    .intituleAction(act.getIntituleAction())
                                    .employeId(act.getEmploye() != null ? act.getEmploye().getId() : null)
                                    .employeMatricule(act.getEmploye() != null ? act.getEmploye().getMatricule() : null)
                                    .employeNom(act.getEmploye() != null ? act.getEmploye().getPrenom() + " " + act.getEmploye().getNom() : null)
                                    .utilisateurId(act.getUtilisateur() != null ? act.getUtilisateur().getId() : null)
                                    .utilisateurNom(act.getUtilisateur() != null ? act.getUtilisateur().getPrenom() + " " + act.getUtilisateur().getNom() : null)
                                    .uniteOrganisationnelleId(act.getUniteOrganisationnelle() != null ? act.getUniteOrganisationnelle().getId() : null)
                                    .uniteOrganisationnelleNom(act.getUniteOrganisationnelle() != null ? act.getUniteOrganisationnelle().getIntitule() : null)
                                    .posteId(act.getPoste() != null ? act.getPoste().getId() : null)
                                    .posteCode(act.getPoste() != null ? act.getPoste().getCodePoste() : null)
                                    .posteIntitule(act.getPoste() != null ? act.getPoste().getIntitule() : null)
                                    .strategieAssignation(act.getStrategieAssignation() != null ? act.getStrategieAssignation().name() : null)
                                    .valeurAssignation(act.getValeurAssignation())
                                    .obligatoire(act.isObligatoire())
                                    .delaiSlaHeures(act.getDelaiSlaHeures())
                                    .modeActionValidation(act.getModeActionValidation() != null ? act.getModeActionValidation().name() : null)
                                    .actif(act.isActif())
                                    .build())
                            .collect(Collectors.toList())
                            : Collections.emptyList();

                    String valeurAssignationEtape = resolveEtapeValeurAssignation(et);

                    return WorkflowDefinitionResponse.WorkflowEtapeResponse.builder()
                            .id(et.getId())
                            .ordre(et.getOrdre())
                            .codeEtape(et.getCodeEtape())
                            .intitule(et.getIntitule())
                            .description(et.getDescription())
                            .typeEtape(et.getTypeEtape() != null ? et.getTypeEtape().name() : null)
                            .politiqueValidation(et.getPolitiqueValidation() != null ? et.getPolitiqueValidation().name() : null)
                            .strategieAssignation(et.getStrategieAssignation() != null ? et.getStrategieAssignation().name() : null)
                            .valeurAssignation(valeurAssignationEtape)
                            .delaiSlaHeures(et.getDelaiSlaHeures())
                            .actionDepassementSla(et.getActionDepassementSla() != null ? et.getActionDepassementSla().name() : ActionDepassementSla.RELANCE_EMAIL.name())
                            .modeActionValidation(et.getModeActionValidation() != null ? et.getModeActionValidation().name() : ValidationStepActionMode.OPTIONAL_FILE_OR_ATTRIBUTE.name())
                            .autoriserAjoutFichier(et.getAutoriserAjoutFichier())
                            .fichierValidationObligatoire(et.getFichierValidationObligatoire())
                            .actif(et.isActif())
                            .acteurs(acteursDto)
                            .build();
                })
                .collect(Collectors.toList());

        return WorkflowDefinitionResponse.builder()
                .id(def.getId())
                .metierId(def.getMetier() != null ? def.getMetier().getId() : null)
                .metierCode(def.getMetier() != null ? def.getMetier().getCode() : null)
                .metierTitle(def.getMetier() != null ? def.getMetier().getTitle() : null)
                .uniteOrganisationnelleId(def.getUniteOrganisationnelle() != null ? def.getUniteOrganisationnelle().getId() : null)
                .uniteOrganisationnelleNom(def.getUniteOrganisationnelle() != null ? def.getUniteOrganisationnelle().getIntitule() : null)
                .code(def.getCode())
                .intitule(def.getIntitule())
                .description(def.getDescription())
                .domaine(def.getDomaine())
                .version(def.getVersion())
                .actif(def.isActif())
                .rolesInitiateurs(def.getRolesInitiateurs())
                .postesInitiateurs(def.getPostesInitiateurs())
                .etapes(etapes)
                .build();
    }

    private WorkflowMetierResponse mapToMetierResponse(WorkflowMetierEntity m) {
        return WorkflowMetierResponse.builder()
                .id(m.getId())
                .code(m.getCode())
                .title(m.getTitle())
                .description(m.getDescription())
                .domain(m.getDomain())
                .isTransversal(m.isTransversal())
                .authorizedUnitId(m.getAuthorizedUnit() != null ? m.getAuthorizedUnit().getId() : null)
                .authorizedUnitName(m.getAuthorizedUnit() != null ? m.getAuthorizedUnit().getIntitule() : null)
                .isActive(m.isActive())
                .createdAt(m.getCreatedAt())
                .build();
    }

    // =========================================================================
    // METHODES DE CONVERGENCE DIRECTE WORKFLOW UNIVERSEL (REMPLACEMENT MAKER-CHECKER)
    // =========================================================================

    public UUID submitChange(MakerCheckerEntityName entityName, String entityPk, Map<String, Object> appliedPatch, MakerCheckerOperationType checkerOperationType) {
        return submitChange(entityName, entityPk, appliedPatch, checkerOperationType, null);
    }

    public UUID submitChange(
            MakerCheckerEntityName entityName,
            String entityPk,
            Map<String, Object> appliedPatch,
            MakerCheckerOperationType checkerOperationType,
            String preferredDefinitionCode
    ) {
        UUID requestId = UUID.randomUUID();

        String makerId = "SYSTEM";
        try {
            if (utilsService != null && utilsService.getCurrentUser() != null && utilsService.getCurrentUser().getUserData().isPresent()) {
                makerId = utilsService.getCurrentUser().getUserData().get().userId();
            }
        } catch (Exception e) {
            log.warn("Impossible de recuperer l'utilisateur connecte pour la soumission: {}", e.getMessage());
        }

        MakerCheckerRequestEntity request = MakerCheckerRequestEntity.builder()
                .id(requestId)
                .entityName(entityName)
                .entityPk(entityPk)
                .makerId(makerId)
                .status(MakerCheckerStatus.PENDING)
                .payload(appliedPatch)
                .checkerOperationType(checkerOperationType)
                .createdAt(Instant.now())
                .expiredAt(Instant.now().plus(makerCheckerDurationDays != null ? makerCheckerDurationDays : 30L, ChronoUnit.DAYS))
                .build();

        request = makerCheckerRequestRepo.save(request);

        // Déclenche l'instance de workflow pour gouverner l'ensemble du cycle d'approbation
        try {
            WorkflowInstanceEntity wfInstance = demarrerWorkflowPourEntite(
                    entityName.name(), entityPk, appliedPatch, request.getId(), checkerOperationType, preferredDefinitionCode);
            if (wfInstance != null) {
                request.setWorkflowInstanceId(wfInstance.getId());
                request = makerCheckerRequestRepo.save(request);
            }
        } catch (Exception e) {
            log.warn("Avertissement: circuit de workflow non initie pour {}/{}: {}", entityName, entityPk, e.getMessage());
        }

        // Publier l'événement de soumission vers ClickHouse via RabbitMQ
        publishMakerCheckerEvent(request, makerId, null);

        return request.getId();
    }

    @Transactional
    public void updateMakerCheckerPayload(UUID requestId, Map<String, Object> consolidatedPayload) {
        if (requestId == null || consolidatedPayload == null || consolidatedPayload.isEmpty()) {
            return;
        }
        makerCheckerRequestRepo.findById(requestId).ifPresent(entity -> {
            Map<String, Object> merged = new LinkedHashMap<>();
            if (entity.getPayload() != null) {
                merged.putAll(entity.getPayload());
            }
            merged.putAll(consolidatedPayload);
            entity.setPayload(merged);
            makerCheckerRequestRepo.save(entity);
        });
    }

    public UUID directSubmitChange(MakerCheckerEntityName entityName, String entityPk, Map<String, Object> appliedPatch, MakerCheckerOperationType checkerOperationType) {
        return submitChange(entityName, entityPk, appliedPatch, checkerOperationType);
    }

    public void validateOrReject(UUID requestId, MakerCheckerStatus decision, String notes) {
        if (decision != null) {
            String checkerId = "SYSTEM";
            try {
                if (utilsService != null && utilsService.getCurrentUserData() != null && utilsService.getCurrentUserData().userId() != null) {
                    checkerId = utilsService.getCurrentUserData().userId();
                }
            } catch (Exception ignored) {}
            update(requestId, decision, notes, checkerId);
        }
    }

    @Transactional(readOnly = true)
    public PageRecord<MakerCheckerSmartOutput> findAllByPage(Pageable pageable) {
        return toMakerCheckerPageRecord(makerCheckerRequestRepo.findAll(pageable));
    }

    @Transactional(readOnly = true)
    public PageRecord<MakerCheckerSmartOutput> findAllByStatusAndByPage(Pageable pageable, MakerCheckerStatus status) {
        return toMakerCheckerPageRecord(makerCheckerRequestRepo.findAllByStatus(status, pageable));
    }

    @Transactional(readOnly = true)
    public PageRecord<MakerCheckerSmartOutput> findAllByEntityNameAndByPage(Pageable pageable, MakerCheckerEntityName entityName) {
        return toMakerCheckerPageRecord(makerCheckerRequestRepo.findAllByEntityName(entityName, pageable));
    }

    @Transactional(readOnly = true)
    public PageRecord<MakerCheckerSmartOutput> findAllByEntityNameAndStatusAndByPage(
            Pageable pageable, MakerCheckerEntityName entityName, MakerCheckerStatus status) {
        return toMakerCheckerPageRecord(makerCheckerRequestRepo.findAllByEntityNameAndStatus(entityName, status, pageable));
    }

    @Transactional(readOnly = true)
    public PageRecord<MakerCheckerSmartOutput> findAllToValideByOther(
            Pageable pageable, MakerCheckerEntityName entityName,
            MakerCheckerStatus status, String makerId) {
        return toMakerCheckerPageRecord(makerCheckerRequestRepo.findAllByEntityNameAndStatusAndMakerIdNot(
                entityName, status, makerId, pageable));
    }

    private PageRecord<MakerCheckerSmartOutput> toMakerCheckerPageRecord(Page<MakerCheckerRequestEntity> page) {
        boolean paged = page.getPageable().isPaged();
        return new PageRecord<>(
                page.getContent().stream().map(makerCheckerMapper::toSmartDTO).collect(Collectors.toList()),
                page.isEmpty(),
                page.isFirst(),
                page.isLast(),
                page.getNumber(),
                page.getNumberOfElements(),
                PageableRecord.builder()
                        .offset(paged ? page.getPageable().getOffset() : 0L)
                        .pageNumber(paged ? (long) page.getPageable().getPageNumber() : 0L)
                        .pageSize(paged ? (long) page.getPageable().getPageSize() : 0L)
                        .paged(paged)
                        .sort(page.getPageable().getSort())
                        .unpaged(!paged)
                        .build(),
                page.getSize(),
                page.getSort(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    public MakerCheckerOutput findById(UUID id) {
        MakerCheckerRequestEntity entity = makerCheckerRequestRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("MakerChecker Request data not found: " + id));
        return this.mappingMakerChecker(entity);
    }

    public MakerCheckerOutput findByEntityIdAndEntityName(UUID id, MakerCheckerEntityName entityName) {
        return null;
    }

    @Transactional
    public void update(UUID id, MakerCheckerStatus status, String note, String checkerId) {
        MakerCheckerRequestEntity entity = makerCheckerRequestRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("MakerChecker Request data not found: " + id));
        entity.setStatus(status);
        entity.setNotes(note);
        entity.setDecidedAt(Instant.now());
        entity.setCheckerId(checkerId);
        entity = makerCheckerRequestRepo.save(entity);

        // Publier l'événement de décision vers ClickHouse via RabbitMQ
        publishMakerCheckerEvent(entity, entity.getMakerId(), checkerId);
    }

    public void validate(UUID id, Instant createdAt, Instant expireAt) {
        Instant now = Instant.now();
        try {
            if (createdAt == null || expireAt == null) {
                throw new RuntimeException("Request creation or expiration date is missing");
            }

            if (now.isBefore(createdAt)) {
                throw new RuntimeException("Request is not yet valid: current date is before the creation date");
            }

            if (now.isAfter(expireAt)) {
                throw new RuntimeException("Request has expired: current date is after the expiration date");
            }
        } catch (Exception ex) {
            update(id, MakerCheckerStatus.EXPIRED, ex.getMessage(), "SYSTEM");
            throw new RuntimeException(ex.getMessage());
        }
    }

    private MakerCheckerOutput mappingMakerChecker(MakerCheckerRequestEntity entity) {
        UserRecordSmartOutput maker = null;
        if (entity.getMakerId() != null) {
            if ("SYSTEM".equals(entity.getMakerId())) {
                maker = UserRecordSmartOutput.builder()
                        .id(UUID.randomUUID())
                        .name("SYSTEM")
                        .image(null)
                        .status(true)
                        .build();
            } else {
                try {
                    UUID uId = UUID.fromString(entity.getMakerId());
                    if (userAdapterInterface != null) {
                        maker = userAdapterInterface.getUserById(uId);
                    }
                    if (maker == null) {
                        java.util.Optional<com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity> uOpt = userRepository.findById(uId);
                        if (uOpt.isPresent()) {
                            com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity u = uOpt.get();
                            maker = UserRecordSmartOutput.builder()
                                    .id(u.getId())
                                    .name(u.getPrenom() != null ? u.getPrenom() + " " + u.getNom() : u.getNom())
                                    .image(u.getPhotoProfile())
                                    .status(u.isActif())
                                    .build();
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        if (maker == null && !"SYSTEM".equals(entity.getMakerId())) {
            maker = UserRecordSmartOutput.builder()
                    .id(UUID.randomUUID())
                    .name("Utilisateur " + entity.getMakerId())
                    .image(null)
                    .status(true)
                    .build();
        }

        UserRecordSmartOutput checker = null;
        if (entity.getCheckerId() != null) {
            if ("SYSTEM".equals(entity.getCheckerId())) {
                checker = UserRecordSmartOutput.builder()
                        .id(UUID.randomUUID())
                        .name("SYSTEM")
                        .image(null)
                        .status(true)
                        .build();
            } else {
                try {
                    UUID cId = UUID.fromString(entity.getCheckerId());
                    if (userAdapterInterface != null) {
                        checker = userAdapterInterface.getUserById(cId);
                    }
                    if (checker == null) {
                        java.util.Optional<com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity> cOpt = userRepository.findById(cId);
                        if (cOpt.isPresent()) {
                            com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity c = cOpt.get();
                            checker = UserRecordSmartOutput.builder()
                                    .id(c.getId())
                                    .name(c.getPrenom() != null ? c.getPrenom() + " " + c.getNom() : c.getNom())
                                    .image(c.getPhotoProfile())
                                    .status(c.isActif())
                                    .build();
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        return MakerCheckerOutput.builder()
                .id(entity.getId())
                .entityName(entity.getEntityName())
                .entityPk(entity.getEntityPk())
                .maker(maker)
                .checker(checker)
                .status(entity.getStatus())
                .payload(entity.getPayload())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .decidedAt(entity.getDecidedAt())
                .expiredAt(entity.getExpiredAt())
                .checkerOperationType(entity.getCheckerOperationType())
                .build();
    }

    private void checkerMakerChecker(MakerCheckerRequestEntity makerCheckerRequest, String maker_Id, String checker_Id) {
        try {
            String payloadJson = objectMapper.writeValueAsString(makerCheckerRequest);
            MakerCheckerMessageInput msg = MakerCheckerMessageInput.builder()
                    .id(UUID.randomUUID())
                    .entityName(makerCheckerRequest.getEntityName().name())
                    .entityPk(makerCheckerRequest.getEntityPk())
                    .payload(payloadJson)
                    .timestamp(java.time.LocalDateTime.now())
                    .maker_id(maker_Id)
                    .checker_id(checker_Id)
                    .build();
            auditEventPublisher.publishMakerChecker(msg);
        } catch (JsonProcessingException e) {
            log.warn("Echec de serialisation du payload MakerChecker pour ClickHouse : {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Echec de publication de l'evenement MakerChecker vers RabbitMQ : {}", e.getMessage());
        }
    }

    private void publishMakerCheckerEvent(MakerCheckerRequestEntity makerCheckerRequest, String maker_Id, String checker_Id) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            checkerMakerChecker(makerCheckerRequest, maker_Id, checker_Id);
        });
    }

}
