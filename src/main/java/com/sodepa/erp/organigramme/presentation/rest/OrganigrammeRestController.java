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
 * Service de Gouvernance de l'Organigramme, des Postes et de la Ligne Managériale SODEPA (Module 01 - REF / Organigramme).
 * <p>
 * Ce service administre le socle organisationnel et hiérarchique de l'entreprise :
 * <ul>
 *   <li><b>Structure arborescente des Unités Organisationnelles</b> : Direction Générale, directions centrales, délégations régionales, ranches d'élevage et abattoirs frigorifiques.</li>
 *   <li><b>Gestion prévisionnelle des effectifs et postes</b> : Création de postes budgétés avec contrôle du plafond d'effectifs autorisés.</li>
 *   <li><b>Affectations des collaborateurs</b> : Affectation d'un agent à un poste avec contrôle d'intégrité (dates d'effet, non-chevauchement).</li>
 *   <li><b>Résolution managériale automatique (N+1 / N+2)</b> : Calcul dynamique de la chaîne de validation hiérarchique alimentant les workflows de congés, d'achats et de dépenses.</li>
 *   <li><b>Délégations de signature et d'intérim</b> : Transfert temporaire de pouvoir d'approbation (ex: congé du titulaire) avec traçabilité juridique.</li>
 *   <li><b>Versioning temporel</b> : Conception de réorganisations futures à date d'effet sans perturber l'exploitation courante.</li>
 *   <li><b>Objectifs de performance</b> : Déclinaison des missions et suivi du taux d'atteinte par unité opérationnelle.</li>
 * </ul>
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

    /**
     * Restitue l'arborescence hierarchique complete de la SODEPA (arbre racine depuis la Direction Generale).
     *
     * @return la liste des noeuds hierarchiques representant l'ensemble de la structure
     */
    @GetMapping("/arborescence")
    @Operation(summary = "Récupérer l'arborescence complète de l'organigramme SODEPA")
    public ResponseEntity<List<ArborescenceNoeudOutput>> getArborescenceComplete() {
        return ResponseEntity.ok(getArborescenceCompleteUseCase.execute());
    }

    /**
     * Extrait le sous-arbre hierarchique descendant d'une unite organisationnelle particuliere.
     * <p>
     * Permet par exemple d'isoler la structure d'une delegation regionale et de l'ensemble de ses ranches rattachés.
     *
     * @param id identifiant unique de l'unite parente
     * @return le noeud arborescent et ses unites filles
     */
    @GetMapping("/unites/{id}/sous-arbre")
    @Operation(summary = "Récupérer le sous-arbre d'une unité organisationnelle spécifique")
    public ResponseEntity<ArborescenceNoeudOutput> getSousArbreUnite(@PathVariable UUID id) {
        return ResponseEntity.ok(getSousArbreUniteUseCase.execute(id));
    }

    /**
     * Liste l'ensemble des unites organisationnelles a plat (vue tabulaire non arborescente).
     *
     * @return la liste complete des entites administratives et operationnelles
     */
    @GetMapping("/unites")
    @Operation(summary = "Lister toutes les unités organisationnelles à plat")
    public ResponseEntity<List<UniteOrganisationnelleOutput>> listerUnites() {
        return ResponseEntity.ok(organigrammePort.listerUnites());
    }

    /**
     * Cree une nouvelle unite organisationnelle (Direction, Division, Ranch, Station d'elevage, Abattoir).
     * <p>
     * Soumet la creation au circuit Maker-Checker pour validation par la Direction Generale.
     *
     * @param request parametres de l'unite (code officiel, intitule, type de structure, unite rattachee)
     * @return l'unite creee
     */
    @PostMapping("/unites")
    @Operation(summary = "Créer une nouvelle unité organisationnelle (Direction, Ranch, Abattoir, etc.)")
    public ResponseEntity<UniteOrganisationnelleOutput> creerUnite(@Valid @RequestBody CreerUniteRequest request) {
        UniteOrganisationnelleOutput output = creerUniteUseCase.execute(request.toInput());
        return ResponseEntity.status(HttpStatus.CREATED).body(output);
    }

    /**
     * Deplace une unite organisationnelle vers un nouveau rattachement hierarchique.
     * <p>
     * Verifie et bloque toute tentative de creation de cycle hierarchique (une unite ne peut devenir descendante d'elle-meme).
     *
     * @param id identifiant de l'unite a deplacer
     * @param request identifiant du nouveau parent hierarchique
     * @return l'unite avec son nouveau chemin materialise mis a jour
     */
    @PutMapping("/unites/{id}/deplacer")
    @Operation(summary = "Déplacer une unité dans l'arbre (avec détection de cycle)")
    public ResponseEntity<UniteOrganisationnelleOutput> deplacerUnite(
            @PathVariable UUID id,
            @Valid @RequestBody DeplacerUniteRequest request) {
        return ResponseEntity.ok(deplacerUniteUseCase.execute(request.toInput(id)));
    }

    /**
     * Traite la decision Maker-Checker sur la creation ou le deplacement d'une unite organisationnelle.
     *
     * @param id identifiant du ticket de soumission
     * @param request approbation ou rejet motive
     * @return confirmation
     */
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

    /**
     * Liste les postes de travail ouverts, avec filtrage optionnel par unite organisationnelle.
     *
     * @param uniteId identifiant optionnel de l'unite cible
     * @return la liste des postes repertories
     */
    @GetMapping("/postes")
    @Operation(summary = "Lister les postes (optionnellement filtrés par unité)")
    public ResponseEntity<List<PosteOutput>> listerPostes(@RequestParam(required = false) UUID uniteId) {
        return ResponseEntity.ok(organigrammePort.listerPostes(uniteId));
    }

    /**
     * Cree un nouveau poste budgete dans l'organigramme (ex: Chef de Ranch, Medecin veterinaire, Comptable).
     *
     * @param request caracteristiques du poste (intitule, niveau hierarchique, plafond d'effectifs, unite d'affectation)
     * @return le poste cree
     */
    @PostMapping("/postes")
    @Operation(summary = "Créer un poste budgété dans l'organigramme")
    public ResponseEntity<PosteOutput> creerPoste(@Valid @RequestBody CreerPosteRequest request) {
        PosteOutput output = creerPosteUseCase.execute(request.toInput());
        return ResponseEntity.status(HttpStatus.CREATED).body(output);
    }

    /**
     * Arbitre la validation d'un nouveau poste budgete dans le circuit a 4 yeux.
     *
     * @param id identifiant du ticket
     * @param request decision formelle
     * @return confirmation
     */
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

    /**
     * Affecte un agent a un poste de travail avec controle automatique du plafond d'effectifs autorises.
     * <p>
     * Empeche tout sur-effectif physique sur un poste n'ayant pas fait l'objet d'une dotation prealable.
     *
     * @param request donnees d'affectation (collaborateur, poste cible, date de debut d'exercice)
     * @return l'affectation formalisee
     */
    @PostMapping("/affectations")
    @Operation(summary = "Affecter un collaborateur à un poste (contrôle du plafond budgété)")
    public ResponseEntity<AffectationOutput> affecterCollaborateur(@Valid @RequestBody AffecterCollaborateurRequest request) {
        AffectationOutput output = affecterCollaborateurUseCase.execute(request.toInput());
        return ResponseEntity.status(HttpStatus.CREATED).body(output);
    }

    /**
     * Met fin a l'affectation d'un agent a un poste (mutation, depart, fin de contrat).
     * <p>
     * Libere le poste pour permettre une nouvelle affectation sans depasser le plafond.
     *
     * @param id identifiant de l'affectation active
     * @param request motif et date de fin d'exercice
     * @return l'affectation cloturee
     */
    @PostMapping("/affectations/{id}/cloturer")
    @Operation(summary = "Clôturer une affectation de poste")
    public ResponseEntity<AffectationOutput> cloturerAffectation(
            @PathVariable UUID id,
            @Valid @RequestBody CloturerAffectationRequest request) {
        return ResponseEntity.ok(cloturerAffectationUseCase.execute(request.toInput(id)));
    }

    /**
     * Valide ou rejette une operation d'affectation ou de mutation de personnel.
     *
     * @param id identifiant du ticket
     * @param request decision d'approbation
     * @return confirmation
     */
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

    /**
     * Resout dynamiquement la ligne hierarchique ascendante d'un agent (superviseur direct N+1, N+2) en integrant les delegations actives.
     * <p>
     * Utilise par les moteurs de workflow pour router automatiquement les demandes de conges,
     * d'ordres de mission ou d'achats vers le manager direct competent.
     *
     * @param id identifiant unique du collaborateur
     * @param maxNiveaux profondeur maximale d'ascendance (10 par defaut pour atteindre la Direction Generale)
     * @return la chaine hierarchique ordonnee avec indication des delegataires eventuels
     */
    @GetMapping("/collaborateurs/{id}/hierarchie")
    @Operation(summary = "Résoudre la chaîne managériale ascendante et les délégations en cours")
    public ResponseEntity<ChaineHierarchiqueOutput> getChaineHierarchique(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "10") int maxNiveaux) {
        return ResponseEntity.ok(resoudreHierarchieUseCase.execute(id, maxNiveaux));
    }

    // --- DÉLÉGATIONS DE POUVOIR ---

    /**
     * Liste les delegations de signature et pouvoirs manageriaux actuellement en vigueur.
     *
     * @return la liste des delegations actives
     */
    @GetMapping("/delegations")
    @Operation(summary = "Lister les délégations managériales actives")
    public ResponseEntity<List<DelegationOutput>> listerDelegations() {
        return ResponseEntity.ok(organigrammePort.listerDelegationsActives());
    }

    /**
     * Etablit une delegation temporaire de pouvoir et signature (ex: conge annuel d'un chef de ranch ou DAF).
     * <p>
     * Oriente automatiquement les taches d'approbation vers le remplacant designe pendant la periode couverte.
     *
     * @param request informations de delegation (titulaire, delegataire, perimetre autorise, dates de debut et de fin)
     * @return la delegation creee
     */
    @PostMapping("/delegations")
    @Operation(summary = "Créer une délégation de signature temporaire")
    public ResponseEntity<DelegationOutput> creerDelegation(@Valid @RequestBody CreerDelegationRequest request) {
        DelegationOutput output = creerDelegationUseCase.execute(request.toInput());
        return ResponseEntity.status(HttpStatus.CREATED).body(output);
    }

    /**
     * Traite l'arbitrage Maker-Checker sur une delegation de signature de direction.
     *
     * @param id identifiant du ticket
     * @param request approbation ou refus
     * @return confirmation
     */
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

    /**
     * Liste les versions historiques, actives et en projet de l'organigramme SODEPA.
     *
     * @return les versions repertoriees
     */
    @GetMapping("/versions")
    @Operation(summary = "Lister les versions d'organigramme")
    public ResponseEntity<List<OrganigrammeVersionOutput>> listerVersions() {
        return ResponseEntity.ok(organigrammePort.listerVersions());
    }

    /**
     * Prepare un projet de version future de l'organigramme (simulation d'une reorganisation ou fusion de services).
     *
     * @param request libelle du projet et date de prise d'effet envisagee
     * @return la version projet creee
     */
    @PostMapping("/versions")
    @Operation(summary = "Créer un projet de version d'organigramme (simulation / restructuration)")
    public ResponseEntity<OrganigrammeVersionOutput> creerVersion(@Valid @RequestBody CreerVersionRequest request) {
        OrganigrammeVersionOutput output = creerVersionUseCase.execute(request.toInput());
        return ResponseEntity.status(HttpStatus.CREATED).body(output);
    }

    /**
     * Rend active et exécutoire une nouvelle version d'organigramme a la date d'effet fixee.
     *
     * @param id identifiant de la version a promulguer
     * @param approuvePar identifiant de l'autorite ayant valide la restructuration (ex: Conseil d'Administration)
     * @return la version officialisee
     */
    @PostMapping("/versions/{id}/activer")
    @Operation(summary = "Activer une version d'organigramme à date d'effet")
    public ResponseEntity<OrganigrammeVersionOutput> activerVersion(
            @PathVariable UUID id,
            @RequestParam UUID approuvePar) {
        return ResponseEntity.ok(activerVersionUseCase.execute(id, approuvePar));
    }

    // --- TYPES DE STRUCTURES ---

    /**
     * Liste l'ensemble des types de structures hierarchiques reconnus (Direction Generale, Direction, Ranch, Abattoir).
     *
     * @return la liste des types de structures
     */
    @GetMapping("/types-structures")
    @Operation(summary = "Lister tous les types de structures hiérarchiques")
    public ResponseEntity<List<TypeStructureOutput>> listerTypesStructures() {
        return ResponseEntity.ok(listerTypesStructuresUseCase.execute());
    }

    /**
     * Consulte les caracteristiques d'un type de structure donne.
     *
     * @param id identifiant du type
     * @return le detail du type
     */
    @GetMapping("/types-structures/{id}")
    @Operation(summary = "Récupérer le détail d'un type de structure")
    public ResponseEntity<TypeStructureOutput> getTypeStructure(@PathVariable UUID id) {
        return ResponseEntity.ok(organigrammePort.getTypeStructure(id));
    }

    /**
     * Cree un nouveau type de structure administrative dans la nomenclature de l'organigramme.
     *
     * @param request donnees de definition du type
     * @return le type cree
     */
    @PostMapping("/types-structures")
    @Operation(summary = "Créer un nouveau type de structure (avec ou sans type parent)")
    public ResponseEntity<TypeStructureOutput> creerTypeStructure(@Valid @RequestBody CreerTypeStructureRequest request) {
        TypeStructureOutput output = creerTypeStructureUseCase.execute(request.toInput());
        return ResponseEntity.status(HttpStatus.CREATED).body(output);
    }

    /**
     * Modifie les caracteristiques d'un type de structure existant.
     *
     * @param id identifiant du type a ajuster
     * @param request nouvelles valeurs
     * @return le type actualise
     */
    @PutMapping("/types-structures/{id}")
    @Operation(summary = "Modifier un type de structure")
    public ResponseEntity<TypeStructureOutput> modifierTypeStructure(
            @PathVariable UUID id,
            @Valid @RequestBody ModifierTypeStructureRequest request) {
        return ResponseEntity.ok(modifierTypeStructureUseCase.execute(request.toInput(id)));
    }

    /**
     * Desactive un type de structure dans le referentiel.
     *
     * @param id identifiant du type a supprimer/desactiver
     * @return confirmation sans contenu
     */
    @DeleteMapping("/types-structures/{id}")
    @Operation(summary = "Désactiver un type de structure")
    public ResponseEntity<Void> supprimerTypeStructure(@PathVariable UUID id) {
        supprimerTypeStructureUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    // --- OBJECTIFS D'UNITÉS (PERFORMANCE & MISSIONS) ---

    /**
     * Liste les objectifs de performance fixes a une unite organisationnelle (ex: tonnage de viande traite, naissances de broutards).
     *
     * @param uniteId identifiant de l'unite
     * @param anneeExercice filtre optionnel par annee d'exercice
     * @return la liste des objectifs assignes
     */
    @GetMapping("/unites/{uniteId}/objectifs")
    @Operation(summary = "Lister les objectifs d'une unité organisationnelle")
    public ResponseEntity<List<ObjectifUniteOutput>> listerObjectifsUnite(
            @PathVariable UUID uniteId,
            @RequestParam(required = false) Integer anneeExercice) {
        return ResponseEntity.ok(listerObjectifsUniteUseCase.execute(uniteId, anneeExercice));
    }

    /**
     * Assigne un nouvel objectif operationnel de performance a une unite pour un exercice donne.
     *
     * @param uniteId unite beneficiaire
     * @param request caracteristiques de l'objectif (intitule, cible chiffree, echeance)
     * @return l'objectif cree
     */
    @PostMapping("/unites/{uniteId}/objectifs")
    @Operation(summary = "Ajouter un objectif opérationnel à une unité organisationnelle")
    public ResponseEntity<ObjectifUniteOutput> ajouterObjectif(
            @PathVariable UUID uniteId,
            @Valid @RequestBody CreerObjectifRequest request) {
        ObjectifUniteOutput output = ajouterObjectifUniteUseCase.execute(request.toInput(uniteId));
        return ResponseEntity.status(HttpStatus.CREATED).body(output);
    }

    /**
     * Met a jour la valeur reelle constatee d'un objectif et recalcule automatiquement son taux d'atteinte (en %).
     *
     * @param id identifiant de l'objectif
     * @param request valeur chiffree realisee constatee sur le terrain
     * @return l'objectif actualise avec son nouveau pourcentage de realisation
     */
    @PatchMapping("/objectifs/{id}/realise")
    @Operation(summary = "Mettre à jour la valeur réalisée d'un objectif et recalculer le taux d'atteinte")
    public ResponseEntity<ObjectifUniteOutput> mettreAJourRealise(
            @PathVariable UUID id,
            @Valid @RequestBody MettreAJourRealiseRequest request) {
        return ResponseEntity.ok(mettreAJourRealiseObjectifUseCase.execute(request.toInput(id)));
    }

    /**
     * Supprime un objectif operationnel affecte a une unite.
     *
     * @param id identifiant de l'objectif a retirer
     * @return confirmation sans contenu
     */
    @DeleteMapping("/objectifs/{id}")
    @Operation(summary = "Supprimer un objectif d'une unité")
    public ResponseEntity<Void> supprimerObjectif(@PathVariable UUID id) {
        supprimerObjectifUniteUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
