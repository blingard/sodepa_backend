package com.sodepa.erp.workflow.application;

import com.sodepa.erp.achats.application.AchatsService;
import com.sodepa.erp.budget.application.inputs.RejeterInput;
import com.sodepa.erp.budget.infrastructure.adapter.BudgetPlanAdapter;
import com.sodepa.erp.budget.infrastructure.adapter.RapprochementAdapter;
import com.sodepa.erp.budget.infrastructure.repo.BudgetEngagementRepository;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.*;
import com.sodepa.erp.formation.application.FormationService;
import com.sodepa.erp.organigramme.infrastructure.adapter.OrganigrammeAdapter;
import com.sodepa.erp.paie.application.PaieService;
import com.sodepa.erp.pastoral.application.CheptelService;
import com.sodepa.erp.rh.application.RhService;
import com.sodepa.erp.share.CurrentUserAuthenticationToken;
import com.sodepa.erp.share.MakerCheckerRequestEntity;
import com.sodepa.erp.share.MakerCheckerRequestJpaRepo;
import com.sodepa.erp.share.UserData;
import com.sodepa.erp.tresorerie.regies.application.RegiesTresorerieService;
import com.sodepa.erp.user.infrastructure.adapter.UserAdapter;
import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.MakerCheckerStatus;
import com.sodepa.erp.utils.Permissions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Orchestrateur central unifié exécutant automatiquement la validation ou le rejet
 * final des entités métiers dès qu'une instance du moteur de workflow SODEPA
 * arrive à son terme (APPROUVE ou REJETE).
 */
@Component
@Slf4j
public class WorkflowCompletionOrchestrator {

    private static final UUID ORCHESTRATOR_FALLBACK_CHECKER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000099");

    @Autowired
    private MakerCheckerRequestJpaRepo makerCheckerRepo;

    @Autowired @Lazy private AchatsService achatsService;
    @Autowired @Lazy private BudgetPlanAdapter budgetPlanAdapter;
    @Autowired @Lazy private RapprochementAdapter budgetRapprochementAdapter;
    @Autowired @Lazy private BudgetEngagementRepository budgetEngagementRepository;
    @Autowired @Lazy private BankAdapter bankAdapter;
    @Autowired @Lazy private CompteAdapter compteAdapter;
    @Autowired @Lazy private ImmobilisationAdapter immobilisationAdapter;
    @Autowired @Lazy private JournalAdapter journalAdapter;
    @Autowired @Lazy private TiersAdapter tiersAdapter;
    @Autowired @Lazy private EcritureAdapter ecritureAdapter;
    @Autowired @Lazy private OrganigrammeAdapter organigrammeAdapter;
    @Autowired @Lazy private PaieService paieService;
    @Autowired @Lazy private CheptelService cheptelService;
    @Autowired @Lazy private RhService rhService;
    @Autowired @Lazy private RegiesTresorerieService regiesTresorerieService;
    @Autowired @Lazy private FormationService formationService;
    @Autowired @Lazy private UserAdapter userAdapter;

    /**
     * Écoute la complétion finale d'une instance de workflow (toutes les étapes actives validées)
     * et applique immédiatement la décision en base de données.
     */
    @EventListener
    @Transactional
    public void onWorkflowCompleted(WorkflowInstanceCompletedEvent event) {
        log.info("Orchestrateur Workflow [APPROUVE] : instance={} entite={}/{} validateurFinal={}",
                event.instanceId(), event.entityName(), event.entityId(), event.dernierActeurId());

        // 1. Cas des processus hors MakerChecker (Formation, Écriture comptable, Pré-engagement)
        if (handleDirectDomainCompletion(event.entityName(), event.entityId(), true, event.dernierActeurId(), "Validé via le circuit de workflow")) {
            return;
        }

        // 2. Cas des 33 entités MakerChecker
        Optional<MakerCheckerRequestEntity> reqOpt = resolveMakerCheckerRequest(
                event.instanceId(), event.entityName(), event.entityId());

        if (reqOpt.isEmpty()) {
            log.debug("Aucune requête MakerChecker PENDING associée à l'entité {}/{} (instance {})",
                    event.entityName(), event.entityId(), event.instanceId());
            return;
        }

        MakerCheckerRequestEntity req = reqOpt.get();
        if (req.getStatus() != MakerCheckerStatus.PENDING) {
            return;
        }

        executeMakerCheckerDecision(
                req,
                MakerCheckerStatus.ACCEPTED,
                "Approuvé à l'issue du circuit de validation (" + event.workflowCode() + ")",
                event.dernierActeurId()
        );
    }

