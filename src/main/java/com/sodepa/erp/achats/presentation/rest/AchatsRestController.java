package com.sodepa.erp.achats.presentation.rest;

import com.sodepa.erp.achats.application.AchatsSecurityService;
import com.sodepa.erp.achats.application.AchatsService;
import com.sodepa.erp.achats.presentation.requests.CreerExpressionBesoinRequest;
import com.sodepa.erp.achats.presentation.requests.CreerFournisseurRequest;
import com.sodepa.erp.achats.presentation.requests.CreerMarcheDossierRequest;
import com.sodepa.erp.achats.presentation.requests.EnregistrerPvReceptionRequest;
import com.sodepa.erp.achats.presentation.requests.RejectOrValidateAchatsSubmitRequest;
import com.sodepa.erp.achats.presentation.responses.ExpressionBesoinResponse;
import com.sodepa.erp.achats.presentation.responses.FournisseurResponse;
import com.sodepa.erp.achats.presentation.responses.MarcheDossierResponse;
import com.sodepa.erp.achats.presentation.responses.MarcheReceptionPvResponse;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.utils.MakerCheckerOperationType;
import com.sodepa.erp.utils.Permissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service de Gestion des Achats, Approvisionnements et Marchés Publics SODEPA (Module 04 - ACH / Marches).
 * <p>
 * Ce service digitalise la chaîne d'acquisition complète de la SODEPA conformément au Code des Marchés Publics :
 * <ul>
 *   <li><b>Gestion du répertoire des fournisseurs et prestataires</b> : Agrément, dossier fiscal à jour et attestation de non-redevance.</li>
 *   <li><b>Expressions de Besoins (EB)</b> : Émission et validation hiérarchique des demandes d'achat par les ranches, abattoirs et services.</li>
 *   <li><b>Dossiers de commande et marchés</b> : Aiguillage automatique selon les seuils (Achat direct &lt; 500k, Bon de Commande, Lettre-Commande, Appel d'offres CIPM).</li>
 *   <li><b>Mise en concurrence obligatoire</b> : Collecte et contrôle d'au moins 3 devis proforma contradictoires pour les commandes en unités.</li>
 *   <li><b>Réception technique et conformité</b> : Constatation contradictoire du service fait par commission de réception, procès-verbaux et liquidation de facture.</li>
 * </ul>
 */
@RestController
@RequestMapping({"/api/achats", "/api/v1/achats"})
@RequiredArgsConstructor
@Tag(name = "Achats & Marchés Publics SODEPA", description = "Gestion des expressions de besoins, bons de commande, lettres-commandes, marchés CIPM et réceptions")
public class AchatsRestController {

    private final AchatsService achatsService;

    // ==========================================
    // FOURNISSEURS & PRESTATAIRES
    // ==========================================

    /**
     * Liste les fournisseurs et prestataires référencés par la SODEPA.
     *
     * @param agreeSeulement si true, filtre uniquement les prestataires titulaires d'un agrément officiel valide
     * @return la liste des fournisseurs correspondants
     */
    @GetMapping("/fournisseurs")
    @Operation(summary = "Lister les prestataires et fournisseurs SODEPA")
    public ResponseEntity<List<FournisseurResponse>> listerFournisseurs(
            @RequestParam(required = false, defaultValue = "false") boolean agreeSeulement) {
        return ResponseEntity.ok(achatsService.listerFournisseurs(agreeSeulement));
    }

    /**
     * Enregistre un nouveau prestataire au fichier central des fournisseurs.
     * <p>
     * Soumet la création à une procédure de validation contradictoire (Maker-Checker)
     * pour vérifier la régularité fiscale et juridique de l'entreprise.
     *
     * @param request données du prestataire (raison sociale, identifiant fiscal unique, localisation, coordonnées)
     * @return l'identifiant du fournisseur créé
     */
    @PostMapping("/fournisseurs")
    @Operation(summary = "Enregistrer un nouveau fournisseur / prestataire")
    public ResponseEntity<UUID> creerFournisseur(@Valid @RequestBody CreerFournisseurRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(achatsService.creerFournisseur(request));
    }

    // ==========================================
    // EXPRESSIONS DE BESOINS (EB)
    // ==========================================

    /**
     * Liste l'ensemble des expressions de besoins émises par les différentes entités de la SODEPA.
     *
     * @return la liste des demandes d'achats en cours
     */
    @GetMapping("/expressions-besoin")
    @Operation(summary = "Lister toutes les expressions de besoins")
    public ResponseEntity<List<ExpressionBesoinResponse>> listerExpressionsBesoin() {
        return ResponseEntity.ok(achatsService.listerExpressionsBesoin());
    }

    /**
     * Émet une nouvelle expression de besoin pour acquisition de fournitures, matériels ou prestations.
     * <p>
     * Constitue le point d'entrée initial de toute commande publique au sein des services ou des ranches.
     *
     * @param request caractéristiques du besoin (désignation des articles, quantités estimées, unité demanderesse)
     * @return l'expression de besoin créée
     */
    @PostMapping("/expressions-besoin")
    @Operation(summary = "Émettre une nouvelle expression de besoin")
    public ResponseEntity<ExpressionBesoinResponse> creerExpressionBesoin(@Valid @RequestBody CreerExpressionBesoinRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(achatsService.creerExpressionBesoin(request));
    }

    /**
     * Consulte le détail exhaustif d'une expression de besoin donnée.
     *
     * @param id identifiant unique de l'expression de besoin
     * @return les données complètes de la demande d'achat
     */
    @GetMapping("/expressions-besoin/{id}")
    @Operation(summary = "Consulter le détail d'une expression de besoin")
    public ResponseEntity<ExpressionBesoinResponse> obtenirExpressionBesoin(@PathVariable UUID id) {
        return ResponseEntity.ok(achatsService.obtenirExpressionBesoin(id));
    }

    // ==========================================
    // DOSSIERS D'ACHAT / BONS DE COMMANDE / MARCHÉS
    // ==========================================

    /**
     * Liste tous les dossiers d'achat, bons de commande et marchés en cours de passation ou d'exécution.
     *
     * @return la liste ordonnée des dossiers d'acquisition
     */
    @GetMapping("/dossiers")
    @Operation(summary = "Lister les dossiers d'achat, bons de commande et marchés")
    public ResponseEntity<List<MarcheDossierResponse>> listerDossiers() {
        return ResponseEntity.ok(achatsService.listerDossiers());
    }

    /**
     * Crée un dossier d'acquisition avec qualification et routage automatique selon les seuils réglementaires.
     * <p>
     * Analyse le montant prévisionnel TTC pour déterminer automatiquement la procédure obligatoire :
     * <ul>
     *   <li>Bon de commande d'unité (&lt; 5 millions FCFA).</li>
     *   <li>Lettre-commande formalisée (5 à 50 millions FCFA).</li>
     *   <li>Dossier d'Appel d'Offres soumis à la Commission Interne des Marchés (&gt; 50 millions FCFA).</li>
     * </ul>
     *
     * @param request caractéristiques du dossier (objet du marché, prestataire retenu, montant estimé, pièces jointes)
     * @return le dossier d'achat initialisé
     */
    @PostMapping("/dossiers")
    @Operation(summary = "Créer un dossier de commande / marché avec routage automatique des seuils SODEPA")
    public ResponseEntity<MarcheDossierResponse> creerDossier(@Valid @RequestBody CreerMarcheDossierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(achatsService.creerDossier(request));
    }

    /**
     * Consulte le détail complet d'un dossier de marché ou bon de commande.
     *
     * @param id identifiant unique du dossier
     * @return le détail administratif, technique et financier du marché
     */
    @GetMapping("/dossiers/{id}")
    @Operation(summary = "Consulter le détail d'un dossier de marché ou bon de commande")
    public ResponseEntity<MarcheDossierResponse> obtenirDossier(@PathVariable UUID id) {
        return ResponseEntity.ok(achatsService.obtenirDossier(id));
    }

    /**
     * Enregistre le dépôt des factures proforma de mise en concurrence (au moins 3 devis requis pour bon de commande).
     *
     * @param id identifiant du dossier d'achat
     * @param count nombre de devis contradictoires reçus
     * @param file document scanné ou bordereau de dépouillement des offres
     * @return le dossier mis à jour avec constatation de la mise en concurrence
     */
    @PostMapping("/dossiers/{id}/proformas")
    @Operation(summary = "Enregistrer la réception des factures proforma (minimum 3 pour BC/LC)")
    public ResponseEntity<MarcheDossierResponse> enregistrerProformas(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "3") int count,
            @RequestParam(required = false) MultipartFile file) {
        return ResponseEntity.ok(achatsService.enregistrerProformas(id, count, file));
    }

    /**
     * Enclenche la réservation budgétaire officielle du bon de commande sur la ligne de crédit associée.
     *
     * @param id identifiant du dossier d'achat
     * @param ligneBudgetaire référence de la rubrique budgétaire d'imputation
     * @return le dossier mis à jour avec visa budgétaire
     */
    @PostMapping("/dossiers/{id}/engager-budget")
    @Operation(summary = "Confirmer l'engagement budgétaire officiel")
    public ResponseEntity<MarcheDossierResponse> engagerBudget(
            @PathVariable UUID id,
            @RequestParam(required = false) String ligneBudgetaire) {
        return ResponseEntity.ok(achatsService.engagerBudget(id, ligneBudgetaire));
    }

    /**
     * Notifie officiellement le bon de commande ou l'ordre de service (OS) de démarrage au prestataire retenu.
     * <p>
     * Déclenche le décompte contractuel des délais d'exécution ou de livraison.
     *
     * @param id identifiant du dossier
     * @param dateNotification date formelle de notification de l'ordre de service
     * @param delaiJours délai contractuel accordé pour l'exécution (en jours calendaires)
     * @return le dossier avec statut 'NOTIFIE'
     */
    @PostMapping("/dossiers/{id}/notifier")
    @Operation(summary = "Notification de l'ordre de service ou bon de commande au prestataire")
    public ResponseEntity<MarcheDossierResponse> notifierFournisseur(
            @PathVariable UUID id,
            @RequestParam(required = false) LocalDate dateNotification,
            @RequestParam(required = false) Integer delaiJours) {
        return ResponseEntity.ok(achatsService.notifierFournisseur(id, dateNotification, delaiJours));
    }

    // ==========================================
    // RÉCEPTIONS TECHNIQUES & LIQUIDATION
    // ==========================================

    /**
     * Liste l'ensemble des procès-verbaux de réceptions techniques établis par les commissions de recette.
     *
     * @return la liste des réceptions techniques enregistrées
     */
    @GetMapping("/receptions")
    @Operation(summary = "Lister les procès-verbaux de réceptions techniques")
    public ResponseEntity<List<MarcheReceptionPvResponse>> listerReceptions() {
        return ResponseEntity.ok(achatsService.listerReceptions());
    }

    /**
     * Établit et enregistre un procès-verbal de réception contradictoire (livraison de bétail, aliments, travaux).
     * <p>
     * Constate la conformité physique, qualitative et quantitative des biens livrés avant toute autorisation de paiement.
     *
     * @param request données du procès-verbal (dossier rattaché, composition de la commission, conformité constatée, réserves éventuelles)
     * @return l'identifiant du procès-verbal de réception
     */
    @PostMapping("/receptions")
    @Operation(summary = "Établir un procès-verbal de réception par la commission technique SODEPA")
    public ResponseEntity<UUID> enregistrerPvReception(@Valid @RequestBody EnregistrerPvReceptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(achatsService.enregistrerPvReception(request));
    }

    /**
     * Traite l'arbitrage Maker-Checker sur la création ou modification d'un prestataire.
     *
     * @param id identifiant du ticket de soumission
     * @param request décision et notes de conformité
     * @return statut de validation
     */
    @PutMapping("/fournisseurs/validate_or_reject/{id}")
    @Operation(summary = "Valider ou rejeter la création d'un fournisseur (Maker-Checker)")
    public ResponseEntity<Void> validateOrRejectFournisseur(
            @PathVariable UUID id,
            @Valid @RequestBody RejectOrValidateAchatsSubmitRequest request) {
        ValidateOrRejectSubmissionInput input = new ValidateOrRejectSubmissionInput(
                id,
                request.decision(),
                request.notes(),
                request.checkerOperationType()
        );
        achatsService.validateOrRejectFournisseur(input);
        return ResponseEntity.ok().build();
    }

    /**
     * Traite l'arbitrage sur une expression de besoin soumise à validation hiérarchique.
     *
     * @param id identifiant de l'expression de besoin
     * @param request décision d'approbation ou de rejet
     * @return confirmation de prise en compte
     */
    @PutMapping("/expressions-besoin/validate_or_reject/{id}")
    @Operation(summary = "Valider ou rejeter une expression de besoin (Maker-Checker)")
    public ResponseEntity<Void> validateOrRejectExpressionBesoin(
            @PathVariable UUID id,
            @Valid @RequestBody RejectOrValidateAchatsSubmitRequest request) {
        ValidateOrRejectSubmissionInput input = new ValidateOrRejectSubmissionInput(
                id,
                request.decision(),
                request.notes(),
                request.checkerOperationType()
        );
        achatsService.validateOrRejectExpressionBesoin(input);
        return ResponseEntity.ok().build();
    }

    /**
     * Traite l'approbation définitive d'un dossier de marché ou bon de commande dans le cadre du contrôle à 4 yeux.
     *
     * @param id identifiant du ticket de passation
     * @param request décision formalisée
     * @return confirmation d'exécution
     */
    @PutMapping("/dossiers/validate_or_reject/{id}")
    @Operation(summary = "Valider ou rejeter un dossier de marché / commande (Maker-Checker)")
    public ResponseEntity<Void> validateOrRejectMarcheDossier(
            @PathVariable UUID id,
            @Valid @RequestBody RejectOrValidateAchatsSubmitRequest request) {
        ValidateOrRejectSubmissionInput input = new ValidateOrRejectSubmissionInput(
                id,
                request.decision(),
                request.notes(),
                request.checkerOperationType()
        );
        achatsService.validateOrRejectMarcheDossier(input);
        return ResponseEntity.ok().build();
    }

    /**
     * Valide ou rejette un procès-verbal de réception technique dressé en unité.
     *
     * @param id identifiant du procès-verbal soumis à vérification
     * @param request décision d'acceptation technique
     * @return confirmation
     */
    @PutMapping("/receptions/validate_or_reject/{id}")
    @Operation(summary = "Valider ou rejeter un procès-verbal de réception (Maker-Checker)")
    public ResponseEntity<Void> validateOrRejectPvReception(
            @PathVariable UUID id,
            @Valid @RequestBody RejectOrValidateAchatsSubmitRequest request) {
        ValidateOrRejectSubmissionInput input = new ValidateOrRejectSubmissionInput(
                id,
                request.decision(),
                request.notes(),
                request.checkerOperationType()
        );
        achatsService.validateOrRejectPvReception(input);
        return ResponseEntity.ok().build();
    }

    /**
     * Arbitre la liquidation définitive d'une facture pour transmission au bon à payer.
     *
     * @param id identifiant du dossier de liquidation
     * @param request décision du responsable financier
     * @return confirmation
     */
    @PutMapping("/liquidations/validate_or_reject/{id}")
    @Operation(summary = "Valider ou rejeter une liquidation de facture (Maker-Checker)")
    public ResponseEntity<Void> validateOrRejectLiquidationFacture(
            @PathVariable UUID id,
            @Valid @RequestBody RejectOrValidateAchatsSubmitRequest request) {
        ValidateOrRejectSubmissionInput input = new ValidateOrRejectSubmissionInput(
                id,
                request.decision(),
                request.notes(),
                request.checkerOperationType()
        );
        achatsService.validateOrRejectLiquidationFacture(input);
        return ResponseEntity.ok().build();
    }

    /**
     * Émet la liquidation financière de la facture après attestation de conformité technique sans réserve.
     * <p>
     * Ouvre la voie à l'ordonnancement et à l'émission de l'ordre de virement par la trésorerie.
     *
     * @param id identifiant de la réception technique rattachée
     * @param numeroFacture numéro officiel de la facture transmise par le prestataire
     * @param montantLiquide montant final arrêté pour le règlement
     * @return le procès-verbal avec visa de liquidation
     */
    @PostMapping("/receptions/{id}/liquider")
    @Operation(summary = "Liquider la facture pour paiement après conformité technique")
    public ResponseEntity<MarcheReceptionPvResponse> liquiderFacture(
            @PathVariable UUID id,
            @RequestParam(required = false) String numeroFacture,
            @RequestParam(required = false) BigDecimal montantLiquide) {
        return ResponseEntity.ok(achatsService.liquiderFacture(id, numeroFacture, montantLiquide));
    }
}
