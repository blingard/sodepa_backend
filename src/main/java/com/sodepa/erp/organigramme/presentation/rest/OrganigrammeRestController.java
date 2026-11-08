package com.sodepa.erp.organigramme.presentation.rest;

import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.organigramme.application.outputs.*;
import com.sodepa.erp.organigramme.application.ports.OrganigrammePort;
import com.sodepa.erp.organigramme.application.usecase.*;
import com.sodepa.erp.organigramme.presentation.requests.*;
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
 * Contrôleur REST exposant les points de terminaison pour `OrganigrammeRestController`.
 * Permet la gestion sécurisée des opérations de l'ERP SODEPA.
 */
@RestController
@RequestMapping({"/api/organigramme", "/api/v1/organigramme"})
@RequiredArgsConstructor
@Tag(name = "Organigramme SODEPA", description = "Endpoints de gestion de la structure organisationnelle, postes, affectations et hiérarchie")
public class OrganigrammeRestController {

    private final CreerUniteOrganisationnelleUseCase creerUniteUseCase;
    private final DeplacerUniteOrganisationnelleUseCase deplacerUniteUseCase;
    private final GetArborescenceCompleteUseCase getArborescenceCompleteUseCase;
    private final GetSousArbreUniteUseCase getSousArbreUniteUseCase;
    private final CreerPosteUseCase creerPosteUseCase;
    private final AffecterCollaborateurUseCase affecterCollaborateurUseCase;
    private final CloturerAffectationUseCase cloturerAffectationUseCase;
    private final ResoudreHierarchieUseCase resoudreHierarchieUseCase;
    private final CreerDelegationPouvoirUseCase creerDelegationUseCase;
    private final CreerVersionOrganigrammeUseCase creerVersionUseCase;
    private final ActiverVersionOrganigrammeUseCase activerVersionUseCase;
    private final ListerTypesStructuresUseCase listerTypesStructuresUseCase;
    private final CreerTypeStructureUseCase creerTypeStructureUseCase;
    private final ModifierTypeStructureUseCase modifierTypeStructureUseCase;
    private final SupprimerTypeStructureUseCase supprimerTypeStructureUseCase;
    private final ListerObjectifsUniteUseCase listerObjectifsUniteUseCase;
    private final AjouterObjectifUniteUseCase ajouterObjectifUniteUseCase;
    private final MettreAJourRealiseObjectifUseCase mettreAJourRealiseObjectifUseCase;
    private final SupprimerObjectifUniteUseCase supprimerObjectifUniteUseCase;
    private final OrganigrammePort organigrammePort;

    // --- UNITÉS ORGANISATIONNELLES ---

    @GetMapping("/arborescence")
    @Operation(summary = "Récupérer l'arborescence complète de l'organigramme SODEPA")
    public ResponseEntity<List<ArborescenceNoeudOutput>> getArborescenceComplete() {
        return ResponseEntity.ok(getArborescenceCompleteUseCase.execute());
    }

    @GetMapping("/unites/{id}/sous-arbre")
    @Operation(summary = "Récupérer le sous-arbre d'une unité organisationnelle spécifique")
    public ResponseEntity<ArborescenceNoeudOutput> getSousArbreUnite(@PathVariable UUID id) {
        return ResponseEntity.ok(getSousArbreUniteUseCase.execute(id));
    }

    @GetMapping("/unites")
    @Operation(summary = "Lister toutes les unités organisationnelles à plat")
    public ResponseEntity<List<UniteOrganisationnelleOutput>> listerUnites() {
        return ResponseEntity.ok(organigrammePort.listerUnites());
    }

    @PostMapping("/unites")
    @Operation(summary = "Créer une nouvelle unité organisationnelle (Direction, Ranch, Abattoir, etc.)")
    public ResponseEntity<UniteOrganisationnelleOutput> creerUnite(@Valid @RequestBody CreerUniteRequest request) {
        UniteOrganisationnelleOutput output = creerUniteUseCase.execute(request.toInput());
        return ResponseEntity.status(HttpStatus.CREATED).body(output);
    }

    @PutMapping("/unites/{id}/deplacer")
    @Operation(summary = "Déplacer une unité dans l'arbre (avec détection de cycle)")
    public ResponseEntity<UniteOrganisationnelleOutput> deplacerUnite(
            @PathVariable UUID id,
            @Valid @RequestBody DeplacerUniteRequest request) {
        return ResponseEntity.ok(deplacerUniteUseCase.execute(request.toInput(id)));
    }

    @PutMapping("/unites/validate_or_reject/{id}")
    @Operation(summary = "Valider ou rejeter la création/déplacement d'une unité organisationnelle (Maker-Checker)")
    public ResponseEntity<Void> validateOrRejectUnite(
            @PathVariable UUID id,
            @Valid @RequestBody RejectOrValidateSubmitRequest request) {
        ValidateOrRejectSubmissionInput input = new ValidateOrRejectSubmissionInput(
                id,
                request.decision(),
                request.notes(),
                request.checkerOperationType()
        );
        organigrammePort.validateOrRejectUnite(input);
        return ResponseEntity.ok().build();
    }

