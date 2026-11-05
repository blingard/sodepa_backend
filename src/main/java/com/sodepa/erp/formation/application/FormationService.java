package com.sodepa.erp.formation.application;

import com.sodepa.erp.configuration.parametres.application.ParametreConfigurationService;
import com.sodepa.erp.formation.infrastructure.entities.*;
import com.sodepa.erp.formation.infrastructure.repo.*;
import com.sodepa.erp.formation.presentation.requests.*;
import com.sodepa.erp.formation.presentation.responses.*;
import com.sodepa.erp.rh.infrastructure.entities.EmployeEntity;
import com.sodepa.erp.rh.infrastructure.entities.GrilleSalarialeEntity;
import com.sodepa.erp.rh.infrastructure.repo.EmployeRepository;
import com.sodepa.erp.rh.infrastructure.repo.GrilleSalarialeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FormationService {

    private final FormationPlanRepository planRepository;
    private final FormationActionRepository actionRepository;
    private final FormationParticipationRepository participationRepository;
    private final StageRepository stageRepository;
    private final FormationReclassementRepository reclassementRepository;
    private final EmployeRepository employeRepository;
    private final GrilleSalarialeRepository grilleSalarialeRepository;
    private final ParametreConfigurationService configService;

    // ==========================================
    // 1. PLANS ANNUELS DE FORMATION N+1
    // ==========================================

    @Transactional(readOnly = true)
    public List<FormationPlanResponse> listerPlans() {
        return planRepository.findAll(Sort.by(Sort.Direction.DESC, "anneeExercice"))
                .stream()
                .map(FormationPlanResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FormationPlanResponse obtenirPlan(UUID id) {
        return planRepository.findById(id)
                .map(FormationPlanResponse::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("Plan de formation introuvable: " + id));
    }

    @Transactional
    public FormationPlanResponse creerPlan(CreerPlanFormationRequest request) {
        if (planRepository.findByAnneeExercice(request.getAnneeExercice()).isPresent()) {
            throw new IllegalArgumentException("Un plan de formation existe déjà pour l'exercice " + request.getAnneeExercice());
        }

        FormationPlanEntity plan = FormationPlanEntity.builder()
                .id(UUID.randomUUID())
                .anneeExercice(request.getAnneeExercice())
                .intitule(request.getIntitule())
                .budgetTotalPrevu(request.getBudgetTotalPrevu() != null ? request.getBudgetTotalPrevu() : BigDecimal.ZERO)
                .statut(StatutPlanFormation.BROUILLON)
                .visaSdrh(false)
                .visaDaf(false)
                .approbationDg(false)
                .observations(request.getObservations())
                .actions(new ArrayList<>())
                .build();

        return FormationPlanResponse.fromEntity(planRepository.save(plan));
    }

    @Transactional
    public FormationActionResponse ajouterAction(CreerActionFormationRequest request) {
        FormationPlanEntity plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Plan de formation introuvable: " + request.getPlanId()));

        long count = actionRepository.count() + 1;
        String code = String.format("ACT-FORM-%d-%03d", plan.getAnneeExercice(), count);

        FormationActionEntity action = FormationActionEntity.builder()
                .id(UUID.randomUUID())
                .plan(plan)
                .code(code)
                .theme(request.getTheme())
                .domaine(request.getDomaine())
                .publicCible(request.getPublicCible())
                .organismeFormateur(request.getOrganismeFormateur())
                .typeFormateur(request.getTypeFormateur() != null ? request.getTypeFormateur() : "EXTERNE")
                .lieu(request.getLieu())
                .dateDebut(request.getDateDebut())
                .dateFin(request.getDateFin())
                .dureeHeures(request.getDureeHeures())
                .coutEstimatifFcfa(request.getCoutEstimatifFcfa() != null ? request.getCoutEstimatifFcfa() : BigDecimal.ZERO)
                .donneDroitReclassement(request.isDonneDroitReclassement())
                .participations(new ArrayList<>())
                .build();

        plan.getActions().add(action);
        // Mise à jour automatique du coût total estimatif du plan
        BigDecimal total = plan.getActions().stream()
                .map(FormationActionEntity::getCoutEstimatifFcfa)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        plan.setBudgetTotalPrevu(total);
        planRepository.save(plan);

        return FormationActionResponse.fromEntity(actionRepository.save(action));
    }

    @Transactional
    public FormationPlanResponse viserSdrh(UUID planId) {
        FormationPlanEntity plan = getPlanOrThrow(planId);
        plan.setVisaSdrh(true);
        plan.setStatut(StatutPlanFormation.SOUMIS_DAF);
        return FormationPlanResponse.fromEntity(planRepository.save(plan));
    }

    @Transactional
    public FormationPlanResponse viserDaf(UUID planId) {
        FormationPlanEntity plan = getPlanOrThrow(planId);
        plan.setVisaDaf(true);
        return FormationPlanResponse.fromEntity(planRepository.save(plan));
    }

    @Transactional
    public FormationPlanResponse approuverDg(UUID planId) {
        FormationPlanEntity plan = getPlanOrThrow(planId);
        plan.setApprobationDg(true);
        plan.setDateApprobationDg(LocalDate.now());
        plan.setStatut(StatutPlanFormation.VALIDE_DG);
        return FormationPlanResponse.fromEntity(planRepository.save(plan));
    }

    // ==========================================
    // 2. PARTICIPATION AUX FORMATIONS
    // ==========================================

    @Transactional
    public void inscrireEmploye(InscrireEmployeFormationRequest request) {
        FormationActionEntity action = actionRepository.findById(request.getActionId())
                .orElseThrow(() -> new IllegalArgumentException("Action de formation introuvable: " + request.getActionId()));

        EmployeEntity employe = employeRepository.findById(request.getEmployeId())
                .orElseThrow(() -> new IllegalArgumentException("Employé introuvable: " + request.getEmployeId()));

        FormationParticipationEntity part = FormationParticipationEntity.builder()
                .id(UUID.randomUUID())
                .action(action)
                .employe(employe)
                .noteEvaluationChaud(request.getNoteEvaluationChaud())
                .attestationDelivree(request.isAttestationDelivree())
                .avisHierarchique(request.getAvisHierarchique() != null ? request.getAvisHierarchique() : "FAVORABLE")
                .build();

        participationRepository.save(part);
    }

    // ==========================================
    // 3. STAGES ACADÉMIQUES ET PROFESSIONNELS
    // ==========================================

    @Transactional(readOnly = true)
    public Page<StageResponse> listerStages(StatutStage statut, Pageable pageable) {
        Page<StageEntity> page = statut != null
                ? stageRepository.findByStatut(statut, pageable)
                : stageRepository.findAll(pageable);
        return page.map(StageResponse::fromEntity);
    }

    @Transactional
    public StageResponse enregistrerStagiaire(EnregistrerStagiaireRequest request) {
        long count = stageRepository.count() + 1;
        int annee = request.getDateDebut().getYear();
        String code = String.format("STG-%d-%04d", annee, count);

        StageEntity stage = StageEntity.builder()
                .id(UUID.randomUUID())
                .codeDossier(code)
                .nomPrenomStagiaire(request.getNomPrenomStagiaire())
                .etablissementOrigine(request.getEtablissementOrigine())
                .niveauEtude(request.getNiveauEtude())
                .typeStage(request.getTypeStage())
                .structureAccueil(request.getStructureAccueil())
                .encadreurNom(request.getEncadreurNom())
                .dateDebut(request.getDateDebut())
                .dateFin(request.getDateFin())
                .lettreAdmissionRef(request.getLettreAdmissionRef() != null ? request.getLettreAdmissionRef() : "ADMIS-SODEPA-" + code)
                .statut(StatutStage.EN_COURS)
                .rallongeAccordee(false)
                .build();

        return StageResponse.fromEntity(stageRepository.save(stage));
    }

    @Transactional
    public StageResponse demanderRallongeStage(DemanderRallongeStageRequest request) {
        StageEntity stage = stageRepository.findById(request.getStageId())
                .orElseThrow(() -> new IllegalArgumentException("Stage introuvable: " + request.getStageId()));

        LocalDate dateDemande = LocalDate.now();
        LocalDate finPrevue = stage.getDateFin();

        // Règle formelle SODEPA : toute demande doit parvenir au moins 2 semaines (14 jours) avant la fin
        long joursRestants = ChronoUnit.DAYS.between(dateDemande, finPrevue);
        int delaiMinJours = configService.getInt("formation.delai_forclusion_stage_jours", 14);
        if (joursRestants < delaiMinJours) {
            stage.setStatut(StatutStage.REJETE);
            stageRepository.save(stage);
            throw new IllegalArgumentException(String.format(
                    "Demande de rallonge rejetée d'office selon le manuel de procédures SODEPA : " +
                    "déposée à %d jours de l'échéance (minimum obligatoire : %d jours / 2 semaines avant la fin).", joursRestants, delaiMinJours));
        }

        stage.setDateDemandeRallonge(dateDemande);
        stage.setStatut(StatutStage.RALLONGE_DEMANDEE);
        return StageResponse.fromEntity(stageRepository.save(stage));
    }

    @Transactional
    public StageResponse validerRallonge(UUID stageId, boolean accord, LocalDate nouvelleDateFin) {
        StageEntity stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Stage introuvable: " + stageId));

        if (accord) {
            stage.setRallongeAccordee(true);
            stage.setStatut(StatutStage.RALLONGE_ACCORDEE);
            if (nouvelleDateFin != null) {
                stage.setDateFin(nouvelleDateFin);
            }
        } else {
            stage.setRallongeAccordee(false);
            stage.setStatut(StatutStage.REJETE);
        }

        return StageResponse.fromEntity(stageRepository.save(stage));
    }

    @Transactional
    public StageResponse cloturerStage(UUID stageId) {
        StageEntity stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Stage introuvable: " + stageId));

        stage.setStatut(StatutStage.TERMINE);
        return StageResponse.fromEntity(stageRepository.save(stage));
    }

    // ==========================================
    // 4. RECLASSEMENTS INDICIAIRES POST-FORMATION
    // ==========================================

    @Transactional(readOnly = true)
    public List<FormationReclassementResponse> listerReclassements() {
        return reclassementRepository.findAll(Sort.by(Sort.Direction.DESC, "dateEffet"))
                .stream()
                .map(FormationReclassementResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public FormationReclassementResponse proposerReclassement(ProposerReclassementRequest request) {
        EmployeEntity employe = employeRepository.findById(request.getEmployeId())
                .orElseThrow(() -> new IllegalArgumentException("Employé introuvable: " + request.getEmployeId()));

        // Calcul de l'ancienneté réelle
        int ancienneteAnnees = Period.between(employe.getDateEmbauche(), LocalDate.now()).getYears();

        // Règle formelle SODEPA : Ancienneté >= 5 ans (configurable)
        int ancienneteMin = configService.getInt("formation.anciennete_min_reclassement_annees", 5);
        if (ancienneteAnnees < ancienneteMin) {
            throw new IllegalArgumentException(String.format(
                    "Reclassement non éligible : l'employé cumule %d ans d'ancienneté (minimum requis selon les procédures SODEPA : %d ans).", ancienneteAnnees, ancienneteMin));
        }

        // Règle formelle SODEPA : Durée de formation certifiante >= 18 mois (configurable)
        int dureeMinMois = configService.getInt("formation.duree_min_formation_reclassement_mois", 18);
        if (request.getDureeFormationMois() < dureeMinMois) {
            throw new IllegalArgumentException(String.format(
                    "Reclassement non éligible : la formation a duré %d mois (minimum requis donnant droit à reclassement : %d mois).", request.getDureeFormationMois(), dureeMinMois));
        }

        GrilleSalarialeEntity grilleActuelle = employe.getGrilleSalariale();
        int catActuelle = grilleActuelle != null ? grilleActuelle.getCategorie() : 1;
        String echActuel = grilleActuelle != null ? grilleActuelle.getEchelon() : "A";

        FormationReclassementEntity reclassement = FormationReclassementEntity.builder()
                .id(UUID.randomUUID())
                .employe(employe)
                .intituleDiplomeFormation(request.getIntituleDiplomeFormation())
                .dureeFormationMois(request.getDureeFormationMois())
                .ancienneteAnnees(ancienneteAnnees)
                .ancienneCategorie(catActuelle)
                .ancienEchelon(echActuel)
                .nouvelleCategorie(request.getNouvelleCategorie())
                .nouvelEchelon(request.getNouvelEchelon())
                .decisionDgRef(request.getDecisionDgRef())
                .dateEffet(request.getDateEffet() != null ? request.getDateEffet() : LocalDate.now())
                .valideDg(false)
                .build();

        return FormationReclassementResponse.fromEntity(reclassementRepository.save(reclassement));
    }

    @Transactional
    public FormationReclassementResponse validerReclassementDg(UUID reclassementId) {
        FormationReclassementEntity reclassement = reclassementRepository.findById(reclassementId)
                .orElseThrow(() -> new IllegalArgumentException("Dossier de reclassement introuvable: " + reclassementId));

        GrilleSalarialeEntity nouvelleGrille = grilleSalarialeRepository
                .findByCategorieAndEchelon(reclassement.getNouvelleCategorie(), reclassement.getNouvelEchelon())
                .orElseThrow(() -> new IllegalArgumentException(String.format("Échelon de grille introuvable : Catégorie %d, Échelon %s",
                        reclassement.getNouvelleCategorie(), reclassement.getNouvelEchelon())));

        EmployeEntity employe = reclassement.getEmploye();
        employe.setGrilleSalariale(nouvelleGrille);
        employeRepository.save(employe);

        reclassement.setValideDg(true);
        reclassement.setDecisionDgRef("DEC-DG-RECLASS-" + LocalDate.now().getYear() + "-" + employe.getMatricule());
        FormationReclassementEntity saved = reclassementRepository.save(reclassement);

        log.info("Reclassement validé par le DG pour l'employé {} : promu en Catégorie {} Échelon {} (Nouveau salaire base: {} FCFA)",
                employe.getMatricule(), nouvelleGrille.getCategorie(), nouvelleGrille.getEchelon(), nouvelleGrille.getSalaireBase());

        return FormationReclassementResponse.fromEntity(saved);
    }

    private FormationPlanEntity getPlanOrThrow(UUID id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan de formation introuvable: " + id));
    }
}
