package com.sodepa.erp.budget.infrastructure.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.audit.application.inputs.MakerCheckerMessageInput;
import com.sodepa.erp.audit.infrastructure.repo.ClickHouseManager;
import com.sodepa.erp.budget.application.inputs.AjouterItemInput;
import com.sodepa.erp.budget.application.inputs.CreerBudgetPlanInput;
import com.sodepa.erp.budget.application.inputs.EngagementInput;
import com.sodepa.erp.budget.application.inputs.ReallocationInput;
import com.sodepa.erp.budget.application.inputs.RechercheBudgetPlanInput;
import com.sodepa.erp.budget.application.BudgetSecurityService;
import com.sodepa.erp.budget.application.inputs.RechercheEngagementInput;
import com.sodepa.erp.organigramme.infrastructure.entities.UniteOrganisationnelleEntity;
import com.sodepa.erp.budget.application.outputs.BudgetEngagementOutput;
import com.sodepa.erp.budget.application.outputs.BudgetItemOutput;
import com.sodepa.erp.budget.application.outputs.BudgetPlanOutput;
import com.sodepa.erp.budget.application.outputs.BudgetPlanSmartOutput;
import com.sodepa.erp.budget.infrastructure.entities.*;
import com.sodepa.erp.budget.infrastructure.repo.*;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.share.erreurs.ConflitMetierException;
import com.sodepa.erp.workflow.application.WorkflowService;
import com.sodepa.erp.workflow.application.WorkflowStateService;
import com.sodepa.erp.workflow.infrastructure.entities.WorkflowInstanceEntity;
import com.sodepa.erp.workflow.presentation.responses.WorkflowCurrentDataResponse;
import com.sodepa.erp.share.MakerCheckerOutput;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.MakerCheckerOperationType;
import com.sodepa.erp.utils.MakerCheckerStatus;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.utils.PageableRecord;
import com.sodepa.erp.utils.Permissions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Adaptateur d'infrastructure et moteur d'execution budgétaire de la SODEPA (Module 02 - BUD).
 * <p>
 * Ce composant central orchestre l'ensemble des règles de gestion financière :
 * <ul>
 *   <li>Contrôle de l'exécutabilité des budgets (interdiction d'engager sur un budget non exécutoire).</li>
 *   <li>Vérification bloquante de la disponibilité des crédits en temps réel (anti-dépassement).</li>
 *   <li>Détermination automatique du circuit de dépense selon le seuil de 500 000 FCFA (Menue vs Grosse dépense).</li>
 *   <li>Classification et routage des réallocations de crédits (Simple DAF vs Significative Conseil d'Administration vs Urgence DG).</li>
 *   <li>Cloisonnement ABAC entre le siège de Yaoundé et les unités opérationnelles (Ranches, Abattoirs).</li>
 *   <li>Traçabilité inaltérable et synchronisation vers la base analytique ClickHouse.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BudgetPlanAdapter {

    private final static int MAX_PAGE_SIZE = 100;

    /** Circuit workflow d'une réallocation simple (≤ 5 % et même section) — RG-BUD-015. */
    public static final String WF_REALLOC_SIMPLE = "WF-DAF-06";
    /** Circuit workflow d'une réallocation significative (> 5 % ou inter-sections) — RG-BUD-016 / RG-BUD-018. */
    public static final String WF_REALLOC_SIGNIFICATIVE = "WF-BUD-REALLOC-SIGNIFICATIVE";
    /** Circuit workflow d'une mesure conservatoire d'extrême urgence — RG-BUD-017. */
    public static final String WF_REALLOC_URGENCE = "WF-BUD-REALLOC-URGENCE";
    /** Seuil pivot (en %) entre réallocation simple et significative. */
    public static final BigDecimal SEUIL_REALLOCATION_SIGNIFICATIVE = BigDecimal.valueOf(5.00);

    /**
     * Classe une réallocation en SIMPLE ou SIGNIFICATIVE selon le taux par rapport à la dotation
     * initiale de la ligne source et l'appartenance à la même section (RG-BUD-018).
     */
    public static TypeReallocation classifierReallocation(BudgetItemEntity source, BudgetItemEntity dest, BigDecimal montant) {
        BigDecimal taux = calculerTauxReallocation(source, montant);
        boolean memeSection = source.getSectionId() != null && source.getSectionId().equals(dest.getSectionId());
        boolean seuilRespecte = taux.compareTo(SEUIL_REALLOCATION_SIGNIFICATIVE) <= 0;
        return (seuilRespecte && memeSection) ? TypeReallocation.SIMPLE : TypeReallocation.SIGNIFICATIVE;
    }

    /** Taux de réallocation (en %) rapporté à la dotation initiale de la ligne source. */
    public static BigDecimal calculerTauxReallocation(BudgetItemEntity source, BigDecimal montant) {
        BigDecimal dotationInitiale = source.getMontantAnnuel() != null && source.getMontantAnnuel().compareTo(BigDecimal.ZERO) > 0
                ? source.getMontantAnnuel()
                : source.getMontantPlanned();
        if (dotationInitiale == null || dotationInitiale.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100);
        }
        return montant.divide(dotationInitiale, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    /**
     * Résout le code du circuit workflow applicable à une réallocation :
     * urgence → {@link #WF_REALLOC_URGENCE}, significative → {@link #WF_REALLOC_SIGNIFICATIVE},
     * sinon {@link #WF_REALLOC_SIMPLE}.
     */
    public static String resoudreCircuitReallocation(TypeReallocation type, boolean urgence) {
        if (urgence) {
            return WF_REALLOC_URGENCE;
        }
        return type == TypeReallocation.SIGNIFICATIVE ? WF_REALLOC_SIGNIFICATIVE : WF_REALLOC_SIMPLE;
    }

    private final BudgetPlanRepository budgetPlanRepository;
    private final BudgetItemRepository budgetItemRepository;
    private final BudgetReallocationRepository budgetReallocationRepository;
    private final BudgetEngagementRepository budgetEngagementRepository;
    private final AuditTrailRepository auditTrailRepository;
    private final WorkflowService workflowService;
    private final UtilsService utilsService;
    private final ObjectMapper objectMapper;
    private final ClickHouseManager clickHouseManager;
    private final BudgetSecurityService budgetSecurityService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private WorkflowStateService workflowStateService;

    private static final List<String> BUDGET_PLAN_WORKFLOW_ENTITY_NAMES = List.of(
            "BUDGET_PLAN",
            BudgetPlanEntity.class.getSimpleName(),
            BudgetPlanEntity.class.getName()
    );

    public boolean canValidate(Permissions permission) {
        try {
            utilsService.hasPermission(permission);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void trackClickHouse(String entityName, String entityPk, Object payloadObj) {
        try {
            String actorId = "SYSTEM";
            try {
                actorId = utilsService.getCurrentUserData().userId();
            } catch (Exception ignored) {
            }

            String payloadStr = objectMapper.writeValueAsString(payloadObj);
            clickHouseManager.saveMakerChecker(
                    MakerCheckerMessageInput.builder()
                            .id(UUID.randomUUID())
                            .entityName(entityName)
                            .entityPk(entityPk)
                            .payload(payloadStr)
                            .timestamp(LocalDateTime.now())
                            .maker_id(actorId)
                            .checker_id(actorId)
                            .build()
            );
        } catch (Exception e) {
            log.warn("Journalisation ClickHouse non bloquante ignorée pour {} ({}): {}", entityName, entityPk, e.getMessage());
        }
    }

    /**
     * Plans budgétaires, paginés et filtrés.
     *
     * <p>
     * Rend des résumés, sans les postes : une liste d'exercices n'a pas à
     * traîner toutes les lignes budgétaires de chacun. Le décompte des postes
     * est obtenu en une requête pour la page entière.
     * </p>
     */
    @Transactional
    public PageRecord<BudgetPlanSmartOutput> getPlansByPage(RechercheBudgetPlanInput input) {
        Pageable pageable = input.pageable();
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            pageable = PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
        }

        Page<BudgetPlanEntity> page = budgetPlanRepository.rechercher(input.annee(), input.statut(), pageable);

        List<UUID> ids = page.getContent().stream().map(BudgetPlanEntity::getId).toList();
        Map<UUID, Long> decomptes = ids.isEmpty()
                ? new HashMap<>()
                : budgetItemRepository.compterParPlan(ids).stream()
                        .collect(Collectors.toMap(
                                BudgetItemRepository.DecompteItems::getPlanId,
                                BudgetItemRepository.DecompteItems::getNombre));

        Map<UUID, WorkflowInstanceEntity> activeById = workflowStateService != null
                ? workflowStateService.findActiveInstancesByEntityUuid(BUDGET_PLAN_WORKFLOW_ENTITY_NAMES)
                : new HashMap<>();

        List<BudgetPlanSmartOutput> content = new ArrayList<>();
        for (BudgetPlanEntity plan : page.getContent()) {
            content.add(mapPlanSmart(plan, decomptes.getOrDefault(plan.getId(), 0L), activeById.get(plan.getId())));
        }

        long extraDraftCount = 0L;
        if (!activeById.isEmpty() && workflowStateService != null) {
            List<BudgetPlanSmartOutput> matchingDrafts = new ArrayList<>();
            for (Map.Entry<UUID, WorkflowInstanceEntity> entry : activeById.entrySet()) {
                UUID draftId = entry.getKey();
                if (budgetPlanRepository.existsById(draftId)) {
                    continue;
                }
                WorkflowInstanceEntity wf = entry.getValue();
                WorkflowCurrentDataResponse current = workflowStateService.getCachedCurrentData(wf);
                Map<String, Object> data = current.getData() != null ? current.getData() : new HashMap<>();
                Integer draftAnnee = asInteger(data.get("annee"));
                StatutBudget draftStatut = resolveStatutBudget(data.get("statut"), StatutBudget.DRAFT);
                if (input.annee() != null && (draftAnnee == null || !input.annee().equals(draftAnnee))) {
                    continue;
                }
                if (input.statut() != null && input.statut() != draftStatut) {
                    continue;
                }
                matchingDrafts.add(buildDraftSmartPlanFromWorkflow(draftId, wf, data, current));
            }
            extraDraftCount = matchingDrafts.size();
            boolean paged = page.getPageable().isPaged();
            if (!paged || page.getNumber() == 0) {
                content.addAll(0, matchingDrafts);
            }
        }

        boolean paged = page.getPageable().isPaged();

        return new PageRecord<>(
                content,
                content.isEmpty(),
                page.isFirst(),
                page.isLast(),
                page.getNumber(),
                content.size(),
                PageableRecord.builder()
                        .offset(paged ? page.getPageable().getOffset() : 0L)
                        .pageNumber(paged ? page.getPageable().getPageNumber() : 0L)
                        .pageSize(paged ? page.getPageable().getPageSize() : 0L)
                        .paged(paged)
                        .sort(page.getPageable().getSort())
                        .unpaged(!paged)
                        .build(),
                page.getSize(),
                page.getSort(),
                page.getTotalElements() + extraDraftCount,
                page.getTotalPages()
        );
    }

    /**
     * Fiche complète d'un plan, postes compris.
     */
    @Transactional
    public BudgetPlanOutput getPlanById(UUID planId) {
        Optional<BudgetPlanEntity> opt = budgetPlanRepository.findByIdAvecItems(planId);
        Optional<WorkflowInstanceEntity> activeWfOpt = workflowStateService != null
                ? workflowStateService.findActiveInstanceForEntity(BUDGET_PLAN_WORKFLOW_ENTITY_NAMES, planId)
                : Optional.empty();
        if (opt.isPresent()) {
            return mapPlanWithWorkflow(opt.get(), activeWfOpt.orElse(null));
        }
        if (activeWfOpt.isPresent() && workflowStateService != null) {
            WorkflowInstanceEntity wf = activeWfOpt.get();
            WorkflowCurrentDataResponse current = workflowStateService.getCurrentData(wf);
            Map<String, Object> data = current.getData() != null ? current.getData() : new HashMap<>();
            return buildDraftPlanFromWorkflow(planId, wf, data, current);
        }
        throw new IllegalArgumentException("Plan budgétaire introuvable.");
    }

    /**
     * Engagements, paginés et filtrés sur le plan et l'état.
     */
    @Transactional(readOnly = true)
    public PageRecord<BudgetEngagementOutput> getEngagementsByPage(RechercheEngagementInput input) {
        budgetSecurityService.checkPermission(Permissions.VIEW_BUDGET);
        Pageable pageable = input.pageable();
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            pageable = PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
        }

        UUID filterUniteId = input.uniteId();
        if (!budgetSecurityService.isGlobalScope()) {
            filterUniteId = budgetSecurityService.getCurrentUserUnite()
                    .map(UniteOrganisationnelleEntity::getId)
                    .orElse(null);
        }

        String statut = (input.statut() == null || input.statut().isBlank()) ? null : input.statut();
        Page<BudgetEngagementEntity> page = budgetEngagementRepository.rechercher(input.planId(), statut, filterUniteId, pageable);
        boolean paged = page.getPageable().isPaged();

        return new PageRecord<>(
                page.getContent().stream().map(this::mapEngagement).toList(),
                page.isEmpty(),
                page.isFirst(),
                page.isLast(),
                page.getNumber(),
                page.getNumberOfElements(),
                PageableRecord.builder()
                        .offset(paged ? page.getPageable().getOffset() : 0L)
                        .pageNumber(paged ? page.getPageable().getPageNumber() : 0L)
                        .pageSize(paged ? page.getPageable().getPageSize() : 0L)
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

    /**
     * Un engagement, par son numéro.
     *
     * <p>
     * Par le numéro et non par l'identifiant technique : c'est lui que portent
     * la liquidation, l'annulation et le workflow.
     * </p>
     */
    @Transactional(readOnly = true)
    public BudgetEngagementOutput getEngagementByNumero(String numeroEngagement) {
        BudgetEngagementEntity engagement = budgetEngagementRepository.findByNumeroEngagement(numeroEngagement)
                .orElseThrow(() -> new IllegalArgumentException("Engagement introuvable."));
        return mapEngagement(engagement);
    }

    @Transactional
    public BudgetPlanOutput creerBudgetPlan(CreerBudgetPlanInput input) {
        utilsService.hasPermission(Permissions.CREATE_BUDGET_PLAN);
        List<BudgetPlanEntity> existants = budgetPlanRepository.findByAnnee(input.annee());
        int activeSameYear = 0;
        if (workflowStateService != null) {
            for (WorkflowInstanceEntity wf : workflowStateService.findActiveInstancesByEntityUuid(BUDGET_PLAN_WORKFLOW_ENTITY_NAMES).values()) {
                Map<String, Object> d = workflowStateService.getLatestMergedDataMap(wf);
                Integer annee = asInteger(d.get("annee"));
                if (annee != null && annee == input.annee()) {
                    activeSameYear++;
                }
            }
        }
        int nouvelleVersion = existants.size() + activeSameYear + 1;

        UUID id = UUID.randomUUID();
        Map<String, Object> payload = new HashMap<>(objectMapper.convertValue(input, Map.class));
        payload.put("id", id.toString());
        payload.put("version", nouvelleVersion);
        payload.put("statut", StatutBudget.DRAFT.name());

        UUID uuid = workflowService.submitChange(
                MakerCheckerEntityName.BUDGET_PLAN,
                id.toString(),
                payload,
                MakerCheckerOperationType.CREATE
        );
        log.info("Plan budgétaire exercice {} V{} soumis au workflow ClickHouse (Ticket: {})", input.annee(), nouvelleVersion, uuid);

        trackClickHouse(MakerCheckerEntityName.BUDGET_PLAN.name(), id.toString(), Map.of(
                "action", "SOUMISSION_CREER_BUDGET_PLAN",
                "annee", input.annee(),
                "intitule", input.intitule(),
                "version", nouvelleVersion,
                "ticket", uuid.toString()
        ));

        WorkflowInstanceEntity activeWf = workflowStateService != null
                ? workflowStateService.findActiveInstanceForEntity(BUDGET_PLAN_WORKFLOW_ENTITY_NAMES, id).orElse(null)
                : null;
        return new BudgetPlanOutput(
                id,
                input.annee(),
                input.intitule(),
                nouvelleVersion,
                StatutBudget.DRAFT,
                BigDecimal.ZERO,
                LocalDateTime.now(),
                input.utilisateurId(),
                null,
                null,
                new ArrayList<>(),
                true,
                activeWf != null,
                activeWf != null ? activeWf.getId() : null,
                activeWf != null && activeWf.getStatut() != null ? activeWf.getStatut().name() : "EN_COURS",
                "CREATE",
                List.of()
        );
    }

    @Transactional
    public BudgetPlanOutput modifierBudgetPlan(UUID planId, CreerBudgetPlanInput input) {
        utilsService.hasPermission(Permissions.UPDATE_BUDGET_PLAN);
        BudgetPlanEntity existing = budgetPlanRepository.findByIdAvecItems(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan budgétaire introuvable."));

        if (workflowStateService != null
                && workflowStateService.findActiveInstanceForEntity(BUDGET_PLAN_WORKFLOW_ENTITY_NAMES, planId).isPresent()) {
            throw new ConflitMetierException("Une demande de modification est déjà en cours de validation pour ce plan budgétaire.");
        }

        Map<String, Object> payload = new HashMap<>(objectMapper.convertValue(input, Map.class));
        payload.put("id", existing.getId().toString());
        payload.put("version", existing.getVersion());

        UUID uuid = workflowService.submitChange(
                MakerCheckerEntityName.BUDGET_PLAN,
                existing.getId().toString(),
                payload,
                MakerCheckerOperationType.UPDATE
        );
        log.info("Modification plan budgétaire {} soumise au workflow (Ticket: {})", existing.getId(), uuid);

        WorkflowInstanceEntity activeWf = workflowStateService != null
                ? workflowStateService.findActiveInstanceForEntity(BUDGET_PLAN_WORKFLOW_ENTITY_NAMES, planId).orElse(null)
                : null;
        return mapPlanWithWorkflow(existing, activeWf);
    }

    @Transactional
    public void finaliserBudgetPlanDepuisWorkflow(UUID planId, Map<String, Object> finalData) {
        if (planId == null || finalData == null || finalData.isEmpty()) {
            return;
        }
        materialiserBudgetPlanFinale(planId, finalData);
    }

    private void materialiserBudgetPlanFinale(UUID planId, Map<String, Object> payload) {
        Map<String, Object> safePayload = payload != null ? payload : new HashMap<>();
        Optional<BudgetPlanEntity> existingOpt = budgetPlanRepository.findById(planId);
        if (existingOpt.isPresent()) {
            miseAJourBudgetPlan(existingOpt.get(), safePayload);
        } else {
            creationBudgetPlan(safePayload, planId);
        }
    }

    private BudgetPlanEntity creationBudgetPlan(Map<String, Object> payload, UUID planId) {
        Integer anneeParsed = asInteger(payload.get("annee"));
        int annee = anneeParsed != null ? anneeParsed : LocalDateTime.now().getYear();
        String intitule = payload.get("intitule") != null ? payload.get("intitule").toString() : "Plan budgétaire " + annee;
        UUID userId = payload.get("utilisateurId") != null ? UUID.fromString(payload.get("utilisateurId").toString()) : null;
        Integer verParsed = asInteger(payload.get("version"));
        int version = verParsed != null ? verParsed : 1;
        StatutBudget finalStatut = resolveStatutBudget(payload.get("statut"), StatutBudget.DRAFT);

        BudgetPlanEntity plan = BudgetPlanEntity.builder()
                .id(planId)
                .annee(annee)
                .intitule(intitule)
                .version(version)
                .statut(finalStatut)
                .totalBudget(BigDecimal.ZERO)
                .creeLe(LocalDateTime.now())
                .creePar(userId)
                .build();

        BudgetPlanEntity saved = budgetPlanRepository.save(plan);

        auditTrailRepository.save(AuditTrailEntity.builder()
                .entiteNom("BudgetPlanEntity")
                .entiteId(saved.getId())
                .action("CREATE")
                .details("Création du plan budgétaire exercice " + annee + " version V" + version)
                .timestamp(LocalDateTime.now())
                .utilisateur(userId)
                .build());

        log.info("Plan budgétaire {} (exercice {} V{}) matérialisé en PostgreSQL à l'approbation finale.",
                saved.getId(), annee, version);
        return saved;
    }

    private BudgetPlanEntity miseAJourBudgetPlan(BudgetPlanEntity plan, Map<String, Object> payload) {
        if (payload.containsKey("intitule") && payload.get("intitule") != null) {
            plan.setIntitule(payload.get("intitule").toString());
        }
        Integer anneeParsed = asInteger(payload.get("annee"));
        if (anneeParsed != null) {
            plan.setAnnee(anneeParsed);
        }
        if (payload.containsKey("statut") && payload.get("statut") != null) {
            plan.setStatut(resolveStatutBudget(payload.get("statut"), plan.getStatut()));
        }
        UUID userId = payload.get("utilisateurId") != null ? parseUuidSafe(payload.get("utilisateurId").toString()) : null;
        plan.setModifieLe(LocalDateTime.now());
        if (userId != null) {
            plan.setModifiePar(userId);
        }
        BudgetPlanEntity saved = budgetPlanRepository.save(plan);
        log.info("Plan budgétaire {} mis à jour en PostgreSQL à l'approbation finale.", saved.getId());
        return saved;
    }

    @Transactional
    public void validateOrRejectBudgetPlan(ValidateOrRejectSubmissionInput input) {
        utilsService.hasPermission(Permissions.VALIDATE_BUDGET_PLAN);
        MakerCheckerOutput checkerOutput = workflowService.findById(input.id());
        MakerCheckerStatus currentStatus = checkerOutput.status();
        if (currentStatus != MakerCheckerStatus.PENDING) {
            throw new RuntimeException(String.format(
                    "Invalid transition: Cannot transition from %s to %s.", currentStatus, input.decision()));
        }

        UUID checkerId = UUID.fromString(utilsService.getCurrentUserData().userId());
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
            log.info("Plan budgétaire rejeté par Checker {} (zéro écriture Postgres). Ticket: {}", checkerId, checkerOutput.id());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID planId = UUID.fromString(checkerOutput.entityPk());
            materialiserBudgetPlanFinale(planId, checkerOutput.payload());
            workflowService.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
            log.info("Plan budgétaire approuvé par Checker {}. Ticket: {}, Plan PK: {}", checkerId, checkerOutput.id(), planId);
        }
    }

    @Transactional
    public BudgetItemOutput ajouterItemAuPlan(AjouterItemInput input) {
        BudgetPlanEntity plan = budgetPlanRepository.findById(input.planId())
                .orElseThrow(() -> new IllegalArgumentException("Plan budgétaire introuvable."));

        if (!plan.getStatut().isModifiable()) {
            throw new IllegalStateException("Impossible d'ajouter des lignes budgétaires. Le plan n'est plus modifiable.");
        }

        Optional<BudgetItemEntity> existantOpt = budgetItemRepository.findByBudgetPlanIdAndCompteCodeAndSectionId(
                input.planId(), input.compteCode(), input.sectionId());
        if (existantOpt.isPresent()) {
            throw new IllegalArgumentException("Une ligne budgétaire pour ce compte et cette section existe déjà dans ce plan.");
        }

        BudgetItemEntity item = BudgetItemEntity.builder()
                .compteCode(input.compteCode())
                .sectionId(input.sectionId())
                .montantAnnuel(input.montant())
                .montantPlanned(input.montant())
                .montantEngage(BigDecimal.ZERO)
                .montantReal(BigDecimal.ZERO)
                .build();

        BigDecimal partMensuelle = input.montant().divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);
        BigDecimal cumul = BigDecimal.ZERO;
        for (int i = 1; i <= 12; i++) {
            BigDecimal montantPeriode = partMensuelle;
            if (i == 12) {
                montantPeriode = input.montant().subtract(cumul);
            } else {
                cumul = cumul.add(partMensuelle);
            }
            BudgetItemPeriodeEntity periode = BudgetItemPeriodeEntity.builder()
                    .periodeNum(i)
                    .montantPlanned(montantPeriode)
                    .montantReal(BigDecimal.ZERO)
                    .build();
            item.addPeriode(periode);
        }

        plan.addItem(item);
        plan.setTotalBudget(plan.getTotalBudget().add(input.montant()));
        budgetPlanRepository.save(plan);

        BudgetItemEntity saved = budgetItemRepository.save(item);
        return mapItem(saved);
    }

    private UUID resolveActorIdOrFallback(UUID requestedUserId) {
        UUID currentId = utilsService.getCurrentUserIdOrNull();
        return currentId != null ? currentId : requestedUserId;
    }

    @Transactional
    public void soumettrePlan(UUID planId, UUID userId) {
        utilsService.hasPermission(Permissions.UPDATE_BUDGET_PLAN);
        UUID actorId = resolveActorIdOrFallback(userId);
        BudgetPlanEntity plan = budgetPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan budgétaire introuvable."));
        if (!plan.getStatut().isModifiable()) {
            throw new IllegalStateException("Le plan n'est pas à l'état DRAFT ou REJECTED.");
        }

        plan.setStatut(StatutBudget.SUBMITTED);
        plan.setModifieLe(LocalDateTime.now());
        plan.setModifiePar(actorId);
        budgetPlanRepository.save(plan);

        auditTrailRepository.save(AuditTrailEntity.builder()
                .entiteNom("BudgetPlanEntity")
                .entiteId(planId)
                .action("SUBMIT")
                .details("Soumission du budget pour approbation.")
                .timestamp(LocalDateTime.now())
                .utilisateur(actorId)
                .build());
    }

    @Transactional
    public void approuverPlan(UUID planId, UUID userId) {
        utilsService.hasPermission(Permissions.VALIDATE_BUDGET_PLAN);
        UUID actorId = resolveActorIdOrFallback(userId);
        BudgetPlanEntity plan = budgetPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan budgétaire introuvable."));
        if (plan.getStatut() != StatutBudget.SUBMITTED && plan.getStatut() != StatutBudget.SOUMIS) {
            throw new IllegalStateException("Le plan doit être soumis pour être approuvé.");
        }
        if (plan.getModifiePar() != null && plan.getModifiePar().equals(actorId) && !utilsService.isAdmin()) {
            throw new ConflitMetierException("Principe des 4 yeux : le soumetteur ne peut pas approuver son propre budget.");
        }

        plan.setStatut(StatutBudget.PUBLISHED);
        plan.setModifieLe(LocalDateTime.now());
        plan.setModifiePar(actorId);
        budgetPlanRepository.save(plan);

        auditTrailRepository.save(AuditTrailEntity.builder()
                .entiteNom("BudgetPlanEntity")
                .entiteId(planId)
                .action("APPROVE")
                .details("Approbation et publication du plan budgétaire.")
                .timestamp(LocalDateTime.now())
                .utilisateur(actorId)
                .build());
    }

    @Transactional
    public void rejeterPlan(UUID planId, UUID userId) {
        utilsService.hasPermission(Permissions.VALIDATE_BUDGET_PLAN);
        UUID actorId = resolveActorIdOrFallback(userId);
        BudgetPlanEntity plan = budgetPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan budgétaire introuvable."));
        if (plan.getStatut() != StatutBudget.SUBMITTED && plan.getStatut() != StatutBudget.SOUMIS) {
            throw new IllegalStateException("Le plan doit être soumis pour être rejeté.");
        }

        plan.setStatut(StatutBudget.REJECTED);
        plan.setModifieLe(LocalDateTime.now());
        plan.setModifiePar(actorId);
        budgetPlanRepository.save(plan);

        auditTrailRepository.save(AuditTrailEntity.builder()
                .entiteNom("BudgetPlanEntity")
                .entiteId(planId)
                .action("REJECT")
                .details("Rejet du plan budgétaire lors du workflow de validation.")
                .timestamp(LocalDateTime.now())
                .utilisateur(actorId)
                .build());
    }

    @Transactional
    public void effectuerReallocation(ReallocationInput input) {
        utilsService.hasPermission(Permissions.CREATE_BUDGET_REALLOCATION);

        boolean urgence = Boolean.TRUE.equals(input.mesureConservatoire());
        if (urgence) {
            // RG-BUD-017 : seule la DG peut décider d'une mesure conservatoire d'urgence
            utilsService.hasPermission(Permissions.BUDGET_REALLOCATE_URGENCE);
            if (input.justificationUrgence() == null || input.justificationUrgence().isBlank()) {
                throw new IllegalArgumentException("Une mesure conservatoire d'urgence exige une justification explicite.");
            }
        }

        TypeReallocation typePrevu = null;
        BudgetItemEntity sourcePrevue = budgetItemRepository.findById(input.sourceItemId()).orElse(null);
        BudgetItemEntity destPrevue = budgetItemRepository.findById(input.destItemId()).orElse(null);
        if (sourcePrevue != null && destPrevue != null) {
            budgetSecurityService.validateAccessToUnite(sourcePrevue.getSectionId());
            typePrevu = classifierReallocation(sourcePrevue, destPrevue, input.montant());
        }
        String circuit = resoudreCircuitReallocation(typePrevu, urgence);

        UUID id = UUID.randomUUID();
        Map<String, Object> payload = new HashMap<>(objectMapper.convertValue(input, Map.class));
        payload.put("id", id.toString());
        payload.put("circuitWorkflow", circuit);
        if (typePrevu != null) {
            payload.put("typeReallocation", typePrevu.name());
        }

        UUID uuid = workflowService.submitChange(
                MakerCheckerEntityName.BUDGET_REALLOCATION,
                id.toString(),
                payload,
                MakerCheckerOperationType.CREATE
        );
        log.info("Réallocation budgétaire soumise au Maker-Checker (Ticket: {}, circuit: {}, type: {})", uuid, circuit, typePrevu);

        trackClickHouse(MakerCheckerEntityName.BUDGET_REALLOCATION.name(), id.toString(), Map.of(
                "action", "SOUMISSION_EFFECTUER_REALLOCATION",
                "sourceItem", input.sourceItemId().toString(),
                "destItem", input.destItemId().toString(),
                "montant", input.montant(),
                "ticket", uuid.toString(),
                "circuit", circuit
        ));

        // RG-BUD-016 : Seules les réallocations simples ou mesures d'urgence validées suivent les circuits spécifiques.
        // Conformément au principe strict des 4 yeux (Séparation Maker/Checker), aucune auto-validation n'est exécutée ici.
    }

    private BudgetItemEntity lockBudgetItemOrFallback(UUID itemId, String notFoundMessage) {
        return budgetItemRepository.findWithLockById(itemId)
                .or(() -> budgetItemRepository.findById(itemId))
                .orElseThrow(() -> new IllegalArgumentException(notFoundMessage));
    }

    private void creationReallocation(Map<String, Object> payload, UUID reallocationId) {
        ReallocationInput input = objectMapper.convertValue(payload, ReallocationInput.class);
        // Ordre déterministe de verrouillage pessimiste pour prévenir tout interblocage (deadlock) concurrent.
        BudgetItemEntity source;
        BudgetItemEntity dest;
        if (input.sourceItemId().compareTo(input.destItemId()) <= 0) {
            source = lockBudgetItemOrFallback(input.sourceItemId(), "Item source introuvable.");
            dest = input.sourceItemId().equals(input.destItemId())
                    ? source
                    : lockBudgetItemOrFallback(input.destItemId(), "Item destinataire introuvable.");
        } else {
            dest = lockBudgetItemOrFallback(input.destItemId(), "Item destinataire introuvable.");
            source = lockBudgetItemOrFallback(input.sourceItemId(), "Item source introuvable.");
        }

        if (!source.getBudgetPlan().getId().equals(dest.getBudgetPlan().getId())) {
            throw new IllegalArgumentException("Les lignes budgétaires doivent appartenir au même plan.");
        }

        budgetSecurityService.validateAccessToUnite(source.getSectionId());

        if (!source.getBudgetPlan().getStatut().isExecutoire()) {
            throw new IllegalStateException("Les réallocations ne sont permises que sur un budget exécutoire.");
        }

        BigDecimal disponible = source.getMontantPlanned()
                .subtract(source.getMontantEngage())
                .subtract(source.getMontantReal());

        if (disponible.compareTo(input.montant()) < 0) {
            throw new IllegalArgumentException("Crédits insuffisants sur le poste source. Disponible: " + disponible);
        }

        // RG-BUD-016 : une réallocation significative hors urgence ne s'exécute qu'après résolution du CA
        if (classifierReallocation(source, dest, input.montant()) == TypeReallocation.SIGNIFICATIVE
                && !Boolean.TRUE.equals(input.mesureConservatoire())
                && (input.numeroResolutionCa() == null || input.numeroResolutionCa().isBlank())) {
            throw new IllegalStateException("Réallocation significative (> 5 % ou inter-sections) : "
                    + "le numéro de résolution du Conseil d'Administration est obligatoire (RG-BUD-016).");
        }

        source.setMontantPlanned(source.getMontantPlanned().subtract(input.montant()));
        dest.setMontantPlanned(dest.getMontantPlanned().add(input.montant()));

        BigDecimal diffPeriode = input.montant().divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);
        BigDecimal cumul = BigDecimal.ZERO;
        for (int i = 0; i < 12; i++) {
            BigDecimal adj = diffPeriode;
            if (i == 11) {
                adj = input.montant().subtract(cumul);
            } else {
                cumul = cumul.add(diffPeriode);
            }
            source.getPeriodes().get(i).setMontantPlanned(source.getPeriodes().get(i).getMontantPlanned().subtract(adj));
            dest.getPeriodes().get(i).setMontantPlanned(dest.getPeriodes().get(i).getMontantPlanned().add(adj));
        }

        budgetItemRepository.save(source);
        budgetItemRepository.save(dest);

        BigDecimal taux = calculerTauxReallocation(source, input.montant());
        TypeReallocation typeRealloc = classifierReallocation(source, dest, input.montant());

        boolean isUrgence = Boolean.TRUE.equals(input.mesureConservatoire());
        boolean resolutionCaFournie = input.numeroResolutionCa() != null && !input.numeroResolutionCa().isBlank();
        StatutReallocation statutRealloc = isUrgence ? StatutReallocation.A_REGULARISER_CA : StatutReallocation.EXECUTEE;

        BudgetReallocationEntity reallocation = BudgetReallocationEntity.builder()
                .id(reallocationId)
                .sourceItem(source)
                .destItem(dest)
                .montant(input.montant())
                .dateTransfert(LocalDateTime.now())
                .validePar(input.responsableId())
                .raison(input.raison())
                .typeReallocation(typeRealloc)
                .tauxReallocation(taux)
                .mesureConservatoire(isUrgence)
                .justificationUrgence(input.justificationUrgence())
                .numeroResolutionCa(input.numeroResolutionCa())
                .dateValidationCa(resolutionCaFournie ? LocalDateTime.now() : null)
                .statut(statutRealloc)
                .build();
        budgetReallocationRepository.save(reallocation);

        auditTrailRepository.save(AuditTrailEntity.builder()
                .entiteNom("BudgetItemEntity")
                .entiteId(input.sourceItemId())
                .action("REALLOCATE")
                .details("Transfert de " + input.montant() + " vers l'item ID " + input.destItemId() + ". Raison : " + input.raison())
                .timestamp(LocalDateTime.now())
                .utilisateur(input.responsableId())
                .build());
    }

    @Transactional
    public void validateOrRejectReallocation(ValidateOrRejectSubmissionInput input) {
        utilsService.hasPermission(Permissions.VALIDATE_BUDGET_REALLOCATION);
        MakerCheckerOutput checkerOutput = workflowService.findById(input.id());
        MakerCheckerStatus currentStatus = checkerOutput.status();
        if (currentStatus != MakerCheckerStatus.PENDING) {
            throw new RuntimeException(String.format(
                    "Invalid transition: Cannot transition from %s to %s.", currentStatus, input.decision()));
        }

        UUID checkerId = UUID.fromString(utilsService.getCurrentUserData().userId());
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
            log.info("Réallocation budgétaire rejetée par Checker {}. Ticket: {}", checkerId, checkerOutput.id());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID reallocationId = UUID.fromString(checkerOutput.entityPk());
            if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                creationReallocation(checkerOutput.payload(), reallocationId);
            }
            workflowService.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
            log.info("Réallocation budgétaire approuvée par Checker {}. Ticket: {}, Reallocation PK: {}", checkerId, checkerOutput.id(), reallocationId);
        }
    }

    @Transactional
    public BudgetEngagementOutput enregistrerEngagement(EngagementInput input) {
        utilsService.hasPermission(Permissions.CREATE_BUDGET_ENGAGEMENT);
        UUID id = UUID.randomUUID();
        Map<String, Object> payload = new HashMap<>(objectMapper.convertValue(input, Map.class));
        payload.put("id", id.toString());

        UUID uuid = workflowService.submitChange(
                MakerCheckerEntityName.BUDGET_ENGAGEMENT,
                id.toString(),
                payload,
                MakerCheckerOperationType.CREATE
        );
        log.info("Engagement budgétaire {} soumis au Maker-Checker (Ticket: {})", input.numeroEngagement(), uuid);

        trackClickHouse(MakerCheckerEntityName.BUDGET_ENGAGEMENT.name(), id.toString(), Map.of(
                "action", "SOUMISSION_ENREGISTRER_ENGAGEMENT",
                "numeroEngagement", input.numeroEngagement(),
                "montant", input.montant(),
                "ticket", uuid.toString()
        ));

        return budgetEngagementRepository.findById(id)
                .map(this::mapEngagement)
                .orElseGet(() -> new BudgetEngagementOutput(
                        id,
                        null,
                        input.numeroEngagement(),
                        input.description(),
                        input.montant(),
                        LocalDateTime.now(),
                        "PENDING_VALIDATION"
                ));
    }

    private BudgetEngagementEntity creationEngagement(Map<String, Object> payload, UUID engagementId) {
        EngagementInput input = objectMapper.convertValue(payload, EngagementInput.class);
        BudgetItemEntity item = budgetItemRepository
                .findWithLockByPlanAndCompteAndSection(input.planId(), input.compteCode(), input.sectionId())
                .or(() -> budgetItemRepository.findByBudgetPlanIdAndCompteCodeAndSectionId(input.planId(), input.compteCode(), input.sectionId()))
                .orElseThrow(() -> new IllegalArgumentException("Ligne budgétaire inexistante pour ce plan, compte et section."));

        if (!item.getBudgetPlan().getStatut().isExecutoire()) {
            throw new IllegalStateException("Impossible d'effectuer un engagement sur un plan budgétaire non exécutoire.");
        }

        BigDecimal disponible = item.getMontantPlanned()
                .subtract(item.getMontantEngage())
                .subtract(item.getMontantReal());

        if (disponible.compareTo(input.montant()) < 0) {
            throw new IllegalArgumentException("Crédit budgétaire insuffisant pour cet engagement (Solde disponible: " 
                    + disponible + ", Demandé: " + input.montant() + "). Opération bloquée.");
        }

        budgetSecurityService.validateAccessToUnite(item.getSectionId());
        UUID userUniteId = budgetSecurityService.getCurrentUserUnite()
                .map(UniteOrganisationnelleEntity::getId)
                .orElse(item.getSectionId());

        String circuit = input.montant().compareTo(BigDecimal.valueOf(500000)) < 0 ? "MENUE_DEPENSE" : "GROSSE_DEPENSE";
        BudgetEngagementEntity engagement = BudgetEngagementEntity.builder()
                .id(engagementId)
                .budgetItem(item)
                .numeroEngagement(input.numeroEngagement())
                .description(input.description())
                .montant(input.montant())
                .dateEngagement(LocalDateTime.now())
                .circuit(circuit)
                .uniteOrganisationnelleId(userUniteId)
                .statut("ENGAGED")
                .build();

        item.setMontantEngage(item.getMontantEngage().add(input.montant()));
        budgetItemRepository.save(item);

        BudgetEngagementEntity saved = budgetEngagementRepository.save(engagement);

        auditTrailRepository.save(AuditTrailEntity.builder()
                .entiteNom("BudgetEngagementEntity")
                .entiteId(saved.getId())
                .action("CREATE")
                .details("Engagement de dépenses numéro " + input.numeroEngagement() + " pour " + input.montant())
                .timestamp(LocalDateTime.now())
                .utilisateur(input.utilisateurId())
                .build());

        return saved;
    }

    @Transactional
    public void validateOrRejectEngagement(ValidateOrRejectSubmissionInput input) {
        utilsService.hasPermission(Permissions.VALIDATE_BUDGET_ENGAGEMENT);
        MakerCheckerOutput checkerOutput = workflowService.findById(input.id());
        MakerCheckerStatus currentStatus = checkerOutput.status();
        if (currentStatus != MakerCheckerStatus.PENDING) {
            throw new RuntimeException(String.format(
                    "Invalid transition: Cannot transition from %s to %s.", currentStatus, input.decision()));
        }

        UUID checkerId = UUID.fromString(utilsService.getCurrentUserData().userId());
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
            log.info("Engagement budgétaire rejeté par Checker {}. Ticket: {}", checkerId, checkerOutput.id());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID engagementId = UUID.fromString(checkerOutput.entityPk());
            if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                creationEngagement(checkerOutput.payload(), engagementId);
            }
            workflowService.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
            log.info("Engagement budgétaire approuvé par Checker {}. Ticket: {}, Engagement PK: {}", checkerId, checkerOutput.id(), engagementId);
        }
    }

    @Transactional
    public void liquiderEngagement(String numeroEngagement, UUID userId) {
        utilsService.hasAnyPermission(Permissions.VALIDATE_BUDGET_ENGAGEMENT, Permissions.LIQUIDATE_INVOICE, Permissions.VALIDATE_LIQUIDATION);
        UUID actorId = resolveActorIdOrFallback(userId);
        BudgetEngagementEntity engagement = budgetEngagementRepository.findByNumeroEngagement(numeroEngagement)
                .orElseThrow(() -> new IllegalArgumentException("Engagement introuvable."));

        if (!"ENGAGED".equals(engagement.getStatut())) {
            throw new IllegalStateException("L'engagement n'est plus à l'état ENGAGED (état actuel: " + engagement.getStatut() + ").");
        }

        BudgetItemEntity item = lockAssociatedBudgetItem(engagement);
        budgetSecurityService.validateAccessToUnite(item.getSectionId());

        engagement.setStatut("CONVERTED_TO_REAL");
        BigDecimal currentEngage = item.getMontantEngage() != null ? item.getMontantEngage() : BigDecimal.ZERO;
        BigDecimal newEngage = currentEngage.subtract(engagement.getMontant());
        item.setMontantEngage(newEngage.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newEngage);

        BigDecimal currentReal = item.getMontantReal() != null ? item.getMontantReal() : BigDecimal.ZERO;
        item.setMontantReal(currentReal.add(engagement.getMontant()));

        budgetItemRepository.save(item);
        budgetEngagementRepository.save(engagement);

        auditTrailRepository.save(AuditTrailEntity.builder()
                .entiteNom("BudgetEngagementEntity")
                .entiteId(engagement.getId())
                .action("LIQUIDATE")
                .details("Liquidation (conversion en réel) de l'engagement " + numeroEngagement)
                .timestamp(LocalDateTime.now())
                .utilisateur(actorId)
                .build());
    }

    private BudgetItemEntity lockAssociatedBudgetItem(BudgetEngagementEntity engagement) {
        BudgetItemEntity attached = engagement.getBudgetItem();
        if (attached == null || attached.getId() == null) {
            return attached;
        }
        return budgetItemRepository.findWithLockById(attached.getId())
                .or(() -> budgetItemRepository.findById(attached.getId()))
                .orElse(attached);
    }

    @Transactional
    public void annulerEngagement(String numeroEngagement, UUID userId) {
        utilsService.hasAnyPermission(Permissions.VALIDATE_BUDGET_ENGAGEMENT, Permissions.CREATE_BUDGET_ENGAGEMENT);
        UUID actorId = resolveActorIdOrFallback(userId);
        BudgetEngagementEntity engagement = budgetEngagementRepository.findByNumeroEngagement(numeroEngagement)
                .orElseThrow(() -> new IllegalArgumentException("Engagement introuvable."));

        if (!"ENGAGED".equals(engagement.getStatut())) {
            throw new IllegalStateException("Seuls les engagements à l'état ENGAGED peuvent être annulés.");
        }

        BudgetItemEntity item = lockAssociatedBudgetItem(engagement);
        budgetSecurityService.validateAccessToUnite(item.getSectionId());

        engagement.setStatut("CANCELLED");
        BigDecimal currentEngage = item.getMontantEngage() != null ? item.getMontantEngage() : BigDecimal.ZERO;
        BigDecimal newEngage = currentEngage.subtract(engagement.getMontant());
        item.setMontantEngage(newEngage.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newEngage);

        budgetItemRepository.save(item);
        budgetEngagementRepository.save(engagement);

        auditTrailRepository.save(AuditTrailEntity.builder()
                .entiteNom("BudgetEngagementEntity")
                .entiteId(engagement.getId())
                .action("CANCEL")
                .details("Annulation de l'engagement budgétaire " + numeroEngagement)
                .timestamp(LocalDateTime.now())
                .utilisateur(actorId)
                .build());
    }

    private BudgetPlanOutput mapPlan(BudgetPlanEntity p) {
        return mapPlanWithWorkflow(p, null);
    }

    private BudgetPlanOutput mapPlanWithWorkflow(BudgetPlanEntity p, WorkflowInstanceEntity activeWf) {
        List<BudgetItemOutput> items = new ArrayList<>();
        if (p.getItems() != null) {
            items = p.getItems().stream().map(this::mapItem).collect(Collectors.toList());
        }
        boolean enCours = activeWf != null;
        WorkflowCurrentDataResponse current = (enCours && workflowStateService != null)
                ? workflowStateService.getCachedCurrentData(activeWf)
                : null;
        return new BudgetPlanOutput(
                p.getId(),
                p.getAnnee(),
                p.getIntitule(),
                p.getVersion(),
                p.getStatut(),
                p.getTotalBudget(),
                p.getCreeLe(),
                p.getCreePar(),
                p.getModifieLe(),
                p.getModifiePar(),
                items,
                enCours,
                enCours,
                activeWf != null ? activeWf.getId() : null,
                activeWf != null && activeWf.getStatut() != null ? activeWf.getStatut().name() : null,
                current != null && current.getOperationType() != null ? current.getOperationType() : (enCours ? "UPDATE" : null),
                current != null && current.getAttachments() != null ? current.getAttachments() : List.of()
        );
    }

    private BudgetPlanSmartOutput mapPlanSmart(BudgetPlanEntity p, long nombreItems, WorkflowInstanceEntity activeWf) {
        boolean enCours = activeWf != null;
        WorkflowCurrentDataResponse current = (enCours && workflowStateService != null)
                ? workflowStateService.getCachedCurrentData(activeWf)
                : null;
        return new BudgetPlanSmartOutput(
                p.getId(),
                p.getAnnee(),
                p.getIntitule(),
                p.getVersion(),
                p.getStatut(),
                p.getTotalBudget(),
                (int) nombreItems,
                p.getCreeLe(),
                p.getCreePar(),
                p.getModifieLe(),
                p.getModifiePar(),
                enCours,
                enCours,
                activeWf != null ? activeWf.getId() : null,
                activeWf != null && activeWf.getStatut() != null ? activeWf.getStatut().name() : null,
                current != null && current.getOperationType() != null ? current.getOperationType() : (enCours ? "UPDATE" : null),
                current != null && current.getAttachments() != null ? current.getAttachments() : List.of()
        );
    }

    private BudgetPlanOutput buildDraftPlanFromWorkflow(
            UUID planId,
            WorkflowInstanceEntity wf,
            Map<String, Object> data,
            WorkflowCurrentDataResponse current
    ) {
        Integer annee = asInteger(data.get("annee"));
        Integer version = asInteger(data.get("version"));
        UUID creePar = parseUuidSafe(data.get("utilisateurId") != null ? data.get("utilisateurId").toString() : null);
        LocalDateTime creeLe = wf != null && wf.getDateInitiation() != null
                ? LocalDateTime.ofInstant(wf.getDateInitiation(), java.time.ZoneId.systemDefault())
                : LocalDateTime.now();
        return new BudgetPlanOutput(
                planId,
                annee != null ? annee : LocalDateTime.now().getYear(),
                data.get("intitule") != null ? data.get("intitule").toString() : "Plan budgétaire en cours",
                version != null ? version : 1,
                resolveStatutBudget(data.get("statut"), StatutBudget.DRAFT),
                BigDecimal.ZERO,
                creeLe,
                creePar,
                null,
                null,
                new ArrayList<>(),
                true,
                wf != null,
                wf != null ? wf.getId() : null,
                wf != null && wf.getStatut() != null ? wf.getStatut().name() : "EN_COURS",
                current != null && current.getOperationType() != null ? current.getOperationType() : "CREATE",
                current != null && current.getAttachments() != null ? current.getAttachments() : List.of()
        );
    }

    private BudgetPlanSmartOutput buildDraftSmartPlanFromWorkflow(
            UUID planId,
            WorkflowInstanceEntity wf,
            Map<String, Object> data,
            WorkflowCurrentDataResponse current
    ) {
        Integer annee = asInteger(data.get("annee"));
        Integer version = asInteger(data.get("version"));
        UUID creePar = parseUuidSafe(data.get("utilisateurId") != null ? data.get("utilisateurId").toString() : null);
        LocalDateTime creeLe = wf != null && wf.getDateInitiation() != null
                ? LocalDateTime.ofInstant(wf.getDateInitiation(), java.time.ZoneId.systemDefault())
                : LocalDateTime.now();
        return new BudgetPlanSmartOutput(
                planId,
                annee != null ? annee : LocalDateTime.now().getYear(),
                data.get("intitule") != null ? data.get("intitule").toString() : "Plan budgétaire en cours",
                version != null ? version : 1,
                resolveStatutBudget(data.get("statut"), StatutBudget.DRAFT),
                BigDecimal.ZERO,
                0,
                creeLe,
                creePar,
                null,
                null,
                true,
                wf != null,
                wf != null ? wf.getId() : null,
                wf != null && wf.getStatut() != null ? wf.getStatut().name() : "EN_COURS",
                current != null && current.getOperationType() != null ? current.getOperationType() : "CREATE",
                current != null && current.getAttachments() != null ? current.getAttachments() : List.of()
        );
    }

    private Integer asInteger(Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(val.toString().trim());
        } catch (NumberFormatException e) {
            return null;
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

    private StatutBudget resolveStatutBudget(Object raw, StatutBudget fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return StatutBudget.valueOf(raw.toString().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private BudgetItemOutput mapItem(BudgetItemEntity i) {
        return new BudgetItemOutput(
                i.getId(),
                i.getCompteCode(),
                i.getSectionId(),
                i.getMontantAnnuel(),
                i.getMontantPlanned(),
                i.getMontantEngage(),
                i.getMontantReal()
        );
    }

    private BudgetEngagementOutput mapEngagement(BudgetEngagementEntity e) {
        return new BudgetEngagementOutput(
                e.getId(),
                e.getBudgetItem().getId(),
                e.getNumeroEngagement(),
                e.getDescription(),
                e.getMontant(),
                e.getDateEngagement(),
                e.getStatut()
        );
    }
}
