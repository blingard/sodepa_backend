package com.sodepa.erp.authorisation.presentation.rest;

import com.sodepa.erp.authorisation.application.IamGouvernanceService;
import com.sodepa.erp.authorisation.infrastructure.entities.AffectationProfilUtilisateurEntity;
import com.sodepa.erp.authorisation.infrastructure.entities.ProfilFonctionnelEntity;
import com.sodepa.erp.authorisation.infrastructure.entities.RegleSodEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service de Gouvernance des Identites, Profils Fonctionnels et Separation des Taches (REF-06 / IAM).
 * <p>
 * Ce service garantit le respect des principes fondamentaux de securite et de controle interne a la SODEPA :
 * <ul>
 *   <li><b>Administration des Profils Fonctionnels</b> : Profils metiers standardises (DG, DAF, Chef Service Budget, Magasinier, Controleur, etc.)
 *       regroupant des paquets de permissions et des plafonds d'engagement financiers.</li>
 *   <li><b>Affectations Multidimensionnelles</b> : Association d'un collaborateur a un profil sur un perimetre geographique
 *       et temporel strict (unite organisationnelle, magasin ou site precis, exercice budgetaire limite).</li>
 *   <li><b>Matrice de Separation des Taches (Segregation of Duties - SoD)</b> : Controle bloquant empechant l'attribution simultanee
 *       de profils ou droits incompatibles (ex: un gestionnaire de stock ne peut pas etre validateur de ses propres ecarts d'inventaire ;
 *       celui qui saisit une facture ne peut pas emettre le bon a payer).</li>
 *   <li><b>Revue d'acces et Synchronisation RH</b> : Suspension ou desactivation automatique immediate des comptes utilisateurs
 *       des que la fin de contrat ou le depart de l'agent est constate dans le module RH.</li>
 * </ul>
 */
@RestController
@RequestMapping({"/api/iam", "/api/v1/iam"})
@RequiredArgsConstructor
@Tag(name = "Gouvernance IAM, Profils Fonctionnels & Matrice SoD (REF-06)",
     description = "Administration des profils métiers SODEPA, affectations par périmètre, contrôle SoD bloquant et désactivation automatique sur sortie RH")
public class IamGouvernanceRestController {

    private final IamGouvernanceService iamService;

    /**
     * Liste l'ensemble des profils fonctionnels reutilisables definis dans l'ERP SODEPA.
     * <p>
     * Presente le libelle du profil, son niveau d'engagement financier maximal et les permissions associees.
     *
     * @return la liste des profils fonctionnels enregistres
     */
    @GetMapping("/profils")
    @Operation(summary = "Lister les profils fonctionnels réutilisables SODEPA")
    public ResponseEntity<List<ProfilFonctionnelEntity>> listerProfils() {
        return ResponseEntity.ok(iamService.listerProfilsFonctionnels());
    }

    /**
     * Cree un nouveau profil metier ou met a jour les droits d'un profil existant.
     * <p>
     * Toute modification des privileges d'un profil fonctionnel est automatiquement propagee
     * a tous les utilisateurs detenant ce profil sans necessiter de re-affectation manuelle.
     *
     * @param request definition du profil (code unique, intitule, permissions affectees, plafond d'engagement, exigence MFA)
     * @return le profil fonctionnel sauvegarde
     */
    @PostMapping("/profils")
    @Operation(summary = "Créer ou mettre à jour un profil fonctionnel (avec propagation automatique)")
    public ResponseEntity<ProfilFonctionnelEntity> creerOuModifierProfil(@Valid @RequestBody ProfilFonctionnelEntity request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(iamService.creerOuMettreAJourProfil(request));
    }

