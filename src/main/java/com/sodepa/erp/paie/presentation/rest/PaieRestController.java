package com.sodepa.erp.paie.presentation.rest;

import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.paie.application.PaieService;
import com.sodepa.erp.paie.presentation.requests.AjouterVariablePaieRequest;
import com.sodepa.erp.paie.presentation.requests.CreerPeriodePaieRequest;
import com.sodepa.erp.paie.presentation.responses.BulletinPaieResponse;
import com.sodepa.erp.paie.presentation.responses.DipeDeclarationResponse;
import com.sodepa.erp.paie.presentation.responses.PeriodePaieResponse;
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
 * Contrôleur REST exposant les points de terminaison pour `PaieRestController`.
 * Permet la gestion sécurisée des opérations de l'ERP SODEPA.
 */
@RestController
@RequestMapping({"/api/paie", "/api/v1/paie"})
@RequiredArgsConstructor
@Tag(name = "Solde, Salaires & Déclarations Cameroun (CNPS / DIPE)", description = "Calcul de la paie mensuelle, cotisations sociales, IRPP/CAC et liasse DIPE")
public class PaieRestController {

    private final PaieService paieService;

    @GetMapping("/periodes")
    @Operation(summary = "Lister les périodes de paie mensuelles SODEPA")
    public ResponseEntity<List<PeriodePaieResponse>> listerPeriodes() {
        return ResponseEntity.ok(paieService.listerPeriodes());
    }

    @PostMapping("/periodes")
    @Operation(summary = "Ouvrir une nouvelle période de paie mensuelle")
    public ResponseEntity<PeriodePaieResponse> creerPeriode(@Valid @RequestBody CreerPeriodePaieRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paieService.creerPeriode(request));
    }

    @PutMapping("/periodes/validate_or_reject/{id}")
    @Operation(summary = "Valider ou rejeter l'ouverture d'une période de paie (Maker-Checker)")
    public ResponseEntity<Void> validateOrRejectPeriodePaie(
            @PathVariable UUID id,
            @Valid @RequestBody RejectOrValidateSubmitRequest request) {
        ValidateOrRejectSubmissionInput input = new ValidateOrRejectSubmissionInput(
                id,
                request.decision(),
                request.notes(),
                request.checkerOperationType()
        );
        paieService.validateOrRejectPeriodePaie(input);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/periodes/{id}")
    @Operation(summary = "Consulter le récapitulatif d'une période de paie")
    public ResponseEntity<PeriodePaieResponse> getPeriodeById(@PathVariable UUID id) {
        return ResponseEntity.ok(paieService.getPeriodeById(id));
    }

    @PostMapping("/periodes/{id}/variables")
    @Operation(summary = "Saisir une variable de paie mensuelle pour un employé (prime, acompte, heures supplémentaires)")
    public ResponseEntity<Void> ajouterVariable(
            @PathVariable UUID id,
            @Valid @RequestBody AjouterVariablePaieRequest request) {
        paieService.ajouterVariable(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/variables/validate_or_reject/{id}")
    @Operation(summary = "Valider ou rejeter une variable de paie (Maker-Checker)")
    public ResponseEntity<Void> validateOrRejectVariablePaie(
            @PathVariable UUID id,
            @Valid @RequestBody RejectOrValidateSubmitRequest request) {
        ValidateOrRejectSubmissionInput input = new ValidateOrRejectSubmissionInput(
                id,
                request.decision(),
                request.notes(),
                request.checkerOperationType()
        );
        paieService.validateOrRejectVariablePaie(input);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/periodes/{id}/calculer")
    @Operation(summary = "Lancer le calcul automatisé de la paie et des bulletins pour l'ensemble du personnel actif")
    public ResponseEntity<PeriodePaieResponse> calculerPaie(@PathVariable UUID id) {
        return ResponseEntity.ok(paieService.calculerPaie(id));
    }

    @PostMapping("/periodes/{id}/valider")
    @Operation(summary = "Valider la paie mensuelle (déclenche l'ordonnancement et les flux budgétaires/comptables)")
    public ResponseEntity<PeriodePaieResponse> validerPaie(@PathVariable UUID id) {
        return ResponseEntity.ok(paieService.validerPaie(id));
    }

    @PostMapping("/periodes/{id}/cloturer")
    @Operation(summary = "Clôturer définitivement la période de paie")
    public ResponseEntity<PeriodePaieResponse> cloturerPeriode(@PathVariable UUID id) {
        return ResponseEntity.ok(paieService.cloturerPeriode(id));
    }

    @GetMapping("/periodes/{id}/bulletins")
    @Operation(summary = "Consulter la liste paginée des bulletins de paie de la période")
    public ResponseEntity<Page<BulletinPaieResponse>> getBulletins(
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(paieService.getBulletins(id, pageable));
    }

    @GetMapping("/bulletins/{bulletinId}")
    @Operation(summary = "Consulter le détail complet d'un bulletin de paie avec toutes ses rubriques")
    public ResponseEntity<BulletinPaieResponse> getBulletinById(@PathVariable UUID bulletinId) {
        return ResponseEntity.ok(paieService.getBulletinById(bulletinId));
    }

    @GetMapping("/periodes/{id}/dipe")
    @Operation(summary = "Générer la liasse officielle DIPE (Document d'Information sur le Personnel Employé - format DGI & CNPS Cameroun)")
    public ResponseEntity<DipeDeclarationResponse> genererDipe(@PathVariable UUID id) {
        return ResponseEntity.ok(paieService.genererDipe(id));
    }
}
