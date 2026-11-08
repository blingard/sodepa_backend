package com.sodepa.erp.budget.presentation.rest;

import com.sodepa.erp.budget.infrastructure.entities.BudgetActiviteEntity;
import com.sodepa.erp.budget.infrastructure.entities.BudgetNatureDepenseEntity;
import com.sodepa.erp.budget.infrastructure.entities.BudgetProgrammeEntity;
import com.sodepa.erp.budget.infrastructure.entities.BudgetSourceFinancementEntity;
import com.sodepa.erp.budget.infrastructure.repo.BudgetActiviteRepository;
import com.sodepa.erp.budget.infrastructure.repo.BudgetNatureDepenseRepository;
import com.sodepa.erp.budget.infrastructure.repo.BudgetProgrammeRepository;
import com.sodepa.erp.budget.infrastructure.repo.BudgetSourceFinancementRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST de la Nomencleéature Budgétaire SODEPA (REF-04 / F-REF-018 / CA-REF-018-01 & 02) :
 * Programmes, Activités, Sources de financement et Natures de dépenses.
 */
@RestController
@RequestMapping({"/api/budget/referentiels", "/api/v1/budget/referentiels"})
@RequiredArgsConstructor
@Tag(name = "Nomencleéature Budgétaire (Programmes, Activités, Financements, Natures)",
     description = "Gouvernance des référentiels budgétaires SODEPA (F-REF-018)")
/**
 * Contrôleur REST exposant les points de terminaison pour `ReferentielBudgetaireRestController`.
 * Permet la gestion sécurisée des opérations de l'ERP SODEPA.
 */
public class ReferentielBudgetaireRestController {

    private final BudgetProgrammeRepository programmeRepository;
    private final BudgetActiviteRepository activiteRepository;
    private final BudgetSourceFinancementRepository sourceRepository;
    private final BudgetNatureDepenseRepository natureRepository;

    /**
     * Exécute l'opération de lister programmes.
     * @return Résultat du traitement (ResponseEntity<List<BudgetProgrammeEntity>>).
     */
    @GetMapping("/programmes")
    @Operation(summary = "Lister les programmes budgétaires actifs (CA-REF-018-01)")
    public ResponseEntity<List<BudgetProgrammeEntity>> listerProgrammes() {
        return ResponseEntity.ok(programmeRepository.findByActifTrueOrderByCodeAsc());
    }

    /**
     * Exécute l'opération de creer programme.
     * @param request Request dans le contexte du traitement creerProgramme.
     * @return Résultat du traitement (ResponseEntity<BudgetProgrammeEntity>).
     */
    @PostMapping("/programmes")
    @Operation(summary = "Créer un programme budgétaire")
    public ResponseEntity<BudgetProgrammeEntity> creerProgramme(@Valid @RequestBody BudgetProgrammeEntity request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(programmeRepository.save(request));
    }

    @GetMapping("/activites")
    @Operation(summary = "Lister les activités budgétaires actives (filtrables par programme)")
    public ResponseEntity<List<BudgetActiviteEntity>> listerActivites(
            @RequestParam(required = false) String programmeCode) {
        if (programmeCode != null && !programmeCode.isBlank()) {
            return ResponseEntity.ok(activiteRepository.findByProgrammeCodeIgnoreCaseAndActifTrue(programmeCode));
        }
        return ResponseEntity.ok(activiteRepository.findByActifTrueOrderByCodeAsc());
    }

    /**
     * Exécute l'opération de creer activite.
     * @param request Request dans le contexte du traitement creerActivite.
     * @return Résultat du traitement (ResponseEntity<BudgetActiviteEntity>).
     */
    @PostMapping("/activites")
    @Operation(summary = "Créer une activité budgétaire rattachée à un programme")
    public ResponseEntity<BudgetActiviteEntity> creerActivite(@Valid @RequestBody BudgetActiviteEntity request) {
        if (programmeRepository.findByCodeIgnoreCase(request.getProgrammeCode()).isEmpty()) {
            throw new IllegalArgumentException("Programme budgétaire introuvable : " + request.getProgrammeCode());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(activiteRepository.save(request));
    }

    /**
     * Exécute l'opération de lister sources financement.
     * @return Résultat du traitement (ResponseEntity<List<BudgetSourceFinancementEntity>>).
     */
    @GetMapping("/sources-financement")
    @Operation(summary = "Lister les sources de financement (Fonds propres, Subvention MINEPIA/BIP, Bailleurs)")
    public ResponseEntity<List<BudgetSourceFinancementEntity>> listerSourcesFinancement() {
        return ResponseEntity.ok(sourceRepository.findByActifTrueOrderByCodeAsc());
    }

    @PostMapping("/sources-financement")
    @Operation(summary = "Créer une source de financement budgétaire")
    public ResponseEntity<BudgetSourceFinancementEntity> creerSourceFinancement(
            @Valid @RequestBody BudgetSourceFinancementEntity request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sourceRepository.save(request));
    }

    /**
     * Exécute l'opération de lister natures depenses.
     * @return Résultat du traitement (ResponseEntity<List<BudgetNatureDepenseEntity>>).
     */
    @GetMapping("/natures-depenses")
    @Operation(summary = "Lister les natures économiques de dépenses et catégories de recettes (CA-REF-018-02)")
    public ResponseEntity<List<BudgetNatureDepenseEntity>> listerNaturesDepenses() {
        return ResponseEntity.ok(natureRepository.findByActifTrueOrderByCodeAsc());
    }

    @PostMapping("/natures-depenses")
    @Operation(summary = "Créer une nature de dépense budgétaire")
    public ResponseEntity<BudgetNatureDepenseEntity> creerNatureDepense(
            @Valid @RequestBody BudgetNatureDepenseEntity request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(natureRepository.save(request));
    }
}