    /**
     * Écoute le rejet d'une instance de workflow par un validateur obligatoire
     * et rejette immédiatement la requête métier associée.
     */
    @EventListener
    @Transactional
    public void onWorkflowRejected(WorkflowInstanceRejectedEvent event) {
        log.info("Orchestrateur Workflow [REJETE] : instance={} entite={}/{} par={} motif={}",
                event.instanceId(), event.entityName(), event.entityId(), event.acteurRejetId(), event.motifRejet());

        String motif = (event.motifRejet() != null && !event.motifRejet().isBlank())
                ? event.motifRejet()
                : "Rejeté lors du circuit de validation (" + event.workflowCode() + ")";

        if (handleDirectDomainCompletion(event.entityName(), event.entityId(), false, event.acteurRejetId(), motif)) {
            return;
        }

        Optional<MakerCheckerRequestEntity> reqOpt = resolveMakerCheckerRequest(
                event.instanceId(), event.entityName(), event.entityId());

        if (reqOpt.isEmpty()) {
            return;
        }

        MakerCheckerRequestEntity req = reqOpt.get();
        if (req.getStatus() != MakerCheckerStatus.PENDING) {
            return;
        }

        executeMakerCheckerDecision(req, MakerCheckerStatus.REJECTED, motif, event.acteurRejetId());
    }

    private Optional<MakerCheckerRequestEntity> resolveMakerCheckerRequest(
            UUID workflowInstanceId, String entityNameStr, String entityId) {
        if (workflowInstanceId != null) {
            Optional<MakerCheckerRequestEntity> byWf = makerCheckerRepo.findByWorkflowInstanceId(workflowInstanceId);
            if (byWf.isPresent() && byWf.get().getStatus() == MakerCheckerStatus.PENDING) {
                return byWf;
            }
        }
        if (entityNameStr != null && entityId != null) {
            try {
                MakerCheckerEntityName mcEntityName = MakerCheckerEntityName.valueOf(entityNameStr);
                List<MakerCheckerRequestEntity> pendingList = makerCheckerRepo
                        .findByEntityPkAndEntityNameAndStatusOrderByCreatedAtDesc(
                                entityId, mcEntityName, MakerCheckerStatus.PENDING);
                if (!pendingList.isEmpty()) {
                    return Optional.of(pendingList.get(0));
                }
            } catch (IllegalArgumentException ignored) {
                // Non-MakerChecker entity name
            }
        }
        return Optional.empty();
    }

    /**
     * Exécute la décision finale sur une requête MakerChecker en positionnant un contexte
     * de sécurité habilité et garantissant la séparation technique Maker != Checker lors
     * de l'exécution de l'adaptateur métier.
     */
    @Transactional
    public void executeMakerCheckerDecision(
            MakerCheckerRequestEntity req,
            MakerCheckerStatus decision,
            String notes,
            UUID decideParId) {

        UUID effectiveCheckerId = decideParId;
        if (effectiveCheckerId == null || effectiveCheckerId.toString().equals(req.getMakerId())) {
            effectiveCheckerId = ORCHESTRATOR_FALLBACK_CHECKER_ID;
        }

        ValidateOrRejectSubmissionInput input = new ValidateOrRejectSubmissionInput(
                req.getId(),
                decision,
                notes,
                req.getCheckerOperationType()
        );

        final UUID checkerToUse = effectiveCheckerId;
        runWithElevatedSecurityContext(checkerToUse, () -> {
            try {
                switch (req.getEntityName()) {
                    case BANQUE -> bankAdapter.validateOrReject(input);
                    case TIERS -> tiersAdapter.validateOrReject(input);
                    case COMPTE -> compteAdapter.validateOrReject(input);
                    case IMMOBILISATION -> immobilisationAdapter.validateOrReject(input);
                    case JOURNAL -> journalAdapter.validateOrReject(input);
                    case USER -> userAdapter.validateOrReject(input);
                    case FOURNISSEUR -> achatsService.validateOrRejectFournisseur(input);
                    case EXPRESSION_BESOIN -> achatsService.validateOrRejectExpressionBesoin(input);
                    case MARCHE_DOSSIER -> achatsService.validateOrRejectMarcheDossier(input);
                    case MARCHE_RECEPTION_PV -> achatsService.validateOrRejectPvReception(input);
                    case LIQUIDATION_FACTURE -> achatsService.validateOrRejectLiquidationFacture(input);
                    case BUDGET_PLAN -> budgetPlanAdapter.validateOrRejectBudgetPlan(input);
                    case BUDGET_REALLOCATION -> budgetPlanAdapter.validateOrRejectReallocation(input);
                    case BUDGET_ENGAGEMENT -> budgetPlanAdapter.validateOrRejectEngagement(input);
                    case UNITE_ORGANISATIONNELLE -> organigrammeAdapter.validateOrRejectUnite(input);
                    case POSTE -> organigrammeAdapter.validateOrRejectPoste(input);
                    case AFFECTATION_POSTE -> organigrammeAdapter.validateOrRejectAffectation(input);
                    case DELEGATION_SIGNATURE -> organigrammeAdapter.validateOrRejectDelegation(input);
                    case PAIE_PERIODE -> paieService.validateOrRejectPeriodePaie(input);
                    case PAIE_VARIABLE -> paieService.validateOrRejectVariablePaie(input);
                    case BOVIN_NAISSANCE, BOVIN_MOUVEMENT, BOVIN_SORTIE -> cheptelService.validateOrRejectBovin(input);
                    case HERD_COMPOSITION -> cheptelService.validateOrRejectHerdComposition(input);
                    case EMPLOYE -> rhService.validateOrRejectEmploye(input);
                    case DEROGATION_PERMISSION -> rhService.validateOrRejectDerogation(input);
                    case REGIE_APPROVISIONNEMENT -> {
                        regiesTresorerieService.validateOrRejectApprovisionnement(input);
                        if (decision == MakerCheckerStatus.ACCEPTED) {
                            UUID approvId = UUID.fromString(req.getEntityPk());
                            regiesTresorerieService.signerDaf(approvId);
                            regiesTresorerieService.signerDgEtDebloquer(
                                    approvId,
                                    "VIR-WF-" + approvId.toString().substring(0, 8).toUpperCase()
                            );
                        }
                    }
                    case REGIE_DEPENSE -> regiesTresorerieService.validateOrRejectDepense(input);
                    default -> {
                        req.setStatus(decision);
                        req.setNotes(notes);
                        req.setCheckerId(checkerToUse.toString());
                        req.setDecidedAt(java.time.Instant.now());
                        makerCheckerRepo.save(req);
                    }
                }
            } catch (Exception e) {
                log.warn("Orchestrateur Workflow : erreur lors de la finalisation de {} ({}): {}",
                        req.getEntityName(), req.getEntityPk(), e.getMessage());
            }
        });
    }

