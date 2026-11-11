package com.sodepa.erp.workflow.presentation.rest;

import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.utils.Permissions;
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
 * Service du Moteur Universel de Workflow et des Circuits d'Approbation SODEPA (REF-07 / Transverse).
 * <p>
 * Ce service constitue l'épine dorsale de la gouvernance administrative et financière de la SODEPA :
 * <ul>
 *   <li><b>Administration des circuits de validation</b> : Paramétrage des étapes, des délais impartis (SLA) et des acteurs habilités (rôles ou postes spécifiques).</li>
 *   <li><b>Exécution du principe Maker-Checker (4-yeux)</b> : Séparation stricte entre celui qui propose un acte (Maker) et ceux qui l'instruisent et le valident (Checkers).</li>
 *   <li><b>Boîte de réception des tâches ('My Tasks')</b> : Interface permettant à chaque chef de service, directeur ou ordonnateur de traiter ses dossiers en attente d'approbation.</li>
 *   <li><b>Suivi de la conformité et politique de consensus</b> : Gestion des signatures électroniques, majorités de vote en commission ou validations unanimes.</li>
 * </ul>
 */
@RestController
@RequestMapping({"/api/workflows", "/api/v1/workflows"})
@RequiredArgsConstructor
@Tag(name = "Moteur de Workflow & Circuits de Validation", description = "Gestion des instances de validation multi-étapes et tâches des agents SODEPA")
public class WorkflowRestController {

    /** Service applicatif orchestrant la logique des circuits et tâches de workflow. */
    private final WorkflowService workflowService;

    /** Service transverse de vérification des habilitations et de sécurité de session. */
    private final UtilsService utilsService;

    /**
     * Liste l'ensemble des opérations métiers enregistrées dans le moteur de workflow (Achats, Budget, Recrutement, Missions).
     *
     * @return la liste exhaustive des domaines et processus métiers outillés
     */
    @GetMapping("/metiers")
    @Operation(summary = "Lister les opérations métiers (Recrutement, Note de service, Budget, etc.)")
    public ResponseEntity<List<WorkflowMetierResponse>> listerMetiers() {
        return ResponseEntity.ok(workflowService.listerMetiers());
    }

    /**
     * Liste les processus métiers actuellement actifs et ouverts aux initiations d'instances avec pagination.
     *
     * @param pageable pagination et critères de tri
     * @return la page des processus métiers actifs
     */
    @GetMapping("/metiers/active")
    @Operation(summary = "Lister les opérations métiers actives avec pagination")
    public ResponseEntity<Page<WorkflowMetierResponse>> listerActiveMetiers(@PageableDefault(size = 20) @NotNull Pageable pageable) {
        return ResponseEntity.ok(workflowService.listerActiveMetiers(pageable));
    }

    /**
     * Liste les définitions de circuits de validation configurées dans l'ERP avec pagination.
     * <p>
     * Filtre automatiquement les circuits selon l'unité organisationnelle de l'utilisateur connecté
     * (sauf pour les administrateurs et superviseurs généraux).
     *
     * @param pageable paramètres de pagination
     * @param active filtre optionnel sur les circuits actifs ou archivés
     * @return la page des définitions de circuits autorisées
     */
    @GetMapping("/definitions")
    @Operation(summary = "Lister les circuits de validation configurés (filtrage par unité organisationnelle)")
    public ResponseEntity<Page<WorkflowDefinitionResponse>> listerDefinitions(
            @PageableDefault(size = 20) @NotNull Pageable pageable,
            @RequestParam(required = false, name = "active") boolean active) {
        return ResponseEntity.ok(workflowService.listerPageDefinitions(pageable, active));
    }