    // --- POSTES & EFFECTIFS ---

    @GetMapping("/postes")
    @Operation(summary = "Lister les postes (optionnellement filtrés par unité)")
    public ResponseEntity<List<PosteOutput>> listerPostes(@RequestParam(required = false) UUID uniteId) {
        return ResponseEntity.ok(organigrammePort.listerPostes(uniteId));
    }

    @PostMapping("/postes")
    @Operation(summary = "Créer un poste budgété dans l'organigramme")
    public ResponseEntity<PosteOutput> creerPoste(@Valid @RequestBody CreerPosteRequest request) {
        PosteOutput output = creerPosteUseCase.execute(request.toInput());
        return ResponseEntity.status(HttpStatus.CREATED).body(output);
    }

    @PutMapping("/postes/validate_or_reject/{id}")
    @Operation(summary = "Valider ou rejeter la création d'un poste (Maker-Checker)")
    public ResponseEntity<Void> validateOrRejectPoste(
            @PathVariable UUID id,
            @Valid @RequestBody RejectOrValidateSubmitRequest request) {
        ValidateOrRejectSubmissionInput input = new ValidateOrRejectSubmissionInput(
                id,
                request.decision(),
                request.notes(),
                request.checkerOperationType()
        );
        organigrammePort.validateOrRejectPoste(input);
        return ResponseEntity.ok().build();
    }

    // --- AFFECTATIONS ---

    @PostMapping("/affectations")
    @Operation(summary = "Affecter un collaborateur à un poste (contrôle du plafond budgété)")
    public ResponseEntity<AffectationOutput> affecterCollaborateur(@Valid @RequestBody AffecterCollaborateurRequest request) {
        AffectationOutput output = affecterCollaborateurUseCase.execute(request.toInput());
        return ResponseEntity.status(HttpStatus.CREATED).body(output);
    }

    @PostMapping("/affectations/{id}/cloturer")
    @Operation(summary = "Clôturer une affectation de poste")
    public ResponseEntity<AffectationOutput> cloturerAffectation(
            @PathVariable UUID id,
            @Valid @RequestBody CloturerAffectationRequest request) {
        return ResponseEntity.ok(cloturerAffectationUseCase.execute(request.toInput(id)));
    }

    @PutMapping("/affectations/validate_or_reject/{id}")
    @Operation(summary = "Valider ou rejeter une affectation ou clôture d'affectation (Maker-Checker)")
    public ResponseEntity<Void> validateOrRejectAffectation(
            @PathVariable UUID id,
            @Valid @RequestBody RejectOrValidateSubmitRequest request) {
        ValidateOrRejectSubmissionInput input = new ValidateOrRejectSubmissionInput(
                id,
                request.decision(),
                request.notes(),
                request.checkerOperationType()
        );
        organigrammePort.validateOrRejectAffectation(input);
        return ResponseEntity.ok().build();
    }

    // --- RÉSOLUTION HIÉRARCHIQUE N+1, N+2 ---

