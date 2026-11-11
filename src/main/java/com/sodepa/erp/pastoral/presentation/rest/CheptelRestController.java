package com.sodepa.erp.pastoral.presentation.rest;

import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.pastoral.application.CheptelService;
import com.sodepa.erp.pastoral.infrastructure.entities.StatutBovin;
import com.sodepa.erp.pastoral.presentation.requests.*;
import com.sodepa.erp.pastoral.presentation.responses.*;
import com.sodepa.erp.share.RejectOrValidateSubmitRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Service de Gestion du Cheptel, de l'Exploitation Pastorale et de la Comptabilité Agricole (Module 08 - PASTORAL).
 * <p>
 * Ce service constitue le cœur de métier historique de la SODEPA (Société de Développement et d'Exploitation des Productions Animales) :
 * <ul>
 *   <li><b>Ranches pastoraux</b> : Gestion des sites d'élevage bovin (Ranches de Dumbo, Jakiri et Faro).</li>
 *   <li><b>Suivi zootechnique individualisé</b> : Identification par boucle auriculaire, date de naissance, race, statut sanitaire et pesées de croissance (gain moyen quotidien).</li>
 *   <li><b>Événements du cycle de vie animal</b> : Constatation des naissances, reclassement par classe d'âge (broutards, génisses, taureaux de reproduction), transferts inter-ranches et départs vers les abattoirs frigorifiques.</li>
 *   <li><b>Passation automatique des écritures SYSCOHADA agricole</b> :
 *       Immobilisations biologiques (comptes 2191, 2462), production immobilisée (7217), amortissement du cheptel reproducteur et valorisation du stock marchand.</li>
 *   <li><b>Inventaire de fin d'exercice (Herd Composition)</b> : Arrêté officiel des effectifs par catégorie zootechnique et constatation des écarts d'inventaire.</li>
 * </ul>
 */
@RestController
@RequestMapping({"/api/pastoral", "/api/v1/pastoral"})
@RequiredArgsConstructor
@Tag(name = "Cheptel, Ranches & Comptabilité Pastorale SODEPA", description = "Gestion zootechnique des bovins (Dumbo, Jakiri, Faro), mouvements et écritures SYSCOHADA (2191, 2496, 2462, 3880)")
public class CheptelRestController {

    private final CheptelService cheptelService;

    // ==========================================
    // RÉFÉRENTIELS RANCHES & CATÉGORIES
    // ==========================================

    /**
     * Liste les ranches et stations pastorales gérés par la SODEPA (Dumbo, Jakiri, Faro).
     *
     * @return la liste des ranches pastoraux en activité
     */
    @GetMapping("/ranches")
    @Operation(summary = "Lister les ranches pastoraux de la SODEPA (Dumbo, Jakiri, Faro)")
    public ResponseEntity<List<PastoralRanchResponse>> listerRanches() {
        return ResponseEntity.ok(cheptelService.listerRanches());
    }

    /**
     * Liste les catégories zootechniques officielles (Veaux, Broutards, Génisses, Taureaux, Vaches de réforme) et leurs comptes comptables rattachés.
     *
     * @return la liste des catégories zootechniques du plan comptable pastoral
     */
    @GetMapping("/categories")
    @Operation(summary = "Lister les catégories zootechniques et comptes SYSCOHADA associés")
    public ResponseEntity<List<PastoralCategorieCheptelResponse>> listerCategories() {
        return ResponseEntity.ok(cheptelService.listerCategories());
    }

    // ==========================================
    // ZOOTECHNIE DU CHEPTEL
    // ==========================================

    /**
     * Recherche et liste les têtes de bétail enregistrées avec pagination et filtrage par ranch et état de santé.
     *
     * @param ranchId filtre optionnel pour restreindre à un ranch précis (ex: Ranch de Faro)
     * @param statut état zootechnique (EN_VIE, SOUS_TRAITEMENT, REFORME, VENDU, MORT)
     * @param pageable critères de pagination et de tri
     * @return la page des fiches bovines correspondantes
     */
    @GetMapping("/bovins")
    @Operation(summary = "Lister les têtes de bétail avec filtres par ranch et statut")
    public ResponseEntity<Page<PastoralBovinResponse>> listerBovins(
            @RequestParam(required = false) UUID ranchId,
            @RequestParam(required = false) StatutBovin statut,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(cheptelService.listerBovins(ranchId, statut, pageable));
    }

    /**
     * Consulte la fiche d'identité zootechnique et sanitaire complète d'un bovin à partir de son identifiant unique.
     *
     * @param id identifiant unique du bovin
     * @return le dossier zootechnique (numéro de boucle, filiation, historique des pesées et traitements)
     */
    @GetMapping("/bovins/{id}")
    @Operation(summary = "Consulter la fiche zootechnique d'un bovin")
    public ResponseEntity<PastoralBovinResponse> obtenirBovin(@PathVariable UUID id) {
        return ResponseEntity.ok(cheptelService.obtenirBovin(id));
    }

    /**
     * Constate et enregistre une naissance de veau dans un ranch pastoral.
     * <p>
     * Attribue un numéro de boucle officiel, identifie la mère et génère automatiquement l'écriture
     * de production immobilisée agricole conforme au SYSCOHADA (Débit 2191 / Crédit 7217).
     *
     * @param request données de mise bas (ranch, date de naissance, sexe, poids à la naissance, mère)
     * @return la fiche du veau créé
     */
    @PostMapping("/bovins/naissance")
    @Operation(summary = "Enregistrer une naissance au ranch (Débit 2191 / Crédit 7217 SYSCOHADA)")
    public ResponseEntity<PastoralBovinResponse> enregistrerNaissance(@Valid @RequestBody EnregistrerNaissanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cheptelService.enregistrerNaissance(request));
    }

    /**
     * Enregistre une pesée périodique et le bilan du contrôle sanitaire d'un animal.
     * <p>
     * Permet aux zootechniciens de calculer le Gain Moyen Quotidien (GMQ) et d'évaluer la croissance pondérale.
     *
     * @param id identifiant du bovin
     * @param request données de pesée (poids mesuré en kg, date, observations vétérinaires)
     * @return la fiche de l'animal mise à jour
     */
    @PostMapping("/bovins/{id}/pesee")
    @Operation(summary = "Enregistrer une pesée de croissance et contrôle sanitaire")
    public ResponseEntity<PastoralBovinResponse> enregistrerPesee(
            @PathVariable UUID id,
            @Valid @RequestBody EnregistrerPeseeRequest request) {
        request.setBovinId(id);
        return ResponseEntity.ok(cheptelService.enregistrerPesee(request));
    }

    /**
     * Opère le reclassement zootechnique d'un animal lors de son passage à la classe d'âge supérieure.
     * <p>
     * Déclenche les écritures de virement d'immobilisation biologique (Transfer-Out ancienne classe / Transfer-In nouvelle classe).
     *
     * @param id identifiant du bovin
     * @param request nouvelle catégorie zootechnique et motif
     * @return la fiche mise à jour
     */
    @PostMapping("/bovins/{id}/changement-categorie")
    @Operation(summary = "Reclassement de catégorie zootechnique (Transfer-Out / Transfer-In SYSCOHADA)")
    public ResponseEntity<PastoralBovinResponse> enregistrerChangementCategorie(
            @PathVariable UUID id,
            @Valid @RequestBody EnregistrerChangementCategorieRequest request) {
        request.setBovinId(id);
        return ResponseEntity.ok(cheptelService.enregistrerChangementCategorie(request));
    }

    /**
     * Enregistre le convoiement et le transfert physique d'un animal vers un autre ranch ou vers un abattoir frigorifique.
     *
     * @param id identifiant de l'animal
     * @param request destination (autre ranch, abattoir de Yaoundé/Douala) et moyen de transport
     * @return la fiche actualisée avec sa nouvelle localisation
     */
    @PostMapping("/bovins/{id}/transfert-ranch")
    @Operation(summary = "Enregistrer un transfert inter-ranches ou vers abattoir")
    public ResponseEntity<PastoralBovinResponse> enregistrerTransfertRanch(
            @PathVariable UUID id,
            @Valid @RequestBody EnregistrerTransfertRanchRequest request) {
        request.setBovinId(id);
        return ResponseEntity.ok(cheptelService.enregistrerTransfertRanch(request));
    }

    /**
     * Constate la sortie définitive d'un animal de l'effectif du cheptel (Mortalité, Vente sur pied, Abattage ou Réforme).
     * <p>
     * Génère les écritures comptables appropriées : charge exceptionnelle pour mortalité (compte 8310),
     * produit de cession pour vente (compte 7360) ou sortie d'immobilisation pour réforme (compte 3880).
     *
     * @param id identifiant de l'animal sorti
     * @param request motif de sortie, date et certificat vétérinaire ou bordereau de vente
     * @return la fiche de l'animal clôturée
     */
    @PostMapping("/bovins/{id}/sortie")
    @Operation(summary = "Enregistrer une sortie de cheptel (Mortalité 8310, Vente/Abattage 7360, Réforme 3880)")
    public ResponseEntity<PastoralBovinResponse> enregistrerSortie(
            @PathVariable UUID id,
            @Valid @RequestBody EnregistrerSortieCheptelRequest request) {
        request.setBovinId(id);
        return ResponseEntity.ok(cheptelService.enregistrerSortie(request));
    }

    /**
     * Traite l'approbation Maker-Checker sur une opération sensible touchant au cheptel (naissance, sortie exceptionnelle).
     *
     * @param id identifiant du ticket de soumission
     * @param request décision formelle
     * @return confirmation
     */
    @PutMapping("/bovins/validate_or_reject/{id}")
    @Operation(summary = "Valider ou rejeter une opération sur bovin (Maker-Checker)")
    public ResponseEntity<Void> validateOrRejectBovin(
            @PathVariable UUID id,
            @Valid @RequestBody RejectOrValidateSubmitRequest request) {
        ValidateOrRejectSubmissionInput input = new ValidateOrRejectSubmissionInput(
                id,
                request.decision(),
                request.notes(),
                request.checkerOperationType()
        );
        cheptelService.validateOrRejectBovin(input);
        return ResponseEntity.ok().build();
    }

    // ==========================================
    // HERD COMPOSITION & TABLEAU DE BORD
    // ==========================================

    /**
     * Consulte les fiches officielles d'inventaire pastoral (Herd Composition) établies lors de la clôture des exercices.
     *
     * @param annee exercice inventorié
     * @return la liste des synthèses d'inventaire
     */
    @GetMapping("/herd-compositions")
    @Operation(summary = "Consulter les fiches d'inventaire Herd Composition de fin d'exercice")
    public ResponseEntity<List<PastoralHerdCompositionResponse>> listerHerdCompositions(
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(cheptelService.listerHerdCompositions(annee));
    }

    /**
     * Calcule, clôture et génère l'inventaire officiel du troupeau (Herd Composition) pour un ranch donné.
     * <p>
     * Dénombre les animaux vivants par catégorie zootechnique et établit le bilan annuel des entrées/sorties.
     *
     * @param ranchId identifiant du ranch audité
     * @param annee exercice de clôture
     * @return le Herd Composition certifié
     */
    @PostMapping("/herd-compositions/generer")
    @Operation(summary = "Générer et clôturer le Herd Composition d'un ranch pour un exercice")
    public ResponseEntity<PastoralHerdCompositionResponse> genererHerdComposition(
            @RequestParam UUID ranchId,
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(cheptelService.genererHerdComposition(ranchId, annee));
    }

    /**
     * Traite la validation contradictoire d'un inventaire Herd Composition dans le cadre du contrôle à 4 yeux.
     *
     * @param id identifiant du ticket
     * @param request décision du commissaire ou chef de service
     * @return confirmation
     */
    @PutMapping("/herd-compositions/validate_or_reject/{id}")
    @Operation(summary = "Valider ou rejeter un inventaire Herd Composition (Maker-Checker)")
    public ResponseEntity<Void> validateOrRejectHerdComposition(
            @PathVariable UUID id,
            @Valid @RequestBody RejectOrValidateSubmitRequest request) {
        ValidateOrRejectSubmissionInput input = new ValidateOrRejectSubmissionInput(
                id,
                request.decision(),
                request.notes(),
                request.checkerOperationType()
        );
        cheptelService.validateOrRejectHerdComposition(input);
        return ResponseEntity.ok().build();
    }

    /**
     * Restitue le tableau de bord exécutif de synthèse du cheptel pour la Direction Générale.
     * <p>
     * Présente les effectifs totaux consolidés, la ventilation par ranch, les taux de mortalité et de natalité de la période.
     *
     * @return les indicateurs zootechniques consolidés de la SODEPA
     */
    @GetMapping("/tableau-bord")
    @Operation(summary = "Tableau de bord de synthèse du cheptel pastoral SODEPA")
    public ResponseEntity<PastoralTableauBordResponse> obtenirTableauBord() {
        return ResponseEntity.ok(cheptelService.obtenirTableauBord());
    }
}
