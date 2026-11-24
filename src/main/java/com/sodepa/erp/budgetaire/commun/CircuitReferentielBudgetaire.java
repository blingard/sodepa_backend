package com.sodepa.erp.budgetaire.commun;

import com.sodepa.erp.share.MakerCheckerRequestEntity;
import com.sodepa.erp.share.MakerCheckerRequestJpaRepo;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.share.erreurs.ConflitMetierException;
import com.sodepa.erp.share.erreurs.RequeteInvalideException;
import com.sodepa.erp.share.erreurs.RessourceIntrouvableException;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import com.sodepa.erp.user.infrastructure.repo.UserRepository;
import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.MakerCheckerOperationType;
import com.sodepa.erp.utils.MakerCheckerStatus;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.utils.PageableRecord;
import com.sodepa.erp.workflow.application.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Circuit maker-checker commun aux objets du module budgétaire (exercices,
 * nomenclature, structures, natures, sources de financement).
 *
 * <p>
 * Chaque adaptateur garde ses règles métier et ses droits ; ce composant porte
 * ce qui est identique partout : refuser une seconde demande en attente sur le
 * même objet, soumettre, ouvrir puis clore une décision avec les garde-fous de
 * la règle des quatre yeux, et lister les demandes à trancher.
 * </p>
 *
 * <p>
 * L'ouverture de la décision relit la demande en base : l'auteur doit être
 * comparé à l'identifiant enregistré, pas à sa projection, qui invente un
 * identifiant quand l'utilisateur n'est pas retrouvé.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CircuitReferentielBudgetaire {

    /** Taille de page maximale, comme partout dans l'API. */
    public static final int TAILLE_PAGE_MAX = 100;

    private final MakerCheckerRequestJpaRepo demandeRepository;
    private final WorkflowService workflowService;
    private final UtilsService utilsService;
    private final UserRepository utilisateurRepository;
    private final Clock horloge;

    /**
     * Décision ouverte : la demande, relue et contrôlée, et son validateur.
     *
     * @param demande    demande maker-checker en attente
     * @param decision   {@code ACCEPTED} ou {@code REJECTED}
     * @param validateur identifiant de l'utilisateur qui tranche, différent de l'auteur
     */
    public record DecisionOuverte(MakerCheckerRequestEntity demande, MakerCheckerStatus decision, String validateur) {

        public boolean acceptee() {
            return decision == MakerCheckerStatus.ACCEPTED;
        }
    }

    /**
     * Refuse une demande quand une autre, non expirée, attend déjà sur le même
     * objet : sans ce contrôle, deux demandes contradictoires pouvaient être
     * validées l'une après l'autre sans que personne voie l'autre.
     */
    public void refuserSiDemandeEnAttente(MakerCheckerEntityName type, UUID objetId, String code, String message) {
        Instant maintenant = horloge.instant();
        boolean enAttente = demandeRepository
                .findByEntityPkAndEntityNameAndStatusOrderByCreatedAtDesc(objetId.toString(), type, MakerCheckerStatus.PENDING)
                .stream()
                .anyMatch(d -> d.getExpiredAt() == null || d.getExpiredAt().isAfter(maintenant));
        if (enAttente) {
            throw new ConflitMetierException(code, message);
        }
    }

    /** Soumet la demande et rend son identifiant. */
    public UUID soumettre(MakerCheckerEntityName type, UUID objetId, Map<String, Object> charge,
                          MakerCheckerOperationType operation) {
        return workflowService.submitChange(type, objetId.toString(), charge, operation);
    }

    /**
     * Ouvre la décision sur une demande : elle doit exister pour ce type
     * d'objet, être en attente et non expirée ; un rejet doit être motivé ;
     * le validateur doit différer de l'auteur.
     *
     * @param introuvable message rendu si la demande n'existe pas pour ce type
     */
    public DecisionOuverte ouvrirDecision(MakerCheckerEntityName type, UUID requestId, MakerCheckerStatus decision,
                                          String notes, String introuvable) {
        MakerCheckerRequestEntity demande = demandeRepository.findById(requestId)
                .filter(d -> d.getEntityName() == type)
                .orElseThrow(() -> new RessourceIntrouvableException(introuvable));

        if (demande.getStatus() != MakerCheckerStatus.PENDING) {
            throw new ConflitMetierException("DEMANDE_DEJA_TRANCHEE", "Cette demande a déjà été traitée.");
        }
        if (decision != MakerCheckerStatus.ACCEPTED && decision != MakerCheckerStatus.REJECTED) {
            throw new RequeteInvalideException("DECISION_INVALIDE", "La décision doit être une validation ou un rejet.");
        }
        if (decision == MakerCheckerStatus.REJECTED && (notes == null || notes.isBlank())) {
            throw new RequeteInvalideException("MOTIF_OBLIGATOIRE", "Indiquez le motif du rejet.");
        }

        String validateur = utilsService.getCurrentUserData().userId();
        if (validateur.equals(demande.getMakerId())) {
            throw new ConflitMetierException("VALIDATEUR_EST_AUTEUR",
                    "Vous ne pouvez pas valider votre propre demande : un second utilisateur doit la traiter.");
        }

        workflowService.validate(demande.getId(), demande.getCreatedAt(), demande.getExpiredAt());
        return new DecisionOuverte(demande, decision, validateur);
    }

    /** Enregistre la décision, une fois la demande appliquée si elle est acceptée. */
    public void cloturer(DecisionOuverte decision, String notes) {
        workflowService.update(decision.demande().getId(), decision.decision(), notes, decision.validateur());
    }

    /**
     * Demandes en attente que l'appelant peut trancher, avec leur contenu.
     *
     * <p>
     * Les siennes sont écartées : il ne pourrait que se les voir refuser. Les
     * demandes sont relues avec leur charge, que {@code projeter} traduit en
     * fiche proposée et fiche en vigueur ; le valideur voit ainsi ce qu'il
     * approuve.
     * </p>
     *
     * <p>
     * Une demande dont la charge ne se relit plus reste listée, sans aperçu :
     * elle doit pouvoir être rejetée, et une seule demande abîmée ne doit pas
     * rendre toute la file illisible.
     * </p>
     *
     * @param projeter traduction d'une demande par le référentiel qu'elle vise
     */
    public <T> PageRecord<DemandeBudgetaireOutput<T>> demandesEnAttente(
            MakerCheckerEntityName type, Pageable pageable,
            Function<MakerCheckerRequestEntity, ProjectionDemande<T>> projeter) {
        String moi = utilsService.getCurrentUserData().userId();
        Page<MakerCheckerRequestEntity> demandes = demandeRepository.findAllByEntityNameAndStatusAndMakerIdNot(
                type, MakerCheckerStatus.PENDING, moi,
                PageRequest.of(numero(pageable), taille(pageable), Sort.by(Sort.Direction.DESC, "createdAt")));
        Map<String, String> auteurs = nomsDesAuteurs(demandes.getContent());
        return versPageRecord(demandes.map(d -> enveloppe(d, projection(d, projeter), auteurs.get(d.getMakerId()))));
    }

    private static <T> ProjectionDemande<T> projection(MakerCheckerRequestEntity demande,
                                                       Function<MakerCheckerRequestEntity, ProjectionDemande<T>> projeter) {
        try {
            return projeter.apply(demande);
        } catch (RuntimeException e) {
            log.warn("Demande {} sur {} affichée sans aperçu : {}", demande.getId(), demande.getEntityName(),
                    e.getMessage());
            return new ProjectionDemande<>(null, null, null, null);
        }
    }

    private static <T> DemandeBudgetaireOutput<T> enveloppe(MakerCheckerRequestEntity d, ProjectionDemande<T> projection,
                                                            String auteurNom) {
        return new DemandeBudgetaireOutput<>(
                d.getId(),
                d.getEntityName(),
                d.getEntityPk(),
                d.getStatus(),
                d.getCheckerOperationType(),
                projection.operation(),
                d.getMakerId(),
                auteurNom,
                d.getCreatedAt(),
                d.getExpiredAt(),
                projection.motif(),
                projection.proposition(),
                projection.actuel());
    }

    /**
     * Prénom et nom des auteurs, en une requête. Un auteur introuvable, ou dont
     * l'identifiant n'est pas un UUID (compte technique), reste sans nom.
     */
    private Map<String, String> nomsDesAuteurs(Iterable<MakerCheckerRequestEntity> demandes) {
        Set<UUID> ids = new HashSet<>();
        for (MakerCheckerRequestEntity d : demandes) {
            try {
                ids.add(UUID.fromString(d.getMakerId()));
            } catch (IllegalArgumentException | NullPointerException e) {
                // Pas un utilisateur de l'ERP : on garde l'identifiant brut.
            }
        }
        Map<String, String> noms = new HashMap<>();
        if (!ids.isEmpty()) {
            for (UtilisateurEntity u : utilisateurRepository.findAllById(ids)) {
                noms.put(u.getId().toString(), nomComplet(u));
            }
        }
        return noms;
    }

    /** « Prénom Nom », sans « null » si l'un des deux manque. */
    public static String nomComplet(UtilisateurEntity u) {
        String prenom = u.getPrenom() == null ? "" : u.getPrenom();
        String nom = u.getNom() == null ? "" : u.getNom();
        String complet = (prenom + " " + nom).strip();
        return complet.isEmpty() ? null : complet;
    }

    // ─── Pagination ─────────────────────────────────────────────────────────

    /** Page demandée, triée comme indiqué, plafonnée à {@link #TAILLE_PAGE_MAX} lignes. */
    public static Pageable page(Pageable pageable, Sort tri) {
        return PageRequest.of(numero(pageable), taille(pageable), tri);
    }

    public static int taille(Pageable pageable) {
        return pageable.isPaged() ? Math.min(Math.max(pageable.getPageSize(), 1), TAILLE_PAGE_MAX) : TAILLE_PAGE_MAX;
    }

    private static int numero(Pageable pageable) {
        return pageable.isPaged() ? pageable.getPageNumber() : 0;
    }

    public static <T> PageRecord<T> versPageRecord(Page<T> page) {
        return new PageRecord<>(
                page.getContent(),
                page.isEmpty(),
                page.isFirst(),
                page.isLast(),
                page.getNumber(),
                page.getNumberOfElements(),
                new PageableRecord(
                        page.getPageable().getOffset(),
                        page.getPageable().getPageNumber(),
                        page.getPageable().getPageSize(),
                        true,
                        page.getSort(),
                        false),
                page.getSize(),
                page.getSort(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
