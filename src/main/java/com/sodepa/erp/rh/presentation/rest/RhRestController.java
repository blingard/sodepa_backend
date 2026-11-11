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
import com.sodepa.erp.rh.infrastructure.entities.StatutContrat;
import com.sodepa.erp.rh.presentation.requests.ApprobationContratDgRequest;
import com.sodepa.erp.rh.presentation.responses.ContratTravailResponse;
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

/**
 * Service de Gestion des Ressources Humaines, du Personnel et des Contrats de Travail (Module 05 - RH).
 * <p>
 * Ce service administre l'ensemble de la carriere des agents de la SODEPA conformement au Statut du Personnel :
 * <ul>
 *   <li><b>Gestion des dossiers employes</b> : Matricule automatique d'entreprise, donnees administratives, etat civil et situation d'activite (actif, detache, conge, retraite).</li>
 *   <li><b>Grille salariale indiciaire SODEPA</b> : Application de la convention d'entreprise (Categories 1 a 12, Echelons A a F, indices de remuneration).</li>
 *   <li><b>Contrats de travail & Recrutement</b> : Cycle de vie contractuel (CDD, CDI, stage, saisonniers) et approbation souveraine obligatoire par le Directeur General.</li>
 *   <li><b>Derogations d'habilitation exceptionnelles</b> : Octroi encadre dans le temps de droits superieurs a un agent sous reserve d'une double validation administrative (Maker-Checker).</li>
 * </ul>
 */
@RestController
@RequestMapping({"/api/rh", "/api/v1/rh"})
@RequiredArgsConstructor
@Tag(name = "Ressources Humaines & Statut SODEPA", description = "Endpoints de gestion du personnel, grille salariale, contrats et permissions dérogées")
public class RhRestController {

    private final RhService rhService;

    /**
     * Consulte la grille indiciaire de remuneration officielle de la SODEPA.
     * <p>
     * Presente le bareme des salaires de base par categorie socioprofessionnelle (ouvriers, agents de maitrise, cadres, directeurs) et echelon d'anciennete.
     *
     * @return la liste des echelons indiciaires en vigueur
     */
    @GetMapping("/grille-salariale")
    @Operation(summary = "Consulter la grille indiciaire de rémunération SODEPA (Catégories 1-12, Échelons A-F)")
    public ResponseEntity<List<GrilleSalarialeResponse>> getGrilleSalariale() {
        return ResponseEntity.ok(rhService.getGrilleSalariale());
    }

    /**
     * Liste les collaborateurs de la SODEPA avec possibilites de filtrage par unite d'affectation, statut et recherche textuelle.
     *
     * @param uniteId filtre optionnel par direction, ranch ou abattoir
     * @param statut situation d'activite (ACTIF, CONGE, DETACHE, RETRAITE, SORTI)
     * @param recherche terme de recherche sur le nom, prenom ou matricule
     * @return la liste des employes correspondants
     */
    @GetMapping("/employes")
    @Operation(summary = "Lister les employés SODEPA avec filtres (unité, statut, recherche)")
    public ResponseEntity<List<EmployeResponse>> listerEmployes(
            @RequestParam(required = false) UUID uniteId,
            @RequestParam(required = false) StatutActivite statut,
            @RequestParam(required = false) String recherche) {
        return ResponseEntity.ok(rhService.listerEmployes(uniteId, statut, recherche));
    }

    /**
     * Consulte le dossier administratif exhaustif d'un employe a partir de son identifiant unique.
     *
     * @param id identifiant unique de l'employe
     * @return le dossier individuel complet de l'agent
     */
    @GetMapping("/employes/{id}")
    @Operation(summary = "Consulter le dossier complet d'un employé par son ID")
    public ResponseEntity<EmployeResponse> getEmployeById(@PathVariable UUID id) {
        return ResponseEntity.ok(rhService.getEmployeById(id));
    }

    /**
     * Recherche et affiche la fiche d'un employe a partir de son numero de matricule officiel SODEPA.
     *
     * @param matricule reference matriculaire de l'agent (ex: 'MAT-2026-0042')
     * @return le dossier individuel
     */
    @GetMapping("/employes/matricule/{matricule}")
    @Operation(summary = "Consulter le dossier d'un employé par son matricule")
    public ResponseEntity<EmployeResponse> getEmployeByMatricule(@PathVariable String matricule) {
        return ResponseEntity.ok(rhService.getEmployeByMatricule(matricule));
    }

