package com.sodepa.erp.fiscalite.presentation.rest;

import com.sodepa.erp.fiscalite.application.FiscaliteService;
import com.sodepa.erp.fiscalite.infrastructure.entities.RegimeFiscalEntity;
import com.sodepa.erp.fiscalite.infrastructure.entities.SeuilGestionEntity;
import com.sodepa.erp.fiscalite.infrastructure.entities.TaxeImpotsEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST du Référentiel Fiscal et de la Table Unique des Seuils de Gestion (REF-04 / F-REF-020).
 */
@RestController
@RequestMapping({"/api/fiscalite", "/api/v1/fiscalite"})
@RequiredArgsConstructor
@Tag(name = "Référentiel Fiscal & Table Unique des Seuils (REF-04)",
     description = "Gouvernance des taxes DGI Cameroun (TVA, AIR, CAC, taxes vétérinaires), régimes fiscaux et seuils de gestion datés")
/**
 * Contrôleur REST exposant les points de terminaison pour `FiscaliteRestController`.
 * Permet la gestion sécurisée des opérations de l'ERP SODEPA.
 */
public class FiscaliteRestController {

    private final FiscaliteService fiscaliteService;

    // --- TAXES & IMPÔTS ---

    /**
     * Liste toutes les taxes, impôts et retenues à la source actifs.
     *
     * @return Réponse HTTP 200 contenant la liste des taxes.
     */
    @GetMapping("/taxes")
    @Operation(summary = "Lister les taxes, impôts et retenues à la source actifs (CA-REF-020-01)")
    public ResponseEntity<List<TaxeImpotsEntity>> listerTaxes() {
        return ResponseEntity.ok(fiscaliteService.listerTaxesActives());
    }

    /**
     * Résout le taux fiscal en vigueur à une date d'opération donnée.
     *
     * @param code          Code de la taxe.
     * @param dateOperation Date de référence (optionnelle).
     * @return Réponse HTTP 200 contenant la règle fiscale applicable.
     */
    @GetMapping("/taxes/{code}/applicable")
    @Operation(summary = "Résoudre le taux fiscal en vigueur à une date d'opération donnée (CA-REF-020-01 / RG-REF-007)")
    public ResponseEntity<TaxeImpotsEntity> resoudreTaxeApplicable(
            @PathVariable String code,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOperation) {
        return ResponseEntity.ok(fiscaliteService.resoudreTaxeApplicable(code, dateOperation));
    }

    /**
     * Consulte l'historique complet des versions d'une taxe par date d'effet.
     *
     * @param code Code de la taxe.
     * @return Réponse HTTP 200 contenant l'historique de la taxe.
     */
    @GetMapping("/taxes/{code}/historique")
    @Operation(summary = "Consulter l'historique complet des versions d'une taxe par date d'effet")
    public ResponseEntity<List<TaxeImpotsEntity>> consulterHistoriqueTaxe(@PathVariable String code) {
        return ResponseEntity.ok(fiscaliteService.consulterHistoriqueTaxe(code));
    }

