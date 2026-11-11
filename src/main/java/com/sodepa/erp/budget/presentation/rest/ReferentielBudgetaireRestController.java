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
 * Service de gestion de la Nomenclature et du Plan de Codification Budgetaire de la SODEPA (REF-04 / Module 02 - BUD).
 * <p>
 * Ce service administre les référentiels structurants de la politique de budget-programme :
 * <ul>
 *   <li><b>Programmes budgétaires</b> : Grands axes stratégiques pluriannuels de la SODEPA (ex: Développement de l'élevage bovin, Modernisation des abattoirs).</li>
 *   <li><b>Activités budgétaires</b> : Actions concrètes concourant à la réalisation des objectifs d'un programme.</li>
 *   <li><b>Sources de financement</b> : Origine des crédits (Fonds propres d'exploitation SODEPA, Subvention État MINEPIA/BIP, Partenaires au développement).</li>
 *   <li><b>Natures économiques</b> : Typologie économique des opérations (Personnel, Fonctionnement, Investissement lourd, Prestations).</li>
 * </ul>
 */
@RestController
@RequestMapping({"/api/budget/referentiels", "/api/v1/budget/referentiels"})
@RequiredArgsConstructor
@Tag(name = "Nomenclature Budgétaire (Programmes, Activités, Financements, Natures)",
     description = "Gouvernance des référentiels structurants de la nomenclature budgétaire SODEPA (F-REF-018)")
public class ReferentielBudgetaireRestController {

    private final BudgetProgrammeRepository programmeRepository;
    private final BudgetActiviteRepository activiteRepository;
    private final BudgetSourceFinancementRepository sourceRepository;
    private final BudgetNatureDepenseRepository natureRepository;

    /**
     * Liste l'ensemble des programmes budgétaires officiels actuellement actifs.
     *
     * @return la liste ordonnée des programmes stratégiques de la SODEPA
     */
    @GetMapping("/programmes")
    @Operation(summary = "Lister les programmes budgétaires actifs")
    public ResponseEntity<List<BudgetProgrammeEntity>> listerProgrammes() {
        return ResponseEntity.ok(programmeRepository.findByActifTrueOrderByCodeAsc());
    }

    /**
     * Crée et enregistre un nouveau programme au sein de la nomenclature budgétaire.
     *
     * @param request informations du programme (code normalisé, libellé, objectifs de performance)
     * @return le programme créé avec statut HTTP 201 (CREATED)
     */
    @PostMapping("/programmes")
    @Operation(summary = "Créer un nouveau programme budgétaire")
    public ResponseEntity<BudgetProgrammeEntity> creerProgramme(@Valid @RequestBody BudgetProgrammeEntity request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(programmeRepository.save(request));
    }

    /**
     * Liste les activités budgétaires opérationnelles, avec filtrage optionnel par programme.
     *
     * @param programmeCode code optionnel du programme pour n'afficher que ses activités rattachées
     * @return la liste des activités budgétaires correspondantes
     */
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
     * Crée une nouvelle activité budgétaire en s'assurant de son rattachement obligatoire à un programme existant.
     *
     * @param request définition de l'activité (code, libellé, code du programme parent)
     * @return l'activité budgétaire créée
     * @throws IllegalArgumentException si le code de programme spécifié n'existe pas dans le référentiel
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
     * Liste l'ensemble des sources de financement reconnues (fonds propres, subventions ministérielles, bailleurs).
     *
     * @return la liste des sources de financement actives
     */
    @GetMapping("/sources-financement")
    @Operation(summary = "Lister les sources de financement autorisées")
    public ResponseEntity<List<BudgetSourceFinancementEntity>> listerSourcesFinancement() {
        return ResponseEntity.ok(sourceRepository.findByActifTrueOrderByCodeAsc());
    }

    /**
     * Enregistre une nouvelle source de financement au plan de codification.
     *
     * @param request caractéristiques de la source (code, libellé, type de bailleur)
     * @return la source de financement créée
     */
    @PostMapping("/sources-financement")
    @Operation(summary = "Créer une source de financement budgétaire")
    public ResponseEntity<BudgetSourceFinancementEntity> creerSourceFinancement(
            @Valid @RequestBody BudgetSourceFinancementEntity request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sourceRepository.save(request));
    }

    /**
     * Liste les natures économiques de dépenses et catégories de recettes budgétaires.
     *
     * @return la liste ordonnée des natures économiques
     */
    @GetMapping("/natures-depenses")
    @Operation(summary = "Lister les natures économiques de dépenses et recettes")
    public ResponseEntity<List<BudgetNatureDepenseEntity>> listerNaturesDepenses() {
        return ResponseEntity.ok(natureRepository.findByActifTrueOrderByCodeAsc());
    }

    /**
     * Crée une nouvelle nature économique de dépense.
     *
     * @param request caractéristiques de la nature de dépense
     * @return la nature de dépense créée
     */
    @PostMapping("/natures-depenses")
    @Operation(summary = "Créer une nature de dépense budgétaire")
    public ResponseEntity<BudgetNatureDepenseEntity> creerNatureDepense(
            @Valid @RequestBody BudgetNatureDepenseEntity request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(natureRepository.save(request));
    }
}
