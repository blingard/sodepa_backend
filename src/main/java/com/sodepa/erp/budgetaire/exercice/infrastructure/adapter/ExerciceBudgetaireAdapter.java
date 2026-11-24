package com.sodepa.erp.budgetaire.exercice.infrastructure.adapter;

import com.sodepa.erp.budgetaire.commun.CircuitReferentielBudgetaire;
import com.sodepa.erp.budgetaire.commun.DemandeBudgetaireOutput;
import com.sodepa.erp.budgetaire.commun.ProjectionDemande;
import com.sodepa.erp.budgetaire.exercice.application.inputs.CreerExerciceInput;
import com.sodepa.erp.budgetaire.exercice.application.inputs.DeciderDemandeExerciceInput;
import com.sodepa.erp.budgetaire.exercice.application.outputs.ExerciceBudgetaireOutput;
import com.sodepa.erp.budgetaire.exercice.infrastructure.entities.EtatExercice;
import com.sodepa.erp.budgetaire.exercice.infrastructure.entities.ExerciceBudgetaireEntity;
import com.sodepa.erp.budgetaire.exercice.infrastructure.event.CreationExerciceEvent;
import com.sodepa.erp.budgetaire.exercice.infrastructure.repo.ExerciceBudgetaireRepository;
import com.sodepa.erp.share.MakerCheckerRequestEntity;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.share.erreurs.ConflitMetierException;
import com.sodepa.erp.share.erreurs.RequeteInvalideException;
import com.sodepa.erp.share.erreurs.RessourceIntrouvableException;
import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.MakerCheckerOperationType;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.utils.Permissions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Year;
import java.util.UUID;

