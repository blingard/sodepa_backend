package com.sodepa.erp.budgetaire.nature.infrastructure.adapter;

import com.sodepa.erp.budgetaire.commun.ChangerEtatInput;
import com.sodepa.erp.budgetaire.commun.CircuitReferentielBudgetaire;
import com.sodepa.erp.budgetaire.commun.CodeReferentiel;
import com.sodepa.erp.budgetaire.commun.DeciderDemandeInput;
import com.sodepa.erp.budgetaire.commun.DemandeBudgetaireOutput;
import com.sodepa.erp.budgetaire.commun.OperationReferentiel;
import com.sodepa.erp.budgetaire.commun.ProjectionDemande;
import com.sodepa.erp.budgetaire.commun.UsageReferentiels;
import com.sodepa.erp.budgetaire.nature.application.inputs.CreerNatureInput;
import com.sodepa.erp.budgetaire.nature.application.inputs.ListerNaturesInput;
import com.sodepa.erp.budgetaire.nature.application.inputs.ModifierNatureInput;
import com.sodepa.erp.budgetaire.nature.application.outputs.NatureBudgetaireOutput;
import com.sodepa.erp.budgetaire.nature.infrastructure.entities.NatureBudgetaireEntity;
import com.sodepa.erp.budgetaire.nature.infrastructure.entities.RegleCompteNature;
import com.sodepa.erp.budgetaire.nature.infrastructure.entities.SectionBudgetaire;
import com.sodepa.erp.budgetaire.nature.infrastructure.entities.SensBudgetaire;
import com.sodepa.erp.budgetaire.nature.infrastructure.event.DemandeNatureEvent;
import com.sodepa.erp.budgetaire.nature.infrastructure.repo.NatureBudgetaireRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.CompteEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.CompteRepository;
import com.sodepa.erp.share.MakerCheckerRequestEntity;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Règles des natures budgétaires (US-BUD-108).
 *
 * <p>
 * Le compte se saisit par son code (« 701 ») : c'est ce que l'utilisateur
 * connaît et ce que portera l'import du budget (US-BUD-111). Il est résolu en
 * identifiant dès la demande, et doit exister au plan comptable : la
 * comptabilité tient ce plan, le budget ne l'enrichit pas. La section, le sens
 * et la classe du compte doivent concorder ({@link RegleCompteNature}).
 * </p>
 *
 * <p>
 * Le circuit maker-checker, commun aux référentiels du module, est porté par
 * {@link CircuitReferentielBudgetaire}. Chaque règle est refaite à la validation.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NatureBudgetaireAdapter {

    private static final MakerCheckerEntityName TYPE = MakerCheckerEntityName.BUDGET_NATURE;

    private final NatureBudgetaireRepository natureRepository;
    private final CompteRepository compteRepository;
    private final CircuitReferentielBudgetaire circuit;
    private final UtilsService utilsService;
    private final UsageReferentiels usageReferentiels;
    private final Clock horloge;

    /** Identifiant d'une nature, déduit de son code normalisé. */
    public static UUID identifiantPour(String codeNormalise) {
        return UUID.nameUUIDFromBytes(("sodepa-budget-nature-" + codeNormalise).getBytes(StandardCharsets.UTF_8));
    }

    // ─── Demandes ───────────────────────────────────────────────────────────

    /** Soumet la création d'une nature. */
    @Transactional
    public SubmissionOutput initCreer(CreerNatureInput input) {
        utilsService.hasPermission(Permissions.INIT_CREATE_BUDGET_NATURE);

        String code = CodeReferentiel.valide(input.code());
        String libelle = libelleValide(input.libelle());
        if (input.section() == null || input.sens() == null) {
            throw new RequeteInvalideException("NATURE_INCOMPLETE", "Indiquez la section et le sens de la nature.");
        }
        CompteEntity compte = compteParCode(input.compteCode());
        controlerCompte(input.section(), input.sens(), compte);
        if (natureRepository.existsByCode(code)) {
            throw codeExistant(code);
        }

        UUID natureId = identifiantPour(code);
        refuserSiDemandeEnAttente(natureId, code);
        return soumettre(natureId, code,
                DemandeNatureEvent.creation(code, libelle, input.section(), input.sens(), compte.getId()));
    }

    /** Soumet le changement de libellé. */
    @Transactional
    public SubmissionOutput initModifier(ModifierNatureInput input) {
        utilsService.hasPermission(Permissions.INIT_UPDATE_BUDGET_NATURE);

        NatureBudgetaireEntity nature = trouver(input.id());
        exigerActive(nature);
        String libelle = libelleValide(input.libelle());
        if (libelle.equals(nature.getLibelle())) {
            throw new RequeteInvalideException("AUCUNE_MODIFICATION",
                    "La demande ne change rien à la nature " + nature.getCode() + ".");
        }
        refuserSiDemandeEnAttente(nature.getId(), nature.getCode());
        return soumettre(nature.getId(), nature.getCode(), DemandeNatureEvent.modification(libelle));
    }

    /** Soumet la désactivation d'une nature ; le motif est obligatoire. */
    @Transactional
    public SubmissionOutput initDesactiver(ChangerEtatInput input) {
        utilsService.hasPermission(Permissions.INIT_UPDATE_BUDGET_NATURE);

        String motif = facultatif(input.motif());
        if (motif == null) {
            throw new RequeteInvalideException("MOTIF_OBLIGATOIRE", "Indiquez le motif de la désactivation.");
        }
        NatureBudgetaireEntity nature = trouver(input.id());
        exigerActive(nature);
        controlerDesactivable(nature);
        refuserSiDemandeEnAttente(nature.getId(), nature.getCode());
        return soumettre(nature.getId(), nature.getCode(),
                DemandeNatureEvent.changementEtat(OperationReferentiel.DESACTIVER, motif));
    }

    /** Soumet la réactivation d'une nature désactivée. */
    @Transactional
    public SubmissionOutput initReactiver(ChangerEtatInput input) {
        utilsService.hasPermission(Permissions.INIT_UPDATE_BUDGET_NATURE);

        NatureBudgetaireEntity nature = trouver(input.id());
        controlerReactivable(nature);
        refuserSiDemandeEnAttente(nature.getId(), nature.getCode());
        return soumettre(nature.getId(), nature.getCode(),
                DemandeNatureEvent.changementEtat(OperationReferentiel.REACTIVER, facultatif(input.motif())));
    }

    /** Tranche une demande portant sur une nature. */
    @Transactional
    public void decider(DeciderDemandeInput input) {
        utilsService.hasPermission(Permissions.VALIDATE_OR_REJECT_BUDGET_NATURE);

        CircuitReferentielBudgetaire.DecisionOuverte decision = circuit.ouvrirDecision(
                TYPE, input.requestId(), input.decision(), input.notes(),
                "Aucune demande sur une nature budgétaire ne porte cet identifiant.");
        if (decision.acceptee()) {
            appliquer(decision.demande(), decision.validateur());
        }
        circuit.cloturer(decision, input.notes());
    }

    // ─── Lectures ───────────────────────────────────────────────────────────

    /** Natures triées par code, filtrées par section et par sens. */
    @Transactional(readOnly = true)
    public PageRecord<NatureBudgetaireOutput> lister(ListerNaturesInput input) {
        utilsService.hasPermission(Permissions.GET_BUDGET_NATURE_INFO);

        Page<NatureBudgetaireEntity> natures = natureRepository.rechercher(input.section(), input.sens(),
                input.inclureInactives(), CircuitReferentielBudgetaire.page(input.pageable(), Sort.by("code")));
        Map<UUID, CompteEntity> comptes = new HashMap<>();
        compteRepository.findAllById(natures.map(NatureBudgetaireEntity::getCompteId).toSet())
                .forEach(c -> comptes.put(c.getId(), c));
        return CircuitReferentielBudgetaire.versPageRecord(natures.map(n -> sortie(n, comptes.get(n.getCompteId()))));
    }

    /** Fiche d'une nature. */
    @Transactional(readOnly = true)
    public NatureBudgetaireOutput consulter(UUID id) {
        utilsService.hasPermission(Permissions.GET_BUDGET_NATURE_INFO);

        NatureBudgetaireEntity nature = trouver(id);
        return sortie(nature, compteRepository.findById(nature.getCompteId()).orElse(null));
    }

    /** Demandes en attente que l'appelant peut trancher (les siennes exclues), avec leur contenu. */
    @Transactional(readOnly = true)
    public PageRecord<DemandeBudgetaireOutput<NatureBudgetaireOutput>> demandesEnAttente(Pageable pageable) {
        utilsService.hasPermission(Permissions.VALIDATE_OR_REJECT_BUDGET_NATURE);
        return circuit.demandesEnAttente(TYPE, pageable, this::projeter);
    }

    /**
     * La nature telle que la demande la laisserait, et telle qu'elle est.
     * Aucune règle n'est rejouée : elles le seront à la validation.
     */
    private ProjectionDemande<NatureBudgetaireOutput> projeter(MakerCheckerRequestEntity demande) {
        DemandeNatureEvent charge = DemandeNatureEvent.depuisCharge(demande.getPayload());
        UUID natureId = UUID.fromString(demande.getEntityPk());
        if (charge.operation() == OperationReferentiel.CREER) {
            CompteEntity compte = compteRepository.findById(charge.compteId()).orElse(null);
            return ProjectionDemande.creation(sortie(nouvelleNature(demande, charge, natureId), compte));
        }
        NatureBudgetaireEntity nature = trouver(natureId);
        CompteEntity compte = compteRepository.findById(nature.getCompteId()).orElse(null);
        NatureBudgetaireEntity projetee = nature.toBuilder().build();
        reporter(projetee, charge);
        return new ProjectionDemande<>(charge.operation(), charge.motif(), sortie(projetee, compte),
                sortie(nature, compte));
    }

    // ─── Application d'une demande validée ──────────────────────────────────

    private void appliquer(MakerCheckerRequestEntity demande, String validateur) {
        DemandeNatureEvent charge = DemandeNatureEvent.depuisCharge(demande.getPayload());
        UUID natureId = UUID.fromString(demande.getEntityPk());
        if (charge.operation() == OperationReferentiel.CREER) {
            creer(demande, charge, natureId, validateur);
            return;
        }

        NatureBudgetaireEntity nature = trouver(natureId);
        switch (charge.operation()) {
            case MODIFIER -> exigerActive(nature);
            case DESACTIVER -> {
                exigerActive(nature);
                controlerDesactivable(nature);
            }
            case REACTIVER -> controlerReactivable(nature);
            default -> throw new IllegalStateException("Opération déjà traitée : " + charge.operation());
        }
        reporter(nature, charge);
        nature.setModifiePar(demande.getMakerId());
        nature.setModifieLe(horloge.instant());
        try {
            natureRepository.saveAndFlush(nature);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ConflitMetierException("NATURE_MODIFIEE_ENTRE_TEMPS",
                    "La nature " + nature.getCode() + " vient d'être modifiée : rechargez-la avant de décider.");
        }
        log.info("Natures : {} de {} appliquée (demande {}).", charge.operation(), nature.getCode(), demande.getId());
    }

    private void creer(MakerCheckerRequestEntity demande, DemandeNatureEvent charge, UUID natureId, String validateur) {
        if (natureRepository.existsByCode(charge.code())) {
            throw codeExistant(charge.code());
        }
        CompteEntity compte = compteRepository.findById(charge.compteId())
                .orElseThrow(() -> new RequeteInvalideException("COMPTE_INCONNU",
                        "Le compte de cette nature a été retiré du plan comptable depuis la demande."));
        controlerCompte(charge.section(), charge.sens(), compte);

        NatureBudgetaireEntity nature = nouvelleNature(demande, charge, natureId);
        nature.setValidePar(validateur);
        nature.setValideLe(horloge.instant());
        try {
            natureRepository.saveAndFlush(nature);
        } catch (DataIntegrityViolationException e) {
            throw codeExistant(charge.code());
        }
        log.info("Natures : {} créée sur le compte {} (demande {}).", charge.code(), compte.getCode(), demande.getId());
    }

    /** La nature que crée la demande, avant validation ; sert aussi à l'aperçu du valideur. */
    private static NatureBudgetaireEntity nouvelleNature(MakerCheckerRequestEntity demande, DemandeNatureEvent charge,
                                                         UUID natureId) {
        return NatureBudgetaireEntity.builder()
                .id(natureId)
                .code(charge.code())
                .libelle(charge.libelle())
                .section(charge.section())
                .sens(charge.sens())
                .compteId(charge.compteId())
                .actif(true)
                .demandeCreationId(demande.getId())
                .creePar(demande.getMakerId())
                .creeLe(demande.getCreatedAt())
                .build();
    }

    /**
     * Reporte sur la nature ce que change une demande autre qu'une création.
     * Partagé par l'application et l'aperçu : le valideur voit exactement ce
     * qui sera écrit.
     */
    private static void reporter(NatureBudgetaireEntity nature, DemandeNatureEvent charge) {
        switch (charge.operation()) {
            case MODIFIER -> nature.setLibelle(charge.libelle());
            case DESACTIVER -> {
                nature.setActif(false);
                nature.setMotifDesactivation(charge.motif());
            }
            case REACTIVER -> {
                nature.setActif(true);
                nature.setMotifDesactivation(null);
            }
            default -> throw new IllegalStateException("Opération sans report : " + charge.operation());
        }
    }

    // ─── Règles ─────────────────────────────────────────────────────────────

    /** Le compte doit exister au plan comptable : le budget ne crée pas de compte. */
    private CompteEntity compteParCode(String compteCode) {
        if (compteCode == null || compteCode.isBlank()) {
            throw new RequeteInvalideException("COMPTE_INCONNU", "Indiquez le compte SYSCOHADA de la nature.");
        }
        String code = compteCode.strip();
        return compteRepository.findByCode(code)
                .orElseThrow(() -> new RequeteInvalideException("COMPTE_INCONNU",
                        "Le compte " + code + " ne figure pas au plan comptable."));
    }

    /** Pas de compte auxiliaire, et un compte de la classe attendue pour la section et le sens. */
    private static void controlerCompte(SectionBudgetaire section, SensBudgetaire sens, CompteEntity compte) {
        if (Boolean.TRUE.equals(compte.getIsAuxiliaire())) {
            throw new RequeteInvalideException("NATURE_INCOHERENTE",
                    "Le compte " + compte.getCode() + " est un compte auxiliaire de tiers : une nature s'impute sur un compte général.");
        }
        if (!RegleCompteNature.admis(section, sens, compte.getCode())) {
            throw new RequeteInvalideException("NATURE_INCOHERENTE",
                    "Le compte " + compte.getCode() + " (" + compte.getIntitule() + ") ne convient pas : "
                            + RegleCompteNature.attendu(section, sens) + ".");
        }
    }

    private void controlerDesactivable(NatureBudgetaireEntity nature) {
        if (usageReferentiels.natureUtilisee(nature.getId())) {
            throw new ConflitMetierException("NATURE_UTILISEE",
                    "La nature " + nature.getCode() + " qualifie des lignes d'un exercice en cours d'exécution : "
                            + "elle ne pourra être désactivée qu'après la clôture de l'exercice.");
        }
    }

    private void controlerReactivable(NatureBudgetaireEntity nature) {
        if (nature.isActif()) {
            throw new ConflitMetierException("NATURE_DEJA_ACTIVE", "La nature " + nature.getCode() + " est déjà active.");
        }
        if (!compteRepository.existsById(nature.getCompteId())) {
            throw new ConflitMetierException("COMPTE_INCONNU",
                    "Le compte de la nature " + nature.getCode() + " a été retiré du plan comptable : créez une nouvelle nature.");
        }
    }

    private static void exigerActive(NatureBudgetaireEntity nature) {
        if (!nature.isActif()) {
            throw new ConflitMetierException("NATURE_INACTIVE", "La nature " + nature.getCode() + " est désactivée.");
        }
    }

    private void refuserSiDemandeEnAttente(UUID natureId, String code) {
        circuit.refuserSiDemandeEnAttente(TYPE, natureId, "NATURE_DEMANDE_EN_ATTENTE",
                "Une demande sur la nature " + code + " attend déjà sa validation.");
    }

    private SubmissionOutput soumettre(UUID natureId, String code, DemandeNatureEvent demande) {
        UUID demandeId = circuit.soumettre(TYPE, natureId, demande.versCharge(), demande.operation().typeMakerChecker());
        log.info("Natures : {} de {} soumise (demande {}).", demande.operation(), code, demandeId);
        return new SubmissionOutput(demandeId, natureId);
    }

    private NatureBudgetaireEntity trouver(UUID id) {
        return natureRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Aucune nature budgétaire ne porte cet identifiant."));
    }

    private static NatureBudgetaireOutput sortie(NatureBudgetaireEntity n, CompteEntity compte) {
        return new NatureBudgetaireOutput(n.getId(), n.getCode(), n.getLibelle(), n.getSection(), n.getSens(),
                n.getCompteId(), compte == null ? null : compte.getCode(), compte == null ? null : compte.getIntitule(),
                n.isActif(), n.getMotifDesactivation(), n.getCreePar(), n.getCreeLe(), n.getValidePar(),
                n.getValideLe(), n.getModifiePar(), n.getModifieLe());
    }

    private static String libelleValide(String libelle) {
        String valeur = facultatif(libelle);
        if (valeur == null) {
            throw new RequeteInvalideException("LIBELLE_OBLIGATOIRE", "Indiquez le libellé.");
        }
        return valeur;
    }

    private static String facultatif(String texte) {
        return texte == null || texte.isBlank() ? null : texte.strip();
    }

    private static ConflitMetierException codeExistant(String code) {
        return new ConflitMetierException("CODE_NATURE_EXISTANT",
                "Le code " + code + " est déjà utilisé par une nature, active ou désactivée.");
    }
}
