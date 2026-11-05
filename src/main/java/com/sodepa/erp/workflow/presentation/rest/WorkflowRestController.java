package com.sodepa.erp.workflow.presentation.rest;

import com.sodepa.erp.workflow.application.WorkflowService;
import com.sodepa.erp.workflow.infrastructure.entities.StatutWorkflowTache;
import com.sodepa.erp.workflow.presentation.requests.DemarrerWorkflowRequest;
import com.sodepa.erp.workflow.presentation.requests.TraiterTacheWorkflowRequest;
import com.sodepa.erp.workflow.presentation.responses.WorkflowDefinitionResponse;
import com.sodepa.erp.workflow.presentation.responses.WorkflowInstanceResponse;
import com.sodepa.erp.workflow.presentation.responses.WorkflowTacheResponse;
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

@RestController
@RequestMapping({"/api/workflows", "/api/v1/workflows"})
@RequiredArgsConstructor
@Tag(name = "Moteur de Workflow & Circuits de Validation", description = "Gestion des instances de validation multi-étapes et tâches des agents SODEPA")
public class WorkflowRestController {

    private final WorkflowService workflowService;

    @GetMapping("/definitions")
    @Operation(summary = "Lister les circuits de validation configurés (Achats, Engagements, Congés, Paie)")
    public ResponseEntity<List<WorkflowDefinitionResponse>> listerDefinitions() {
        return ResponseEntity.ok(workflowService.listerDefinitions());
    }

    @PostMapping("/instances/demarrer")
    @Operation(summary = "Démarrer une nouvelle instance de workflow pour un objet métier (bon de commande, engagement, etc.)")
    public ResponseEntity<WorkflowInstanceResponse> demarrerWorkflow(@Valid @RequestBody DemarrerWorkflowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowService.demarrerWorkflow(request));
    }

    @GetMapping("/instances/{id}")
    @Operation(summary = "Consulter le détail et l'avancement d'une instance de workflow")
    public ResponseEntity<WorkflowInstanceResponse> getInstanceById(@PathVariable UUID id) {
        return ResponseEntity.ok(workflowService.getInstanceById(id));
    }

    @GetMapping("/instances/entite/{entityName}/{entityId}")
    @Operation(summary = "Consulter l'instance de workflow active liée à une entité métier")
    public ResponseEntity<WorkflowInstanceResponse> getInstanceParEntite(
            @PathVariable String entityName,
            @PathVariable String entityId) {
        return workflowService.getInstanceParEntite(entityName, entityId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/taches/mes-taches")
    @Operation(summary = "Lister les tâches de validation assignées à l'utilisateur courant")
    public ResponseEntity<Page<WorkflowTacheResponse>> getMesTaches(
            @RequestParam UUID utilisateurId,
            @RequestParam(required = false) StatutWorkflowTache statut,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(workflowService.getMesTaches(utilisateurId, statut, pageable));
    }

    @PostMapping("/taches/{tacheId}/decision")
    @Operation(summary = "Approuver, rejeter ou demander des modifications sur une tâche")
    public ResponseEntity<WorkflowTacheResponse> traiterTache(
            @PathVariable UUID tacheId,
            @Valid @RequestBody TraiterTacheWorkflowRequest request) {
        return ResponseEntity.ok(workflowService.traiterTache(tacheId, request));
    }
}