/**
 * Règles de l'exercice budgétaire : droits, doublons, maker-checker (US-BUD-101).
 *
 * <p>
 * Un exercice n'existe qu'après validation par un second utilisateur. La
 * demande ne porte que l'année : les dates (1er janvier – 31 décembre) et
 * l'état initial sont fixés ici, jamais reçus du client.
 * </p>
 *
 * <p>
 * L'identifiant d'un exercice dérive de son année ({@link #identifiantPour}).
 * Deux demandes pour la même année portent donc sur la même entité, ce qui
 * permet de repérer une demande concurrente déjà en attente, et la clé
 * primaire comme la contrainte d'unicité sur l'année empêchent d'en créer deux.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExerciceBudgetaireAdapter {

    /** Plus ancien exercice accepté : en deçà, c'est une faute de frappe. */
    static final int PREMIERE_ANNEE = 2000;

    private final ExerciceBudgetaireRepository exerciceRepository;
    private final CircuitReferentielBudgetaire circuit;
    private final UtilsService utilsService;
    private final Clock horloge;

    /**
     * Identifiant d'un exercice, déduit de son année.
     *
     * <p>Stable d'une demande à l'autre : c'est ce qui relie une nouvelle
     * demande à celles déjà en attente pour la même année.</p>
     */
    public static UUID identifiantPour(int annee) {
        return UUID.nameUUIDFromBytes(("sodepa-budget-exercice-" + annee).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Soumet la création d'un exercice au maker-checker.
     *
     * <p>
     * Refuse d'emblée ce que la validation refuserait : un exercice existant,
     * ou une demande déjà en attente pour la même année. Sans ce second
     * contrôle, deux chefs du budget pouvaient déposer chacun leur demande et
     * le validateur découvrir le doublon trop tard.
     * </p>
     */
    @Transactional
    public SubmissionOutput initCreer(CreerExerciceInput input) {
        utilsService.hasPermission(Permissions.INIT_CREATE_BUDGET_EXERCICE);

        int annee = anneeValide(input.annee());
        if (exerciceRepository.existsByAnnee(annee)) {
            throw exerciceExistant(annee);
        }

        UUID exerciceId = identifiantPour(annee);
        circuit.refuserSiDemandeEnAttente(MakerCheckerEntityName.BUDGET_EXERCICE, exerciceId,
                "EXERCICE_DEMANDE_EN_ATTENTE",
                "Une demande de création de l'exercice " + annee + " attend déjà sa validation.");

        UUID demandeId = circuit.soumettre(MakerCheckerEntityName.BUDGET_EXERCICE, exerciceId,
                new CreationExerciceEvent(annee).versCharge(), MakerCheckerOperationType.CREATE);
        log.info("Création de l'exercice budgétaire {} soumise (demande {}).", annee, demandeId);
        return new SubmissionOutput(demandeId, exerciceId);
    }

    /**
     * Tranche une demande portant sur un exercice.
     *
     * <p>
     * La demande est relue directement en base : l'auteur doit être comparé à
     * l'identifiant enregistré, pas à sa projection, qui invente un identifiant
     * quand l'utilisateur n'est pas retrouvé et laisserait passer l'auteur.
     * </p>
     */
    @Transactional
    public void decider(DeciderDemandeExerciceInput input) {
        utilsService.hasPermission(Permissions.VALIDATE_OR_REJECT_BUDGET_EXERCICE);

        CircuitReferentielBudgetaire.DecisionOuverte decision = circuit.ouvrirDecision(
                MakerCheckerEntityName.BUDGET_EXERCICE, input.requestId(), input.decision(), input.notes(),
                "Aucune demande sur un exercice budgétaire ne porte cet identifiant.");
        if (decision.acceptee()) {
            creer(decision.demande(), decision.validateur());
        }
        circuit.cloturer(decision, input.notes());
    }

    /** Liste des exercices, du plus récent au plus ancien. */
    @Transactional(readOnly = true)
    public PageRecord<ExerciceBudgetaireOutput> lister(Pageable pageable) {
        utilsService.hasPermission(Permissions.GET_BUDGET_EXERCICE_INFO);

        Pageable parAnnee = CircuitReferentielBudgetaire.page(pageable, Sort.by(Sort.Direction.DESC, "annee"));
        return CircuitReferentielBudgetaire.versPageRecord(
                exerciceRepository.findAll(parAnnee).map(ExerciceBudgetaireOutput::depuis));
    }

    /** Un exercice, désigné par son année. */
    @Transactional(readOnly = true)
    public ExerciceBudgetaireOutput consulter(int annee) {
        utilsService.hasPermission(Permissions.GET_BUDGET_EXERCICE_INFO);

        return exerciceRepository.findByAnnee(annee)
                .map(ExerciceBudgetaireOutput::depuis)
                .orElseThrow(() -> new RessourceIntrouvableException("L'exercice " + annee + " n'existe pas."));
    }

    /**
     * Demandes en attente que l'appelant peut trancher, avec l'exercice proposé.
     *
     * <p>Les siennes sont écartées : il ne pourrait que se les voir refuser.</p>
     */
    @Transactional(readOnly = true)
    public PageRecord<DemandeBudgetaireOutput<ExerciceBudgetaireOutput>> demandesEnAttente(Pageable pageable) {
        utilsService.hasPermission(Permissions.VALIDATE_OR_REJECT_BUDGET_EXERCICE);

        return circuit.demandesEnAttente(MakerCheckerEntityName.BUDGET_EXERCICE, pageable, this::projeter);
    }

    /** Un exercice ne connaît que la création : la projection est l'exercice qui naîtra. */
    private ProjectionDemande<ExerciceBudgetaireOutput> projeter(MakerCheckerRequestEntity demande) {
        int annee = CreationExerciceEvent.depuisCharge(demande.getPayload()).annee();
        return ProjectionDemande.creation(ExerciceBudgetaireOutput.depuis(nouvelExercice(demande, annee)));
    }

    // ─── Règles internes ────────────────────────────────────────────────────

    private void creer(MakerCheckerRequestEntity demande, String validateur) {
        int annee = CreationExerciceEvent.depuisCharge(demande.getPayload()).annee();
        if (exerciceRepository.existsByAnnee(annee)) {
            throw exerciceExistant(annee);
        }

        ExerciceBudgetaireEntity exercice = nouvelExercice(demande, annee);
        exercice.setValidePar(validateur);
        exercice.setValideLe(horloge.instant());
        try {
            // Écriture immédiate : une validation concurrente de la même année
            // échoue ici, sur la contrainte d'unicité, et non au commit en 500.
            exerciceRepository.saveAndFlush(exercice);
        } catch (DataIntegrityViolationException e) {
            throw exerciceExistant(annee);
        }
        log.info("Exercice budgétaire {} créé à l'état {} (demande {}).", annee, exercice.getEtat(), demande.getId());
    }

    /**
     * L'exercice que crée la demande, avant validation : dates et état initial
     * sont fixés ici. Sert à la création et à l'aperçu du valideur, qui voit
     * ainsi exactement ce qui naîtra.
     */
    private static ExerciceBudgetaireEntity nouvelExercice(MakerCheckerRequestEntity demande, int annee) {
        return ExerciceBudgetaireEntity.builder()
                .id(UUID.fromString(demande.getEntityPk()))
                .annee(annee)
                .etat(EtatExercice.EN_PREPARATION)
                .dateDebut(LocalDate.of(annee, 1, 1))
                .dateFin(LocalDate.of(annee, 12, 31))
                .demandeCreationId(demande.getId())
                .creePar(demande.getMakerId())
                .creeLe(demande.getCreatedAt())
                .build();
    }

    /**
     * Contrôle l'année demandée.
     *
     * <p>
     * Au plus l'exercice suivant : la procédure prépare le budget N+1 pendant
     * l'exercice N, jamais au-delà. Une année plus lointaine est presque
     * toujours une faute de frappe (2207 pour 2027), et un exercice créé ne se
     * supprime pas.
     * </p>
     */
    private int anneeValide(Integer annee) {
        if (annee == null) {
            throw new RequeteInvalideException("ANNEE_OBLIGATOIRE", "Indiquez l'année de l'exercice.");
        }
        int anneeMax = Year.now(horloge).getValue() + 1;
        if (annee < PREMIERE_ANNEE || annee > anneeMax) {
            throw new RequeteInvalideException("ANNEE_HORS_BORNES",
                    "L'année de l'exercice doit être comprise entre " + PREMIERE_ANNEE + " et " + anneeMax
                            + " : seul l'exercice suivant se prépare à l'avance.");
        }
        return annee;
    }

    private static ConflitMetierException exerciceExistant(int annee) {
        return new ConflitMetierException("EXERCICE_EXISTANT", "L'exercice " + annee + " existe déjà");
    }

}