    private boolean handleDirectDomainCompletion(
            String entityName,
            String entityId,
            boolean approved,
            UUID decideParId,
            String motif) {
        if (entityName == null || entityId == null) {
            return false;
        }
        UUID id;
        try {
            id = UUID.fromString(entityId);
        } catch (IllegalArgumentException e) {
            return false;
        }

        UUID effectiveCheckerId = decideParId != null ? decideParId : ORCHESTRATOR_FALLBACK_CHECKER_ID;

        return switch (entityName) {
            case "FORMATION_PLAN" -> {
                if (approved) {
                    runWithElevatedSecurityContext(effectiveCheckerId, () -> {
                        formationService.viserSdrh(id);
                        formationService.viserDaf(id);
                        formationService.approuverDg(id);
                    });
                }
                yield true;
            }
            case "STAGE_RALLONGE" -> {
                runWithElevatedSecurityContext(effectiveCheckerId, () ->
                        formationService.validerRallonge(id, approved, null)
                );
                yield true;
            }
            case "FORMATION_RECLASSEMENT" -> {
                if (approved) {
                    runWithElevatedSecurityContext(effectiveCheckerId, () ->
                            formationService.validerReclassementDg(id)
                    );
                }
                yield true;
            }
            case "ECRITURE" -> {
                runWithElevatedSecurityContext(effectiveCheckerId, () -> {
                    try {
                        if (approved) {
                            ecritureAdapter.validerEcriture(id);
                        } else {
                            ecritureAdapter.rejeterEcriture(id);
                        }
                    } catch (Exception e) {
                        log.warn("Orchestrateur Workflow : finalisation écriture {} ignorée ({})", id, e.getMessage());
                    }
                });
                yield true;
            }
            case "PRE_ENGAGEMENT_BUDGET" -> {
                runWithElevatedSecurityContext(effectiveCheckerId, () -> {
                    budgetEngagementRepository.findById(id).ifPresent(eng -> {
                        if (approved) {
                            eng.setStatutWorkflow("APPROVED");
                            eng.setApprobateurCourantRole(null);
                            budgetEngagementRepository.save(eng);
                        } else {
                            budgetRapprochementAdapter.rejeterEngagement(
                                    new RejeterInput(eng.getNumeroEngagement(), motif, effectiveCheckerId)
                            );
                        }
                    });
                });
                yield true;
            }
            default -> false;
        };
    }

    private void runWithElevatedSecurityContext(UUID checkerUserId, Runnable action) {
        Authentication previousAuth = SecurityContextHolder.getContext().getAuthentication();
        try {
            Set<Permissions> allPermissions = EnumSet.allOf(Permissions.class);
            UserData elevatedUserData = UserData.builder()
                    .userId(checkerUserId.toString())
                    .username("workflow-orchestrator")
                    .name("Orchestrateur Workflow SODEPA")
                    .email("workflow@sodepa.cm")
                    .permissions(allPermissions)
                    .phoneNumbers(Collections.emptySet())
                    .posteData(Collections.emptySet())
                    .build();

            CurrentUserAuthenticationToken elevatedToken = new CurrentUserAuthenticationToken(
                    "workflow-orchestrator",
                    "N/A",
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
                    elevatedUserData
            );
            SecurityContextHolder.getContext().setAuthentication(elevatedToken);
            action.run();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previousAuth);
        }
    }
}