    @GetMapping("/collaborateurs/{id}/hierarchie")
    @Operation(summary = "Résoudre la chaîne managériale ascendante et les délégations en cours")
    public ResponseEntity<ChaineHierarchiqueOutput> getChaineHierarchique(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "10") int maxNiveaux) {
        return ResponseEntity.ok(resoudreHierarchieUseCase.execute(id, maxNiveaux));
    }

    // --- DÉLÉGATIONS DE POUVOIR ---

    @GetMapping("/delegations")
    @Operation(summary = "Lister les délégations managériales actives")
    public ResponseEntity<List<DelegationOutput>> listerDelegations() {
        return ResponseEntity.ok(organigrammePort.listerDelegationsActives());
    }

    @PostMapping("/delegations")
    @Operation(summary = "Créer une délégation de signature temporaire")
    public ResponseEntity<DelegationOutput> creerDelegation(@Valid @RequestBody CreerDelegationRequest request) {
        DelegationOutput output = creerDelegationUseCase.execute(request.toInput());
        return ResponseEntity.status(HttpStatus.CREATED).body(output);
    }

    @PutMapping("/delegations/validate_or_reject/{id}")
    @Operation(summary = "Valider ou rejeter une délégation de pouvoir (Maker-Checker)")
    public ResponseEntity<Void> validateOrRejectDelegation(
            @PathVariable UUID id,
            @Valid @RequestBody RejectOrValidateSubmitRequest request) {
        ValidateOrRejectSubmissionInput input = new ValidateOrRejectSubmissionInput(
                id,
                request.decision(),
                request.notes(),
                request.checkerOperationType()
        );
        organigrammePort.validateOrRejectDelegation(input);
        return ResponseEntity.ok().build();
    }

    // --- VERSIONING TEMPOREL ---

    @GetMapping("/versions")
    @Operation(summary = "Lister les versions d'organigramme")
    public ResponseEntity<List<OrganigrammeVersionOutput>> listerVersions() {
        return ResponseEntity.ok(organigrammePort.listerVersions());
    }

    @PostMapping("/versions")
    @Operation(summary = "Créer un projet de version d'organigramme (simulation / restructuration)")
    public ResponseEntity<OrganigrammeVersionOutput> creerVersion(@Valid @RequestBody CreerVersionRequest request) {
        OrganigrammeVersionOutput output = creerVersionUseCase.execute(request.toInput());
        return ResponseEntity.status(HttpStatus.CREATED).body(output);
    }

    @PostMapping("/versions/{id}/activer")
    @Operation(summary = "Activer une version d'organigramme à date d'effet")
    public ResponseEntity<OrganigrammeVersionOutput> activerVersion(
            @PathVariable UUID id,
            @RequestParam UUID approuvePar) {
        return ResponseEntity.ok(activerVersionUseCase.execute(id, approuvePar));
    }

    // --- TYPES DE STRUCTURES ---

    @GetMapping("/types-structures")
    @Operation(summary = "Lister tous les types de structures hiérarchiques")
    public ResponseEntity<List<TypeStructureOutput>> listerTypesStructures() {
        return ResponseEntity.ok(listerTypesStructuresUseCase.execute());
    }

    @GetMapping("/types-structures/{id}")
    @Operation(summary = "Récupérer le détail d'un type de structure")
    public ResponseEntity<TypeStructureOutput> getTypeStructure(@PathVariable UUID id) {
        return ResponseEntity.ok(organigrammePort.getTypeStructure(id));
    }

    @PostMapping("/types-structures")
    @Operation(summary = "Créer un nouveau type de structure (avec ou sans type parent)")
    public ResponseEntity<TypeStructureOutput> creerTypeStructure(@Valid @RequestBody CreerTypeStructureRequest request) {
        TypeStructureOutput output = creerTypeStructureUseCase.execute(request.toInput());
        return ResponseEntity.status(HttpStatus.CREATED).body(output);
    }

    @PutMapping("/types-structures/{id}")
    @Operation(summary = "Modifier un type de structure")
    public ResponseEntity<TypeStructureOutput> modifierTypeStructure(
            @PathVariable UUID id,
            @Valid @RequestBody ModifierTypeStructureRequest request) {
        return ResponseEntity.ok(modifierTypeStructureUseCase.execute(request.toInput(id)));
    }

    @DeleteMapping("/types-structures/{id}")
    @Operation(summary = "Désactiver un type de structure")
    public ResponseEntity<Void> supprimerTypeStructure(@PathVariable UUID id) {
        supprimerTypeStructureUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    // --- OBJECTIFS D'UNITÉS (PERFORMANCE & MISSIONS) ---

    @GetMapping("/unites/{uniteId}/objectifs")
    @Operation(summary = "Lister les objectifs d'une unité organisationnelle")
    public ResponseEntity<List<ObjectifUniteOutput>> listerObjectifsUnite(
            @PathVariable UUID uniteId,
            @RequestParam(required = false) Integer anneeExercice) {
        return ResponseEntity.ok(listerObjectifsUniteUseCase.execute(uniteId, anneeExercice));
    }

    @PostMapping("/unites/{uniteId}/objectifs")
    @Operation(summary = "Ajouter un objectif opérationnel à une unité organisationnelle")
    public ResponseEntity<ObjectifUniteOutput> ajouterObjectif(
            @PathVariable UUID uniteId,
            @Valid @RequestBody CreerObjectifRequest request) {
        ObjectifUniteOutput output = ajouterObjectifUniteUseCase.execute(request.toInput(uniteId));
        return ResponseEntity.status(HttpStatus.CREATED).body(output);
    }

    @PatchMapping("/objectifs/{id}/realise")
    @Operation(summary = "Mettre à jour la valeur réalisée d'un objectif et recalculer le taux d'atteinte")
    public ResponseEntity<ObjectifUniteOutput> mettreAJourRealise(
            @PathVariable UUID id,
            @Valid @RequestBody MettreAJourRealiseRequest request) {
        return ResponseEntity.ok(mettreAJourRealiseObjectifUseCase.execute(request.toInput(id)));
    }

    @DeleteMapping("/objectifs/{id}")
    @Operation(summary = "Supprimer un objectif d'une unité")
    public ResponseEntity<Void> supprimerObjectif(@PathVariable UUID id) {
        supprimerObjectifUniteUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
