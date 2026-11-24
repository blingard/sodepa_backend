package com.sodepa.erp.budgetaire.structure.infrastructure.adapter;

import com.sodepa.erp.budgetaire.commun.CircuitReferentielBudgetaire;
import com.sodepa.erp.budgetaire.commun.DemandeBudgetaireOutput;
import com.sodepa.erp.budgetaire.commun.OperationReferentiel;
import com.sodepa.erp.budgetaire.commun.ProjectionDemande;
import com.sodepa.erp.budgetaire.commun.UsageReferentiels;
import com.sodepa.erp.budgetaire.structure.application.inputs.ChangerEtatStructureInput;
import com.sodepa.erp.budgetaire.structure.application.inputs.CreerStructureInput;
import com.sodepa.erp.budgetaire.structure.application.inputs.DeciderDemandeStructureInput;
import com.sodepa.erp.budgetaire.structure.application.inputs.ListerStructuresInput;
import com.sodepa.erp.budgetaire.structure.application.inputs.ModifierStructureInput;
import com.sodepa.erp.budgetaire.structure.application.outputs.StructureBudgetaireOutput;
import com.sodepa.erp.budgetaire.structure.infrastructure.entities.StructureBudgetaireEntity;
import com.sodepa.erp.budgetaire.structure.infrastructure.event.DemandeStructureEvent;
import com.sodepa.erp.budgetaire.structure.infrastructure.repo.StructureBudgetaireRepository;
import com.sodepa.erp.organigramme.infrastructure.entities.AffectationPosteEntity;
import com.sodepa.erp.organigramme.infrastructure.entities.PosteEntity;
import com.sodepa.erp.organigramme.infrastructure.entities.StatutAffectation;
import com.sodepa.erp.organigramme.infrastructure.entities.StatutPoste;
import com.sodepa.erp.organigramme.infrastructure.entities.StatutUnite;
import com.sodepa.erp.organigramme.infrastructure.entities.UniteOrganisationnelleEntity;
import com.sodepa.erp.organigramme.infrastructure.repo.AffectationPosteRepository;
import com.sodepa.erp.organigramme.infrastructure.repo.PosteRepository;
import com.sodepa.erp.organigramme.infrastructure.repo.UniteOrganisationnelleRepository;
import com.sodepa.erp.share.MakerCheckerRequestEntity;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.share.erreurs.ConflitMetierException;
import com.sodepa.erp.share.erreurs.RequeteInvalideException;
import com.sodepa.erp.share.erreurs.RessourceIntrouvableException;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.MakerCheckerStatus;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.utils.Permissions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Règles des structures budgétaires (US-BUD-107).
 *
 * <p>
 * Une structure se choisit dans l'organigramme : son unité, le poste de son
 * ordonnateur et son service d'engagement sont des objets de l'organigramme,
 * jamais des libellés saisis. Toute écriture passe par le maker-checker, et
 * chaque règle est refaite à la validation, parce que l'organigramme a pu
 * changer entre-temps (unité fermée, poste supprimé, autre structure déclarée).
 * </p>
 *
 * <p>
 * L'identifiant d'une structure dérive de son unité ({@link #identifiantPour}) :
 * deux demandes pour la même unité portent sur la même entité, et une seule
 * demande à la fois peut attendre sur une structure.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StructureBudgetaireAdapter {

    private final StructureBudgetaireRepository structureRepository;
    private final UniteOrganisationnelleRepository uniteRepository;
    private final PosteRepository posteRepository;
    private final AffectationPosteRepository affectationRepository;
    private final CircuitReferentielBudgetaire circuit;
    private final UtilsService utilsService;
    private final UsageReferentiels usageReferentiels;
    private final Clock horloge;

    /** Identifiant d'une structure, déduit de son unité. */
    public static UUID identifiantPour(UUID uniteId) {
        return UUID.nameUUIDFromBytes(("sodepa-budget-structure-" + uniteId).getBytes(StandardCharsets.UTF_8));
    }

    // ─── Demandes ───────────────────────────────────────────────────────────

    /** Soumet la déclaration d'une unité comme structure budgétaire. */
    @Transactional
    public SubmissionOutput initCreer(CreerStructureInput input) {
        utilsService.hasPermission(Permissions.INIT_CREATE_BUDGET_STRUCTURE);

        UniteOrganisationnelleEntity unite = uniteDeclarable(input.uniteId());
        if (structureRepository.existsByUniteId(unite.getId())) {
            throw structureExistante(unite);
        }
        UUID structureId = identifiantPour(unite.getId());
        controlerOrdonnateur(unite, input.ordonnateurPosteId());
        controlerService(unite, input.serviceEngagementUniteId(), structureId);
        controlerEnglobement(unite, structureId);

        refuserSiDemandeEnAttente(structureId, unite);
        return soumettre(structureId, DemandeStructureEvent.creation(
                unite.getId(), input.ordonnateurPosteId(), input.serviceEngagementUniteId()), unite);
    }

    /** Soumet le changement d'ordonnateur ou de service d'engagement. */
    @Transactional
    public SubmissionOutput initModifier(ModifierStructureInput input) {
        utilsService.hasPermission(Permissions.INIT_UPDATE_BUDGET_STRUCTURE);

        StructureBudgetaireEntity structure = trouver(input.id());
        UniteOrganisationnelleEntity unite = uniteDe(structure);
        exigerActive(structure, unite);
        uniteActive(unite);
        controlerOrdonnateur(unite, input.ordonnateurPosteId());
        controlerService(unite, input.serviceEngagementUniteId(), structure.getId());
        if (Objects.equals(input.ordonnateurPosteId(), structure.getOrdonnateurPosteId())
                && Objects.equals(input.serviceEngagementUniteId(), structure.getServiceEngagementUniteId())) {
            throw new RequeteInvalideException("AUCUNE_MODIFICATION",
                    "La demande ne change rien à la structure " + unite.getIntitule() + ".");
        }

        refuserSiDemandeEnAttente(structure.getId(), unite);
        return soumettre(structure.getId(), DemandeStructureEvent.modification(
                input.ordonnateurPosteId(), input.serviceEngagementUniteId()), unite);
    }

    /** Soumet la désactivation d'une structure ; le motif est obligatoire. */
    @Transactional
    public SubmissionOutput initDesactiver(ChangerEtatStructureInput input) {
        utilsService.hasPermission(Permissions.INIT_UPDATE_BUDGET_STRUCTURE);

        String motif = facultatif(input.motif());
        if (motif == null) {
            throw new RequeteInvalideException("MOTIF_OBLIGATOIRE", "Indiquez le motif de la désactivation.");
        }
        StructureBudgetaireEntity structure = trouver(input.id());
        UniteOrganisationnelleEntity unite = uniteDe(structure);
        exigerActive(structure, unite);
        controlerDesactivable(structure, unite);

        refuserSiDemandeEnAttente(structure.getId(), unite);
        return soumettre(structure.getId(),
                DemandeStructureEvent.changementEtat(OperationReferentiel.DESACTIVER, motif), unite);
    }

    /**
     * Soumet la réactivation d'une structure.
     *
     * <p>L'organigramme a pu changer depuis la désactivation : l'unité, le
     * poste et le service sont contrôlés comme à la déclaration.</p>
     */
    @Transactional
    public SubmissionOutput initReactiver(ChangerEtatStructureInput input) {
        utilsService.hasPermission(Permissions.INIT_UPDATE_BUDGET_STRUCTURE);

        StructureBudgetaireEntity structure = trouver(input.id());
        UniteOrganisationnelleEntity unite = uniteDe(structure);
        controlerReactivable(structure, unite);

        refuserSiDemandeEnAttente(structure.getId(), unite);
        return soumettre(structure.getId(),
                DemandeStructureEvent.changementEtat(OperationReferentiel.REACTIVER, facultatif(input.motif())), unite);
    }

    /**
     * Tranche une demande portant sur une structure.
     *
     * <p>
     * Mêmes garde-fous que pour l'exercice et la nomenclature. Si une règle
     * n'est plus respectée au moment de valider, la validation est refusée et
     * la demande reste en attente : le validateur la rejette avec le motif affiché.
     * </p>
     */
    @Transactional
    public void decider(DeciderDemandeStructureInput input) {
        utilsService.hasPermission(Permissions.VALIDATE_OR_REJECT_BUDGET_STRUCTURE);

        CircuitReferentielBudgetaire.DecisionOuverte decision = circuit.ouvrirDecision(
                MakerCheckerEntityName.BUDGET_STRUCTURE, input.requestId(), input.decision(), input.notes(),
                "Aucune demande sur une structure budgétaire ne porte cet identifiant.");
        if (decision.acceptee()) {
            appliquer(decision.demande(), decision.validateur());
        }
        circuit.cloturer(decision, input.notes());
    }

    // ─── Lectures ───────────────────────────────────────────────────────────

    /** Structures, de la plus ancienne déclaration à la plus récente. */
    @Transactional(readOnly = true)
    public PageRecord<StructureBudgetaireOutput> lister(ListerStructuresInput input) {
        utilsService.hasPermission(Permissions.GET_BUDGET_STRUCTURE_INFO);

        Pageable page = CircuitReferentielBudgetaire.page(input.pageable(), Sort.by(Sort.Direction.ASC, "creeLe"));
        Page<StructureBudgetaireEntity> structures = input.inclureInactives()
                ? structureRepository.findAll(page)
                : structureRepository.findAllByActifTrue(page);

        Set<UUID> uniteIds = new HashSet<>();
        structures.forEach(s -> {
            uniteIds.add(s.getUniteId());
            uniteIds.add(s.getServiceEngagementUniteId());
        });
        Map<UUID, UniteOrganisationnelleEntity> unites = parId(uniteRepository.findAllById(uniteIds),
                UniteOrganisationnelleEntity::getId);
        Map<UUID, PosteEntity> postes = parId(posteRepository.findAllById(
                structures.map(StructureBudgetaireEntity::getOrdonnateurPosteId).toSet()), PosteEntity::getId);
        return CircuitReferentielBudgetaire.versPageRecord(structures.map(s -> sortie(s, unites, postes)));
    }

    /** Fiche d'une structure, avec le titulaire actuel du poste d'ordonnateur. */
    @Transactional(readOnly = true)
    public StructureBudgetaireOutput consulter(UUID id) {
        utilsService.hasPermission(Permissions.GET_BUDGET_STRUCTURE_INFO);

        return fiche(trouver(id));
    }

    /** Demandes en attente que l'appelant peut trancher (les siennes exclues), avec leur contenu. */
    @Transactional(readOnly = true)
    public PageRecord<DemandeBudgetaireOutput<StructureBudgetaireOutput>> demandesEnAttente(Pageable pageable) {
        utilsService.hasPermission(Permissions.VALIDATE_OR_REJECT_BUDGET_STRUCTURE);

        return circuit.demandesEnAttente(MakerCheckerEntityName.BUDGET_STRUCTURE, pageable, this::projeter);
    }

    /**
     * La structure telle que la demande la laisserait, et telle qu'elle est,
     * avec l'unité, le poste d'ordonnateur, son titulaire et le service
     * d'engagement résolus. Aucune règle n'est rejouée : elles le seront à la
     * validation.
     */
    private ProjectionDemande<StructureBudgetaireOutput> projeter(MakerCheckerRequestEntity demande) {
        DemandeStructureEvent charge = DemandeStructureEvent.depuisCharge(demande.getPayload());
        UUID structureId = UUID.fromString(demande.getEntityPk());
        if (charge.operation() == OperationReferentiel.CREER) {
            return ProjectionDemande.creation(fiche(nouvelleStructure(demande, charge, structureId)));
        }
        StructureBudgetaireEntity structure = trouver(structureId);
        StructureBudgetaireEntity projetee = structure.toBuilder().build();
        reporter(projetee, charge);
        return new ProjectionDemande<>(charge.operation(), charge.motif(), fiche(projetee), fiche(structure));
    }

    // ─── Application d'une demande validée ──────────────────────────────────

    private void appliquer(MakerCheckerRequestEntity demande, String validateur) {
        DemandeStructureEvent charge = DemandeStructureEvent.depuisCharge(demande.getPayload());
        UUID structureId = UUID.fromString(demande.getEntityPk());
        if (charge.operation() == OperationReferentiel.CREER) {
            creer(demande, charge, structureId, validateur);
            return;
        }

        StructureBudgetaireEntity structure = trouver(structureId);
        UniteOrganisationnelleEntity unite = uniteDe(structure);
        switch (charge.operation()) {
            case MODIFIER -> {
                exigerActive(structure, unite);
                uniteActive(unite);
                controlerOrdonnateur(unite, charge.ordonnateurPosteId());
                controlerService(unite, charge.serviceEngagementUniteId(), structure.getId());
            }
            case DESACTIVER -> {
                exigerActive(structure, unite);
                controlerDesactivable(structure, unite);
            }
            case REACTIVER -> controlerReactivable(structure, unite);
            default -> throw new IllegalStateException("Opération déjà traitée : " + charge.operation());
        }
        reporter(structure, charge);
        structure.setModifiePar(demande.getMakerId());
        structure.setModifieLe(horloge.instant());
        try {
            structureRepository.saveAndFlush(structure);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ConflitMetierException("STRUCTURE_MODIFIEE_ENTRE_TEMPS",
                    "La structure " + unite.getIntitule() + " vient d'être modifiée : rechargez-la avant de décider.");
        }
        log.info("Structures budgétaires : {} de {} appliquée (demande {}).",
                charge.operation(), unite.getCode(), demande.getId());
    }

    private void creer(MakerCheckerRequestEntity demande, DemandeStructureEvent charge, UUID structureId,
                       String validateur) {
        UniteOrganisationnelleEntity unite = uniteDeclarable(charge.uniteId());
        if (structureRepository.existsByUniteId(unite.getId())) {
            throw structureExistante(unite);
        }
        controlerOrdonnateur(unite, charge.ordonnateurPosteId());
        controlerService(unite, charge.serviceEngagementUniteId(), structureId);
        controlerEnglobement(unite, structureId);

        StructureBudgetaireEntity structure = nouvelleStructure(demande, charge, structureId);
        structure.setValidePar(validateur);
        structure.setValideLe(horloge.instant());
        try {
            structureRepository.saveAndFlush(structure);
        } catch (DataIntegrityViolationException e) {
            throw structureExistante(unite);
        }
        log.info("Structures budgétaires : {} déclarée (demande {}).", unite.getCode(), demande.getId());
    }

    /** La structure que crée la demande, avant validation ; sert aussi à l'aperçu du valideur. */
    private static StructureBudgetaireEntity nouvelleStructure(MakerCheckerRequestEntity demande,
                                                               DemandeStructureEvent charge, UUID structureId) {
        return StructureBudgetaireEntity.builder()
                .id(structureId)
                .uniteId(charge.uniteId())
                .ordonnateurPosteId(charge.ordonnateurPosteId())
                .serviceEngagementUniteId(charge.serviceEngagementUniteId())
                .actif(true)
                .demandeCreationId(demande.getId())
                .creePar(demande.getMakerId())
                .creeLe(demande.getCreatedAt())
                .build();
    }

    /**
     * Reporte sur la structure ce que change une demande autre qu'une
     * création. Partagé par l'application et l'aperçu : le valideur voit
     * exactement ce qui sera écrit.
     */
    private static void reporter(StructureBudgetaireEntity structure, DemandeStructureEvent charge) {
        switch (charge.operation()) {
            case MODIFIER -> {
                structure.setOrdonnateurPosteId(charge.ordonnateurPosteId());
                structure.setServiceEngagementUniteId(charge.serviceEngagementUniteId());
            }
            case DESACTIVER -> {
                structure.setActif(false);
                structure.setMotifDesactivation(charge.motif());
            }
            case REACTIVER -> {
                structure.setActif(true);
                structure.setMotifDesactivation(null);
            }
            default -> throw new IllegalStateException("Opération sans report : " + charge.operation());
        }
    }

    // ─── Règles ─────────────────────────────────────────────────────────────

    /** L'unité doit exister dans l'organigramme et y être active. */
    private UniteOrganisationnelleEntity uniteDeclarable(UUID uniteId) {
        UniteOrganisationnelleEntity unite = Optional.ofNullable(uniteId).flatMap(uniteRepository::findById)
                .orElseThrow(() -> new RequeteInvalideException("UNITE_INVALIDE",
                        "La structure se choisit dans l'organigramme : cette unité n'y figure pas."));
        uniteActive(unite);
        return unite;
    }

    private static void uniteActive(UniteOrganisationnelleEntity unite) {
        if (unite.getStatut() != StatutUnite.ACTIF) {
            throw new ConflitMetierException("UNITE_INACTIVE",
                    "L'unité " + unite.getIntitule() + " n'est pas active dans l'organigramme.");
        }
    }

    /**
     * L'ordonnateur est un poste existant de l'unité elle-même : le DG pour la
     * Direction Générale, le directeur pour un ranch. Un poste vacant est
     * accepté : la vacance est souvent temporaire, et le routage d'une
     * dépense (lot B2) la refusera le moment venu.
     */
    private void controlerOrdonnateur(UniteOrganisationnelleEntity unite, UUID posteId) {
        if (posteId == null) {
            throw new RequeteInvalideException("ORDONNATEUR_INVALIDE", "Désignez le poste de l'ordonnateur.");
        }
        PosteEntity poste = posteRepository.findById(posteId)
                .orElseThrow(() -> new RequeteInvalideException("ORDONNATEUR_INVALIDE",
                        "Le poste désigné comme ordonnateur n'existe pas dans l'organigramme."));
        if (poste.getStatut() == StatutPoste.SUPPRIME) {
            throw new RequeteInvalideException("ORDONNATEUR_INVALIDE",
                    "Le poste « " + poste.getIntitule() + " » a été supprimé de l'organigramme.");
        }
        if (!unite.getId().equals(poste.getUniteOrganisationnelle().getId())) {
            throw new RequeteInvalideException("ORDONNATEUR_INVALIDE",
                    "L'ordonnateur de la structure « " + unite.getIntitule() + " » est un poste de cette unité : « "
                            + poste.getIntitule() + " » relève d'une autre unité.");
        }
    }

    /**
     * Le service d'engagement est une unité active du sous-arbre de la
     * structure, distincte d'elle, et qui ne relève pas d'une sous-structure
     * ayant son propre budget.
     */
    private void controlerService(UniteOrganisationnelleEntity unite, UUID serviceId, UUID structureId) {
        if (serviceId == null) {
            throw new RequeteInvalideException("SERVICE_ENGAGEMENT_INVALIDE", "Désignez le service d'engagement.");
        }
        UniteOrganisationnelleEntity service = uniteRepository.findById(serviceId)
                .orElseThrow(() -> new RequeteInvalideException("SERVICE_ENGAGEMENT_INVALIDE",
                        "Le service d'engagement se choisit dans l'organigramme : cette unité n'y figure pas."));
        if (service.getStatut() != StatutUnite.ACTIF) {
            throw new RequeteInvalideException("SERVICE_ENGAGEMENT_INVALIDE",
                    "Le service " + service.getIntitule() + " n'est pas actif dans l'organigramme.");
        }
        if (service.getId().equals(unite.getId())) {
            throw new RequeteInvalideException("SERVICE_ENGAGEMENT_INVALIDE",
                    "Le service d'engagement est une unité distincte de la structure « " + unite.getIntitule()
                            + " », qui en dépend : par exemple son service administratif et financier.");
        }
        if (!sousUnite(service, unite)) {
            throw new RequeteInvalideException("SERVICE_ENGAGEMENT_INVALIDE",
                    "Le service d'engagement de la structure « " + unite.getIntitule() + " » est une unité qui en dépend : « "
                            + service.getIntitule() + " » n'en relève pas.");
        }
        for (Map.Entry<StructureBudgetaireEntity, UniteOrganisationnelleEntity> autre : autresStructures(structureId).entrySet()) {
            UniteOrganisationnelleEntity uniteAutre = autre.getValue();
            if (sousUnite(uniteAutre, unite) && enDessousOuEgal(service, uniteAutre)) {
                throw new RequeteInvalideException("SERVICE_ENGAGEMENT_INVALIDE",
                        "Le service « " + service.getIntitule() + " » relève de la structure « " + uniteAutre.getIntitule()
                                + " », qui a son propre budget.");
            }
        }
    }

    /**
     * Déclarer une unité ne doit pas faire passer dans son périmètre le service
     * d'engagement d'une structure qui la contient : celle-ci perdrait son
     * service (par exemple, déclarer la Sous-Direction FBM ferait passer le
     * Service du Budget de la Direction Générale dans son budget).
     */
    private void controlerEnglobement(UniteOrganisationnelleEntity unite, UUID structureId) {
        Map<StructureBudgetaireEntity, UniteOrganisationnelleEntity> autres = autresStructures(structureId);
        if (autres.isEmpty()) {
            return;
        }
        Map<UUID, UniteOrganisationnelleEntity> services = parId(uniteRepository.findAllById(
                autres.keySet().stream().map(StructureBudgetaireEntity::getServiceEngagementUniteId).toList()),
                UniteOrganisationnelleEntity::getId);
        for (Map.Entry<StructureBudgetaireEntity, UniteOrganisationnelleEntity> autre : autres.entrySet()) {
            UniteOrganisationnelleEntity service = services.get(autre.getKey().getServiceEngagementUniteId());
            if (service != null && sousUnite(unite, autre.getValue()) && enDessousOuEgal(service, unite)) {
                throw new ConflitMetierException("STRUCTURE_ENGLOBE_UN_SERVICE",
                        "Déclarer « " + unite.getIntitule() + " » ferait passer dans son budget « " + service.getIntitule()
                                + " », service d'engagement de la structure « " + autre.getValue().getIntitule() + " ».");
            }
        }
    }

    /** Une structure qui porte du budget en cours d'exécution ne se désactive pas. */
    private void controlerDesactivable(StructureBudgetaireEntity structure, UniteOrganisationnelleEntity unite) {
        if (usageReferentiels.structureUtilisee(structure.getId())) {
            throw new ConflitMetierException("STRUCTURE_UTILISEE",
                    "La structure " + unite.getIntitule() + " porte des lignes d'un exercice en cours d'exécution : "
                            + "elle ne pourra être désactivée qu'après la clôture de l'exercice.");
        }
    }

    private void controlerReactivable(StructureBudgetaireEntity structure, UniteOrganisationnelleEntity unite) {
        if (structure.isActif()) {
            throw new ConflitMetierException("STRUCTURE_DEJA_ACTIVE",
                    "La structure " + unite.getIntitule() + " est déjà active.");
        }
        uniteActive(unite);
        controlerOrdonnateur(unite, structure.getOrdonnateurPosteId());
        controlerService(unite, structure.getServiceEngagementUniteId(), structure.getId());
        controlerEnglobement(unite, structure.getId());
    }

    private static void exigerActive(StructureBudgetaireEntity structure, UniteOrganisationnelleEntity unite) {
        if (!structure.isActif()) {
            throw new ConflitMetierException("STRUCTURE_INACTIVE",
                    "La structure " + unite.getIntitule() + " est désactivée.");
        }
    }

    /** Une seule demande à la fois par structure, comme pour la nomenclature. */
    private void refuserSiDemandeEnAttente(UUID structureId, UniteOrganisationnelleEntity unite) {
        circuit.refuserSiDemandeEnAttente(MakerCheckerEntityName.BUDGET_STRUCTURE, structureId, "STRUCTURE_DEMANDE_EN_ATTENTE",
                "Une demande sur la structure " + unite.getIntitule() + " attend déjà sa validation.");
    }

    private SubmissionOutput soumettre(UUID structureId, DemandeStructureEvent demande, UniteOrganisationnelleEntity unite) {
        UUID demandeId = circuit.soumettre(MakerCheckerEntityName.BUDGET_STRUCTURE, structureId, demande.versCharge(),
                demande.operation().typeMakerChecker());
        log.info("Structures budgétaires : {} de {} soumise (demande {}).", demande.operation(), unite.getCode(), demandeId);
        return new SubmissionOutput(demandeId, structureId);
    }

    // ─── Outils ─────────────────────────────────────────────────────────────

    /** Structures actives autres que celle en cause, avec leur unité. */
    private Map<StructureBudgetaireEntity, UniteOrganisationnelleEntity> autresStructures(UUID structureId) {
        List<StructureBudgetaireEntity> autres = structureRepository.findAllByActifTrue().stream()
                .filter(s -> !s.getId().equals(structureId))
                .toList();
        if (autres.isEmpty()) {
            return Map.of();
        }
        Map<UUID, UniteOrganisationnelleEntity> unites = parId(uniteRepository.findAllById(
                autres.stream().map(StructureBudgetaireEntity::getUniteId).toList()), UniteOrganisationnelleEntity::getId);
        return autres.stream().filter(s -> unites.containsKey(s.getUniteId()))
                .collect(Collectors.toMap(Function.identity(), s -> unites.get(s.getUniteId())));
    }

    /** Vrai si {@code u} est strictement sous {@code ancetre} (chemins terminés par « / »). */
    static boolean sousUnite(UniteOrganisationnelleEntity u, UniteOrganisationnelleEntity ancetre) {
        return !u.getMaterializedPath().equals(ancetre.getMaterializedPath())
                && u.getMaterializedPath().startsWith(ancetre.getMaterializedPath());
    }

    private static boolean enDessousOuEgal(UniteOrganisationnelleEntity u, UniteOrganisationnelleEntity ancetre) {
        return u.getMaterializedPath().startsWith(ancetre.getMaterializedPath());
    }

    private StructureBudgetaireEntity trouver(UUID id) {
        return structureRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Aucune structure budgétaire ne porte cet identifiant."));
    }

    private UniteOrganisationnelleEntity uniteDe(StructureBudgetaireEntity structure) {
        return uniteRepository.findById(structure.getUniteId())
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "L'unité de cette structure n'existe plus dans l'organigramme."));
    }

    /** Fiche d'une structure, avec ses unités et son poste d'ordonnateur résolus. */
    private StructureBudgetaireOutput fiche(StructureBudgetaireEntity structure) {
        Set<UUID> uniteIds = new HashSet<>();
        uniteIds.add(structure.getUniteId());
        if (structure.getServiceEngagementUniteId() != null) {
            uniteIds.add(structure.getServiceEngagementUniteId());
        }
        Map<UUID, UniteOrganisationnelleEntity> unites = parId(uniteRepository.findAllById(uniteIds),
                UniteOrganisationnelleEntity::getId);
        Map<UUID, PosteEntity> postes = structure.getOrdonnateurPosteId() == null ? Map.of()
                : parId(posteRepository.findAllById(Set.of(structure.getOrdonnateurPosteId())), PosteEntity::getId);
        return sortie(structure, unites, postes);
    }

    private StructureBudgetaireOutput sortie(StructureBudgetaireEntity s, Map<UUID, UniteOrganisationnelleEntity> unites,
                                             Map<UUID, PosteEntity> postes) {
        UniteOrganisationnelleEntity unite = unites.get(s.getUniteId());
        UniteOrganisationnelleEntity service = unites.get(s.getServiceEngagementUniteId());
        PosteEntity poste = postes.get(s.getOrdonnateurPosteId());
        Optional<UtilisateurEntity> titulaire = titulaire(s.getOrdonnateurPosteId());
        return new StructureBudgetaireOutput(
                s.getId(),
                s.getUniteId(),
                unite == null ? null : unite.getCode(),
                unite == null ? null : unite.getIntitule(),
                unite != null && unite.getStatut() == StatutUnite.ACTIF,
                s.getOrdonnateurPosteId(),
                poste == null ? null : poste.getIntitule(),
                titulaire.map(UtilisateurEntity::getId).orElse(null),
                titulaire.map(u -> (u.getPrenom() + " " + u.getNom()).strip()).orElse(null),
                s.getServiceEngagementUniteId(),
                service == null ? null : service.getCode(),
                service == null ? null : service.getIntitule(),
                s.isActif(),
                s.getMotifDesactivation(),
                s.getCreePar(),
                s.getCreeLe(),
                s.getValidePar(),
                s.getValideLe(),
                s.getModifiePar(),
                s.getModifieLe());
    }

    /** Titulaire actuel du poste : l'affectation active la plus récente. */
    private Optional<UtilisateurEntity> titulaire(UUID posteId) {
        return affectationRepository.findByPosteIdAndStatut(posteId, StatutAffectation.ACTIF).stream()
                .max(Comparator.comparing(AffectationPosteEntity::getDateDebut,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(AffectationPosteEntity::getUtilisateur);
    }

    private static <T> Map<UUID, T> parId(Iterable<T> elements, Function<T, UUID> id) {
        Map<UUID, T> resultat = new HashMap<>();
        elements.forEach(e -> resultat.put(id.apply(e), e));
        return resultat;
    }

    private static String facultatif(String texte) {
        return texte == null || texte.isBlank() ? null : texte.strip();
    }

    private static ConflitMetierException structureExistante(UniteOrganisationnelleEntity unite) {
        return new ConflitMetierException("STRUCTURE_EXISTANTE",
                "L'unité " + unite.getIntitule() + " est déjà déclarée comme structure budgétaire.");
    }
}
