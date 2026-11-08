package com.sodepa.erp.dashboard.presentation.rest;

import com.sodepa.erp.dashboard.application.GouvernanceMdmService;
import com.sodepa.erp.dashboard.infrastructure.entities.FamilleReferentielEntity;
import com.sodepa.erp.dashboard.infrastructure.entities.FicheReferentielVersionEntity;
import com.sodepa.erp.dashboard.infrastructure.entities.ModeleDocumentNumMasqueEntity;
import com.sodepa.erp.organigramme.infrastructure.entities.EvolutionStructureOrganigrammeEntity;
import com.sodepa.erp.utils.StatutCycleVieReferentiel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Contrôleur REST de Gouvernance MDM (REF-01), Évolutions d'Organigramme (REF-02),
 * Modèles &amp; Masques de Numérotation (REF-07) et Tableau de Bord / Réversibilité (EP-REF-08).
 */
@RestController
@RequestMapping({"/api/gouvernance-mdm", "/api/v1/gouvernance-mdm"})
@Tag(name = "Gouvernance MDM, Cycle de Vie, Réversibilité & Tableau de Bord (REF-01 / REF-02 / EP-REF-08)",
     description = "Administration des familles de référentiels, fiches versionnées à date d'effet, dédoublonnage, import/export, modèles de documents et réversibilité")
/**
 * Contrôleur REST exposant les points de terminaison pour `GouvernanceMdmRestController`.
 * Permet la gestion sécurisée des opérations de l'ERP SODEPA.
 */
public class GouvernanceMdmRestController {

    private final GouvernanceMdmService service;

    /**
     * Constructeur du contrôleur de gouvernance MDM.
     *
     * @param service Service applicatif MDM.
     */
    public GouvernanceMdmRestController(GouvernanceMdmService service) {
        this.service = service;
    }

    // --- F-REF-001 & F-REF-003 : Familles de référentiels & Codification ---

    /**
     * Liste les familles de référentiels gouvernées, optionnellement filtrées par propriétaire métier.
     *
     * @param proprietaireMetier Code optionnel de la direction propriétaire métier.
     * @return Réponse HTTP 200 contenant la liste des familles.
     */
    @GetMapping("/familles")
    @Operation(summary = "Lister les familles de référentiels et leurs propriétaires métiers (F-REF-001)")
    public ResponseEntity<List<FamilleReferentielEntity>> listerFamilles(
            @RequestParam(required = false) String proprietaireMetier) {
        return ResponseEntity.ok(service.listerFamilles(proprietaireMetier));
    }

