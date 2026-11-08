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
 * Contrôleur REST de Gouvernance IAM, Profils Fonctionnels, Permissions Multidimensionnelles
 * et Matrice de Séparation des Tâches (SoD) (REF-06 / F-REF-024 à F-REF-028).
 */
@RestController
@RequestMapping({"/api/iam", "/api/v1/iam"})
@RequiredArgsConstructor
@Tag(name = "Gouvernance IAM, Profils Fonctionnels & Matrice SoD (REF-06)",
     description = "Administration des 13 profils fonctionnels SODEPA, affectations par périmètre (unité/site/exercice), contrôle SoD et désactivation automatique sur sortie RH")
/**
 * Contrôleur REST exposant les points de terminaison pour `IamGouvernanceRestController`.
 * Permet la gestion sécurisée des opérations de l'ERP SODEPA.
 */
public class IamGouvernanceRestController {

    private final IamGouvernanceService iamService;

    /**
     * Liste tous les profils fonctionnels réutilisables configurés dans l'ERP SODEPA.
     *
     * @return Réponse HTTP 200 contenant la liste des profils fonctionnels.
     */
    @GetMapping("/profils")
    @Operation(summary = "Lister les profils fonctionnels réutilisables SODEPA (CA-REF-025-03 / RG-REF-018)")
    public ResponseEntity<List<ProfilFonctionnelEntity>> listerProfils() {
        return ResponseEntity.ok(iamService.listerProfilsFonctionnels());
    }

    /**
     * Crée ou met à jour un profil fonctionnel et propage automatiquement ses droits à tous ses porteurs.
     *
     * @param request Données du profil fonctionnel.
     * @return Réponse HTTP 201 contenant le profil sauvegardé.
     */
    @PostMapping("/profils")
    @Operation(summary = "Créer ou mettre à jour un profil fonctionnel avec propagation automatique à ses porteurs (CA-REF-025-03)")
    public ResponseEntity<ProfilFonctionnelEntity> creerOuModifierProfil(@Valid @RequestBody ProfilFonctionnelEntity request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(iamService.creerOuMettreAJourProfil(request));
    }

    /**
     * Attribue un profil fonctionnel à un utilisateur sur un périmètre donné après contrôle bloquant de la matrice SoD.
     *
     * @param utilisateurId            Identifiant UUID de l'utilisateur.
     * @param codeProfil               Code du profil fonctionnel à attribuer.
     * @param uniteOrganisationnelleId Périmètre d'unité organisationnelle (optionnel).
     * @param codeMagasinOuSite        Périmètre de site ou magasin (optionnel).
     * @param exerciceAutorise         Exercice budgétaire/comptable autorisé (optionnel).
     * @param dateFin                  Date de fin de validité de l'affectation (optionnelle).
     * @return Réponse HTTP 201 contenant l'affectation crée.
     */
    @PostMapping("/utilisateurs/{utilisateurId}/affecter-profil")
    @Operation(summary = "Attribuer un profil fonctionnel avec périmètre (unité, magasin/site, exercice) et vérification SoD bloquante (CA-REF-025-01 & CA-REF-027-01)")
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
     * Liste les profils fonctionnels et périmètres actifs d'un utilisateur.
     *
     * @param utilisateurId Identifiant UUID de l'utilisateur.
     * @return Réponse HTTP 200 contenant la liste des affectations actives.
     */
    @GetMapping("/utilisateurs/{utilisateurId}/profils")
    @Operation(summary = "Consulter les profils fonctionnels et périmètres actifs d'un utilisateur")
    public ResponseEntity<List<AffectationProfilUtilisateurEntity>> listerProfilsUtilisateur(@PathVariable UUID utilisateurId) {
        return ResponseEntity.ok(iamService.listerAffectationsUtilisateur(utilisateurId));
    }

    // --- MATRICE SOD (CA-REF-027-01 & 02) ---

    /**
     * Liste toutes les règles actives d'incompatibilité de la matrice de Séparation des Tâches (SoD).
     *
     * @return Réponse HTTP 200 contenant les règles SoD.
     */
    @GetMapping("/sod/regles")
    @Operation(summary = "Consulter la matrice des règles d'incompatibilité de Séparation des Tâches (SoD)")
    public ResponseEntity<List<RegleSodEntity>> listerReglesSod() {
        return ResponseEntity.ok(iamService.listerReglesSod());
    }

    /**
     * Crée une nouvelle règle d'incompatibilité dans la matrice SoD.
     *
     * @param request Données de la règle SoD.
     * @return Réponse HTTP 201 contenant la règle crée.
     */
    @PostMapping("/sod/regles")
    @Operation(summary = "Ajouter une règle d'incompatibilité dans la matrice SoD")
    public ResponseEntity<RegleSodEntity> creerRegleSod(@Valid @RequestBody RegleSodEntity request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(iamService.creerRegleSod(request));
    }

    /**
     * Génère l'état d'audit des conflits SoD potentiels sur l'ensemble des comptes utilisateurs actifs.
     *
     * @return Réponse HTTP 200 contenant la liste des conflits détectés.
     */
    @GetMapping("/sod/audit-conflits")
    @Operation(summary = "Générer le rapport d'audit des conflits SoD potentiels sur les comptes actifs (CA-REF-027-02)")
    public ResponseEntity<List<Map<String, Object>>> auditerConflitsSod() {
        return ResponseEntity.ok(iamService.auditerConflitsSodUtilisateurs());
    }

    // --- DÉSACTIVATION AUTOMATIQUE RH & EXPIRATION (CA-REF-024-02 & 03) ---

    /**
     * Décleéenche la revue et la désactivation automatique des comptes d'agents sortis (fin de contrat RH) ou expirés.
     *
     * @return Réponse HTTP 200 contenant le rapport d'exécution.
     */
    @PostMapping("/comptes/executer-desactivation-rh")
    @Operation(summary = "Exécuter la désactivation automatique des comptes d'agents sortis (fin de contrat RH) ou expirés (CA-REF-024-02 & 03 / RG-REF-024)")
    public ResponseEntity<Map<String, Object>> executerDesactivationRh() {
        return ResponseEntity.ok(iamService.executerRevueAutomatiqueComptesEtSortiesRh());
    }
}