    /**
     * Crée ou révise un taux fiscal avec date d'effet et historisation automatique de la version précédente.
     *
     * @param request Données de la taxe.
     * @return Réponse HTTP 201 contenant la taxe enregistrée.
     */
    @PostMapping("/taxes")
    @Operation(summary = "Créer ou réviser un taux fiscal avec date d'effet et historisation automatique (CA-REF-020-01)")
    public ResponseEntity<TaxeImpotsEntity> enregistréerTaxe(@Valid @RequestBody TaxeImpotsEntity request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fiscaliteService.enregistréerOuRevisionnerTaxe(request));
    }

    /**
     * Simule le calcul de la TVA et de la retenue à la source (AIR) selon le régime fiscal et la date d'opération.
     *
     * @param montantHt        Montant hors taxes en FCFA.
     * @param codeRegimeFiscal Code du régime fiscal (par défaut {@code REEL}).
     * @param dateOperation    Date de l'opération.
     * @return Réponse HTTP 200 contenant le détail du calcul fiscal.
     */
    @GetMapping("/simuler")
    @Operation(summary = "Simuler le calcul TVA + Retenue à la source (AIR) selon le régime fiscal et la date d'opération")
    public ResponseEntity<Map<String, Object>> simulerCalculFiscal(
            @RequestParam BigDecimal montantHt,
            @RequestParam(required = false, defaultValue = "REEL") String codeRegimeFiscal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOperation) {
        return ResponseEntity.ok(fiscaliteService.simulerCalculFiscal(montantHt, codeRegimeFiscal, dateOperation));
    }

    // --- RÉGIMES FISCAUX ---

    /**
     * Liste les régimes fiscaux DGI (Réel, Simplifié, Libératoire, Exonéré).
     *
     * @return Réponse HTTP 200 contenant les régimes fiscaux.
     */
    @GetMapping("/regimes")
    @Operation(summary = "Lister les régimes fiscaux DGI (Réel, Simplifié, Libératoire, Exonéré)")
    public ResponseEntity<List<RegimeFiscalEntity>> listerRegimes() {
        return ResponseEntity.ok(fiscaliteService.listerRegimesFiscaux());
    }

    /**
     * Crée un nouveau régime fiscal.
     *
     * @param request Données du régime fiscal.
     * @return Réponse HTTP 201 contenant le régime cré.
     */
    @PostMapping("/regimes")
    @Operation(summary = "Créer un régime fiscal")
    public ResponseEntity<RegimeFiscalEntity> creerRegime(@Valid @RequestBody RegimeFiscalEntity request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fiscaliteService.creerRegimeFiscal(request));
    }

    // --- TABLE UNIQUE DES SEUILS DE GESTION (CA-REF-020-02 & CA-REF-020-03) ---

    /**
     * Consulte la table unique transversale des seuils de gestion (Budget, Achats, Trésorerie/Caisses).
     *
     * @param domaine Filtre optionnel par domaine fonctionnel.
     * @return Réponse HTTP 200 contenant la liste des seuils.
     */
    @GetMapping("/seuils")
    @Operation(summary = "Consulter la table unique transversale des seuils de gestion (Budget, Achats, Trésorerie/Caisses)")
    public ResponseEntity<List<SeuilGestionEntity>> listerSeuils(@RequestParam(required = false) String domaine) {
        return ResponseEntity.ok(fiscaliteService.listerSeuilsActifs(domaine));
    }

    /**
     * Résout un seuil de gestion par son code et sa date d'effet.
     *
     * @param code          Code du seuil.
     * @param dateOperation Date de référence.
     * @return Réponse HTTP 200 contenant le seuil applicable.
     */
    @GetMapping("/seuils/{code}/applicable")
    @Operation(summary = "Résoudre un seuil de gestion par son code et sa date d'effet (CA-REF-020-03)")
    public ResponseEntity<SeuilGestionEntity> resoudreSeuilParCodeEtDate(
            @PathVariable String code,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOperation) {
        return ResponseEntity.ok(fiscaliteService.resoudreSeuilParCodeEtDate(code, dateOperation));
    }

    /**
     * Détermine la procédure et l'autorité de validation applicables pour un domaine, un montant et une date.
     *
     * @param domaine       Domaine fonctionnel.
     * @param montantFcfa   Montant en FCFA.
     * @param dateOperation Date de l'opération.
     * @return Réponse HTTP 200 contenant le seuil applicable.
     */
    @GetMapping("/seuils/resoudre-montant")
    @Operation(summary = "Déterminer la procédure et l'autorité de validation applicables pour un domaine, un montant et une date (CA-REF-020-02 & 03)")
    public ResponseEntity<SeuilGestionEntity> resoudreSeuilPourDomaineEtMontant(
            @RequestParam String domaine,
            @RequestParam BigDecimal montantFcfa,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOperation) {
        return ResponseEntity.ok(fiscaliteService.resoudreSeuilPourDomaineEtMontant(domaine, montantFcfa, dateOperation));
    }

    /**
     * Consulte l'historique complet des révisions d'un seuil de gestion.
     *
     * @param code Code du seuil.
     * @return Réponse HTTP 200 contenant l'historique des versions du seuil.
     */
    @GetMapping("/seuils/{code}/historique")
    @Operation(summary = "Consulter l'historique des révisions d'un seuil de gestion")
    public ResponseEntity<List<SeuilGestionEntity>> consulterHistoriqueSeuil(@PathVariable String code) {
        return ResponseEntity.ok(fiscaliteService.consulterHistoriqueSeuil(code));
    }

    /**
     * Crée ou révise un seuil de gestion avec date d'effet sans écrasement rétroactif.
     *
     * @param request Données du seuil de gestion.
     * @return Réponse HTTP 201 contenant le seuil enregistré.
     */
    @PostMapping("/seuils")
    @Operation(summary = "Créer ou réviser un seuil de gestion avec date d'effet sans écrasement rétroactif (CA-REF-020-03)")
    public ResponseEntity<SeuilGestionEntity> enregistréerSeuil(@Valid @RequestBody SeuilGestionEntity request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fiscaliteService.enregistréerOuRevisionnerSeuil(request));
    }
}