    /**
     * Contrôle préalable vérifiant si un utilisateur a le droit d'initier un dossier sur un circuit donné (Maker pre-check).
     * <p>
     * Permet à l'interface utilisateur d'activer ou désactiver les boutons d'action avant toute soumission.
     *
     * @param code identifiant unique du circuit (ex: 'WF-DAF-01', 'WF-DAF-06')
     * @return true si l'utilisateur est habilité à initier l'opération, false dans le cas contraire
     */
    @GetMapping("/verifier-maker")
    @Operation(summary = "Vérifier si un utilisateur est habilité à initier un workflow (Maker pre-check)")
    public ResponseEntity<Boolean> verifierMaker(
            @Parameter(description = "Code du circuit de validation") @RequestParam(name = "code") String code) {
        return ResponseEntity.ok(workflowService.canInitiateWorkflow(code));
    }

    /**
     * Consulte l'état d'avancement détaillé d'un dossier en cours de validation (Need-to-Know).
     * <p>
     * Restitue l'historique de chaque étape franchie (qui a signé, à quelle date, avec quel commentaire)
     * et l'étape courante en attente de visa.
     *
     * @param id identifiant unique de l'instance de workflow
     * @return le détail complet du dossier et de ses visas successifs
     */
    @GetMapping("/instances/{id}")
    @Operation(summary = "Consulter le détail et l'avancement d'un dossier (Need-to-Know)")
    public ResponseEntity<WorkflowInstanceResponse> getInstanceById(
            @Parameter(description = "Identifiant UUID de l'instance") @PathVariable UUID id) {
        return ResponseEntity.ok(workflowService.getInstanceById(id));
    }

