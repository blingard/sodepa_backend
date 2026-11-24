package com.sodepa.erp.achats.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.achats.infrastructure.PvEventInput;
import com.sodepa.erp.achats.infrastructure.entities.*;
import com.sodepa.erp.achats.infrastructure.repo.*;
import com.sodepa.erp.achats.presentation.requests.*;
import com.sodepa.erp.achats.presentation.responses.*;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.configuration.parametres.application.ParametreConfigurationService;
import com.sodepa.erp.ged.infrastructure.entities.DocumentGedEntity;
import com.sodepa.erp.ged.infrastructure.repo.DocumentGedRepository;
import com.sodepa.erp.organigramme.infrastructure.entities.UniteOrganisationnelleEntity;
import com.sodepa.erp.organigramme.infrastructure.repo.UniteOrganisationnelleRepository;
import com.sodepa.erp.rh.infrastructure.repo.EmployeRepository;
import com.sodepa.erp.share.CodeSequenceService;
import com.sodepa.erp.share.GlobalConstants;
import com.sodepa.erp.share.MakerCheckerOutput;
import com.sodepa.erp.share.MinioService;
import com.sodepa.erp.share.UserData;
import com.sodepa.erp.share.erreurs.ConflitMetierException;
import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.MakerCheckerOperationType;
import com.sodepa.erp.utils.MakerCheckerStatus;
import com.sodepa.erp.utils.Permissions;
import com.sodepa.erp.workflow.application.WorkflowService;
import com.sodepa.erp.workflow.application.WorkflowSnapshotMerger;
import com.sodepa.erp.workflow.application.WorkflowStateService;
import com.sodepa.erp.workflow.infrastructure.entities.StatutWorkflowInstance;
import com.sodepa.erp.workflow.infrastructure.entities.WorkflowInstanceEntity;
import com.sodepa.erp.workflow.infrastructure.repo.WorkflowInstanceRepository;
import com.sodepa.erp.workflow.presentation.requests.DemarrerWorkflowRequest;
import com.sodepa.erp.workflow.presentation.responses.WorkflowCurrentDataResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service métier d'application implémentant la logique et les règles de gestion pour `AchatsService` au sein de la SODEPA.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AchatsService {


    private final AchatsSecurityService securityService;
    private final FournisseurRepository fournisseurRepository;
    private final ExpressionBesoinRepository expressionBesoinRepository;
    private final ExpressionBesoinLigneRepository expressionBesoinLigneRepository;
    private final MarcheDossierRepository marcheDossierRepository;
    private final MarcheDossierLigneRepository marcheDossierLigneRepository;
    private final MarcheReceptionPvRepository marcheReceptionPvRepository;
    private final EmployeRepository employeRepository;
    private final UniteOrganisationnelleRepository uniteOrganisationnelleRepository;
    private final ParametreConfigurationService configService;
    private final MinioService minioService;
    private final DocumentGedRepository documentGedRepository;
    private final WorkflowService workflowService;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final WorkflowStateService workflowStateService;
    private final WorkflowSnapshotMerger workflowSnapshotMerger;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CodeSequenceService codeSequenceService;

    // ==========================================
    // FOURNISSEURS
    // ==========================================

    private static final List<String> FRN_WORKFLOW_ENTITY_NAMES = List.of(
            "FOURNISSEUR",
            "FRN",
            FournisseurEntity.class.getSimpleName(),
            FournisseurEntity.class.getName()
    );

    @Transactional
    public List<FournisseurResponse> listerFournisseurs(boolean agreeSeulement) {
        securityService.checkPermission(Permissions.LIST_PROVIDERS);
        List<FournisseurEntity> list = agreeSeulement
                ? fournisseurRepository.findByAgreeSodepaTrueAndActifTrue()
                : fournisseurRepository.findAll(Sort.by(Sort.Direction.ASC, "raisonSociale"));

        Map<UUID, WorkflowInstanceEntity> activeById = workflowStateService.findActiveInstancesByEntityUuid(FRN_WORKFLOW_ENTITY_NAMES);
        java.util.Set<UUID> materializedIds = new java.util.HashSet<>();
        List<FournisseurResponse> responses = new ArrayList<>();

        for (FournisseurEntity entity : list) {
            materializedIds.add(entity.getId());
            FournisseurResponse res = FournisseurResponse.fromEntity(entity);
            WorkflowInstanceEntity activeWf = activeById.get(entity.getId());
            if (activeWf != null) {
                enrichirFournisseurAvecWorkflow(res, activeWf);
            }
            responses.add(res);
        }

        // Si on ne demande pas exclusivement les fournisseurs déjà agréés, projeter les créations en cours depuis ClickHouse/Outbox
        if (!agreeSeulement) {
            for (Map.Entry<UUID, WorkflowInstanceEntity> entry : activeById.entrySet()) {
                if (materializedIds.contains(entry.getKey())) {
                    continue;
                }
                WorkflowInstanceEntity wf = entry.getValue();
                WorkflowCurrentDataResponse current = workflowStateService.getCachedCurrentData(wf);
                Map<String, Object> data = current.getData() != null ? current.getData() : new java.util.LinkedHashMap<>();
                responses.add(buildDraftFournisseurFromWorkflow(entry.getKey(), wf, data, current));
            }
        }

        responses.sort(java.util.Comparator.comparing(
                r -> r.getRaisonSociale() != null ? r.getRaisonSociale().toLowerCase() : "",
                java.util.Comparator.naturalOrder()));
        return responses;
    }

    @Transactional
    public FournisseurResponse obtenirFournisseur(UUID id) {
        securityService.checkPermission(Permissions.LIST_PROVIDERS);
        Optional<FournisseurEntity> opt = fournisseurRepository.findById(id);
        Optional<WorkflowInstanceEntity> activeWfOpt = workflowStateService.findActiveInstanceForEntity(FRN_WORKFLOW_ENTITY_NAMES, id);

        if (opt.isPresent()) {
            FournisseurResponse res = FournisseurResponse.fromEntity(opt.get());
            activeWfOpt.ifPresent(wf -> enrichirFournisseurAvecWorkflow(res, wf));
            return res;
        }

        if (activeWfOpt.isPresent()) {
            WorkflowInstanceEntity wf = activeWfOpt.get();
            WorkflowCurrentDataResponse current = workflowStateService.getCurrentData(wf);
            Map<String, Object> data = current.getData() != null ? current.getData() : new java.util.LinkedHashMap<>();
            return buildDraftFournisseurFromWorkflow(id, wf, data, current);
        }

        throw new IllegalArgumentException("Fournisseur introuvable avec l'ID: " + id);
    }

    @Transactional
    public UUID creerFournisseur(CreerFournisseurRequest request) {
        securityService.checkPermission(Permissions.CREATE_PROVIDER);
        long activeCount = workflowStateService.findActiveInstancesByEntityUuid(FRN_WORKFLOW_ENTITY_NAMES).size();
        long count = fournisseurRepository.count() + activeCount + 1;
        String code = String.format("FRN-%04d", count);
        while (fournisseurRepository.findByCode(code).isPresent()) {
            count++;
            code = String.format("FRN-%04d", count);
        }
        UUID id = UUID.randomUUID();

        Map<String, Object> payload = new java.util.LinkedHashMap<>(objectMapper.convertValue(request, Map.class));
        payload.put("id", id.toString());
        payload.put("code", code);

        // Zéro auto-validation ni persistance anticipée en base PostgreSQL :
        // l'état v1 est écrit de manière synchrone dans ClickHouse (ou Outbox).
        UUID uuid = workflowService.submitChange(
                MakerCheckerEntityName.FOURNISSEUR,
                id.toString(),
                payload,
                MakerCheckerOperationType.CREATE
        );
        log.info("Création de fournisseur soumise au workflow (ClickHouse State Store). ID: {}, Code: {}, Raison Sociale: {} (Ticket: {})",
                id, code, request.getRaisonSociale(), uuid);
        return id;
    }

    @Transactional
    public FournisseurResponse modifierFournisseur(UUID id, CreerFournisseurRequest request) {
        securityService.checkPermission(Permissions.CREATE_PROVIDER);
        FournisseurEntity existing = fournisseurRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fournisseur introuvable avec l'ID: " + id));

        if (workflowStateService.findActiveInstanceForEntity(FRN_WORKFLOW_ENTITY_NAMES, id).isPresent()) {
            throw new ConflitMetierException("Une demande de modification est déjà en cours de validation pour ce fournisseur.");
        }

        Map<String, Object> payload = new java.util.LinkedHashMap<>(objectMapper.convertValue(request, Map.class));
        payload.put("id", existing.getId().toString());
        payload.put("code", existing.getCode());

        UUID uuid = workflowService.submitChange(
                MakerCheckerEntityName.FOURNISSEUR,
                existing.getId().toString(),
                payload,
                MakerCheckerOperationType.UPDATE
        );
        log.info("Modification fournisseur {} soumise au workflow (Ticket: {})", existing.getCode(), uuid);

        FournisseurResponse res = FournisseurResponse.fromEntity(existing);
        workflowStateService.findActiveInstanceForEntity(FRN_WORKFLOW_ENTITY_NAMES, id)
                .ifPresent(wf -> enrichirFournisseurAvecWorkflow(res, wf));
        return res;
    }

    @Transactional
    public void validateOrRejectFournisseur(ValidateOrRejectSubmissionInput input) {
        securityService.checkPermission(Permissions.VALIDATE_PROVIDER);
        MakerCheckerOutput checkerOutput = workflowService.findById(input.id());
        MakerCheckerStatus currentStatus = checkerOutput.status();
        if (currentStatus != MakerCheckerStatus.PENDING) {
            throw new RuntimeException(String.format(
                    "Invalid transition: Cannot transition from %s to %s.", currentStatus, input.decision()));
        }

        UUID checkerId = UUID.fromString(securityService.getUtilsService().getCurrentUserData().userId());
        if (checkerOutput.maker() != null && checkerOutput.maker().id() != null && checkerOutput.maker().id().equals(checkerId)) {
            throw new RuntimeException("A checker cannot vote on their own request.");
        }

        MakerCheckerStatus newStatus;
        if (MakerCheckerStatus.ACCEPTED.equals(input.decision())) {
            newStatus = MakerCheckerStatus.ACCEPTED;
        } else if (MakerCheckerStatus.REJECTED.equals(input.decision())) {
            newStatus = MakerCheckerStatus.REJECTED;
        } else {
            throw new RuntimeException("Invalid decision: " + input.decision());
        }

        workflowService.validate(checkerOutput.id(), checkerOutput.createdAt(), checkerOutput.expiredAt());

        if (MakerCheckerStatus.REJECTED.equals(newStatus)) {
            workflowService.update(checkerOutput.id(), newStatus, input.notes(), checkerId.toString());
            log.info("Fournisseur rejeté par le Checker {} (zéro écriture Postgres). Ticket: {}", checkerId, checkerOutput.id());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID frnId = UUID.fromString(checkerOutput.entityPk());
            materialiserFournisseurFinale(frnId, checkerOutput.payload());
            workflowService.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
            log.info("Fournisseur approuvé et matérialisé en PostgreSQL. Ticket: {}, PK: {}", checkerId, checkerOutput.id(), frnId);
        }
    }

    @Transactional
    public void finaliserFournisseurDepuisWorkflow(UUID frnId, Map<String, Object> finalData) {
        if (frnId == null || finalData == null || finalData.isEmpty()) {
            return;
        }
        materialiserFournisseurFinale(frnId, finalData);
    }

    private void materialiserFournisseurFinale(UUID frnId, Map<String, Object> payload) {
        Map<String, Object> safePayload = payload != null ? payload : new java.util.LinkedHashMap<>();
        Optional<FournisseurEntity> existingOpt = fournisseurRepository.findById(frnId);
        if (existingOpt.isPresent()) {
            miseAJourFournisseur(existingOpt.get(), safePayload);
        } else {
            creationFournisseur(safePayload, frnId);
        }
    }

    private FournisseurEntity creationFournisseur(Map<String, Object> payload, UUID frnId) {
        String code = asString(payload.get("code"));
        if (code == null || code.isBlank() || fournisseurRepository.findByCode(code).isPresent()) {
            long seq = CodeSequenceService.nextOrCount(codeSequenceService, "seq_code_fournisseur", fournisseurRepository::count);
            code = String.format("FRN-%04d", seq);
            while (fournisseurRepository.findByCode(code).isPresent()) {
                seq++;
                code = String.format("FRN-%04d", seq);
            }
        }
        FournisseurEntity entity = FournisseurEntity.builder()
                .id(frnId)
                .code(code)
                .raisonSociale(nonBlankOrDefault(asString(payload.get("raisonSociale")), "Fournisseur SODEPA"))
                .numeroContribuable(asString(payload.get("numeroContribuable")))
                .rccm(asString(payload.get("rccm")))
                .regimeFiscal(nonBlankOrDefault(asString(payload.get("regimeFiscal")), "REEL"))
                .specialite(asString(payload.get("specialite")))
                .adresse(asString(payload.get("adresse")))
                .telephone(asString(payload.get("telephone")))
                .email(asString(payload.get("email")))
                .banqueNom(asString(payload.get("banqueNom")))
                .compteBancaireRib(asString(payload.get("compteBancaireRib")))
                .agreeSodepa(asBoolean(payload.get("agreeSodepa"), true))
                .dateAgrement(LocalDate.now())
                .actif(asBoolean(payload.get("actif"), true))
                .build();
        FournisseurEntity saved = fournisseurRepository.save(entity);
        log.info("Fournisseur matérialisé en base PostgreSQL à l'approbation finale: {} ({})", saved.getRaisonSociale(), saved.getCode());
        return saved;
    }

    private FournisseurEntity miseAJourFournisseur(FournisseurEntity entity, Map<String, Object> payload) {
        if (payload.containsKey("raisonSociale") && asString(payload.get("raisonSociale")) != null) {
            entity.setRaisonSociale(asString(payload.get("raisonSociale")));
        }
        if (payload.containsKey("numeroContribuable")) {
            entity.setNumeroContribuable(asString(payload.get("numeroContribuable")));
        }
        if (payload.containsKey("rccm")) {
            entity.setRccm(asString(payload.get("rccm")));
        }
        if (payload.containsKey("regimeFiscal") && asString(payload.get("regimeFiscal")) != null) {
            entity.setRegimeFiscal(asString(payload.get("regimeFiscal")));
        }
        if (payload.containsKey("specialite")) {
            entity.setSpecialite(asString(payload.get("specialite")));
        }
        if (payload.containsKey("adresse")) {
            entity.setAdresse(asString(payload.get("adresse")));
        }
        if (payload.containsKey("telephone")) {
            entity.setTelephone(asString(payload.get("telephone")));
        }
        if (payload.containsKey("email")) {
            entity.setEmail(asString(payload.get("email")));
        }
        if (payload.containsKey("banqueNom")) {
            entity.setBanqueNom(asString(payload.get("banqueNom")));
        }
        if (payload.containsKey("compteBancaireRib")) {
            entity.setCompteBancaireRib(asString(payload.get("compteBancaireRib")));
        }
        if (payload.containsKey("agreeSodepa") && payload.get("agreeSodepa") != null) {
            entity.setAgreeSodepa(asBoolean(payload.get("agreeSodepa"), entity.isAgreeSodepa()));
        }
        if (payload.containsKey("actif") && payload.get("actif") != null) {
            entity.setActif(asBoolean(payload.get("actif"), entity.isActif()));
        }
        FournisseurEntity saved = fournisseurRepository.save(entity);
        log.info("Fournisseur {} mis à jour en base PostgreSQL à l'approbation finale.", saved.getCode());
        return saved;
    }

    private void enrichirFournisseurAvecWorkflow(FournisseurResponse res, WorkflowInstanceEntity wf) {
        if (res == null || wf == null) {
            return;
        }
        res.setEnCoursDeModification(true);
        res.setWorkflowEnCours(true);
        res.setWorkflowInstanceId(wf.getId());
        res.setWorkflowStatut(wf.getStatut() != null ? wf.getStatut().name() : null);
        WorkflowCurrentDataResponse current = workflowStateService.getCachedCurrentData(wf);
        res.setWorkflowOperationType(current.getOperationType() != null ? current.getOperationType() : "UPDATE");
        if (current.getAttachments() != null && !current.getAttachments().isEmpty()) {
            res.setPiecesJointesWorkflow(current.getAttachments());
        }
    }

    private FournisseurResponse buildDraftFournisseurFromWorkflow(
            UUID frnId,
            WorkflowInstanceEntity wf,
            Map<String, Object> data,
            WorkflowCurrentDataResponse current
    ) {
        return FournisseurResponse.builder()
                .id(frnId)
                .code(asString(data.get("code")))
                .raisonSociale(asString(data.get("raisonSociale")))
                .numeroContribuable(asString(data.get("numeroContribuable")))
                .rccm(asString(data.get("rccm")))
                .regimeFiscal(nonBlankOrDefault(asString(data.get("regimeFiscal")), "REEL"))
                .specialite(asString(data.get("specialite")))
                .adresse(asString(data.get("adresse")))
                .telephone(asString(data.get("telephone")))
                .email(asString(data.get("email")))
                .banqueNom(asString(data.get("banqueNom")))
                .compteBancaireRib(asString(data.get("compteBancaireRib")))
                .agreeSodepa(false)
                .actif(false)
                .enCoursDeModification(true)
                .workflowEnCours(wf != null)
                .workflowInstanceId(wf != null ? wf.getId() : null)
                .workflowStatut(wf != null && wf.getStatut() != null ? wf.getStatut().name() : "EN_COURS")
                .workflowOperationType(current != null && current.getOperationType() != null ? current.getOperationType() : "CREATE")
                .piecesJointesWorkflow(current != null ? current.getAttachments() : new ArrayList<>())
                .build();
    }

    // ==========================================
    // EXPRESSIONS DE BESOINS (EB)
    // ==========================================

    private static final List<String> EB_WORKFLOW_ENTITY_NAMES = List.of(
            "EXPRESSION_BESOIN",
            "EB",
            ExpressionBesoinEntity.class.getSimpleName(),
            ExpressionBesoinEntity.class.getName()
    );

    private static final List<StatutWorkflowInstance> ACTIVE_WORKFLOW_STATUTS = List.of(
            StatutWorkflowInstance.EN_COURS,
            StatutWorkflowInstance.SUSPENDU
    );

    @Transactional
    public List<ExpressionBesoinResponse> listerExpressionsBesoin() {
        securityService.checkPermission(Permissions.LIST_EXPRESSIONS);
        String scope = securityService.getScopeMaterializedPath();
        List<ExpressionBesoinEntity> list;
        if (scope == null) {
            list = expressionBesoinRepository.findAll(Sort.by(Sort.Direction.DESC, "dateCreation"));
        } else {
            list = expressionBesoinRepository.findByMaterializedPathStartingWith(scope);
        }

        Map<UUID, WorkflowInstanceEntity> activeWorkflowsByEbId = findActiveEbWorkflowsById();
        java.util.Set<UUID> materializedIds = new java.util.HashSet<>();
        List<ExpressionBesoinResponse> responses = new ArrayList<>();

        for (ExpressionBesoinEntity entity : list) {
            materializedIds.add(entity.getId());
            ExpressionBesoinResponse res = ExpressionBesoinResponse.fromEntity(entity);
            WorkflowInstanceEntity activeWf = activeWorkflowsByEbId.get(entity.getId());
            if (activeWf != null) {
                enrichirAvecWorkflowActif(res, activeWf);
            }
            responses.add(res);
        }

        // Projets en cours de création (non encore persistés dans achat_expressions_besoin) depuis ClickHouse / Outbox
        for (Map.Entry<UUID, WorkflowInstanceEntity> entry : activeWorkflowsByEbId.entrySet()) {
            UUID ebId = entry.getKey();
            if (materializedIds.contains(ebId)) {
                continue;
            }
            WorkflowInstanceEntity wf = entry.getValue();
            WorkflowCurrentDataResponse current = workflowStateService.getCachedCurrentData(wf);
            Map<String, Object> data = current.getData() != null ? current.getData() : new java.util.LinkedHashMap<>();
            if (!matchesScope(scope, wf, data)) {
                continue;
            }
            responses.add(buildDraftResponseFromWorkflow(ebId, wf, data, current));
        }

        responses.sort(java.util.Comparator.comparing(
                ExpressionBesoinResponse::getDateCreation,
                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));
        return responses;
    }

    @Transactional
    public ExpressionBesoinResponse obtenirExpressionBesoin(UUID id) {
        securityService.checkPermission(Permissions.VIEW_EXPRESSION);
        Optional<ExpressionBesoinEntity> ebOpt = expressionBesoinRepository.findById(id);
        Optional<WorkflowInstanceEntity> activeWfOpt = findActiveEbWorkflow(id);

        if (ebOpt.isPresent()) {
            ExpressionBesoinEntity eb = ebOpt.get();
            securityService.validateAccessToUnite(eb.getUniteOrganisationnelle(),
                    "Accès refusé à cette expression de besoin : hors de votre périmètre hiérarchique.");
            ExpressionBesoinResponse res = ExpressionBesoinResponse.fromEntity(eb);
            activeWfOpt.ifPresent(wf -> enrichirAvecWorkflowActif(res, wf));
            return res;
        }

        if (activeWfOpt.isPresent()) {
            WorkflowInstanceEntity wf = activeWfOpt.get();
            WorkflowCurrentDataResponse current = workflowStateService.getCurrentData(wf);
            Map<String, Object> data = current.getData() != null ? current.getData() : new java.util.LinkedHashMap<>();
            UniteOrganisationnelleEntity unite = resolveUnite(wf, data);
            if (unite != null) {
                securityService.validateAccessToUnite(unite,
                        "Accès refusé à cette expression de besoin : hors de votre périmètre hiérarchique.");
            }
            return buildDraftResponseFromWorkflow(id, wf, data, current);
        }

        throw new IllegalArgumentException(GlobalConstants.NEED_EXPRESSION_NOT_FOUND);
    }

    @Transactional
    public ExpressionBesoinResponse creerExpressionBesoin(CreerExpressionBesoinRequest request) {
        securityService.checkPermission(Permissions.CREATE_EXPRESSION);
        long activeCount = findActiveEbWorkflowsById().size();
        long count = expressionBesoinRepository.count() + activeCount + 1;
        int annee = LocalDate.now().getYear();
        String numero = String.format("EB-%d-%04d", annee, count);
        while (expressionBesoinRepository.findByNumero(numero).isPresent()) {
            count++;
            numero = String.format("EB-%d-%04d", annee, count);
        }
        UUID id = UUID.randomUUID();

        // Résolution de l'unité de rattachement depuis l'affectation active si omise
        UniteOrganisationnelleEntity unite = null;
        if (request.getUniteOrganisationnelleId() != null) {
            unite = uniteOrganisationnelleRepository.findById(request.getUniteOrganisationnelleId()).orElse(null);
        }
        if (unite == null) {
            unite = securityService.getCurrentUserUnite().orElse(null);
        }
        if (unite != null) {
            securityService.validateAccessToUnite(unite, Permissions.CREATE_EXPRESSION.name());
        }

        String structureNom = request.getStructureNom();
        if (structureNom == null && unite != null) {
            structureNom = unite.getIntitule();
        }

        UserData ud = securityService.getCurrentUserData();
        String demandeurNom = request.getDemandeurNom();
        if (demandeurNom == null) {
            demandeurNom = ud.name();
        }

        Map<String, Object> payload = new java.util.LinkedHashMap<>(objectMapper.convertValue(request, Map.class));
        payload.put("id", id.toString());
        payload.put("numero", numero);
        payload.put("structureNom", structureNom);
        payload.put("demandeurNom", demandeurNom);
        if (ud.userId() != null && payload.get("demandeurId") == null) {
            employeRepository.findByUtilisateurId(UUID.fromString(ud.userId()))
                    .ifPresent(emp -> payload.put("demandeurId", emp.getId().toString()));
        }
        if (unite != null) {
            payload.put("uniteOrganisationnelleId", unite.getId().toString());
        }

        // Soumission sans aucune écriture ni auto-validation anticipée dans achat_expressions_besoin :
        // l'état initial v1 est stocké de façon synchrone dans ClickHouse (ou Outbox PostgreSQL).
        UUID requestId = workflowService.submitChange(
                MakerCheckerEntityName.EXPRESSION_BESOIN,
                id.toString(),
                payload,
                MakerCheckerOperationType.CREATE
        );
        log.info("Expression de besoin soumise au workflow (ClickHouse State Store): {} (Ticket: {})", numero, requestId);

        WorkflowInstanceEntity activeWf = findActiveEbWorkflow(id).orElse(null);
        WorkflowCurrentDataResponse current = activeWf != null ? workflowStateService.getCurrentData(activeWf) : null;
        return buildDraftResponseFromWorkflow(id, activeWf, payload, current);
    }

    @Transactional
    public ExpressionBesoinResponse modifierExpressionBesoin(UUID id, CreerExpressionBesoinRequest request) {
        securityService.checkPermission(Permissions.UPDATE_EXPRESSION);
        ExpressionBesoinEntity existing = getEbOrThrow(id);
        securityService.validateAccessToUnite(existing.getUniteOrganisationnelle(),
                "Accès refusé pour modifier cette expression de besoin.");

        if (findActiveEbWorkflow(id).isPresent()) {
            throw new ConflitMetierException("Une demande de modification est déjà en cours de validation pour cette expression de besoin.");
        }

        UniteOrganisationnelleEntity targetUnite = existing.getUniteOrganisationnelle();
        if (request.getUniteOrganisationnelleId() != null) {
            targetUnite = uniteOrganisationnelleRepository.findById(request.getUniteOrganisationnelleId()).orElse(targetUnite);
            if (targetUnite != null) {
                securityService.validateAccessToUnite(targetUnite, Permissions.UPDATE_EXPRESSION.name());
            }
        }

        Map<String, Object> payload = new java.util.LinkedHashMap<>(objectMapper.convertValue(request, Map.class));
        payload.put("id", existing.getId().toString());
        payload.put("numero", existing.getNumero());
        if (payload.get("demandeurNom") == null) {
            payload.put("demandeurNom", existing.getDemandeurNom());
        }
        if (payload.get("structureNom") == null) {
            payload.put("structureNom", targetUnite != null ? targetUnite.getIntitule() : existing.getStructureNom());
        }
        if (targetUnite != null) {
            payload.put("uniteOrganisationnelleId", targetUnite.getId().toString());
        }

        // L'entité existante en PostgreSQL reste intacte tant que le workflow UPDATE n'est pas approuvé jusqu'au bout.
        UUID requestId = workflowService.submitChange(
                MakerCheckerEntityName.EXPRESSION_BESOIN,
                existing.getId().toString(),
                payload,
                MakerCheckerOperationType.UPDATE
        );
        log.info("Modification de l'EB {} soumise au workflow (Ticket: {})", existing.getNumero(), requestId);

        ExpressionBesoinResponse response = ExpressionBesoinResponse.fromEntity(existing);
        findActiveEbWorkflow(existing.getId()).ifPresent(wf -> enrichirAvecWorkflowActif(response, wf));
        return response;
    }

    @Transactional
    public void validateOrRejectExpressionBesoin(ValidateOrRejectSubmissionInput input) {
        securityService.checkPermission(Permissions.VALIDATE_EXPRESSION);
        securityService.validateAccessToUnite(input.id(), MakerCheckerEntityName.EXPRESSION_BESOIN,
                "Accès refusé pour validation/rejet de l'EB : hors de votre périmètre hiérarchique.");
        MakerCheckerOutput checkerOutput = workflowService.findById(input.id());
        MakerCheckerStatus currentStatus = checkerOutput.status();
        if (currentStatus != MakerCheckerStatus.PENDING) {
            throw new RuntimeException(String.format(
                    "Invalid transition: Cannot transition from %s to %s.", currentStatus, input.decision()));
        }

        UUID checkerId = UUID.fromString(securityService.getUtilsService().getCurrentUserData().userId());
        if (checkerOutput.maker() != null && checkerOutput.maker().id() != null && checkerOutput.maker().id().equals(checkerId)) {
            throw new RuntimeException("A checker cannot vote on their own request.");
        }

        MakerCheckerStatus newStatus;
        if (MakerCheckerStatus.ACCEPTED.equals(input.decision())) {
            newStatus = MakerCheckerStatus.ACCEPTED;
        } else if (MakerCheckerStatus.REJECTED.equals(input.decision())) {
            newStatus = MakerCheckerStatus.REJECTED;
        } else {
            throw new RuntimeException("Invalid decision: " + input.decision());
        }

        workflowService.validate(checkerOutput.id(), checkerOutput.createdAt(), checkerOutput.expiredAt());

        if (MakerCheckerStatus.REJECTED.equals(newStatus)) {
            // Rejet : aucune création ni altération dans achat_expressions_besoin PostgreSQL
            workflowService.update(checkerOutput.id(), newStatus, input.notes(), checkerId.toString());
            log.info("Expression de besoin rejetée par le Checker {} (zéro persistance métier Postgres). Ticket: {}",
                    checkerId, checkerOutput.id());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID ebId = UUID.fromString(checkerOutput.entityPk());
            Map<String, Object> payload = checkerOutput.payload();
            materialiserExpressionBesoinFinale(ebId, payload);
            workflowService.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
            log.info("Expression de besoin approuvée et matérialisée dans PostgreSQL. Ticket: {}, PK: {}", checkerOutput.id(), ebId);
        }
    }

    @Transactional
    public void finaliserExpressionBesoinDepuisWorkflow(UUID ebId, Map<String, Object> finalData) {
        if (ebId == null || finalData == null || finalData.isEmpty()) {
            return;
        }
        materialiserExpressionBesoinFinale(ebId, finalData);
    }

    private void materialiserExpressionBesoinFinale(UUID ebId, Map<String, Object> payload) {
        Map<String, Object> safePayload = payload != null ? payload : new java.util.LinkedHashMap<>();
        Optional<ExpressionBesoinEntity> existingOpt = expressionBesoinRepository.findById(ebId);
        if (existingOpt.isPresent()) {
            miseAJourExpressionBesoin(existingOpt.get(), safePayload, StatutExpressionBesoin.APPROUVEE_DG);
        } else {
            creationExpressionBesoin(safePayload, ebId, StatutExpressionBesoin.APPROUVEE_DG);
        }
    }

    private ExpressionBesoinEntity creationExpressionBesoin(
            Map<String, Object> payload, UUID ebId, StatutExpressionBesoin initialStatut) {
        UniteOrganisationnelleEntity unite = resolveUniteFromPayload(payload);

        String numero = asString(payload.get("numero"));
        if (numero == null || numero.isBlank() || expressionBesoinRepository.findByNumero(numero).isPresent()) {
            int annee = LocalDate.now().getYear();
            long seq = CodeSequenceService.nextOrCount(codeSequenceService, "seq_code_expression_besoin", expressionBesoinRepository::count);
            numero = String.format("EB-%d-%04d", annee, seq);
            while (expressionBesoinRepository.findByNumero(numero).isPresent()) {
                seq++;
                numero = String.format("EB-%d-%04d", annee, seq);
            }
        }

        ExpressionBesoinEntity eb = ExpressionBesoinEntity.builder()
                .id(ebId)
                .numero(numero)
                .demandeurNom(nonBlankOrDefault(asString(payload.get("demandeurNom")), "SODEPA"))
                .structureNom(asString(payload.get("structureNom")))
                .motif(nonBlankOrDefault(asString(payload.get("motif")), "Expression de besoin validée par workflow"))
                .dateExpression(LocalDate.now())
                .stockDisponible(asBoolean(payload.get("stockDisponible"), false))
                .attestationCarenceEtablie(asBoolean(payload.get("attestationCarenceEtablie"), true))
                .budgetDisponible(asBoolean(payload.get("budgetDisponible"), true))
                .ligneBudgetaireImputation(asString(payload.get("ligneBudgetaireImputation")))
                .statut(initialStatut)
                .dateCreation(Instant.now())
                .lignes(new ArrayList<>())
                .uniteOrganisationnelle(unite)
                .build();

        applyEmployeAndLignes(eb, payload, true);
        ExpressionBesoinEntity saved = expressionBesoinRepository.save(eb);
        log.info("Expression de besoin créée en base PostgreSQL à l'approbation finale: {} (montant={})",
                saved.getNumero(), saved.getMontantEstimeTotal());
        return saved;
    }

    private ExpressionBesoinEntity miseAJourExpressionBesoin(
            ExpressionBesoinEntity eb, Map<String, Object> payload, StatutExpressionBesoin finalStatut) {
        if (payload.containsKey("motif") && asString(payload.get("motif")) != null) {
            eb.setMotif(asString(payload.get("motif")));
        }
        if (payload.containsKey("demandeurNom") && asString(payload.get("demandeurNom")) != null) {
            eb.setDemandeurNom(asString(payload.get("demandeurNom")));
        }
        if (payload.containsKey("structureNom") && asString(payload.get("structureNom")) != null) {
            eb.setStructureNom(asString(payload.get("structureNom")));
        }
        if (payload.containsKey("ligneBudgetaireImputation")) {
            eb.setLigneBudgetaireImputation(asString(payload.get("ligneBudgetaireImputation")));
        }
        if (payload.containsKey("stockDisponible") && payload.get("stockDisponible") != null) {
            eb.setStockDisponible(asBoolean(payload.get("stockDisponible"), eb.isStockDisponible()));
        }
        if (payload.containsKey("attestationCarenceEtablie") && payload.get("attestationCarenceEtablie") != null) {
            eb.setAttestationCarenceEtablie(asBoolean(payload.get("attestationCarenceEtablie"), eb.isAttestationCarenceEtablie()));
        }
        if (payload.containsKey("budgetDisponible") && payload.get("budgetDisponible") != null) {
            eb.setBudgetDisponible(asBoolean(payload.get("budgetDisponible"), eb.isBudgetDisponible()));
        }
        if (payload.containsKey("uniteOrganisationnelleId") && payload.get("uniteOrganisationnelleId") != null) {
            UniteOrganisationnelleEntity unite = resolveUniteFromPayload(payload);
            if (unite != null) {
                eb.setUniteOrganisationnelle(unite);
            }
        }
        applyEmployeAndLignes(eb, payload, payload.containsKey("lignes"));
        eb.setStatut(finalStatut);

        ExpressionBesoinEntity saved = expressionBesoinRepository.save(eb);
        log.info("Expression de besoin {} mise à jour en base PostgreSQL à l'approbation finale (montant={})",
                saved.getNumero(), saved.getMontantEstimeTotal());
        return saved;
    }

    private void applyEmployeAndLignes(ExpressionBesoinEntity eb, Map<String, Object> payload, boolean applyLignes) {
        Object demandeurIdObj = payload.get("demandeurId");
        if (demandeurIdObj != null) {
            try {
                employeRepository.findById(UUID.fromString(demandeurIdObj.toString())).ifPresent(eb::setDemandeur);
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (!applyLignes) {
            return;
        }
        Object lignesObj = payload.get("lignes");
        if (lignesObj instanceof List<?> rawList) {
            if (eb.getLignes() == null) {
                eb.setLignes(new ArrayList<>());
            } else {
                eb.getLignes().clear();
            }
            BigDecimal totalEstime = BigDecimal.ZERO;
            int ordre = 1;
            for (Object item : rawList) {
                CreerExpressionBesoinRequest.LigneExpressionBesoinRequest l = toLigneRequest(item);
                if (l == null || l.getQuantite() == null) {
                    continue;
                }
                BigDecimal pu = l.getPrixUnitaireEstime() != null ? l.getPrixUnitaireEstime() : BigDecimal.ZERO;
                BigDecimal totalLigne = pu.multiply(l.getQuantite()).setScale(2, RoundingMode.HALF_UP);
                totalEstime = totalEstime.add(totalLigne);

                ExpressionBesoinLigneEntity ligneEntity = ExpressionBesoinLigneEntity.builder()
                        .id(UUID.randomUUID())
                        .expressionBesoin(eb)
                        .designation(l.getDesignation())
                        .specificationsTechniques(l.getSpecificationsTechniques())
                        .quantite(l.getQuantite())
                        .uniteMesure(l.getUniteMesure() != null ? l.getUniteMesure() : "U")
                        .prixUnitaireEstime(pu)
                        .montantTotalEstime(totalLigne)
                        .ordre(ordre++)
                        .build();
                eb.getLignes().add(ligneEntity);
            }
            eb.setMontantEstimeTotal(totalEstime);
        }
    }

    private CreerExpressionBesoinRequest.LigneExpressionBesoinRequest toLigneRequest(Object item) {
        if (item == null) {
            return null;
        }
        if (item instanceof CreerExpressionBesoinRequest.LigneExpressionBesoinRequest req) {
            return req;
        }
        if (item instanceof Map<?, ?> map) {
            return CreerExpressionBesoinRequest.LigneExpressionBesoinRequest.builder()
                    .designation(asString(map.get("designation")))
                    .specificationsTechniques(asString(map.get("specificationsTechniques")))
                    .quantite(asBigDecimal(map.get("quantite")))
                    .uniteMesure(asString(map.get("uniteMesure")))
                    .prixUnitaireEstime(asBigDecimal(map.get("prixUnitaireEstime")))
                    .build();
        }
        return objectMapper.convertValue(item, CreerExpressionBesoinRequest.LigneExpressionBesoinRequest.class);
    }

    private Map<UUID, WorkflowInstanceEntity> findActiveEbWorkflowsById() {
        List<WorkflowInstanceEntity> active = workflowInstanceRepository
                .findByEntityNameInAndStatutIn(EB_WORKFLOW_ENTITY_NAMES, ACTIVE_WORKFLOW_STATUTS);
        Map<UUID, WorkflowInstanceEntity> map = new java.util.LinkedHashMap<>();
        for (WorkflowInstanceEntity wf : active) {
            UUID ebId = parseUuidSafe(wf.getEntityId());
            if (ebId == null) {
                continue;
            }
            WorkflowInstanceEntity existing = map.get(ebId);
            if (existing == null || (wf.getDateInitiation() != null && existing.getDateInitiation() != null
                    && wf.getDateInitiation().isAfter(existing.getDateInitiation()))) {
                map.put(ebId, wf);
            }
        }
        return map;
    }

    private Optional<WorkflowInstanceEntity> findActiveEbWorkflow(UUID ebId) {
        if (ebId == null) {
            return Optional.empty();
        }
        return workflowInstanceRepository.findFirstByEntityNameInAndEntityIdAndStatutInOrderByDateInitiationDesc(
                EB_WORKFLOW_ENTITY_NAMES, ebId.toString(), ACTIVE_WORKFLOW_STATUTS);
    }

    private void enrichirAvecWorkflowActif(ExpressionBesoinResponse res, WorkflowInstanceEntity wf) {
        if (res == null || wf == null) {
            return;
        }
        res.setEnCoursDeModification(true);
        res.setWorkflowEnCours(true);
        res.setWorkflowInstanceId(wf.getId());
        res.setWorkflowStatut(wf.getStatut() != null ? wf.getStatut().name() : null);
        WorkflowCurrentDataResponse current = workflowStateService.getCachedCurrentData(wf);
        res.setWorkflowOperationType(current.getOperationType() != null ? current.getOperationType() : "UPDATE");
        if (current.getAttachments() != null && !current.getAttachments().isEmpty()) {
            res.setPiecesJointesWorkflow(current.getAttachments());
        }
    }

    private ExpressionBesoinResponse buildDraftResponseFromWorkflow(
            UUID ebId,
            WorkflowInstanceEntity wf,
            Map<String, Object> data,
            WorkflowCurrentDataResponse current
    ) {
        UniteOrganisationnelleEntity unite = resolveUnite(wf, data);
        List<ExpressionBesoinResponse.LigneExpressionBesoinResponse> lignesResp = new ArrayList<>();
        BigDecimal totalEstime = BigDecimal.ZERO;
        Object lignesObj = data.get("lignes");
        if (lignesObj instanceof List<?> rawList) {
            int ordre = 1;
            for (Object item : rawList) {
                CreerExpressionBesoinRequest.LigneExpressionBesoinRequest l = toLigneRequest(item);
                if (l == null || l.getQuantite() == null) {
                    continue;
                }
                BigDecimal pu = l.getPrixUnitaireEstime() != null ? l.getPrixUnitaireEstime() : BigDecimal.ZERO;
                BigDecimal totalLigne = pu.multiply(l.getQuantite()).setScale(2, RoundingMode.HALF_UP);
                totalEstime = totalEstime.add(totalLigne);
                lignesResp.add(ExpressionBesoinResponse.LigneExpressionBesoinResponse.builder()
                        .designation(l.getDesignation())
                        .specificationsTechniques(l.getSpecificationsTechniques())
                        .quantite(l.getQuantite())
                        .uniteMesure(l.getUniteMesure() != null ? l.getUniteMesure() : "U")
                        .prixUnitaireEstime(pu)
                        .montantTotalEstime(totalLigne)
                        .ordre(ordre++)
                        .build());
            }
        }

        UUID demandeurId = parseUuidSafe(asString(data.get("demandeurId")));
        if (demandeurId == null && wf != null && wf.getInitiateurEmploye() != null) {
            demandeurId = wf.getInitiateurEmploye().getId();
        }
        String demandeurNom = asString(data.get("demandeurNom"));
        if (demandeurNom == null && wf != null && wf.getInitiateurEmploye() != null) {
            demandeurNom = wf.getInitiateurEmploye().getPrenom() + " " + wf.getInitiateurEmploye().getNom();
        }
        String structureNom = asString(data.get("structureNom"));
        if (structureNom == null && unite != null) {
            structureNom = unite.getIntitule();
        }

        return ExpressionBesoinResponse.builder()
                .id(ebId)
                .numero(asString(data.get("numero")))
                .demandeurId(demandeurId)
                .demandeurNom(demandeurNom)
                .uniteOrganisationnelleId(unite != null ? unite.getId() : parseUuidSafe(asString(data.get("uniteOrganisationnelleId"))))
                .structureNom(structureNom)
                .motif(asString(data.get("motif")))
                .dateExpression(LocalDate.now())
                .montantEstimeTotal(totalEstime)
                .stockDisponible(asBoolean(data.get("stockDisponible"), false))
                .attestationCarenceEtablie(asBoolean(data.get("attestationCarenceEtablie"), true))
                .budgetDisponible(asBoolean(data.get("budgetDisponible"), true))
                .ligneBudgetaireImputation(asString(data.get("ligneBudgetaireImputation")))
                .statut(StatutExpressionBesoin.BROUILLON)
                .dateCreation(wf != null && wf.getDateInitiation() != null ? wf.getDateInitiation() : Instant.now())
                .enCoursDeModification(true)
                .workflowEnCours(wf != null)
                .workflowInstanceId(wf != null ? wf.getId() : null)
                .workflowStatut(wf != null && wf.getStatut() != null ? wf.getStatut().name() : "EN_COURS")
                .workflowOperationType(current != null && current.getOperationType() != null ? current.getOperationType() : "CREATE")
                .piecesJointesWorkflow(current != null ? current.getAttachments() : new ArrayList<>())
                .lignes(lignesResp)
                .build();
    }

    private UniteOrganisationnelleEntity resolveUniteFromPayload(Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }
        UUID uId = parseUuidSafe(asString(payload.get("uniteOrganisationnelleId")));
        return uId != null ? uniteOrganisationnelleRepository.findById(uId).orElse(null) : null;
    }

    private UniteOrganisationnelleEntity resolveUnite(WorkflowInstanceEntity wf, Map<String, Object> data) {
        UniteOrganisationnelleEntity unite = resolveUniteFromPayload(data);
        if (unite == null && wf != null) {
            unite = wf.getUniteOrganisationnelle();
        }
        return unite;
    }

    private boolean matchesScope(String scope, WorkflowInstanceEntity wf, Map<String, Object> data) {
        if (scope == null) {
            return true;
        }
        UniteOrganisationnelleEntity unite = resolveUnite(wf, data);
        if (unite == null || unite.getMaterializedPath() == null) {
            return false;
        }
        return unite.getMaterializedPath().startsWith(scope);
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

    private String asString(Object val) {
        return val != null ? val.toString() : null;
    }

    private String nonBlankOrDefault(String val, String fallback) {
        return (val != null && !val.isBlank()) ? val : fallback;
    }

    private boolean asBoolean(Object val, boolean fallback) {
        if (val == null) {
            return fallback;
        }
        if (val instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(val.toString());
    }

    private BigDecimal asBigDecimal(Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof BigDecimal bd) {
            return bd;
        }
        if (val instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        try {
            return new BigDecimal(val.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ExpressionBesoinEntity getEbOrThrow(UUID id) {
        return expressionBesoinRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(GlobalConstants.NEED_EXPRESSION_NOT_FOUND));
    }

    // ==========================================
    // DOSSIERS D'ACHAT / MARCHÉS / BONS DE COMMANDE
    // ==========================================

    @Transactional(readOnly = true)
    public List<MarcheDossierResponse> listerDossiers() {
        securityService.checkPermission(Permissions.LIST_FOLDERS);
        String scope = securityService.getScopeMaterializedPath();
        List<MarcheDossierEntity> list;
        if (scope == null) {
            list = marcheDossierRepository.findAll(Sort.by(Sort.Direction.DESC, "dateCreation"));
        } else {
            list = marcheDossierRepository.findByExpressionBesoinMaterializedPathStartingWith(scope);
        }
        return list.stream()
                .map(MarcheDossierResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MarcheDossierResponse obtenirDossier(UUID id) {
        securityService.checkPermission(Permissions.VIEW_FOLDER);
        MarcheDossierEntity dossier = getDossierOrThrow(id);
        if (dossier.getExpressionBesoin() != null) {
            securityService.validateAccessToUnite(dossier.getExpressionBesoin().getUniteOrganisationnelle(),
                    "Accès refusé à ce dossier de commande : hors de votre périmètre hiérarchique.");
        }
        return MarcheDossierResponse.fromEntity(dossier);
    }

    @Transactional
    public MarcheDossierResponse creerDossier(CreerMarcheDossierRequest request) {
        securityService.checkPermission(Permissions.CREATE_FOLDER);
        FournisseurEntity frn = fournisseurRepository.findById(request.getFournisseurId())
                .orElseThrow(() -> new IllegalArgumentException(GlobalConstants.SUPPLIER_NOT_FOUND));

        ExpressionBesoinEntity eb = null;
        if (request.getExpressionBesoinId() != null) {
            eb = getEbOrThrow(request.getExpressionBesoinId());
            securityService.validateAccessToUnite(eb.getUniteOrganisationnelle(),
                    "Accès refusé pour la création du dossier : l'expression de besoin est hors périmètre.");
        }

        BigDecimal totalHt = BigDecimal.ZERO;
        BigDecimal totalTva = BigDecimal.ZERO;

        List<Map<String, Object>> lignesPayload = new ArrayList<>();
        int ordre = 1;
        for (CreerMarcheDossierRequest.LigneMarcheDossierRequest l : request.getLignes()) {
            BigDecimal pu = l.getPrixUnitaireHt();
            BigDecimal htLigne = pu.multiply(l.getQuantite()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal tauxDefaut = configService.getDecimal(GlobalConstants.ACHATS_TAUX_TVA, BigDecimal.valueOf(19.25));
            BigDecimal taux = l.getTauxTva() != null ? l.getTauxTva() : tauxDefaut;
            BigDecimal tvaLigne = htLigne.multiply(taux).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal ttcLigne = htLigne.add(tvaLigne);

            totalHt = totalHt.add(htLigne);
            totalTva = totalTva.add(tvaLigne);

            Map<String, Object> ligneMap = Map.of(
                    "designation", l.getDesignation(),
                    "quantite", l.getQuantite(),
                    "uniteMesure", l.getUniteMesure() != null ? l.getUniteMesure() : "U",
                    "prixUnitaireHt", pu,
                    "montantHt", htLigne,
                    "tauxTva", taux,
                    "montantTtc", ttcLigne,
                    "ordre", ordre++
            );
            lignesPayload.add(ligneMap);
        }

        BigDecimal totalTtc = totalHt.add(totalTva);

        // Détermination automatique de la procédure selon les seuils SODEPA
        TypeProcedureAchat procedure = determinerProcedure(totalTtc);
        boolean cipmRequis = (procedure == TypeProcedureAchat.MARCHE_PUBLIC_CIPM || procedure == TypeProcedureAchat.LETTRE_COMMANDE);

        int annee = LocalDate.now().getYear();
        long count = CodeSequenceService.nextOrCount(codeSequenceService, "seq_code_marche_dossier", marcheDossierRepository::count);
        String numeroDossier = String.format("%s-%d-%04d", procedure.getPrefix(), annee, count);
        UUID id = UUID.randomUUID();

        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("id", id.toString());
        payload.put("numeroDossier", numeroDossier);
        payload.put("typeProcedure", procedure.name());
        payload.put("objet", request.getObjet());
        payload.put("fournisseurId", frn.getId().toString());
        payload.put("fournisseurNom", frn.getRaisonSociale());
        payload.put("montantHt", totalHt);
        payload.put("montantTva", totalTva);
        payload.put("montantTtc", totalTtc);
        payload.put("delaiLivraisonJours", request.getDelaiLivraisonJours() != null ? request.getDelaiLivraisonJours() : 90);
        payload.put("examenCipmRequis", cipmRequis);
        payload.put("lignes", lignesPayload);
        if (eb != null) {
            payload.put("expressionBesoinId", eb.getId().toString());
            payload.put("ligneBudgetaireCode", request.getLigneBudgetaireCode() != null ? request.getLigneBudgetaireCode() : eb.getLigneBudgetaireImputation());
        } else if (request.getLigneBudgetaireCode() != null) {
            payload.put("ligneBudgetaireCode", request.getLigneBudgetaireCode());
        }

        UUID uuid = workflowService.submitChange(
                MakerCheckerEntityName.MARCHE_DOSSIER,
                id.toString(),
                payload,
                MakerCheckerOperationType.CREATE
        );
        log.info("Dossier de commande soumis au Maker-Checker: {} (Ticket: {})", numeroDossier, uuid);

        final BigDecimal finalTotalHt = totalHt;
        final BigDecimal finalTotalTva = totalTva;

        return marcheDossierRepository.findById(id)
                .map(MarcheDossierResponse::fromEntity)
                .orElseGet(() -> MarcheDossierResponse.builder()
                        .id(id)
                        .numeroDossier(numeroDossier)
                        .typeProcedure(procedure)
                        .objet(request.getObjet())
                        .fournisseurNom(frn.getRaisonSociale())
                        .montantHt(finalTotalHt)
                        .montantTva(finalTotalTva)
                        .montantTtc(totalTtc)
                        .statut(StatutMarcheDossier.BROUILLON)
                        .build());
    }

    @Transactional
    public void validateOrRejectMarcheDossier(ValidateOrRejectSubmissionInput input) {
        securityService.checkPermission(Permissions.VALIDATE_FOLDER);
        securityService.validateAccessToUnite(input.id(), MakerCheckerEntityName.MARCHE_DOSSIER,
                "Accès refusé pour validation/rejet du dossier : hors de votre périmètre hiérarchique.");
        MakerCheckerOutput checkerOutput = workflowService.findById(input.id());
        MakerCheckerStatus currentStatus = checkerOutput.status();
        if (currentStatus != MakerCheckerStatus.PENDING) {
            throw new RuntimeException(String.format(
                    "Invalid transition: Cannot transition from %s to %s.", currentStatus, input.decision()));
        }

        UUID checkerId = UUID.fromString(securityService.getUtilsService().getCurrentUserData().userId());
        if (checkerOutput.maker() != null && checkerOutput.maker().id() != null && checkerOutput.maker().id().equals(checkerId)) {
            throw new RuntimeException("A checker cannot vote on their own request.");
        }

        MakerCheckerStatus newStatus;
        if (MakerCheckerStatus.ACCEPTED.equals(input.decision())) {
            newStatus = MakerCheckerStatus.ACCEPTED;
        } else if (MakerCheckerStatus.REJECTED.equals(input.decision())) {
            newStatus = MakerCheckerStatus.REJECTED;
        } else {
            throw new RuntimeException("Invalid decision: " + input.decision());
        }

        workflowService.validate(checkerOutput.id(), checkerOutput.createdAt(), checkerOutput.expiredAt());

        if (MakerCheckerStatus.REJECTED.equals(newStatus)) {
            workflowService.update(checkerOutput.id(), newStatus, input.notes(), checkerId.toString());
            log.info("Dossier de marché rejeté par le Checker {}. Ticket: {}", checkerId, checkerOutput.id());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID dossierId = UUID.fromString(checkerOutput.entityPk());
            Map<String, Object> payload = checkerOutput.payload();
            if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                creationMarcheDossier(payload, dossierId);
            }
            workflowService.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
            log.info("Dossier de marché approuvé par le Checker {}. Ticket: {}, PK: {}", checkerId, checkerOutput.id(), dossierId);
        }
    }

    private MarcheDossierEntity creationMarcheDossier(Map<String, Object> payload, UUID dossierId) {
        FournisseurEntity frn = null;
        Object frnIdObj = payload.get("fournisseurId");
        if (frnIdObj != null) {
            frn = fournisseurRepository.findById(UUID.fromString(frnIdObj.toString())).orElse(null);
        }

        ExpressionBesoinEntity eb = null;
        Object ebIdObj = payload.get("expressionBesoinId");
        if (ebIdObj != null) {
            eb = expressionBesoinRepository.findById(UUID.fromString(ebIdObj.toString())).orElse(null);
        }

        TypeProcedureAchat procedure = TypeProcedureAchat.valueOf((String) payload.get("typeProcedure"));
        BigDecimal montantHt = new BigDecimal(payload.get("montantHt").toString());
        BigDecimal montantTva = new BigDecimal(payload.get("montantTva").toString());
        BigDecimal montantTtc = new BigDecimal(payload.get("montantTtc").toString());
        Integer delai = payload.get("delaiLivraisonJours") != null ? Integer.valueOf(payload.get("delaiLivraisonJours").toString()) : 90;
        Boolean cipmRequis = Boolean.valueOf(payload.get("examenCipmRequis").toString());

        MarcheDossierEntity dossier = MarcheDossierEntity.builder()
                .id(dossierId)
                .numeroDossier((String) payload.get("numeroDossier"))
                .typeProcedure(procedure)
                .objet((String) payload.get("objet"))
                .expressionBesoin(eb)
                .fournisseur(frn)
                .fournisseurNom((String) payload.get("fournisseurNom"))
                .montantHt(montantHt)
                .montantTva(montantTva)
                .montantTtc(montantTtc)
                .ligneBudgetaireCode((String) payload.get("ligneBudgetaireCode"))
                .delaiLivraisonJours(delai)
                .statut(StatutMarcheDossier.BROUILLON)
                .proformasObtenuesCount(0)
                .examenCipmRequis(cipmRequis)
                .dateCreation(Instant.now())
                .lignes(new ArrayList<>())
                .procesVerbaux(new ArrayList<>())
                .build();

        Object lignesObj = payload.get("lignes");
        if (lignesObj instanceof List<?> rawList) {
            for (Object item : rawList) {
                Map<String, Object> l = (Map<String, Object>) item;
                MarcheDossierLigneEntity ligneEntity = MarcheDossierLigneEntity.builder()
                        .id(UUID.randomUUID())
                        .dossier(dossier)
                        .designation((String) l.get("designation"))
                        .quantite(new BigDecimal(l.get("quantite").toString()))
                        .uniteMesure((String) l.get("uniteMesure"))
                        .prixUnitaireHt(new BigDecimal(l.get("prixUnitaireHt").toString()))
                        .montantHt(new BigDecimal(l.get("montantHt").toString()))
                        .tauxTva(new BigDecimal(l.get("tauxTva").toString()))
                        .montantTtc(new BigDecimal(l.get("montantTtc").toString()))
                        .ordre(Integer.parseInt(l.get("ordre").toString()))
                        .build();
                dossier.getLignes().add(ligneEntity);
            }
        }

        MarcheDossierEntity saved = marcheDossierRepository.save(dossier);
        log.info("Dossier de marché matérialisé en base: {} ({})", saved.getNumeroDossier(), saved.getTypeProcedure());
        return saved;
    }

    @Transactional
    public MarcheDossierResponse enregistrerProformas(UUID dossierId, int count, MultipartFile file) {
        securityService.checkPermission(Permissions.UPDATE_FOLDER);
        MarcheDossierEntity dossier = getDossierOrThrow(dossierId);
        if (dossier.getExpressionBesoin() != null) {
            securityService.validateAccessToUnite(dossier.getExpressionBesoin().getUniteOrganisationnelle(),
                    "Accès refusé au dossier : hors périmètre hiérarchique.");
        }

        String cle = "";
        if (file != null && !file.isEmpty()) {
            try {
                byte[] bytes = file.getBytes();
                String hashSha256 = calculateSha256(bytes);
                cle = "achats/dossiers/" + dossierId + "/proformas_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
                String bucket = "sodepa-ged";

                try {
                    minioService.uploadFile(file, cle);
                } catch (Exception e) {
                    log.warn("MinIO indisponible pour l'upload du proforma : {}", e.getMessage());
                }

                DocumentGedEntity docGed = DocumentGedEntity.builder()
                        .id(UUID.randomUUID())
                        .nomFichier(file.getOriginalFilename())
                        .tailleOctets(file.getSize())
                        .contentType(file.getContentType() != null ? file.getContentType() : "application/pdf")
                        .hashSha256(hashSha256)
                        .s3Cle(cle)
                        .s3Bucket(bucket)
                        .typeEntite("MARCHE_DOSSIER")
                        .entiteId(dossierId)
                        .description("Factures proforma (au moins " + count + " reçues) pour dossier " + dossier.getNumeroDossier())
                        .uploadPar(securityService.getCurrentUserData().username())
                        .dateUpload(LocalDateTime.now())
                        .build();
                documentGedRepository.save(docGed);
                log.info("Pièce jointe proforma archivée en GED MinIO avec hash SHA-256: {}", hashSha256);
            } catch (Exception e) {
                log.error("Erreur lors de l'archivage GED de la proforma", e);
            }
        }

        Map<String, Object> payload = Map.of(
                "action", "ENREGISTRER_PROFORMAS",
                "dossierId", dossierId.toString(),
                "proformasCount", count,
                "s3Cle", cle
        );

        UUID uuid = workflowService.submitChange(
                MakerCheckerEntityName.MARCHE_DOSSIER,
                dossierId.toString(),
                payload,
                MakerCheckerOperationType.UPDATE
        );
        log.info("Réception des proformas soumise au Maker-Checker pour dossier {} (Ticket: {})", dossier.getNumeroDossier(), uuid);

        return MarcheDossierResponse.fromEntity(marcheDossierRepository.findById(dossierId).orElse(dossier));
    }

    @Transactional
    public MarcheDossierResponse engagerBudget(UUID dossierId, String ligneBudgetaire) {
        securityService.checkPermission(Permissions.ENGAGE_BUDGET);
        MarcheDossierEntity dossier = getDossierOrThrow(dossierId);
        if (dossier.getExpressionBesoin() != null) {
            securityService.validateAccessToUnite(dossier.getExpressionBesoin().getUniteOrganisationnelle(),
                    "Accès refusé pour engagement budgétaire : hors de votre périmètre hiérarchique.");
        }

        Map<String, Object> payload = Map.of(
                "action", "ENGAGER_BUDGET",
                "dossierId", dossierId.toString(),
                "ligneBudgetaireCode", ligneBudgetaire != null ? ligneBudgetaire : ""
        );

        UUID uuid = workflowService.submitChange(
                MakerCheckerEntityName.MARCHE_DOSSIER,
                dossierId.toString(),
                payload,
                MakerCheckerOperationType.UPDATE
        );
        log.info("Engagement budgétaire soumis au Maker-Checker pour dossier {} (Ticket: {})", dossier.getNumeroDossier(), uuid);

        return MarcheDossierResponse.fromEntity(marcheDossierRepository.findById(dossierId).orElse(dossier));
    }

    @Transactional
    public MarcheDossierResponse signerDG(UUID dossierId) {
        securityService.checkPermission(Permissions.VALIDATE_FOLDER);
        MarcheDossierEntity dossier = getDossierOrThrow(dossierId);
        dossier.setStatut(StatutMarcheDossier.SIGNE);
        return MarcheDossierResponse.fromEntity(marcheDossierRepository.save(dossier));
    }

    @Transactional
    public MarcheDossierResponse notifierFournisseur(UUID dossierId, LocalDate dateNotification, Integer delaiJours) {
        securityService.checkPermission(Permissions.NOTIFY_PROVIDER);
        MarcheDossierEntity dossier = getDossierOrThrow(dossierId);
        if (dossier.getExpressionBesoin() != null) {
            securityService.validateAccessToUnite(dossier.getExpressionBesoin().getUniteOrganisationnelle(),
                    "Accès refusé pour notification prestataire : hors de votre structure.");
        }

        LocalDate dateNotif = dateNotification != null ? dateNotification : LocalDate.now();
        int delai = delaiJours != null ? delaiJours : (dossier.getDelaiLivraisonJours() != null ? dossier.getDelaiLivraisonJours() : GlobalConstants.DELAIS_LIVRAISON_PAR_DEFAULT.intValue());

        Map<String, Object> payload = Map.of(
                "action", "NOTIFIER_FOURNISSEUR",
                "dossierId", dossierId.toString(),
                "dateNotification", dateNotif.toString(),
                "delaiJours", delai
        );

        UUID uuid = workflowService.submitChange(
                MakerCheckerEntityName.MARCHE_DOSSIER,
                dossierId.toString(),
                payload,
                MakerCheckerOperationType.UPDATE
        );
        log.info("Notification fournisseur soumise au Maker-Checker pour dossier {} (Ticket: {})", dossier.getNumeroDossier(), uuid);

        return MarcheDossierResponse.fromEntity(marcheDossierRepository.findById(dossierId).orElse(dossier));
    }

    private void updateMarcheDossier(Map<String, Object> payload, UUID dossierId) {
        MarcheDossierEntity dossier = getDossierOrThrow(dossierId);
        String action = (String) payload.get("action");
        if ("ENREGISTRER_PROFORMAS".equals(action)) {
            Object countObj = payload.get("proformasCount");
            if (countObj != null) {
                dossier.setProformasObtenuesCount(Integer.parseInt(countObj.toString()));
            }
            dossier.setStatut(StatutMarcheDossier.SELECTION_PRESTATAIRE);
        } else if ("ENGAGER_BUDGET".equals(action)) {
            Object lb = payload.get("ligneBudgetaireCode");
            if (lb != null && !lb.toString().isBlank()) {
                dossier.setLigneBudgetaireCode(lb.toString());
            }
            dossier.setStatut(StatutMarcheDossier.ENGAGE_BUDGET);
        } else if ("NOTIFIER_FOURNISSEUR".equals(action)) {
            Object dNotif = payload.get("dateNotification");
            if (dNotif != null) {
                dossier.setDateNotification(LocalDate.parse(dNotif.toString()));
            }
            Object delai = payload.get("delaiJours");
            if (delai != null) {
                dossier.setDelaiLivraisonJours(Integer.parseInt(delai.toString()));
            }
            if (dossier.getDateNotification() != null && dossier.getDelaiLivraisonJours() != null) {
                dossier.setDateLimiteLivraison(dossier.getDateNotification().plusDays(dossier.getDelaiLivraisonJours()));
            }
            dossier.setStatut(StatutMarcheDossier.NOTIFIE);
        }
        marcheDossierRepository.save(dossier);
        log.info("Dossier de marché mis à jour en base (Action: {}). PK: {}", action, dossierId);
    }

    private MarcheDossierEntity getDossierOrThrow(UUID id) {
        return marcheDossierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(GlobalConstants.ORDER_OR_CONTRACT_FILE_NOT_FOUND));
    }

    // ==========================================
    // RÉCEPTIONS TECHNIQUES & LIQUIDATION
    // ==========================================

    @Transactional(readOnly = true)
    public List<MarcheReceptionPvResponse> listerReceptions() {
        securityService.checkPermission(Permissions.LIST_RECEPTIONS);
        String scope = securityService.getScopeMaterializedPath();
        List<MarcheReceptionPvEntity> list;
        if (scope == null) {
            list = marcheReceptionPvRepository.findAll(Sort.by(Sort.Direction.DESC, "dateReception"));
        } else {
            list = marcheReceptionPvRepository.findByMaterializedPathStartingWith(scope);
        }
        return list.stream()
                .map(MarcheReceptionPvResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public UUID enregistrerPvReception(EnregistrerPvReceptionRequest request) {
        securityService.checkPermission(Permissions.CREATE_RECEPTION);
        MarcheDossierEntity dossier = getDossierOrThrow(request.getDossierId());
        if (dossier.getExpressionBesoin() != null) {
            securityService.validateAccessToUnite(dossier.getExpressionBesoin().getUniteOrganisationnelle(),
                    Permissions.CREATE_RECEPTION.name());
        }

        long count = CodeSequenceService.nextOrCount(codeSequenceService, "seq_code_reception_pv", marcheReceptionPvRepository::count);
        int annee = LocalDate.now().getYear();
        String numeroPv = String.format(GlobalConstants.PV_REC, annee, count);
        UUID id = UUID.randomUUID();
        String fichierUrl = minioService.uploadFile(request.getFichierPv(), "achats/receptions/" + id + "/pv_reception_" + System.currentTimeMillis() + "_" +numeroPv );
        PvEventInput pvEventInput = PvEventInput.builder()
                .id(id)
                .numeroPv(numeroPv)
                .dossierId(request.getDossierId())
                .dateReception(request.getDateReception())
                .lieuReception(request.getLieuReception())
                .resultatConformite(request.getResultatConformite())
                .observations(request.getObservations())
                .numeroFactureDefinitive(request.getNumeroFactureDefinitive())
                .montantFactureLiquide(request.getMontantFactureLiquide())
                .fichierUrl(fichierUrl)
                .build();
        Map<String, Object> payload = objectMapper.convertValue(pvEventInput, Map.class);
        UUID uuid = workflowService.submitChange(
                MakerCheckerEntityName.MARCHE_RECEPTION_PV,
                id.toString(),
                payload,
                MakerCheckerOperationType.CREATE
        );
        log.info("Procès-verbal de réception soumis au pattern Maker-Checker. PV {} pour le dossier {}", numeroPv, dossier.getNumeroDossier());
        return uuid;
    }

    @Transactional
    public void validateOrRejectPvReception(ValidateOrRejectSubmissionInput input) {
        securityService.checkPermission(Permissions.VALIDATE_RECEPTION);
        securityService.validateAccessToUnite(input.id(), MakerCheckerEntityName.MARCHE_RECEPTION_PV,
                "Accès refusé pour validation/rejet du PV : hors de votre périmètre hiérarchique.");
        MakerCheckerOutput checkerOutput = workflowService.findById(input.id());
        MakerCheckerStatus currentStatus = checkerOutput.status();
        if (currentStatus != MakerCheckerStatus.PENDING) {
            throw new RuntimeException(String.format(
                    "Invalid transition: Cannot transition from %s to %s.", currentStatus, input.decision()));
        }

        UUID checkerId = UUID.fromString(securityService.getUtilsService().getCurrentUserData().userId());

        if (checkerOutput.maker() != null && checkerOutput.maker().id() != null && checkerOutput.maker().id().equals(checkerId)) {
            throw new RuntimeException("A checker cannot vote on their own request.");
        }

        MakerCheckerStatus newStatus;
        if (MakerCheckerStatus.ACCEPTED.equals(input.decision())) {
            newStatus = MakerCheckerStatus.ACCEPTED;
        } else if (MakerCheckerStatus.REJECTED.equals(input.decision())) {
            newStatus = MakerCheckerStatus.REJECTED;
        } else {
            throw new RuntimeException("Invalid decision: " + input.decision());
        }

        workflowService.validate(checkerOutput.id(), checkerOutput.createdAt(), checkerOutput.expiredAt());

        if (MakerCheckerStatus.REJECTED.equals(newStatus)) {
            workflowService.update(checkerOutput.id(), newStatus, input.notes(), checkerId.toString());
            log.info("Procès-verbal de réception rejeté par le Checker {}. Ticket: {}", checkerId, checkerOutput.id());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID pvId = UUID.fromString(checkerOutput.entityPk());
            PvEventInput pvEventInput = objectMapper.convertValue(checkerOutput.payload(), PvEventInput.class);
            if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                creationPvReception(pvEventInput, pvId);
            }
            workflowService.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
            log.info("Procès-verbal de réception approuvé par le Checker {}. Ticket: {}, PV PK: {}", checkerId, checkerOutput.id(), pvId);
        }
    }

    private MarcheReceptionPvEntity creationPvReception(PvEventInput input, UUID pvId) {
        MarcheDossierEntity dossier = getDossierOrThrow(input.dossierId());
        MarcheReceptionPvEntity entity = MarcheReceptionPvEntity.builder()
                .id(pvId)
                .numeroPv(input.numeroPv())
                .dossier(dossier)
                .dateReception(input.dateReception())
                .lieuReception(input.lieuReception())
                .resultatConformite(input.resultatConformite() != null ? input.resultatConformite() : ResultatConformitePv.CONFORME_SANS_RESERVE)
                .observations(input.observations())
                .numeroFactureDefinitive(input.numeroFactureDefinitive())
                .montantFactureLiquide(input.montantFactureLiquide())
                .fichierUrl(input.fichierUrl())
                .liquidationValidee(false)
                .build();
        MarcheReceptionPvEntity saved = marcheReceptionPvRepository.save(entity);

        dossier.setStatut(StatutMarcheDossier.RECEPTIONNE);
        marcheDossierRepository.save(dossier);

        log.info("Procès-verbal de réception matérialisé en base. PK: {}, N°: {}", pvId, input.numeroPv());
        return saved;
    }

    @Transactional
    public MarcheReceptionPvResponse liquiderFacture(UUID receptionPvId, String numeroFacture, BigDecimal montantLiquide) {
        securityService.checkPermission(Permissions.LIQUIDATE_INVOICE);
        MarcheReceptionPvEntity pv = marcheReceptionPvRepository.findById(receptionPvId)
                .orElseThrow(() -> new IllegalArgumentException(GlobalConstants.PV_REC_NOT_FOUND));

        if (pv.getDossier() != null && pv.getDossier().getExpressionBesoin() != null) {
            securityService.validateAccessToUnite(pv.getDossier().getExpressionBesoin().getUniteOrganisationnelle(),
                    "Accès refusé pour liquidation : hors périmètre hiérarchique.");
        }

        Map<String, Object> payload = Map.of(
                "receptionPvId", receptionPvId.toString(),
                "numeroFacture", numeroFacture != null ? numeroFacture : (pv.getNumeroFactureDefinitive() != null ? pv.getNumeroFactureDefinitive() : ""),
                "montantLiquide", montantLiquide != null ? montantLiquide.toString() : (pv.getMontantFactureLiquide() != null ? pv.getMontantFactureLiquide().toString() : "0")
        );

        UUID uuid = workflowService.submitChange(
                MakerCheckerEntityName.LIQUIDATION_FACTURE,
                receptionPvId.toString(),
                payload,
                MakerCheckerOperationType.UPDATE
        );
        log.info("Liquidation de facture soumise au Maker-Checker: PV {} (Ticket: {})", pv.getNumeroPv(), uuid);

        return marcheReceptionPvRepository.findById(receptionPvId)
                .map(MarcheReceptionPvResponse::fromEntity)
                .orElseGet(() -> MarcheReceptionPvResponse.fromEntity(pv));
    }

    @Transactional
    public void validateOrRejectLiquidationFacture(ValidateOrRejectSubmissionInput input) {
        securityService.checkPermission(Permissions.VALIDATE_LIQUIDATION);
        securityService.validateAccessToUnite(input.id(), MakerCheckerEntityName.LIQUIDATION_FACTURE,
                "Accès refusé pour validation/rejet de liquidation : hors de votre périmètre hiérarchique.");
        MakerCheckerOutput checkerOutput = workflowService.findById(input.id());
        MakerCheckerStatus currentStatus = checkerOutput.status();
        if (currentStatus != MakerCheckerStatus.PENDING) {
            throw new RuntimeException(String.format(
                    "Invalid transition: Cannot transition from %s to %s.", currentStatus, input.decision()));
        }

        UUID checkerId = UUID.fromString(securityService.getUtilsService().getCurrentUserData().userId());
        if (checkerOutput.maker() != null && checkerOutput.maker().id() != null && checkerOutput.maker().id().equals(checkerId)) {
            throw new RuntimeException("A checker cannot vote on their own request.");
        }

        MakerCheckerStatus newStatus;
        if (MakerCheckerStatus.ACCEPTED.equals(input.decision())) {
            newStatus = MakerCheckerStatus.ACCEPTED;
        } else if (MakerCheckerStatus.REJECTED.equals(input.decision())) {
            newStatus = MakerCheckerStatus.REJECTED;
        } else {
            throw new RuntimeException("Invalid decision: " + input.decision());
        }

        workflowService.validate(checkerOutput.id(), checkerOutput.createdAt(), checkerOutput.expiredAt());

        if (MakerCheckerStatus.REJECTED.equals(newStatus)) {
            workflowService.update(checkerOutput.id(), newStatus, input.notes(), checkerId.toString());
            log.info("Liquidation de facture rejetée par le Checker {}. Ticket: {}", checkerId, checkerOutput.id());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID pvId = UUID.fromString(checkerOutput.entityPk());
            applyLiquidationFacture(checkerOutput.payload(), pvId);
            workflowService.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
            log.info("Liquidation de facture approuvée par le Checker {}. Ticket: {}, PV PK: {}", checkerId, checkerOutput.id(), pvId);
        }
    }

    private MarcheReceptionPvEntity applyLiquidationFacture(Map<String, Object> payload, UUID pvId) {
        MarcheReceptionPvEntity pv = marcheReceptionPvRepository.findById(pvId)
                .orElseThrow(() -> new IllegalArgumentException(GlobalConstants.PV_REC_NOT_FOUND));

        Object numFactureObj = payload.get("numeroFacture");
        if (numFactureObj != null && !numFactureObj.toString().isBlank()) {
            pv.setNumeroFactureDefinitive(numFactureObj.toString());
        }
        Object montantObj = payload.get("montantLiquide");
        if (montantObj != null && !montantObj.toString().isBlank()) {
            pv.setMontantFactureLiquide(new BigDecimal(montantObj.toString()));
        }
        pv.setLiquidationValidee(true);
        pv.setDateLiquidation(Instant.now());

        MarcheReceptionPvEntity savedPv = marcheReceptionPvRepository.save(pv);

        MarcheDossierEntity dossier = pv.getDossier();
        if (dossier != null) {
            dossier.setStatut(StatutMarcheDossier.LIQUIDE);
            marcheDossierRepository.save(dossier);
        }
        log.info("Liquidation matérialisée en base pour le PV {}", savedPv.getNumeroPv());
        return savedPv;
    }

    // ==========================================
    // ROUTING DES PROCÉDURES SELON SEUILS
    // ==========================================

    public TypeProcedureAchat determinerProcedure(BigDecimal montantTtc) {
        BigDecimal seuilDirect = configService.getDecimal(GlobalConstants.ACHATS_SEUIL_ACHAT_DIRECT, BigDecimal.valueOf(500_000));
        BigDecimal seuilBc = configService.getDecimal(GlobalConstants.ACHATS_SEUIL_BON_COMMAND, BigDecimal.valueOf(15_000_000));
        BigDecimal seuilLc = configService.getDecimal(GlobalConstants.ACHATS_SEUIL_LETTRE_COMMAND, BigDecimal.valueOf(50_000_000));

        if (montantTtc == null || montantTtc.compareTo(seuilDirect) < 0) {
            return TypeProcedureAchat.ACHAT_DIRECT;
        } else if (montantTtc.compareTo(seuilBc) < 0) {
            return TypeProcedureAchat.BON_COMMANDE;
        } else if (montantTtc.compareTo(seuilLc) < 0) {
            return TypeProcedureAchat.LETTRE_COMMANDE;
        } else {
            return TypeProcedureAchat.MARCHE_PUBLIC_CIPM;
        }
    }

    private String calculateSha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Erreur de calcul du hash SHA-256", e);
        }
    }
}
