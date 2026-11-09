package com.sodepa.erp.workflow.presentation.rest;

import com.sodepa.erp.workflow.application.WorkflowService;
import com.sodepa.erp.workflow.infrastructure.entities.StatutWorkflowTache;
import com.sodepa.erp.workflow.presentation.requests.*;
import com.sodepa.erp.workflow.presentation.responses.WorkflowDefinitionResponse;
import com.sodepa.erp.workflow.presentation.responses.WorkflowInstanceResponse;
import com.sodepa.erp.workflow.presentation.responses.WorkflowMetierResponse;
import com.sodepa.erp.workflow.presentation.responses.WorkflowTacheResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
 * Contrôleur REST exposant les points d'entrée de l'API pour le moteur de workflow universel SODEPA.
 */
@RestController
@RequestMapping({"/api/workflows", "/api/v1/workflows"})
@RequiredArgsConstructor
@Tag(name = "Moteur de Workflow & Circuits de Validation", description = "Gestion des instances de validation multi-étapes et tâches des agents SODEPA")
public class WorkflowRestController {

    /** Injection du service applicatif de workflow. */
    private final WorkflowService workflowService;

    /**
     * Récupère la liste de toutes les opérations métiers paramétrées (ex: RECRUITMENT, SERVICE_MEMO).
     *
     * @return Réponse HTTP 200 contenant la liste des métiers.
     */
    @GetMapping("/metiers")
    @Operation(summary = "Lister les opérations métiers (Recrutement, Note de service, Budget, etc.)")
    public ResponseEntity<List<WorkflowMetierResponse>> listerMetiers() {
        return ResponseEntity.ok(workflowService.listerMetiers());
    }


    /**
     * Récupère la liste de toutes les opérations métiers paramétrées active (ex: RECRUITMENT, SERVICE_MEMO).
     *
     * @return Réponse HTTP 200 contenant la liste des métiers.
     */
    @GetMapping("/metiers/active")
    @Operation(summary = "Lister les opérations métiers active(Recrutement, Note de service, Budget, etc.)")
    public ResponseEntity<Page<WorkflowMetierResponse>> listerActiveMetiers(@PageableDefault(size = 20) @NotNull Pageable pageable) {
        return ResponseEntity.ok(workflowService.listerActiveMetiers(pageable));
    }

    /**
     * Récupère la liste de tous les circuits de validation configurés dans l'ERP par page,
     * filtrés selon l'unité organisationnelle de l'utilisateur connecté (sauf Superviseur/Admin).
     *
     * @param pageable Paramètres de pagination (page, size, sort).
     * @return Réponse HTTP 200 contenant la liste des définitions autorisées.
     */
    @GetMapping("/definitions")
    @Operation(summary = "Lister les circuits de validation configurés par page (avec filtrage par unité organisationnelle)")
    public ResponseEntity<Page<WorkflowDefinitionResponse>> listerDefinitions(@PageableDefault(size = 20) @NotNull Pageable pageable, @RequestParam(required = false, name = "active") boolean active) {
        return ResponseEntity.ok(workflowService.listerPageDefinitions(pageable, active));
    }

    /**
     * Vérifie en amont si un workflow actif existe et si l'utilisateur est habilité comme Maker.
     */
    @GetMapping("/verifier-maker")
    @Operation(summary = "Vérifier si un utilisateur est habilité à initier un workflow (Maker pre-check)")
    public ResponseEntity<Boolean> verifierMaker(
            @Parameter(description = "Code du circuit de validation") @RequestParam(name = "code") String code) {
        return ResponseEntity.ok(workflowService.canInitiateWorkflow(code));
    }


    /**
     * Récupère le détail d'une instance avec application stricte du principe Need-to-Know et bypass Superviseurs.
     *
     * @param id              Identifiant de l'instance.
     * @return L'instance si autorisée.
     */
    @GetMapping("/instances/{id}")
    @Operation(summary = "Consulter le détail et l'avancement d'une instance (sécurité Need-to-Know)")
    public ResponseEntity<WorkflowInstanceResponse> getInstanceById(
            @Parameter(description = "Identifiant UUID de l'instance") @PathVariable UUID id) {
        return ResponseEntity.ok(workflowService.getInstanceById(id));
    }