    /**
     * Attribue un profil metier a un collaborateur sur un perimetre organisationnel delimite.
     * <p>
     * Realise une verification prealable bloquante aupres de la matrice de Separation des Taches (SoD) :
     * si l'attribution cree un cumul de fonctions interdit, l'operation est immediatement rejetee.
     *
     * @param utilisateurId identifiant unique du collaborateur
     * @param codeProfil reference du profil fonctionnel a assigner (ex: 'PROFIL_GESTIONNAIRE_ACHATS')
     * @param uniteOrganisationnelleId perimetre optionnel limitant les droits a une direction, ranch ou abattoir
     * @param codeMagasinOuSite perimetre optionnel limitant les droits a un entrepot ou magasin physique
     * @param exerciceAutorise annee budgetaire d'autorisation optionnelle (ex: 2026)
     * @param dateFin date d'expiration optionnelle de l'habilitation
     * @return l'affectation formalisee
     */
    @PostMapping("/utilisateurs/{utilisateurId}/affecter-profil")
    @Operation(summary = "Attribuer un profil fonctionnel avec périmètre et vérification SoD bloquante")
    public ResponseEntity<AffectationProfilUtilisateurEntity> affecterProfil(
            @PathVariable UUID utilisateurId,
            @RequestParam String codeProfil,
            @RequestParam(required = false) UUID uniteOrganisationnelleId,
            @RequestParam(required = false) String codeMagasinOuSite,
            @RequestParam(required = false) Integer exerciceAutorise,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                iamService.affecterProfilAUtilisateur(
                        utilisateurId, codeProfil, uniteOrganisationnelleId, codeMagasinOuSite, exerciceAutorise, dateFin));
    }

    /**
     * Consulte l'ensemble des profils fonctionnels et des perimetres actifs attribues a un agent donne.
     *
     * @param utilisateurId identifiant du collaborateur audite
     * @return la liste de ses affectations de droits actives
     */
    @GetMapping("/utilisateurs/{utilisateurId}/profils")
    @Operation(summary = "Consulter les profils fonctionnels et périmètres actifs d'un utilisateur")
    public ResponseEntity<List<AffectationProfilUtilisateurEntity>> listerProfilsUtilisateur(@PathVariable UUID utilisateurId) {
        return ResponseEntity.ok(iamService.listerAffectationsUtilisateur(utilisateurId));
    }

    // --- MATRICE DE SÉPARATION DES TÂCHES (SoD) ---

    /**
     * Liste l'ensemble des regles formelles d'incompatibilite et d'antagonisme de la matrice SoD.
     *
     * @return la liste des regles d'incompatibilite actives
     */
    @GetMapping("/sod/regles")
    @Operation(summary = "Consulter la matrice des règles d'incompatibilité de Séparation des Tâches (SoD)")
    public ResponseEntity<List<RegleSodEntity>> listerReglesSod() {
        return ResponseEntity.ok(iamService.listerReglesSod());
    }

    /**
     * Enregistre une nouvelle regle d'incompatibilite stricte au sein de la matrice de separation des taches.
     * <p>
     * Definit deux profils ou actions incompatibles (ex: 'Saisie Facture' et 'Paiement Facture') ainsi que le niveau bloquant.
     *
     * @param request caracteristiques de la regle d'interdiction
     * @return la regle SoD enregistree
     */
    @PostMapping("/sod/regles")
    @Operation(summary = "Ajouter une règle d'incompatibilité dans la matrice SoD")
    public ResponseEntity<RegleSodEntity> creerRegleSod(@Valid @RequestBody RegleSodEntity request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(iamService.creerRegleSod(request));
    }

    /**
     * Execute un audit de controle global sur l'ensemble des comptes utilisateurs actifs.
     * <p>
     * Detecte tout conflit potentiel ou derogation de separation des taches existant dans la base
     * et produit un rapport de conformite a destination des auditeurs internes.
     *
     * @return la liste des anomalies et cumuls de droits suspects detectes
     */
    @GetMapping("/sod/audit-conflits")
    @Operation(summary = "Générer le rapport d'audit des conflits SoD sur les comptes actifs")
    public ResponseEntity<List<Map<String, Object>>> auditerConflitsSod() {
        return ResponseEntity.ok(iamService.auditerConflitsSodUtilisateurs());
    }

    // --- DÉSACTIVATION AUTOMATIQUE RH & EXPIRATION ---

    /**
     * Declenche la revue systematique et la desactivation automatique des comptes d'agents dont le contrat est arrive a echeance.
     * <p>
     * Interroge les donnees du module RH pour identifier les departs ou fins de mission et verrouille
     * immediatement les identifiants Keycloak et sessions actives correspondantes.
     *
     * @return le bilan de la revue automatique (nombre de comptes controles, desactives ou suspendus)
     */
    @PostMapping("/comptes/executer-desactivation-rh")
    @Operation(summary = "Exécuter la désactivation automatique des comptes d'agents sortis du personnel RH")
    public ResponseEntity<Map<String, Object>> executerDesactivationRh() {
        return ResponseEntity.ok(iamService.executerRevueAutomatiqueComptesEtSortiesRh());
    }
}
