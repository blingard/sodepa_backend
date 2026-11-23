package com.sodepa.erp.budgetaire.nomenclature.infrastructure.adapter;

import com.sodepa.erp.budgetaire.commun.CircuitReferentielBudgetaire;
import com.sodepa.erp.budgetaire.commun.OperationReferentiel;
import com.sodepa.erp.budgetaire.commun.UsageReferentiels;
import com.sodepa.erp.budgetaire.nomenclature.application.inputs.ChangerEtatElementInput;
import com.sodepa.erp.budgetaire.nomenclature.application.inputs.CreerElementInput;
import com.sodepa.erp.budgetaire.nomenclature.application.inputs.DeciderDemandeNomenclatureInput;
import com.sodepa.erp.budgetaire.nomenclature.application.inputs.ModifierElementInput;
import com.sodepa.erp.budgetaire.nomenclature.application.outputs.ElementProgrammatiqueOutput;
import com.sodepa.erp.budgetaire.nomenclature.application.outputs.NoeudNomenclatureOutput;
import com.sodepa.erp.budgetaire.nomenclature.infrastructure.entities.ElementProgrammatiqueEntity;
import com.sodepa.erp.budgetaire.nomenclature.infrastructure.entities.NiveauProgrammatique;
import com.sodepa.erp.budgetaire.nomenclature.infrastructure.event.DemandeNomenclatureEvent;
import com.sodepa.erp.budgetaire.nomenclature.infrastructure.repo.ElementProgrammatiqueRepository;
import com.sodepa.erp.share.MakerCheckerRequestEntity;
import com.sodepa.erp.share.MakerCheckerSmartOutput;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.share.erreurs.ConflitMetierException;
import com.sodepa.erp.share.erreurs.RequeteInvalideException;
import com.sodepa.erp.share.erreurs.RessourceIntrouvableException;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import com.sodepa.erp.user.infrastructure.repo.UserRepository;
import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.MakerCheckerStatus;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Règles de la nomenclature budgétaire : programmes et actions (US-BUD-104).
 *
 * <p>
 * Toute écriture passe par le maker-checker : création, modification,
 * désactivation et réactivation. Chaque règle est contrôlée deux fois, à la
 * demande pour répondre tout de suite, puis à la validation, parce que l'arbre
 * a pu changer entre-temps (parent désactivé, enfant ajouté, code pris).
 * </p>
 *
 * <p>
 * L'identifiant d'un élément dérive de son code ({@link #identifiantPour}).
 * Deux demandes de création du même code portent donc sur la même entité, et
 * une seule demande à la fois peut attendre sur un élément donné.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NomenclatureAdapter {

    /**
     * Caractères admis dans un code. Ce n'est pas un masque de codification :
     * le plan réel de la SODEPA n'est pas encore connu (Q-01, H-01). On écarte
     * seulement ce qui casserait un export ou une recherche (espaces, accents,
     * barres obliques).
     */
    private static final Pattern FORME_CODE = Pattern.compile("[A-Z0-9][A-Z0-9._-]{0,29}");

    private final ElementProgrammatiqueRepository elementRepository;
    private final CircuitReferentielBudgetaire circuit;
    private final UtilsService utilsService;
    private final UserRepository utilisateurRepository;
    private final UsageReferentiels usageReferentiels;
    private final Clock horloge;

    /**
     * Identifiant d'un élément, déduit de son code normalisé.
     *
     * <p>Stable d'une demande à l'autre : c'est ce qui relie une nouvelle
     * demande de création à celles déjà en attente pour le même code.</p>
     */
    public static UUID identifiantPour(String codeNormalise) {
        return UUID.nameUUIDFromBytes(("sodepa-budget-element-" + codeNormalise).getBytes(StandardCharsets.UTF_8));
    }

    // ─── Demandes ───────────────────────────────────────────────────────────

    /** Soumet la création d'un programme ou d'une action. */
    @Transactional
    public SubmissionOutput initCreer(CreerElementInput input) {
        utilsService.hasPermission(Permissions.INIT_CREATE_BUDGET_NOMENCLATURE);

        NiveauProgrammatique niveau = niveauGere(input.niveau());
        String code = codeValide(input.code());
        String libelle = libelleValide(input.libelle());
        String description = facultatif(input.description());
        controlerParent(niveau, input.parentId());
        controlerResponsable(niveau, input.responsableId());
        if (elementRepository.existsByCode(code)) {
            throw codeExistant(code);
        }

        UUID elementId = identifiantPour(code);
        refuserSiDemandeEnAttente(elementId, code);
        DemandeNomenclatureEvent demande = DemandeNomenclatureEvent.creation(
                niveau, code, libelle, description, input.parentId(), input.responsableId());
        return soumettre(elementId, demande, code);
    }

    /** Soumet la modification du libellé, de la description ou du responsable. */
    @Transactional
    public SubmissionOutput initModifier(ModifierElementInput input) {
        utilsService.hasPermission(Permissions.INIT_UPDATE_BUDGET_NOMENCLATURE);

        ElementProgrammatiqueEntity element = trouver(input.id());
        exigerActif(element);
        String libelle = libelleValide(input.libelle());
        String description = facultatif(input.description());
        controlerResponsable(element.getNiveau(), input.responsableId());
        if (libelle.equals(element.getLibelle())
                && Objects.equals(description, element.getDescription())
                && Objects.equals(input.responsableId(), element.getResponsableId())) {
            throw new RequeteInvalideException("AUCUNE_MODIFICATION",
                    "La demande ne change rien à l'élément " + element.getCode() + ".");
        }

        refuserSiDemandeEnAttente(element.getId(), element.getCode());
        return soumettre(element.getId(),
                DemandeNomenclatureEvent.modification(libelle, description, input.responsableId()),
                element.getCode());
    }

    /**
     * Soumet la désactivation d'un élément.
     *
     * <p>Un élément désactivé reste dans l'arbre pour les exercices qui le
     * citent ; il ne reçoit plus ni ligne ni enfant. Le motif est obligatoire :
     * il reste affiché sur la fiche tant que l'élément est inactif.</p>
     */
    @Transactional
    public SubmissionOutput initDesactiver(ChangerEtatElementInput input) {
        utilsService.hasPermission(Permissions.INIT_UPDATE_BUDGET_NOMENCLATURE);

        String motif = facultatif(input.motif());
        if (motif == null) {
            throw new RequeteInvalideException("MOTIF_OBLIGATOIRE", "Indiquez le motif de la désactivation.");
        }
        ElementProgrammatiqueEntity element = trouver(input.id());
        if (!element.isActif()) {
            throw new ConflitMetierException("ELEMENT_DEJA_INACTIF",
                    "L'élément " + element.getCode() + " est déjà désactivé.");
        }
        controlerDesactivable(element);

        refuserSiDemandeEnAttente(element.getId(), element.getCode());
        return soumettre(element.getId(),
                DemandeNomenclatureEvent.changementEtat(OperationReferentiel.DESACTIVER, motif), element.getCode());
    }

    /** Soumet la réactivation d'un élément désactivé. */
    @Transactional
    public SubmissionOutput initReactiver(ChangerEtatElementInput input) {
        utilsService.hasPermission(Permissions.INIT_UPDATE_BUDGET_NOMENCLATURE);

        ElementProgrammatiqueEntity element = trouver(input.id());
        if (element.isActif()) {
            throw new ConflitMetierException("ELEMENT_DEJA_ACTIF",
                    "L'élément " + element.getCode() + " est déjà actif.");
        }
        controlerParentActif(element);

        refuserSiDemandeEnAttente(element.getId(), element.getCode());
        return soumettre(element.getId(),
                DemandeNomenclatureEvent.changementEtat(OperationReferentiel.REACTIVER, facultatif(input.motif())),
                element.getCode());
    }

    /**
     * Tranche une demande portant sur la nomenclature.
     *
     * <p>
     * Mêmes garde-fous que pour l'exercice : demande relue en base, auteur
     * comparé à l'identifiant enregistré, motif exigé pour un rejet. Si une
     * règle n'est plus respectée au moment de valider, la validation est
     * refusée et la demande reste en attente : le validateur la rejette alors
     * avec le motif affiché.
     * </p>
     */
    @Transactional
    public void decider(DeciderDemandeNomenclatureInput input) {
        utilsService.hasPermission(Permissions.VALIDATE_OR_REJECT_BUDGET_NOMENCLATURE);

        CircuitReferentielBudgetaire.DecisionOuverte decision = circuit.ouvrirDecision(
                MakerCheckerEntityName.BUDGET_NOMENCLATURE, input.requestId(), input.decision(), input.notes(),
                "Aucune demande sur la nomenclature budgétaire ne porte cet identifiant.");
        if (decision.acceptee()) {
            appliquer(decision.demande(), decision.validateur());
        }
        circuit.cloturer(decision, input.notes());
    }

    // ─── Lectures ───────────────────────────────────────────────────────────

    /**
     * L'arbre complet de la nomenclature, programmes en tête, enfants triés par code.
     *
     * <p>
     * Non paginé, par exception à la règle du dépôt : un arbre coupé en pages
     * n'a pas de sens, et la nomenclature de la SODEPA compte au plus quelques
     * centaines d'éléments, lus en une requête.
     * </p>
     *
     * @param inclureInactifs vrai pour montrer aussi les éléments désactivés
     */
    @Transactional(readOnly = true)
    public List<NoeudNomenclatureOutput> arbre(boolean inclureInactifs) {
        utilsService.hasPermission(Permissions.GET_BUDGET_NOMENCLATURE_INFO);

        List<ElementProgrammatiqueEntity> elements = elementRepository.findAll(Sort.by("code")).stream()
                .filter(e -> inclureInactifs || e.isActif())
                .toList();
        Map<UUID, String> noms = nomsDesResponsables(elements);
        Map<UUID, List<ElementProgrammatiqueEntity>> parParent = new HashMap<>();
        List<ElementProgrammatiqueEntity> racines = new ArrayList<>();
        for (ElementProgrammatiqueEntity e : elements) {
            if (e.getParentId() == null) {
                racines.add(e);
            } else {
                parParent.computeIfAbsent(e.getParentId(), k -> new ArrayList<>()).add(e);
            }
        }
        return racines.stream().map(r -> noeud(r, parParent, noms)).toList();
    }

    /** Fiche d'un élément. */
    @Transactional(readOnly = true)
    public ElementProgrammatiqueOutput consulter(UUID id) {
        utilsService.hasPermission(Permissions.GET_BUDGET_NOMENCLATURE_INFO);

        ElementProgrammatiqueEntity element = trouver(id);
        String parentCode = element.getParentId() == null ? null
                : elementRepository.findById(element.getParentId()).map(ElementProgrammatiqueEntity::getCode).orElse(null);
        return ElementProgrammatiqueOutput.depuis(element, parentCode,
                nomDuResponsable(nomsDesResponsables(List.of(element)), element.getResponsableId()));
    }

    /**
     * Demandes en attente que l'appelant peut trancher.
     *
     * <p>Les siennes sont écartées : il ne pourrait que se les voir refuser.</p>
     */
    @Transactional(readOnly = true)
    public PageRecord<MakerCheckerSmartOutput> demandesEnAttente(Pageable pageable) {
        utilsService.hasPermission(Permissions.VALIDATE_OR_REJECT_BUDGET_NOMENCLATURE);

        return circuit.demandesEnAttente(MakerCheckerEntityName.BUDGET_NOMENCLATURE, pageable);
    }

    // ─── Application d'une demande validée ──────────────────────────────────

    private void appliquer(MakerCheckerRequestEntity demande, String validateur) {
        DemandeNomenclatureEvent charge = DemandeNomenclatureEvent.depuisCharge(demande.getPayload());
        UUID elementId = UUID.fromString(demande.getEntityPk());
        if (charge.operation() == OperationReferentiel.CREER) {
            creer(demande, charge, elementId, validateur);
            return;
        }

        ElementProgrammatiqueEntity element = trouver(elementId);
        switch (charge.operation()) {
            case MODIFIER -> {
                exigerActif(element);
                controlerResponsable(element.getNiveau(), charge.responsableId());
                element.setLibelle(charge.libelle());
                element.setDescription(charge.description());
                element.setResponsableId(charge.responsableId());
            }
            case DESACTIVER -> {
                if (!element.isActif()) {
                    throw new ConflitMetierException("ELEMENT_DEJA_INACTIF",
                            "L'élément " + element.getCode() + " est déjà désactivé.");
                }
                controlerDesactivable(element);
                element.setActif(false);
                element.setMotifDesactivation(charge.motif());
            }
            case REACTIVER -> {
                if (element.isActif()) {
                    throw new ConflitMetierException("ELEMENT_DEJA_ACTIF",
                            "L'élément " + element.getCode() + " est déjà actif.");
                }
                controlerParentActif(element);
                element.setActif(true);
                element.setMotifDesactivation(null);
            }
            default -> throw new IllegalStateException("Opération déjà traitée : " + charge.operation());
        }
        element.setModifiePar(demande.getMakerId());
        element.setModifieLe(horloge.instant());
        try {
            elementRepository.saveAndFlush(element);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ConflitMetierException("ELEMENT_MODIFIE_ENTRE_TEMPS",
                    "L'élément " + element.getCode() + " vient d'être modifié : rechargez-le avant de décider.");
        }
        log.info("Nomenclature : {} de l'élément {} appliquée (demande {}).",
                charge.operation(), element.getCode(), demande.getId());
    }

    private void creer(MakerCheckerRequestEntity demande, DemandeNomenclatureEvent charge, UUID elementId,
                       String validateur) {
        if (elementRepository.existsByCode(charge.code())) {
            throw codeExistant(charge.code());
        }
        controlerParent(charge.niveau(), charge.parentId());
        controlerResponsable(charge.niveau(), charge.responsableId());

        ElementProgrammatiqueEntity element = ElementProgrammatiqueEntity.builder()
                .id(elementId)
                .code(charge.code())
                .libelle(charge.libelle())
                .description(charge.description())
                .niveau(charge.niveau())
                .parentId(charge.parentId())
                .responsableId(charge.responsableId())
                .actif(true)
                .demandeCreationId(demande.getId())
                .creePar(demande.getMakerId())
                .creeLe(demande.getCreatedAt())
                .validePar(validateur)
                .valideLe(horloge.instant())
                .build();
        try {
            // Écriture immédiate : une validation concurrente du même code échoue
            // ici, sur la contrainte d'unicité, et non au commit en 500.
            elementRepository.saveAndFlush(element);
        } catch (DataIntegrityViolationException e) {
            throw codeExistant(charge.code());
        }
        log.info("Nomenclature : {} {} créé (demande {}).", charge.niveau(), charge.code(), demande.getId());
    }

    // ─── Règles ─────────────────────────────────────────────────────────────

    private NiveauProgrammatique niveauGere(NiveauProgrammatique niveau) {
        if (niveau == null) {
            throw new RequeteInvalideException("NIVEAU_OBLIGATOIRE",
                    "Indiquez le niveau de l'élément : programme ou action.");
        }
        if (!niveau.gereDepuis104()) {
            throw new RequeteInvalideException("NIVEAU_NON_GERE",
                    "Les activités et les tâches ne se saisissent pas encore : seuls les programmes et les actions sont gérés.");
        }
        return niveau;
    }

    /** Code en majuscules, sans espaces autour, dans les caractères admis. */
    static String codeValide(String code) {
        if (code == null || code.isBlank()) {
            throw new RequeteInvalideException("CODE_OBLIGATOIRE", "Indiquez le code de l'élément.");
        }
        String normalise = code.strip().toUpperCase(Locale.ROOT);
        if (!FORME_CODE.matcher(normalise).matches()) {
            throw new RequeteInvalideException("CODE_INVALIDE",
                    "Le code ne doit contenir que des lettres sans accent, des chiffres et les séparateurs "
                            + "« - », « . » ou « _ », sans espace, et 30 caractères au plus.");
        }
        return normalise;
    }

    private static String libelleValide(String libelle) {
        String valeur = facultatif(libelle);
        if (valeur == null) {
            throw new RequeteInvalideException("LIBELLE_OBLIGATOIRE", "Indiquez le libellé de l'élément.");
        }
        return valeur;
    }

    /**
     * Le parent doit exister, être du niveau juste au-dessus et actif.
     * Un programme n'a pas de parent.
     */
    private void controlerParent(NiveauProgrammatique niveau, UUID parentId) {
        NiveauProgrammatique attendu = niveau.niveauParent();
        if (attendu == null) {
            if (parentId != null) {
                throw new RequeteInvalideException("PARENT_INVALIDE", "Un programme ne se rattache à aucun parent.");
            }
            return;
        }
        String phrase = majuscule(niveau.avecArticle()) + " se rattache à " + attendu.avecArticle();
        if (parentId == null) {
            throw new RequeteInvalideException("PARENT_INVALIDE", phrase + " : indiquez-le.");
        }
        ElementProgrammatiqueEntity parent = elementRepository.findById(parentId)
                .orElseThrow(() -> new RequeteInvalideException("PARENT_INVALIDE",
                        "Le parent désigné n'existe pas dans la nomenclature."));
        if (parent.getNiveau() != attendu) {
            throw new RequeteInvalideException("PARENT_INVALIDE",
                    phrase + ", pas à " + parent.getNiveau().avecArticle() + " (" + parent.getCode() + ").");
        }
        if (!parent.isActif()) {
            throw new ConflitMetierException("PARENT_INACTIF",
                    "L'élément " + parent.getCode() + " est désactivé : il ne reçoit plus de nouvel élément.");
        }
    }

    /** Réactiver un enfant sous un parent désactivé recréerait une branche morte. */
    private void controlerParentActif(ElementProgrammatiqueEntity element) {
        if (element.getParentId() == null) {
            return;
        }
        elementRepository.findById(element.getParentId())
                .filter(parent -> !parent.isActif())
                .ifPresent(parent -> {
                    throw new ConflitMetierException("PARENT_INACTIF",
                            "L'élément " + parent.getCode() + " est désactivé : réactivez-le d'abord.");
                });
    }

    /**
     * Le responsable est un utilisateur actif de l'ERP : il recevra les
     * notifications et la consultation de son programme. Obligatoire pour un
     * programme, facultatif en dessous.
     */
    private void controlerResponsable(NiveauProgrammatique niveau, UUID responsableId) {
        if (responsableId == null) {
            if (niveau == NiveauProgrammatique.PROGRAMME) {
                throw new RequeteInvalideException("RESPONSABLE_INVALIDE", "Désignez le responsable du programme.");
            }
            return;
        }
        boolean actif = utilisateurRepository.findById(responsableId).map(UtilisateurEntity::isActif).orElse(false);
        if (!actif) {
            throw new RequeteInvalideException("RESPONSABLE_INVALIDE",
                    "Le responsable désigné n'est pas un utilisateur actif de l'ERP.");
        }
    }

    /**
     * Pas de désactivation en cascade : chaque enfant se désactive d'abord,
     * pour que personne ne retire une branche entière sans la voir. Un élément
     * qui porte du budget en cours d'exécution ne se désactive pas.
     */
    private void controlerDesactivable(ElementProgrammatiqueEntity element) {
        if (elementRepository.existsByParentIdAndActifTrue(element.getId())) {
            throw new ConflitMetierException("ELEMENT_A_DES_ENFANTS_ACTIFS",
                    "L'élément " + element.getCode() + " a encore des éléments actifs : désactivez-les d'abord.");
        }
        if (usageReferentiels.elementUtilise(element.getId())) {
            throw new ConflitMetierException("ELEMENT_UTILISE",
                    "L'élément " + element.getCode() + " porte des lignes d'un exercice en cours d'exécution : "
                            + "il ne pourra être désactivé qu'après la clôture de l'exercice.");
        }
    }

    private void exigerActif(ElementProgrammatiqueEntity element) {
        if (!element.isActif()) {
            throw new ConflitMetierException("ELEMENT_INACTIF",
                    "L'élément " + element.getCode() + " est désactivé : réactivez-le avant de le modifier.");
        }
    }

    /**
     * Une seule demande à la fois par élément : sans ce contrôle, deux
     * demandes contradictoires (modifier et désactiver) pouvaient être
     * validées l'une après l'autre sans que personne voie l'autre.
     */
    private void refuserSiDemandeEnAttente(UUID elementId, String code) {
        circuit.refuserSiDemandeEnAttente(MakerCheckerEntityName.BUDGET_NOMENCLATURE, elementId, "NOMENCLATURE_DEMANDE_EN_ATTENTE",
                "Une demande sur l'élément " + code + " attend déjà sa validation.");
    }

    private SubmissionOutput soumettre(UUID elementId, DemandeNomenclatureEvent demande, String code) {
        UUID demandeId = circuit.soumettre(MakerCheckerEntityName.BUDGET_NOMENCLATURE, elementId, demande.versCharge(),
                demande.operation().typeMakerChecker());
        log.info("Nomenclature : {} de l'élément {} soumise (demande {}).", demande.operation(), code, demandeId);
        return new SubmissionOutput(demandeId, elementId);
    }

    private ElementProgrammatiqueEntity trouver(UUID id) {
        return elementRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Aucun élément de la nomenclature ne porte cet identifiant."));
    }

    private Map<UUID, String> nomsDesResponsables(Collection<ElementProgrammatiqueEntity> elements) {
        List<UUID> ids = elements.stream().map(ElementProgrammatiqueEntity::getResponsableId)
                .filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return utilisateurRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(UtilisateurEntity::getId, u -> (u.getPrenom() + " " + u.getNom()).strip()));
    }

    /** Une action peut n'avoir aucun responsable : {@code Map.of()} refuse une clé nulle. */
    private static String nomDuResponsable(Map<UUID, String> noms, UUID responsableId) {
        return responsableId == null ? null : noms.get(responsableId);
    }

    private static NoeudNomenclatureOutput noeud(ElementProgrammatiqueEntity e,
                                                 Map<UUID, List<ElementProgrammatiqueEntity>> parParent,
                                                 Map<UUID, String> noms) {
        List<NoeudNomenclatureOutput> enfants = parParent.getOrDefault(e.getId(), List.of()).stream()
                .sorted(Comparator.comparing(ElementProgrammatiqueEntity::getCode))
                .map(enfant -> noeud(enfant, parParent, noms))
                .toList();
        return new NoeudNomenclatureOutput(e.getId(), e.getCode(), e.getLibelle(), e.getNiveau(),
                e.getResponsableId(), nomDuResponsable(noms, e.getResponsableId()), e.isActif(), enfants);
    }

    private static ConflitMetierException codeExistant(String code) {
        return new ConflitMetierException("CODE_NOMENCLATURE_EXISTANT",
                "Le code " + code + " est déjà utilisé dans la nomenclature, par un élément actif ou désactivé.");
    }

    private static String facultatif(String texte) {
        return texte == null || texte.isBlank() ? null : texte.strip();
    }

    private static String majuscule(String texte) {
        return Character.toUpperCase(texte.charAt(0)) + texte.substring(1);
    }
}
