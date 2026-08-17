package com.sodepa.erp.comptabilite.analytique.presentation.rest;

import com.sodepa.erp.comptabilite.analytique.application.usecase.BudgetUseCase;
import com.sodepa.erp.comptabilite.analytique.infrastructure.entities.BudgetEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Contrôleur REST exposant les API d'imputation et de suivi budgétaire analytique.
 */
@RestController
@RequestMapping("/api/comptabilite/analytique/budgets")
@RequiredArgsConstructor
public class BudgetRestController {

    /**
     * Cas d'usage pour la gestion des budgets.
     */
    private final BudgetUseCase budgetUseCase;

    /**
     * Définit ou met à jour une ligne budgétaire pour un exercice, une section et un compte général.
     * 
     * @param request le DTO contenant les informations budgétaires
     * @return la réponse HTTP avec la ligne enregistrée ou mise à jour, ou un message d'erreur
     */
    @PostMapping
    public ResponseEntity<?> definirBudget(@RequestBody BudgetUseCase.BudgetRequest request) {
        try {
            BudgetEntity budget = budgetUseCase.definirBudget(request);
            return ResponseEntity.ok(budget);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Récupère toutes les enveloppes budgétaires d'une année.
     * 
     * @param annee l'exercice comptable concerné
     * @return la liste des budgets de l'année
     */
    @GetMapping("/{annee}")
    public ResponseEntity<List<BudgetEntity>> listerBudgetsParAnnee(@PathVariable int annee) {
        return ResponseEntity.ok(budgetUseCase.listerBudgetsParAnnee(annee));
    }

    /**
     * Récupère les enveloppes budgétaires d'une section analytique spécifique pour une année.
     * 
     * @param annee l'exercice budgétaire concerné
     * @param sectionId l'identifiant unique de la section analytique
     * @return la liste des budgets de la section
     */
    @GetMapping("/{annee}/sections/{sectionId}")
    public ResponseEntity<List<BudgetEntity>> listerBudgetsParSection(
            @PathVariable int annee,
            @PathVariable UUID sectionId) {
        return ResponseEntity.ok(budgetUseCase.listerBudgetsParSection(annee, sectionId));
    }
}
