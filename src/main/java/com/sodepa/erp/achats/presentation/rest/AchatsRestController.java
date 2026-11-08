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
 * Contrôleur REST exposant les points de terminaison pour `AchatsRestController`.
 * Permet la gestion sécurisée des opérations de l'ERP SODEPA.
 */
@RestController
@RequestMapping({"/api/achats", "/api/v1/achats"})
@RequiredArgsConstructor
@Tag(name = "Achats & Marchés Publics SODEPA", description = "Gestion des expressions de besoins, bons de commande, lettres-commandes, marchés CIPM et réceptions")
public class AchatsRestController {

    private final AchatsService achatsService;

    // ==========================================
    // FOURNISSEURS
    // ==========================================

    @GetMapping("/fournisseurs")
    @Operation(summary = "Lister les prestataires et fournisseurs SODEPA")
    public ResponseEntity<List<FournisseurResponse>> listerFournisseurs(
            @RequestParam(required = false, defaultValue = "false") boolean agreeSeulement) {
        return ResponseEntity.ok(achatsService.listerFournisseurs(agreeSeulement));
    }

    @PostMapping("/fournisseurs")
    @Operation(summary = "Enregistrer un nouveau fournisseur / prestataire")
    public ResponseEntity<UUID> creerFournisseur(@Valid @RequestBody CreerFournisseurRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(achatsService.creerFournisseur(request));
    }

    // ==========================================
    // EXPRESSIONS DE BESOINS (EB)
    // ==========================================

    @GetMapping("/expressions-besoin")
    @Operation(summary = "Lister toutes les expressions de besoins")
    public ResponseEntity<List<ExpressionBesoinResponse>> listerExpressionsBesoin() {
        return ResponseEntity.ok(achatsService.listerExpressionsBesoin());
    }

    @PostMapping("/expressions-besoin")
    @Operation(summary = "Émettre une nouvelle expression de besoin")
    public ResponseEntity<ExpressionBesoinResponse> creerExpressionBesoin(@Valid @RequestBody CreerExpressionBesoinRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(achatsService.creerExpressionBesoin(request));
    }

    @GetMapping("/expressions-besoin/{id}")
    @Operation(summary = "Consulter le détail d'une expression de besoin")
    public ResponseEntity<ExpressionBesoinResponse> obtenirExpressionBesoin(@PathVariable UUID id) {
        return ResponseEntity.ok(achatsService.obtenirExpressionBesoin(id));
    }

    // ==========================================
    // DOSSIERS D'ACHAT / BONS DE COMMANDE / MARCHÉS
    // ==========================================

    @GetMapping("/dossiers")
    @Operation(summary = "Lister les dossiers d'achat, bons de commande et marchés")
    public ResponseEntity<List<MarcheDossierResponse>> listerDossiers() {
        return ResponseEntity.ok(achatsService.listerDossiers());
    }

    @PostMapping("/dossiers")
    @Operation(summary = "Créer un dossier de commande / marché avec routage automatique des seuils SODEPA")
    public ResponseEntity<MarcheDossierResponse> creerDossier(@Valid @RequestBody CreerMarcheDossierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(achatsService.creerDossier(request));
    }

    @GetMapping("/dossiers/{id}")
    @Operation(summary = "Consulter le détail d'un dossier de marché ou bon de commande")
    public ResponseEntity<MarcheDossierResponse> obtenirDossier(@PathVariable UUID id) {
        return ResponseEntity.ok(achatsService.obtenirDossier(id));
    }

    @PostMapping("/dossiers/{id}/proformas")
    @Operation(summary = "Enregistrer la réception des factures proforma (minimum 3 pour BC/LC)")
    public ResponseEntity<MarcheDossierResponse> enregistrerProformas(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "3") int count,
            @RequestParam(required = false) MultipartFile file) {
        return ResponseEntity.ok(achatsService.enregistrerProformas(id, count, file));
    }

    @PostMapping("/dossiers/{id}/engager-budget")
    @Operation(summary = "Confirmer l'engagement budgétaire officiel")
    public ResponseEntity<MarcheDossierResponse> engagerBudget(
            @PathVariable UUID id,
            @RequestParam(required = false) String ligneBudgetaire) {
        return ResponseEntity.ok(achatsService.engagerBudget(id, ligneBudgetaire));
    }

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

    @GetMapping("/receptions")
    @Operation(summary = "Lister les procès-verbaux de réceptions techniques")
    public ResponseEntity<List<MarcheReceptionPvResponse>> listerReceptions() {
        return ResponseEntity.ok(achatsService.listerReceptions());
    }

    @PostMapping("/receptions")
    @Operation(summary = "Établir un procès-verbal de réception par la commission technique SODEPA")
    public ResponseEntity<UUID> enregistrerPvReception(@Valid @RequestBody EnregistrerPvReceptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(achatsService.enregistrerPvReception(request));
    }

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

    @PostMapping("/receptions/{id}/liquider")
    @Operation(summary = "Liquider la facture pour paiement après conformité technique")
    public ResponseEntity<MarcheReceptionPvResponse> liquiderFacture(
            @PathVariable UUID id,
            @RequestParam(required = false) String numeroFacture,
            @RequestParam(required = false) BigDecimal montantLiquide) {
        return ResponseEntity.ok(achatsService.liquiderFacture(id, numeroFacture, montantLiquide));
    }
}