    /**
     * Crée une nouvelle famille de référentiel avec contrôle du propriétaire métier.
     *
     * @param famille Données de la famille à créer.
     * @return Réponse HTTP 201 contenant la famille crée.
     */
    @PostMapping("/familles")
    @Operation(summary = "Créer une famille de référentiel avec masque et propriétaire métier obligatoire (CA-REF-001-02 & 04)")
    public ResponseEntity<FamilleReferentielEntity> creerFamille(@RequestBody FamilleReferentielEntity famille) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creerFamille(famille));
    }

    /**
     * Génère le prochain code séquentiel unique d'une famille de référentiel selon son masque.
     *
     * @param codeFamille Code de la famille.
     * @return Réponse HTTP 200 contenant le code généré.
     */
    @PostMapping("/familles/{codeFamille}/generer-code")
    @Operation(summary = "Générer le prochain code séquentiel d'une famille selon son masque (F-REF-003)")
    public ResponseEntity<Map<String, String>> genererProchainCode(@PathVariable String codeFamille) {
        return ResponseEntity.ok(Map.of(
                "codeFamille", codeFamille.toUpperCase(),
                "codeGenere", service.genererProchainCodeFiche(codeFamille)
        ));
    }

    // --- F-REF-002, F-REF-005 & F-REF-006 : Fiches MDM, Cycle de vie & Dates d'effet ---

    /**
     * Recherche multicritère de fiches de référentiels avec filtrage par périmètre d'unité.
     *
     * @param codeFamille        Code de la famille.
     * @param statut             Filtre optionnel sur le statut du cycleée de vie.
     * @param motCle             Mot-cleé optionnel.
     * @param codeUnitePerimetre Code d'unité organisationnelle pour le filtrage de périmètre.
     * @return Réponse HTTP 200 contenant les fiches correspondantes.
     */
    @GetMapping("/fiches")
    @Operation(summary = "Rechercher des fiches de référentiels par famille, statut, mot-cleé et périmètre (F-REF-005)")
    public ResponseEntity<List<FicheReferentielVersionEntity>> rechercherFiches(
            @RequestParam String codeFamille,
            @RequestParam(required = false) StatutCycleVieReferentiel statut,
            @RequestParam(required = false) String motCle,
            @RequestParam(required = false) String codeUnitePerimetre) {
        return ResponseEntity.ok(service.rechercherFiches(codeFamille, statut, motCle, codeUnitePerimetre));
    }

    /**
     * Restitue la version exacte d'une fiche de référentiel en vigueur à une date donnée ({@code asOfDate}).
     *
     * @param codeFamille Code de la famille.
     * @param codeFiche   Code de la fiche.
     * @param asOfDate    Date de référence.
     * @return Réponse HTTP 200 contenant la version active à la date indiquée.
     */
    @GetMapping("/fiches/a-date")
    @Operation(summary = "Consulter l'état d'une fiche de référentiel à une date d'effet passée ou future (F-REF-006)")
    public ResponseEntity<FicheReferentielVersionEntity> consulterFicheADate(
            @RequestParam String codeFamille,
            @RequestParam String codeFiche,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        return ResponseEntity.ok(service.consulterFicheADate(codeFamille, codeFiche, asOfDate));
    }

    /**
     * Soumet une nouvelle fiche de référentiel ou crée une nouvelle version datée.
     *
     * @param fiche Données de la fiche à versionner.
     * @return Réponse HTTP 201 contenant la version crée.
     */
    @PostMapping("/fiches")
    @Operation(summary = "Soumettre ou créer une nouvelle version datée d'une fiche de référentiel (F-REF-002 & F-REF-006)")
    public ResponseEntity<FicheReferentielVersionEntity> soumettreOuVersionnerFiche(
            @RequestBody FicheReferentielVersionEntity fiche) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.soumettreOuVersionnerFiche(fiche));
    }

    /**
     * Valide ou rejette fonctionnellement une fiche de référentiel avec séparation des tâches et notification Email/SMS.
     *
     * @param ficheId          Identifiant UUID de la fiche.
     * @param valideurUsername Identifiant du valideur métier.
     * @param roleValideur     Rôle du valideur.
     * @param approuver        {@code true} pour approuver, {@code false} pour rejeter.
     * @param motif            Motif obligatoire en cas de rejet.
     * @return Réponse HTTP 200 contenant la fiche mise à jour.
     */
    @PostMapping("/fiches/{ficheId}/decision-metier")
    @Operation(summary = "Valider ou rejeter une fiche de référentiel avec séparation des tâches et notification Email+SMS (CA-REF-002-07)")
    public ResponseEntity<FicheReferentielVersionEntity> validerOuRejeterFicheMetier(
            @PathVariable UUID ficheId,
            @RequestParam String valideurUsername,
            @RequestParam(defaultValue = "PROPRIETAIRE_METIER") String roleValideur,
            @RequestParam boolean approuver,
            @RequestParam(required = false) String motif) {
        return ResponseEntity.ok(service.validerOuRejeterFicheMetier(ficheId, valideurUsername, roleValideur, approuver, motif));
    }

    /**
     * Suspend ou archive une fiche de référentiel après vérification de l'absence de transactions non soldées.
     *
     * @param codeFamille Code de la famille.
     * @param codeFiche   Code de la fiche.
     * @param statutCible Statut cible ({@code SUSPENDU} ou {@code ARCHIVE}).
     * @param motif       Motif de l'action.
     * @return Réponse HTTP 200 contenant la fiche mise à jour.
     */
    @PatchMapping("/fiches/{codeFamille}/{codeFiche}/statut")
    @Operation(summary = "Suspendre ou archiver une fiche de référentiel sans transactions non soldées (CA-REF-002-03)")
    public ResponseEntity<FicheReferentielVersionEntity> suspendreOuArchiverFiche(
            @PathVariable String codeFamille,
            @PathVariable String codeFiche,
            @RequestParam StatutCycleVieReferentiel statutCible,
            @RequestParam String motif) {
        return ResponseEntity.ok(service.suspendreOuArchiverFiche(codeFamille, codeFiche, statutCible, motif));
    }

    /**
     * Supprime physiquement une fiche uniquement si elle est à l'état {@code BROUILLON}.
     *
     * @param ficheId Identifiant UUID de la fiche.
     * @return Réponse HTTP 204 après suppression.
     */
    @DeleteMapping("/fiches/{ficheId}/brouillon")
    @Operation(summary = "Supprimer physiquement une fiche à l'état BROUILLON jamais utilisée (CA-REF-002-04 & 05)")
    public ResponseEntity<Void> suppriméerFicheBrouillon(@PathVariable UUID ficheId) {
        service.suppriméerFicheBrouillon(ficheId);
        return ResponseEntity.noContent().build();
    }

    // --- F-REF-004 : Détection et fusion des doublons ---

    /**
     * Détecte les doublons potentiels au sein d'une famille de référentiel.
     *
     * @param codeFamille     Code de la famille.
     * @param libelleCandidat Libellé à contrôler.
     * @return Réponse HTTP 200 contenant les doublons potentiels détectés.
     */
    @GetMapping("/doublons/detecter")
    @Operation(summary = "Détecter les doublons potentiels par similarité normalisée (F-REF-004)")
    public ResponseEntity<List<FicheReferentielVersionEntity>> detecterDoublons(
            @RequestParam String codeFamille,
            @RequestParam String libelleCandidat) {
        return ResponseEntity.ok(service.detecterDoublonsPotentiels(codeFamille, libelleCandidat));
    }

    /**
     * Fusionne deux fiches doublons en réaffectant les transactions vers la fiche maître.
     *
     * @param codeFamille         Code de la famille.
     * @param codeFicheMaitre     Code de la fiche maître conservée.
     * @param codeFicheSecondaire Code de la fiche secondaire archivée.
     * @param operateurUsername   Identifiant de l'opérateur.
     * @return Réponse HTTP 200 contenant le rapport de fusion.
     */
    @PostMapping("/doublons/fusionner")
    @Operation(summary = "Fusionner deux fiches doublons avec réaffectation des transactions et archivage tracé (CA-REF-004-02)")
    public ResponseEntity<Map<String, Object>> fusionnerDoublons(
            @RequestParam String codeFamille,
            @RequestParam String codeFicheMaitre,
            @RequestParam String codeFicheSecondaire,
            @RequestParam String operateurUsername) {
        return ResponseEntity.ok(service.fusionnerFichesDoublons(
                codeFamille, codeFicheMaitre, codeFicheSecondaire, operateurUsername));
    }

    // --- F-REF-007 & CT-REF-08 : Import et Export massifs ---

    /**
     * Importe en masse un lot de fiches en statut {@code BROUILLON} avec rapport d'erreurs détaillé ligne par ligne.
     *
     * @param codeFamille    Code de la famille cible.
     * @param auteurUsername Identifiant de l'opérateur d'import.
     * @param lignes         Lignes à importer.
     * @return Réponse HTTP 200 contenant le rapport d'importation.
     */
    @PostMapping("/import-masse/{codeFamille}")
    @Operation(summary = "Importer en masse des fiches en BROUILLON avec rapport d'erreurs ligne par ligne (F-REF-007)")
    public ResponseEntity<Map<String, Object>> importerEnMasse(
            @PathVariable String codeFamille,
            @RequestParam(defaultValue = "import.operator") String auteurUsername,
            @RequestBody List<Map<String, String>> lignes) {
        return ResponseEntity.ok(service.importerFichesEnMasse(codeFamille, lignes, auteurUsername));
    }

    /**
     * Exporte les fiches d'une famille au format CSV selon les filtres et le périmètre.
     *
     * @param codeFamille        Code de la famille.
     * @param statut             Statut optionnel.
     * @param codeUnitePerimetre Périmètre d'unité optionnel.
     * @return Fichier CSV en téléchargement.
     */
    @GetMapping("/export-csv/{codeFamille}")
    @Operation(summary = "Exporter les fiches d'une famille au format CSV (F-REF-007 / CT-REF-08)")
    public ResponseEntity<String> exporterCsv(
            @PathVariable String codeFamille,
            @RequestParam(required = false) StatutCycleVieReferentiel statut,
            @RequestParam(required = false) String codeUnitePerimetre) {
        String csv = service.exporterFichesCsv(codeFamille, statut, codeUnitePerimetre);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"referentiel_" + codeFamille + ".csv\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv);
    }

    // --- F-REF-008 à F-REF-011 : Fiches d'unités opérationnelles & Évolutions ---

    /**
     * Liste l'historique des évolutions d'organigramme et fiches d'unités opérationnelles.
     *
     * @param codeUnite Code optionnel d'unité organisationnelle.
     * @return Réponse HTTP 200 contenant l'historique des évolutions.
     */
    @GetMapping("/organigramme-evolutions")
    @Operation(summary = "Consulter les fiches d'unités opérationnelles et l'historique des évolutions d'organigramme (F-REF-008 à 011)")
    public ResponseEntity<List<EvolutionStructureOrganigrammeEntity>> listerEvolutionsStructure(
            @RequestParam(required = false) String codeUnite) {
        return ResponseEntity.ok(service.listerEvolutionsStructure(codeUnite));
    }

    /**
     * Enregistre une fiche complète d'unité opérationnelle ou une évolution d'organigramme datée.
     *
     * @param evolution Données de la fiche ou de l'évolution.
     * @return Réponse HTTP 201 contenant l'enregistréement cré.
     */
    @PostMapping("/organigramme-evolutions")
    @Operation(summary = "Enregistrer une fiche d'unité ou une évolution d'organigramme avec texte organique obligatoire (RG-REF-017)")
    public ResponseEntity<EvolutionStructureOrganigrammeEntity> enregistréerFicheOuEvolutionUnite(
            @RequestBody EvolutionStructureOrganigrammeEntity evolution) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.enregistréerFicheOuEvolutionUnite(evolution));
    }

    // --- F-REF-029 : Modèles de documents officiels SODEPA & Masques de numérotation ---

    /**
     * Liste les modèles de documents officiels SODEPA et leurs masques de numérotation.
     *
     * @return Réponse HTTP 200 contenant la liste des modèles.
     */
    @GetMapping("/modeles-documents")
    @Operation(summary = "Lister les modèles de documents officiels SODEPA et leurs masques de numérotation (F-REF-029)")
    public ResponseEntity<List<ModeleDocumentNumMasqueEntity>> listerModelesDocuments() {
        return ResponseEntity.ok(service.listerModelesDocuments());
    }

    /**
     * Crée ou met à jour un modèle de document officiel SODEPA.
     *
     * @param modele Données du modèle.
     * @return Réponse HTTP 201 contenant le modèle cré.
     */
    @PostMapping("/modeles-documents")
    @Operation(summary = "Créer un modèle de document officiel SODEPA avec son masque de numérotation (F-REF-029)")
    public ResponseEntity<ModeleDocumentNumMasqueEntity> creerModeleDocument(
            @RequestBody ModeleDocumentNumMasqueEntity modele) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creerModeleDocument(modele));
    }

    /**
     * Génère un numéro séquentiel continu pour un document officiel SODEPA selon son masque.
     *
     * @param codeModele    Code du modèle de document.
     * @param codeUnite     Code de l'unité émettrice.
     * @param annéeeExercice Année de l'exercice.
     * @return Réponse HTTP 200 contenant le numéro généré.
     */
    @PostMapping("/modeles-documents/{codeModele}/numeroter")
    @Operation(summary = "Générer un numéro séquentiel continu sans trou ni doublon pour un document officiel (CA-REF-029-01 & 02)")
    public ResponseEntity<Map<String, Object>> genererNumeroDocument(
            @PathVariable String codeModele,
            @RequestParam String codeUnite,
            @RequestParam int anneeExercice) {
        return ResponseEntity.ok(service.genererNumeroDocumentOfficiel(codeModele, codeUnite, anneeExercice));
    }

    // --- F-REF-032 à F-REF-034 : Mode dégradé, Réversibilité & Tableau de bord ---

    /**
     * Exporte le pack de cache local en lecture seule pour une unité opérationnelle distante (mode dégradé).
     *
     * @param codeUnite Code de l'unité distante.
     * @return Réponse HTTP 200 contenant le pack JSON autonome.
     */
    @GetMapping("/mode-degrade/pack-unite/{codeUnite}")
    @Operation(summary = "Exporter le pack de consultation hors ligne (mode dégradé) pour une unité distante (CA-REF-032-03)")
    public ResponseEntity<Map<String, Object>> exporterPackModeDegrade(@PathVariable String codeUnite) {
        return ResponseEntity.ok(service.exporterPackModeDegradeUnite(codeUnite));
    }

    /**
     * Exporte l'intégralité du référentiel, de ses versions et de ses structures pour la réversibilité.
     *
     * @return Réponse HTTP 200 contenant l'archive complète de réversibilité.
     */
    @GetMapping("/reversibilite/export-integral")
    @Operation(summary = "Exporter l'intégralité du Référentiel et de ses historiques pour réversibilité sans perte (F-REF-033 / RG-REF-015)")
    public ResponseEntity<Map<String, Object>> exporterReversibiliteIntegrale() {
        return ResponseEntity.ok(service.exporterReversibiliteIntegrale());
    }

    /**
     * Retourne le tableau de bord de gouvernance et de suivi de la qualité du Référentiel MDM.
     *
     * @param proprietaireMetier Filtre optionnel par propriétaire métier.
     * @return Réponse HTTP 200 contenant les indicateurs du tableau de bord.
     */
    @GetMapping("/dashboard")
    @Operation(summary = "Consulter le tableau de bord de gouvernance et qualité du Référentiel MDM (F-REF-034)")
    public ResponseEntity<Map<String, Object>> getTableauDeBordGouvernance(
            @RequestParam(required = false) String proprietaireMetier) {
        return ResponseEntity.ok(service.calculerTableauDeBordGouvernance(proprietaireMetier));
    }
}