    /**
     * Recherche l'instance de workflow active associée à un objet métier particulier (bon de commande, budget, recrutement).
     *
     * @param entityName nom de l'objet métier (ex: 'BudgetPlanEntity', 'DossierAchatEntity')
     * @param entityId identifiant unique de l'objet concerné
     * @return l'instance de workflow active si existante, ou réponse 204 (No Content)
     */
    @GetMapping("/instances/entite/{entityName}/{entityId}")
    @Operation(summary = "Consulter le workflow actif associé à un document métier")
    public ResponseEntity<WorkflowInstanceResponse> getInstanceParEntite(
            @PathVariable String entityName,
            @PathVariable String entityId) {
        return workflowService.getInstanceParEntite(entityName, entityId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Restitue la boîte de réception des dossiers en attente d'approbation pour l'agent connecté ('Mes Tâches').
     * <p>
     * Affiche l'ensemble des tâches dont la validation incombe personnellement au collaborateur
     * ou au poste qu'il occupe actuellement.
     *
     * @param statut filtre optionnel sur l'état de la tâche (A_FAIRE, EN_COURS, TERMINEE)
     * @param pageable pagination des tâches
     * @return la page des dossiers soumis au visa de l'utilisateur
     */
    @GetMapping("/taches/my-tasks")
    @Operation(summary = "Lister les dossiers en attente d'approbation pour l'utilisateur courant")
    public ResponseEntity<Page<WorkflowTacheResponse>> getMesTaches(
            @RequestParam(required = false) StatutWorkflowTache statut,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(workflowService.getMesTaches(statut, pageable));
    }

    /**
     * Enregistre l'arbitrage ou la décision d'un approbateur sur un dossier qui lui est assigné.
     * <p>
     * Permet d'approuver (franchissement vers l'étape suivante), de rejeter définitivement
     * ou de renvoyer le dossier à l'initiateur avec une demande de complément d'information.
     * Applique une protection anti-usurpation d'assignation.
     *
     * @param request décision formalisée (ACCORD, REJET, DEMANDE_MODIFICATION) accompagnée des notes justificatives
     * @return confirmation d'enregistrement de la décision
     */
    @PostMapping({"/task/decision"})
    @Operation(summary = "Approuver, rejeter ou demander des corrections sur un dossier")
    public ResponseEntity<Void> traiterTache(@Valid @RequestBody TraitTaskWorkflowRequest request) {
        // 1. Contrôle d'habilitation de l'autorité décisionnaire
        utilsService.hasPermission(Permissions.WORKFLOW_DECISION);

        // 2. Traitement et application de la transition de statut
        workflowService.traiterTache(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Active ou suspend l'ouverture d'un processus métier dans l'ERP.
     *
     * @param id identifiant du processus métier
     * @param isActive indicateur d'activation ou de mise en veille
     * @return le processus métier actualisé
     */
    @PatchMapping("/metiers/{id}/statut")
    @Operation(summary = "Activer ou suspendre une opération métier")
    public ResponseEntity<WorkflowMetierResponse> changerStatutMetier(
            @PathVariable UUID id,
            @RequestParam(name = "active") boolean isActive) {
        utilsService.hasPermission(Permissions.WORKFLOW_CONFIG);
        return ResponseEntity.ok(workflowService.changerStatutMetier(id, isActive));
    }

    /**
     * Crée une nouvelle définition de circuit d'approbation formel dans l'ERP.
     * <p>
     * Permet aux administrateurs de concevoir le parcours d'un dossier, d'ordonnancer les étapes,
     * de fixer les délais d'alerte (SLA) et de désigner les postes habilités à signer.
     *
     * @param request structure du circuit complet (libellé, étapes ordonnées, délais, politiques de consensus)
     * @return le circuit configuré
     */
    @PostMapping("/definitions")
    @Operation(summary = "Créer un nouveau circuit de workflow avec ses étapes et validateurs")
    public ResponseEntity<WorkflowDefinitionResponse> creerDefinition(
            @Valid @RequestBody CreerWorkflowDefinitionRequest request) {
        utilsService.hasPermission(Permissions.WORKFLOW_CONFIG);
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowService.creerDefinition(request));
    }

    /**
     * Modifie les paramètres généraux d'un circuit de validation existant.
     *
     * @param id identifiant unique du circuit
     * @param request nouvelles caractéristiques de la définition
     * @return le circuit mis à jour
     */
    @PutMapping("/definitions/{id}")
    @Operation(summary = "Modifier la configuration d'un circuit de validation existant")
    public ResponseEntity<WorkflowDefinitionResponse> modifierDefinition(
            @PathVariable UUID id,
            @Valid @RequestBody ModifierWorkflowDefinitionRequest request) {
        utilsService.hasPermission(Permissions.WORKFLOW_CONFIG);
        return ResponseEntity.ok(workflowService.modifierDefinition(id, request));
    }

    /**
     * Active ou désactive temporairement une étape particulière au sein d'un circuit de validation.
     *
     * @param definitionId identifiant du circuit parent
     * @param etapeId identifiant de l'étape ciblée
     * @param actif état activé ou court-circuité
     * @return la définition du circuit mise à jour
     */
    @PatchMapping("/definitions/{definitionId}/etapes/{etapeId}/statut")
    @Operation(summary = "Activer ou désactiver une étape d'un circuit de workflow")
    public ResponseEntity<WorkflowDefinitionResponse> changerStatutEtape(
            @PathVariable UUID definitionId,
            @PathVariable UUID etapeId,
            @RequestParam boolean actif) {
        utilsService.hasPermission(Permissions.WORKFLOW_CONFIG);
        return ResponseEntity.ok(workflowService.changerStatutEtape(definitionId, etapeId, actif));
    }

    /**
     * Modifie l'habilitation d'un validateur ou rôle d'approbation sur une étape spécifique.
     *
     * @param definitionId identifiant du circuit
     * @param etapeId identifiant de l'étape
     * @param acteurId identifiant du poste ou de l'acteur désigné
     * @param actif statut actif ou suspendu pour ce signataire
     * @return le circuit mis à jour
     */
    @PatchMapping("/definitions/{definitionId}/etapes/{etapeId}/acteurs/{acteurId}/statut")
    @Operation(summary = "Activer ou désactiver un validateur sur une étape")
    public ResponseEntity<WorkflowDefinitionResponse> changerStatutActeur(
            @PathVariable UUID definitionId,
            @PathVariable UUID etapeId,
            @PathVariable UUID acteurId,
            @RequestParam boolean actif) {
        utilsService.hasPermission(Permissions.WORKFLOW_CONFIG);
        return ResponseEntity.ok(workflowService.changerStatutActeur(definitionId, etapeId, acteurId, actif));
    }
}
