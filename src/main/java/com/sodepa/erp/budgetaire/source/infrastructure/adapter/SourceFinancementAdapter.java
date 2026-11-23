package com.sodepa.erp.budgetaire.source.infrastructure.adapter;

import com.sodepa.erp.budgetaire.commun.ChangerEtatInput;
import com.sodepa.erp.budgetaire.commun.CircuitReferentielBudgetaire;
import com.sodepa.erp.budgetaire.commun.CodeReferentiel;
import com.sodepa.erp.budgetaire.commun.DeciderDemandeInput;
import com.sodepa.erp.budgetaire.commun.OperationReferentiel;
import com.sodepa.erp.budgetaire.commun.UsageReferentiels;
import com.sodepa.erp.budgetaire.source.application.inputs.CreerSourceInput;
import com.sodepa.erp.budgetaire.source.application.inputs.ListerSourcesInput;
import com.sodepa.erp.budgetaire.source.application.inputs.ModifierSourceInput;
import com.sodepa.erp.budgetaire.source.application.outputs.SourceFinancementOutput;
import com.sodepa.erp.budgetaire.source.infrastructure.entities.SourceFinancementEntity;
import com.sodepa.erp.budgetaire.source.infrastructure.entities.TypeSourceFinancement;
import com.sodepa.erp.budgetaire.source.infrastructure.event.DemandeSourceEvent;
import com.sodepa.erp.budgetaire.source.infrastructure.repo.SourceFinancementRepository;
import com.sodepa.erp.share.MakerCheckerRequestEntity;
import com.sodepa.erp.share.MakerCheckerSmartOutput;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.share.erreurs.ConflitMetierException;
import com.sodepa.erp.share.erreurs.RequeteInvalideException;
import com.sodepa.erp.share.erreurs.RessourceIntrouvableException;
import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.utils.Permissions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/**
 * Règles des sources de financement (US-BUD-108).
 *
 * <p>
 * Aucune source n'est semée d'office, pas même « Ressources propres » : le
 * chef du budget la crée sous maker-checker, comme les autres, pour que son
 * auteur et son validateur soient connus. Le circuit maker-checker est porté
 * par {@link CircuitReferentielBudgetaire}.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SourceFinancementAdapter {

    private static final MakerCheckerEntityName TYPE = MakerCheckerEntityName.BUDGET_SOURCE_FINANCEMENT;

    private final SourceFinancementRepository sourceRepository;
    private final CircuitReferentielBudgetaire circuit;
    private final UtilsService utilsService;
    private final UsageReferentiels usageReferentiels;
    private final Clock horloge;

    /** Identifiant d'une source, déduit de son code normalisé. */
    public static UUID identifiantPour(String codeNormalise) {
        return UUID.nameUUIDFromBytes(("sodepa-budget-source-" + codeNormalise).getBytes(StandardCharsets.UTF_8));
    }

    // ─── Demandes ───────────────────────────────────────────────────────────

    /** Soumet la création d'une source. */
    @Transactional
    public SubmissionOutput initCreer(CreerSourceInput input) {
        utilsService.hasPermission(Permissions.INIT_CREATE_BUDGET_SOURCE_FINANCEMENT);

        String code = CodeReferentiel.valide(input.code());
        String libelle = obligatoire(input.libelle(), "LIBELLE_OBLIGATOIRE", "Indiquez le libellé.");
        if (input.type() == null) {
            throw new RequeteInvalideException("TYPE_OBLIGATOIRE", "Indiquez le type de la source de financement.");
        }
        String bailleur = bailleurValide(input.type(), input.bailleur());
        if (sourceRepository.existsByCode(code)) {
            throw codeExistant(code);
        }

        UUID sourceId = identifiantPour(code);
        refuserSiDemandeEnAttente(sourceId, code);
        return soumettre(sourceId, code, DemandeSourceEvent.creation(code, libelle, input.type(), bailleur));
    }

    /** Soumet le changement de libellé ou de bailleur. */
    @Transactional
    public SubmissionOutput initModifier(ModifierSourceInput input) {
        utilsService.hasPermission(Permissions.INIT_UPDATE_BUDGET_SOURCE_FINANCEMENT);

        SourceFinancementEntity source = trouver(input.id());
        exigerActive(source);
        String libelle = obligatoire(input.libelle(), "LIBELLE_OBLIGATOIRE", "Indiquez le libellé.");
        String bailleur = bailleurValide(source.getType(), input.bailleur());
        if (libelle.equals(source.getLibelle()) && Objects.equals(bailleur, source.getBailleur())) {
            throw new RequeteInvalideException("AUCUNE_MODIFICATION",
                    "La demande ne change rien à la source " + source.getCode() + ".");
        }
        refuserSiDemandeEnAttente(source.getId(), source.getCode());
        return soumettre(source.getId(), source.getCode(), DemandeSourceEvent.modification(libelle, bailleur));
    }

    /** Soumet la désactivation d'une source ; le motif est obligatoire. */
    @Transactional
    public SubmissionOutput initDesactiver(ChangerEtatInput input) {
        utilsService.hasPermission(Permissions.INIT_UPDATE_BUDGET_SOURCE_FINANCEMENT);

        String motif = facultatif(input.motif());
        if (motif == null) {
            throw new RequeteInvalideException("MOTIF_OBLIGATOIRE", "Indiquez le motif de la désactivation.");
        }
        SourceFinancementEntity source = trouver(input.id());
        exigerActive(source);
        controlerDesactivable(source);
        refuserSiDemandeEnAttente(source.getId(), source.getCode());
        return soumettre(source.getId(), source.getCode(),
                DemandeSourceEvent.changementEtat(OperationReferentiel.DESACTIVER, motif));
    }

    /** Soumet la réactivation d'une source désactivée. */
    @Transactional
    public SubmissionOutput initReactiver(ChangerEtatInput input) {
        utilsService.hasPermission(Permissions.INIT_UPDATE_BUDGET_SOURCE_FINANCEMENT);

        SourceFinancementEntity source = trouver(input.id());
        exigerInactive(source);
        refuserSiDemandeEnAttente(source.getId(), source.getCode());
        return soumettre(source.getId(), source.getCode(),
                DemandeSourceEvent.changementEtat(OperationReferentiel.REACTIVER, facultatif(input.motif())));
    }

    /** Tranche une demande portant sur une source. */
    @Transactional
    public void decider(DeciderDemandeInput input) {
        utilsService.hasPermission(Permissions.VALIDATE_OR_REJECT_BUDGET_SOURCE_FINANCEMENT);

        CircuitReferentielBudgetaire.DecisionOuverte decision = circuit.ouvrirDecision(
                TYPE, input.requestId(), input.decision(), input.notes(),
                "Aucune demande sur une source de financement ne porte cet identifiant.");
        if (decision.acceptee()) {
            appliquer(decision.demande(), decision.validateur());
        }
        circuit.cloturer(decision, input.notes());
    }

    // ─── Lectures ───────────────────────────────────────────────────────────

    /** Sources triées par code, filtrées par type. */
    @Transactional(readOnly = true)
    public PageRecord<SourceFinancementOutput> lister(ListerSourcesInput input) {
        utilsService.hasPermission(Permissions.GET_BUDGET_SOURCE_FINANCEMENT_INFO);

        return CircuitReferentielBudgetaire.versPageRecord(sourceRepository
                .rechercher(input.type(), input.inclureInactives(),
                        CircuitReferentielBudgetaire.page(input.pageable(), Sort.by("code")))
                .map(SourceFinancementOutput::depuis));
    }

    /** Fiche d'une source. */
    @Transactional(readOnly = true)
    public SourceFinancementOutput consulter(UUID id) {
        utilsService.hasPermission(Permissions.GET_BUDGET_SOURCE_FINANCEMENT_INFO);
        return SourceFinancementOutput.depuis(trouver(id));
    }

    /** Demandes en attente que l'appelant peut trancher (les siennes exclues). */
    @Transactional(readOnly = true)
    public PageRecord<MakerCheckerSmartOutput> demandesEnAttente(Pageable pageable) {
        utilsService.hasPermission(Permissions.VALIDATE_OR_REJECT_BUDGET_SOURCE_FINANCEMENT);
        return circuit.demandesEnAttente(TYPE, pageable);
    }

    // ─── Application d'une demande validée ──────────────────────────────────

    private void appliquer(MakerCheckerRequestEntity demande, String validateur) {
        DemandeSourceEvent charge = DemandeSourceEvent.depuisCharge(demande.getPayload());
        UUID sourceId = UUID.fromString(demande.getEntityPk());
        if (charge.operation() == OperationReferentiel.CREER) {
            creer(demande, charge, sourceId, validateur);
            return;
        }

        SourceFinancementEntity source = trouver(sourceId);
        switch (charge.operation()) {
            case MODIFIER -> {
                exigerActive(source);
                source.setLibelle(charge.libelle());
                source.setBailleur(bailleurValide(source.getType(), charge.bailleur()));
            }
            case DESACTIVER -> {
                exigerActive(source);
                controlerDesactivable(source);
                source.setActif(false);
                source.setMotifDesactivation(charge.motif());
            }
            case REACTIVER -> {
                exigerInactive(source);
                source.setActif(true);
                source.setMotifDesactivation(null);
            }
            default -> throw new IllegalStateException("Opération déjà traitée : " + charge.operation());
        }
        source.setModifiePar(demande.getMakerId());
        source.setModifieLe(horloge.instant());
        try {
            sourceRepository.saveAndFlush(source);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ConflitMetierException("SOURCE_MODIFIEE_ENTRE_TEMPS",
                    "La source " + source.getCode() + " vient d'être modifiée : rechargez-la avant de décider.");
        }
        log.info("Sources de financement : {} de {} appliquée (demande {}).",
                charge.operation(), source.getCode(), demande.getId());
    }

    private void creer(MakerCheckerRequestEntity demande, DemandeSourceEvent charge, UUID sourceId, String validateur) {
        if (sourceRepository.existsByCode(charge.code())) {
            throw codeExistant(charge.code());
        }
        SourceFinancementEntity source = SourceFinancementEntity.builder()
                .id(sourceId)
                .code(charge.code())
                .libelle(charge.libelle())
                .type(charge.type())
                .bailleur(bailleurValide(charge.type(), charge.bailleur()))
                .actif(true)
                .demandeCreationId(demande.getId())
                .creePar(demande.getMakerId())
                .creeLe(demande.getCreatedAt())
                .validePar(validateur)
                .valideLe(horloge.instant())
                .build();
        try {
            sourceRepository.saveAndFlush(source);
        } catch (DataIntegrityViolationException e) {
            throw codeExistant(charge.code());
        }
        log.info("Sources de financement : {} créée (demande {}).", charge.code(), demande.getId());
    }

    // ─── Règles ─────────────────────────────────────────────────────────────

    /** Un partenaire se nomme : c'est ce qui permet de suivre ce qu'il finance (H-30). */
    private static String bailleurValide(TypeSourceFinancement type, String bailleur) {
        String valeur = facultatif(bailleur);
        if (type == TypeSourceFinancement.PARTENAIRE && valeur == null) {
            throw new RequeteInvalideException("BAILLEUR_OBLIGATOIRE",
                    "Nommez le partenaire ou le bailleur de cette source de financement.");
        }
        return valeur;
    }

    private void controlerDesactivable(SourceFinancementEntity source) {
        if (usageReferentiels.sourceUtilisee(source.getId())) {
            throw new ConflitMetierException("SOURCE_UTILISEE",
                    "La source " + source.getCode() + " finance des lignes d'un exercice en cours d'exécution : "
                            + "elle ne pourra être désactivée qu'après la clôture de l'exercice.");
        }
    }

    private static void exigerActive(SourceFinancementEntity source) {
        if (!source.isActif()) {
            throw new ConflitMetierException("SOURCE_INACTIVE", "La source " + source.getCode() + " est désactivée.");
        }
    }

    private static void exigerInactive(SourceFinancementEntity source) {
        if (source.isActif()) {
            throw new ConflitMetierException("SOURCE_DEJA_ACTIVE", "La source " + source.getCode() + " est déjà active.");
        }
    }

    private void refuserSiDemandeEnAttente(UUID sourceId, String code) {
        circuit.refuserSiDemandeEnAttente(TYPE, sourceId, "SOURCE_DEMANDE_EN_ATTENTE",
                "Une demande sur la source " + code + " attend déjà sa validation.");
    }

    private SubmissionOutput soumettre(UUID sourceId, String code, DemandeSourceEvent demande) {
        UUID demandeId = circuit.soumettre(TYPE, sourceId, demande.versCharge(), demande.operation().typeMakerChecker());
        log.info("Sources de financement : {} de {} soumise (demande {}).", demande.operation(), code, demandeId);
        return new SubmissionOutput(demandeId, sourceId);
    }

    private SourceFinancementEntity trouver(UUID id) {
        return sourceRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Aucune source de financement ne porte cet identifiant."));
    }

    private static String obligatoire(String texte, String code, String message) {
        String valeur = facultatif(texte);
        if (valeur == null) {
            throw new RequeteInvalideException(code, message);
        }
        return valeur;
    }

    private static String facultatif(String texte) {
        return texte == null || texte.isBlank() ? null : texte.strip();
    }

    private static ConflitMetierException codeExistant(String code) {
        return new ConflitMetierException("CODE_SOURCE_EXISTANT",
                "Le code " + code + " est déjà utilisé par une source de financement, active ou désactivée.");
    }
}
