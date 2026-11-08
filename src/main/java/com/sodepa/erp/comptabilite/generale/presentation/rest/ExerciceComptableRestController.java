package com.sodepa.erp.comptabilite.generale.presentation.rest;

import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.ExerciceComptableService;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.ExerciceComptableEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.PeriodeComptableEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.SchemaEcritureComptableEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST des Exercices Comptables, Périodes Mensuelles et Schémas d'écritures (REF-04 / F-REF-017).
 */
@RestController
@RequestMapping({"/api/comptabilite/exercices", "/api/v1/comptabilite/exercices"})
@RequiredArgsConstructor
@Tag(name = "Exercices Comptables, 12 Périodes Mensuelles & Schémas d'Écritures (F-REF-017)",
     description = "Ouverture d'exercice avec génération automatique des 12 périodes mensuelles et paramétrage des schémas d'écritures SYSCOHADA")
/**
 * Contrôleur REST exposant les points de terminaison pour `ExerciceComptableRestController`.
 * Permet la gestion sécurisée des opérations de l'ERP SODEPA.
 */
public class ExerciceComptableRestController {

    private final ExerciceComptableService exerciceService;

    /**
     * Exécute l'opération de lister exercices.
     * @return Résultat du traitement (ResponseEntity<List<ExerciceComptableEntity>>).
     */
    @GetMapping
    @Operation(summary = "Lister tous les exercices comptables et leurs 12 périodes mensuelles")
    public ResponseEntity<List<ExerciceComptableEntity>> listerExercices() {
        return ResponseEntity.ok(exerciceService.listerExercices());
    }

    /**
     * Exécute l'opération de obtenir exercice.
     * @param annee Annee dans le contexte du traitement obtenirExercice.
     * @return Résultat du traitement (ResponseEntity<ExerciceComptableEntity>).
     */
    @GetMapping("/{annee}")
    @Operation(summary = "Consulter un exercice comptable et ses 12 périodes mensuelles")
    public ResponseEntity<ExerciceComptableEntity> obtenirExercice(@PathVariable int annee) {
        return ResponseEntity.ok(exerciceService.obtenirExerciceParAnnee(annee));
    }

    @PostMapping("/ouvrir")
    @Operation(summary = "Ouvrir un nouvel exercice comptable et générer ses 12 périodes mensuelles (CA-REF-017-03)")
    public ResponseEntity<ExerciceComptableEntity> ouvrirExercice(
            @RequestParam int annee,
            @RequestParam(required = false) String libelle,
            @RequestParam(required = false) String journauxAutorises) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(exerciceService.ouvrirExercice(annee, libelle, journauxAutorises));
    }

    /**
     * Exécute l'opération de lister periodes.
     * @param annee Annee dans le contexte du traitement listerPeriodes.
     * @return Résultat du traitement (ResponseEntity<List<PeriodeComptableEntity>>).
     */
    @GetMapping("/{annee}/periodes")
    @Operation(summary = "Lister les 12 périodes mensuelles d'un exercice comptable (CA-REF-017-03)")
    public ResponseEntity<List<PeriodeComptableEntity>> listerPeriodes(@PathVariable int annee) {
        return ResponseEntity.ok(exerciceService.listerPeriodesExercice(annee));
    }

    @PatchMapping("/periodes/{codePeriode}/statut")
    @Operation(summary = "Verrouiller ou cleéôturer une période mensuelle (ex: 2026-01)")
    public ResponseEntity<PeriodeComptableEntity> changerStatutPeriode(
            @PathVariable String codePeriode,
            @RequestParam String statut) {
        return ResponseEntity.ok(exerciceService.changerStatutPeriode(codePeriode, statut));
    }

    /**
     * Exécute l'opération de lister schemas ecritures.
     * @return Résultat du traitement (ResponseEntity<List<SchemaEcritureComptableEntity>>).
     */
    @GetMapping("/schemas-ecritures")
    @Operation(summary = "Lister les schémas d'écritures comptables automatiques SYSCOHADA (BF-REF-025)")
    public ResponseEntity<List<SchemaEcritureComptableEntity>> listerSchemasEcritures() {
        return ResponseEntity.ok(exerciceService.listerSchemasEcritures());
    }

    @PostMapping("/schemas-ecritures")
    @Operation(summary = "Créer un schéma d'écriture comptable automatique")
    public ResponseEntity<SchemaEcritureComptableEntity> creerSchemaEcriture(
            @Valid @RequestBody SchemaEcritureComptableEntity request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exerciceService.creerSchemaEcriture(request));
    }
}
