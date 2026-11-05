package com.sodepa.erp.rh.presentation.rest;

import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.organigramme.presentation.requests.AccorderDerogationRequest;
import com.sodepa.erp.organigramme.presentation.responses.DerogationPermissionResponse;
import com.sodepa.erp.rh.application.RhService;
import com.sodepa.erp.rh.infrastructure.entities.StatutActivite;
import com.sodepa.erp.rh.presentation.requests.CreerEmployeRequest;
import com.sodepa.erp.rh.presentation.requests.ModifierEmployeRequest;
import com.sodepa.erp.rh.presentation.responses.EmployeResponse;
import com.sodepa.erp.rh.presentation.responses.GrilleSalarialeResponse;
import com.sodepa.erp.share.RejectOrValidateSubmitRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/rh", "/api/v1/rh"})
@RequiredArgsConstructor
@Tag(name = "Ressources Humaines & Statut SODEPA", description = "Endpoints de gestion du personnel, grille salariale, contrats et permissions dérogées")
public class RhRestController {

    private final RhService rhService;

    @GetMapping("/grille-salariale")
    @Operation(summary = "Consulter la grille indiciaire de rémunération SODEPA (Catégories 1-12, Échelons A-F)")
    public ResponseEntity<List<GrilleSalarialeResponse>> getGrilleSalariale() {
        return ResponseEntity.ok(rhService.getGrilleSalariale());
    }

    @GetMapping("/employes")
    @Operation(summary = "Lister les employés SODEPA avec filtres (unité, statut, recherche)")
    public ResponseEntity<List<EmployeResponse>> listerEmployes(
            @RequestParam(required = false) UUID uniteId,
            @RequestParam(required = false) StatutActivite statut,
            @RequestParam(required = false) String recherche) {
        return ResponseEntity.ok(rhService.listerEmployes(uniteId, statut, recherche));
    }

    @GetMapping("/employes/{id}")
    @Operation(summary = "Consulter le dossier complet d'un employé par son ID")
    public ResponseEntity<EmployeResponse> getEmployeById(@PathVariable UUID id) {
        return ResponseEntity.ok(rhService.getEmployeById(id));
    }

    @GetMapping("/employes/matricule/{matricule}")
    @Operation(summary = "Consulter le dossier d'un employé par son matricule")
    public ResponseEntity<EmployeResponse> getEmployeByMatricule(@PathVariable String matricule) {
        return ResponseEntity.ok(rhService.getEmployeByMatricule(matricule));
    }

    @PostMapping("/employes")
    @Operation(summary = "Créer un nouvel employé SODEPA (avec matricule automatique et attribution de grille)")
    public ResponseEntity<EmployeResponse> creerEmploye(@Valid @RequestBody CreerEmployeRequest request) {
        EmployeResponse response = rhService.creerEmploye(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/employes/{id}")
    @Operation(summary = "Mettre à jour le dossier administratif d'un employé")
    public ResponseEntity<EmployeResponse> modifierEmploye(
            @PathVariable UUID id,
            @Valid @RequestBody ModifierEmployeRequest request) {
        return ResponseEntity.ok(rhService.modifierEmploye(id, request));
    }

    @DeleteMapping("/employes/{id}")
    @Operation(summary = "Supprimer un dossier employé")
    public ResponseEntity<Void> supprimerEmploye(@PathVariable UUID id) {
        rhService.supprimerEmploye(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/employes/validate_or_reject/{id}")
    @Operation(summary = "Valider ou rejeter la création/modification/suppression d'un employé (Maker-Checker)")
    public ResponseEntity<Void> validateOrRejectEmploye(
            @PathVariable UUID id,
            @Valid @RequestBody RejectOrValidateSubmitRequest request) {
        ValidateOrRejectSubmissionInput input = new ValidateOrRejectSubmissionInput(
                id,
                request.decision(),
                request.notes(),
                request.checkerOperationType()
        );
        rhService.validateOrRejectEmploye(input);
        return ResponseEntity.ok().build();
    }

    // ==========================================
    // DÉROGATIONS D'HABILITATION
    // ==========================================

    @PostMapping("/derogations")
    @Operation(summary = "Accorder une dérogation temporaire d'habilitation à un collaborateur")
    public ResponseEntity<DerogationPermissionResponse> accorderDerogation(@Valid @RequestBody AccorderDerogationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rhService.accorderDerogation(request));
    }

    @PostMapping("/derogations/{id}/revoquer")
    @Operation(summary = "Révoquer immédiatement une dérogation d'habilitation")
    public ResponseEntity<DerogationPermissionResponse> revoquerDerogation(@PathVariable UUID id) {
        return ResponseEntity.ok(rhService.revoquerDerogation(id));
    }

    @PutMapping("/derogations/validate_or_reject/{id}")
    @Operation(summary = "Valider ou rejeter une demande d'octroi ou de révocation de dérogation (Maker-Checker)")
    public ResponseEntity<Void> validateOrRejectDerogation(
            @PathVariable UUID id,
            @Valid @RequestBody RejectOrValidateSubmitRequest request) {
        ValidateOrRejectSubmissionInput input = new ValidateOrRejectSubmissionInput(
                id,
                request.decision(),
                request.notes(),
                request.checkerOperationType()
        );
        rhService.validateOrRejectDerogation(input);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/derogations")
    @Operation(summary = "Lister les dérogations d'habilitations (filtrables par utilisateur)")
    public ResponseEntity<List<DerogationPermissionResponse>> listerDerogations(
            @RequestParam(required = false) UUID utilisateurId) {
        return ResponseEntity.ok(rhService.listerDerogations(utilisateurId));
    }
}
