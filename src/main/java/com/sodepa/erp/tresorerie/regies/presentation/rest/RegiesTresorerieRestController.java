package com.sodepa.erp.tresorerie.regies.presentation.rest;

import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.share.RejectOrValidateSubmitRequest;
import com.sodepa.erp.tresorerie.regies.application.RegiesTresorerieService;
import com.sodepa.erp.tresorerie.regies.presentation.requests.CreerDemandeApprovisionnementRequest;
import com.sodepa.erp.tresorerie.regies.presentation.requests.EnregistrerDepenseRegieRequest;
import com.sodepa.erp.tresorerie.regies.presentation.responses.TresorerieRegieApprovisionnementResponse;
import com.sodepa.erp.tresorerie.regies.presentation.responses.TresorerieRegieDepenseResponse;
import com.sodepa.erp.tresorerie.regies.presentation.responses.TresorerieRegieResponse;
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
 * Contrôleur REST exposant les points de terminaison pour `RegiesTresorerieRestController`.
 * Permet la gestion sécurisée des opérations de l'ERP SODEPA.
 */
@RestController
@RequestMapping({"/api/tresorerie/regies", "/api/v1/tresorerie/regies"})
@RequiredArgsConstructor
@Tag(name = "Trésorerie & Régies d'Avances (Ranches & Abattoirs)", description = "Gestion des caisses locales, approvisionnements par double signature DG+DAF et menues dépenses")
public class RegiesTresorerieRestController {

    private final RegiesTresorerieService regiesService;

    @GetMapping
    @Operation(summary = "Lister les caisses de régie des ranches et abattoirs")
    public ResponseEntity<List<TresorerieRegieResponse>> listerRegies() {
        return ResponseEntity.ok(regiesService.listerRegies());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter le détail et le solde d'une régie")
    public ResponseEntity<TresorerieRegieResponse> obtenirRegie(@PathVariable UUID id) {
        return ResponseEntity.ok(regiesService.obtenirRegie(id));
    }

    @GetMapping("/approvisionnements")
    @Operation(summary = "Lister les demandes de réapprovisionnement")
    public ResponseEntity<List<TresorerieRegieApprovisionnementResponse>> listerApprovisionnements(
            @RequestParam(required = false) UUID regieId) {
        return ResponseEntity.ok(regiesService.listerApprovisionnements(regieId));
    }

    @PostMapping("/approvisionnements")
    @Operation(summary = "Initier une demande de réapprovisionnement avec état des dépenses")
    public ResponseEntity<TresorerieRegieApprovisionnementResponse> creerDemandeApprovisionnement(
            @Valid @RequestBody CreerDemandeApprovisionnementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(regiesService.creerDemandeApprovisionnement(request));
    }

    @PostMapping("/approvisionnements/{id}/signer-daf")
    @Operation(summary = "Visa préalable du Directeur Administratif et Financier (DAF)")
    public ResponseEntity<TresorerieRegieApprovisionnementResponse> signerDaf(@PathVariable UUID id) {
        return ResponseEntity.ok(regiesService.signerDaf(id));
    }

    @PostMapping("/approvisionnements/{id}/signer-dg")
    @Operation(summary = "Approbation finale du Directeur Général et déblocage des fonds")
    public ResponseEntity<TresorerieRegieApprovisionnementResponse> signerDg(
            @PathVariable UUID id,
            @RequestParam(required = false) String referenceVirement) {
        return ResponseEntity.ok(regiesService.signerDgEtDebloquer(id, referenceVirement));
    }

    @PutMapping("/approvisionnements/validate_or_reject/{id}")
    @Operation(summary = "Valider ou rejeter une demande d'approvisionnement de régie (Maker-Checker)")
    public ResponseEntity<Void> validateOrRejectApprovisionnement(
            @PathVariable UUID id,
            @Valid @RequestBody RejectOrValidateSubmitRequest request) {
        ValidateOrRejectSubmissionInput input = new ValidateOrRejectSubmissionInput(
                id,
                request.decision(),
                request.notes(),
                request.checkerOperationType()
        );
        regiesService.validateOrRejectApprovisionnement(input);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/depenses")
    @Operation(summary = "Lister les menues dépenses effectuées sur la caisse locale")
    public ResponseEntity<Page<TresorerieRegieDepenseResponse>> listerDepenses(
            @RequestParam(required = false) UUID regieId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(regiesService.listerDepenses(regieId, pageable));
    }

    @PostMapping("/depenses")
    @Operation(summary = "Enregistrer une menue dépense locale sur bon de caisse (≤ 100 000 FCFA)")
    public ResponseEntity<TresorerieRegieDepenseResponse> enregistrerDepense(
            @Valid @RequestBody EnregistrerDepenseRegieRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(regiesService.enregistrerDepense(request));
    }

    @PutMapping("/depenses/validate_or_reject/{id}")
    @Operation(summary = "Valider ou rejeter une dépense de régie (Maker-Checker)")
    public ResponseEntity<Void> validateOrRejectDepense(
            @PathVariable UUID id,
            @Valid @RequestBody RejectOrValidateSubmitRequest request) {
        ValidateOrRejectSubmissionInput input = new ValidateOrRejectSubmissionInput(
                id,
                request.decision(),
                request.notes(),
                request.checkerOperationType()
        );
        regiesService.validateOrRejectDepense(input);
        return ResponseEntity.ok().build();
    }
}