    /**
     * Recherche l'instance active liée à une entité métier.
     */
    @GetMapping("/instances/entite/{entityName}/{entityId}")
    @Operation(summary = "Consulter l'instance de workflow active liée à une entité métier")
    public ResponseEntity<WorkflowInstanceResponse> getInstanceParEntite(
            @PathVariable String entityName,
            @PathVariable String entityId) {
        return workflowService.getInstanceParEntite(entityName, entityId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Récupère les tâches de validation assignées à l'utilisateur courant.
     */
    @GetMapping("/taches/mes-taches")
    @Operation(summary = "Lister les tâches de validation assignées à l'utilisateur courant")
    public ResponseEntity<Page<WorkflowTacheResponse>> getMesTaches(
            @RequestParam UUID utilisateurId,
            @RequestParam(required = false) StatutWorkflowTache statut,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(workflowService.getMesTaches(utilisateurId, statut, pageable));
    }

    /**
     * Enregistre la décision d'un approbateur avec verrou anti-usurpation d'assignation.
     */
    @PostMapping("/tack/decision")
    @Operation(summary = "Approuver, rejeter ou demander des modifications sur une tâche")
    public ResponseEntity<WorkflowTacheResponse> traiterTache(@Valid @RequestBody TraitTaskWorkflowRequest request) {
        return ResponseEntity.ok(workflowService.traiterTache(request));
    }

    /**
     * Crée un nouveau processus métier dans le registre transversal.
     * Soumis à workflow d'approbation si l'utilisateur n'est pas Admin/DG/DAF.
     */
    @PostMapping("/metiers")
    @Operation(summary = "Déclarer un nouveau processus métier (RH, Finance, etc.)")
    public ResponseEntity<WorkflowMetierResponse> creerMetier(
            @Valid @RequestBody CreerWorkflowMetierRequest request,
            @RequestParam(required = false) UUID connectedUserId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowService.creerMetier(request, connectedUserId));
    }

    /**
     * Modifie un processus métier existant.
     */
    @PutMapping("/metiers/{id}")
    @Operation(summary = "Modifier une opération métier")
    public ResponseEntity<WorkflowMetierResponse> modifierMetier(
            @PathVariable UUID id,
            @Valid @RequestBody ModifierWorkflowMetierRequest request) {
        return ResponseEntity.ok(workflowService.modifierMetier(id, request));
    }

    /**
     * Active ou désactive un processus métier.
     */
    @PatchMapping("/metiers/{id}/statut")
    @Operation(summary = "Activer ou désactiver une opération métier")
    public ResponseEntity<WorkflowMetierResponse> changerStatutMetier(
            @PathVariable UUID id,
            @RequestParam boolean isActive) {
        return ResponseEntity.ok(workflowService.changerStatutMetier(id, isActive));
    }

    /**
     * Crée une nouvelle définition de circuit de workflow.
     */
    @PostMapping("/definitions")
    @Operation(summary = "Créer un nouveau circuit de workflow avec ses étapes et validateurs")
    public ResponseEntity<WorkflowDefinitionResponse> creerDefinition(
            @Valid @RequestBody CreerWorkflowDefinitionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowService.creerDefinition(request));
    }

    /**
     * Modifie une définition de circuit de workflow existante.
     */
    @PutMapping("/definitions/{id}")
    @Operation(summary = "Modifier un circuit de validation existant")
    public ResponseEntity<WorkflowDefinitionResponse> modifierDefinition(
            @PathVariable UUID id,
            @Valid @RequestBody ModifierWorkflowDefinitionRequest request) {
        return ResponseEntity.ok(workflowService.modifierDefinition(id, request));
    }

    /**
     * Active ou désactive une étape de workflow.
     */
    @PatchMapping("/definitions/{definitionId}/etapes/{etapeId}/statut")
    @Operation(summary = "Activer ou désactiver une étape de workflow")
    public ResponseEntity<WorkflowDefinitionResponse> changerStatutEtape(
            @PathVariable UUID definitionId,
            @PathVariable UUID etapeId,
            @RequestParam boolean actif) {
        return ResponseEntity.ok(workflowService.changerStatutEtape(definitionId, etapeId, actif));
    }

    /**
     * Active ou désactive un validateur spécifique d'une étape de workflow.
     */
    @PatchMapping("/definitions/{definitionId}/etapes/{etapeId}/acteurs/{acteurId}/statut")
    @Operation(summary = "Activer ou désactiver un validateur sur une étape")
    public ResponseEntity<WorkflowDefinitionResponse> changerStatutActeur(
            @PathVariable UUID definitionId,
            @PathVariable UUID etapeId,
            @PathVariable UUID acteurId,
            @RequestParam boolean actif) {
        return ResponseEntity.ok(workflowService.changerStatutActeur(definitionId, etapeId, acteurId, actif));
    }
}
