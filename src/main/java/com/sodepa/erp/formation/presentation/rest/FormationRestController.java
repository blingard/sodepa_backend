package com.sodepa.erp.formation.presentation.rest;

import com.sodepa.erp.formation.application.FormationService;
import com.sodepa.erp.formation.infrastructure.entities.StatutStage;
import com.sodepa.erp.formation.presentation.requests.*;
import com.sodepa.erp.formation.presentation.responses.*;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Contrôleur REST exposant les points de terminaison pour `FormationRestController`.
 * Permet la gestion sécurisée des opérations de l'ERP SODEPA.
 */
@RestController
@RequestMapping({"/api/formation", "/api/v1/formation"})
@RequiredArgsConstructor
@Tag(name = "Formation du Personnel, Stages & Reclassements SODEPA", description = "Plan de formation annuel N+1, conventions de stage, règle des 2 semaines et reclassement indiciaire")
public class FormationRestController {

    private final FormationService formationService;

    // ==========================================
    // PLANS ANNUELS DE FORMATION
    // ==========================================

    @GetMapping("/plans")
    @Operation(summary = "Lister les plans annuels de formation")
    public ResponseEntity<List<FormationPlanResponse>> listerPlans() {
        return ResponseEntity.ok(formationService.listerPlans());
    }

    @PostMapping("/plans")
    @Operation(summary = "Créer un nouveau plan de formation pour l'exercice N+1")
    public ResponseEntity<FormationPlanResponse> creerPlan(@Valid @RequestBody CreerPlanFormationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(formationService.creerPlan(request));
    }

    @GetMapping("/plans/{id}")
    @Operation(summary = "Consulter le détail d'un plan de formation et ses actions")
    public ResponseEntity<FormationPlanResponse> obtenirPlan(@PathVariable UUID id) {
        return ResponseEntity.ok(formationService.obtenirPlan(id));
    }

    @PostMapping("/plans/{id}/actions")
    @Operation(summary = "Ajouter une action de formation au plan annuel")
    public ResponseEntity<FormationActionResponse> ajouterAction(
            @PathVariable UUID id,
            @Valid @RequestBody CreerActionFormationRequest request) {
        request.setPlanId(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(formationService.ajouterAction(request));
    }

    @PostMapping("/plans/{id}/viser-sdrh")
    @Operation(summary = "Visa du Sous-Directeur des Ressources Humaines (SDRH)")
    public ResponseEntity<FormationPlanResponse> viserSdrh(@PathVariable UUID id) {
        return ResponseEntity.ok(formationService.viserSdrh(id));
    }

    @PostMapping("/plans/{id}/viser-daf")
    @Operation(summary = "Visa du Directeur Administratif et Financier (DAF)")
    public ResponseEntity<FormationPlanResponse> viserDaf(@PathVariable UUID id) {
        return ResponseEntity.ok(formationService.viserDaf(id));
    }

    @PostMapping("/plans/{id}/approuver-dg")
    @Operation(summary = "Approbation et signature finale par le Directeur Général")
    public ResponseEntity<FormationPlanResponse> approuverDg(@PathVariable UUID id) {
        return ResponseEntity.ok(formationService.approuverDg(id));
    }

    @PostMapping("/inscriptions")
    @Operation(summary = "Inscrire un employé à une action de formation")
    public ResponseEntity<Void> inscrireEmploye(@Valid @RequestBody InscrireEmployeFormationRequest request) {
        formationService.inscrireEmploye(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // ==========================================
    // STAGES ACADÉMIQUES ET PROFESSIONNELS
    // ==========================================

    @GetMapping("/stages")
    @Operation(summary = "Lister les stagiaires de la SODEPA")
    public ResponseEntity<Page<StageResponse>> listerStages(
            @RequestParam(required = false) StatutStage statut,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(formationService.listerStages(statut, pageable));
    }

    @PostMapping("/stages")
    @Operation(summary = "Enregistrer l'admission d'un stagiaire")
    public ResponseEntity<StageResponse> enregistrerStagiaire(@Valid @RequestBody EnregistrerStagiaireRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(formationService.enregistrerStagiaire(request));
    }

    @PostMapping("/stages/{id}/demander-rallonge")
    @Operation(summary = "Demande de rallonge de stage (soumise au strict respect de la règle des ≥ 2 semaines)")
    public ResponseEntity<StageResponse> demanderRallonge(
            @PathVariable UUID id,
            @Valid @RequestBody DemanderRallongeStageRequest request) {
        request.setStageId(id);
        return ResponseEntity.ok(formationService.demanderRallongeStage(request));
    }

    @PostMapping("/stages/{id}/valider-rallonge")
    @Operation(summary = "Décision de la Direction Générale sur la rallonge de stage")
    public ResponseEntity<StageResponse> validerRallonge(
            @PathVariable UUID id,
            @RequestParam boolean accord,
            @RequestParam(required = false) LocalDate nouvelleDateFin) {
        return ResponseEntity.ok(formationService.validerRallonge(id, accord, nouvelleDateFin));
    }

    @PostMapping("/stages/{id}/cloturer")
    @Operation(summary = "Notification de fin de stage et remise de l'attestation")
    public ResponseEntity<StageResponse> cloturerStage(@PathVariable UUID id) {
        return ResponseEntity.ok(formationService.cloturerStage(id));
    }

    // ==========================================
    // RECLASSEMENTS INDICIAIRES
    // ==========================================

    @GetMapping("/reclassements")
    @Operation(summary = "Lister les dossiers de reclassement indiciaire post-formation")
    public ResponseEntity<List<FormationReclassementResponse>> listerReclassements() {
        return ResponseEntity.ok(formationService.listerReclassements());
    }

    @PostMapping("/reclassements")
    @Operation(summary = "Proposer un reclassement d'échelon/catégorie (vérifie ancienneté ≥ 5 ans & durée ≥ 18 mois)")
    public ResponseEntity<FormationReclassementResponse> proposerReclassement(
            @Valid @RequestBody ProposerReclassementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(formationService.proposerReclassement(request));
    }

    @PostMapping("/reclassements/{id}/valider-dg")
    @Operation(summary = "Validation finale par le DG et mise à jour automatique de la grille salariale de l'employé")
    public ResponseEntity<FormationReclassementResponse> validerReclassementDg(@PathVariable UUID id) {
        return ResponseEntity.ok(formationService.validerReclassementDg(id));
    }
}