    /**
     * Enregistre l'embauche d'un nouvel employe au sein de la SODEPA.
     * <p>
     * Genere automatiquement le matricule officiel chronologique, assigne la position dans la grille indiciaire
     * et soumet le dossier au circuit Maker-Checker pour controle d'embauche.
     *
     * @param request informations signaletiques, date de recrutement, categorie, echelon et unite d'affectation
     * @return le dossier employe initialise
     */
    @PostMapping("/employes")
    @Operation(summary = "Créer un nouvel employé SODEPA (avec matricule automatique et attribution de grille)")
    public ResponseEntity<EmployeResponse> creerEmploye(@Valid @RequestBody CreerEmployeRequest request) {
        EmployeResponse response = rhService.creerEmploye(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Met a jour les donnees administratives, l'echelonnement indiciaire ou le poste d'un collaborateur.
     *
     * @param id identifiant de l'employe a actualiser
     * @param request nouvelles donnees administratives
     * @return le dossier actualise
     */
    @PutMapping("/employes/{id}")
    @Operation(summary = "Mettre à jour le dossier administratif d'un employé")
    public ResponseEntity<EmployeResponse> modifierEmploye(
            @PathVariable UUID id,
            @Valid @RequestBody ModifierEmployeRequest request) {
        return ResponseEntity.ok(rhService.modifierEmploye(id, request));
    }

    /**
     * Supprime ou archive le dossier d'un agent du fichier actif des employes.
     *
     * @param id identifiant de l'agent
     * @return confirmation de suppression sans contenu
     */
    @DeleteMapping("/employes/{id}")
    @Operation(summary = "Supprimer un dossier employé")
    public ResponseEntity<Void> supprimerEmploye(@PathVariable UUID id) {
        rhService.supprimerEmploye(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Traite l'arbitrage Maker-Checker sur la creation, mise a jour ou radiation d'un collaborateur.
     *
     * @param id identifiant du ticket de recrutement/mutation
     * @param request decision formelle de l'autorite RH
     * @return confirmation
     */
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

    /**
     * Accorde une derogation exceptionnelle et temporaire d'acces ou de privilege a un agent.
     * <p>
     * Utilise lors de remplacements urgents ou d'audits ponctuels avec echeance temporelle stricte.
     *
     * @param request motif, perimetre de la permission derogee et date de fin de validite
     * @return la derogation enregistree
     */
    @PostMapping("/derogations")
    @Operation(summary = "Accorder une dérogation temporaire d'habilitation à un collaborateur")
    public ResponseEntity<DerogationPermissionResponse> accorderDerogation(@Valid @RequestBody AccorderDerogationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rhService.accorderDerogation(request));
    }

    /**
     * Interrompt et revoque immediatement une derogation d'habilitation avant son echeance prevue.
     *
     * @param id identifiant de la derogation a clore
     * @return la derogation mise a jour a l'etat revoque
     */
    @PostMapping("/derogations/{id}/revoquer")
    @Operation(summary = "Révoquer immédiatement une dérogation d'habilitation")
    public ResponseEntity<DerogationPermissionResponse> revoquerDerogation(@PathVariable UUID id) {
        return ResponseEntity.ok(rhService.revoquerDerogation(id));
    }

    /**
     * Valide ou rejette une demande d'octroi de derogation dans le cadre du controle a 4 yeux.
     *
     * @param id identifiant du ticket
     * @param request decision d'approbation
     * @return confirmation
     */
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

    /**
     * Liste les derogations de privileges actives ou historisees, avec filtrage optionnel par utilisateur.
     *
     * @param utilisateurId identifiant optionnel de l'agent audite
     * @return la liste des derogations
     */
    @GetMapping("/derogations")
    @Operation(summary = "Lister les dérogations d'habilitations (filtrables par utilisateur)")
    public ResponseEntity<List<DerogationPermissionResponse>> listerDerogations(
            @RequestParam(required = false) UUID utilisateurId) {
        return ResponseEntity.ok(rhService.listerDerogations(utilisateurId));
    }

    // ==========================================
    // CONTRATS DE TRAVAIL & APPROBATION DG
    // ==========================================

    /**
     * Liste les contrats de travail du personnel SODEPA (CDD, CDI, saisonniers des ranches et abattoirs).
     *
     * @param employeId filtre optionnel pour cibler un agent
     * @param statut etat du contrat (EN_ATTENTE_SIGNATURE, ACTIF, SUSPENDU, ECHU, RESILIE)
     * @return la liste des contrats de travail
     */
    @GetMapping("/contrats")
    @Operation(summary = "Lister les contrats de travail du personnel SODEPA")
    public ResponseEntity<List<ContratTravailResponse>> listerContrats(
            @RequestParam(required = false) UUID employeId,
            @RequestParam(required = false) StatutContrat statut) {
        return ResponseEntity.ok(rhService.listerContrats(employeId, statut));
    }

    /**
     * Consulte les clauses et modalites completes d'un contrat de travail a partir de son identifiant.
     *
     * @param id identifiant unique du contrat
     * @return les donnees contractuelles detaillees
     */
    @GetMapping("/contrats/{id}")
    @Operation(summary = "Consulter les détails d'un contrat de travail par son ID")
    public ResponseEntity<ContratTravailResponse> getContratById(@PathVariable UUID id) {
        return ResponseEntity.ok(rhService.getContratById(id));
    }

    /**
     * Appose la signature et l'approbation institutionnelle definitive du Directeur General sur un contrat de travail.
     * <p>
     * Conforme a la procedure officielle SODEPA (circuit WF-DAF-07), un contrat ne devient legalement
     * executoire qu'apres signature officielle du Directeur General et rattachement d'une Note de Service ou Circulaire de nomination.
     *
     * @param id identifiant du contrat instruit
     * @param request reference officielle de l'acte de nomination (Note de service / Circulaire DG)
     * @return le contrat de travail approuve et promulgue
     */
    @PostMapping("/contrats/{id}/approuver-dg")
    @Operation(summary = "Approbation définitive du contrat par le Directeur Général (avec Note de Service ou Circulaire)")
    public ResponseEntity<ContratTravailResponse> approuverContratDg(
            @PathVariable UUID id,
            @Valid @RequestBody ApprobationContratDgRequest request) {
        return ResponseEntity.ok(rhService.approuverContratDg(id, request));
    }
}
