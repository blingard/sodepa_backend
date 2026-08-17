package com.sodepa.erp.comptabilite.analytique.application.usecase;

import com.sodepa.erp.comptabilite.analytique.infrastructure.entities.BudgetEntity;
import com.sodepa.erp.comptabilite.analytique.infrastructure.entities.SectionAnalytiqueEntity;
import com.sodepa.erp.comptabilite.analytique.infrastructure.repo.BudgetRepository;
import com.sodepa.erp.comptabilite.analytique.infrastructure.repo.SectionAnalytiqueRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service applicatif (Use Case) gérant les budgets prévisionnels analytiques.
 * Permet d'imputer des enveloppes budgétaires sur les sections analytiques pour le contrôle de gestion.
 */
@Service
@RequiredArgsConstructor
public class BudgetUseCase {

    /**
     * Dépôt de données pour les budgets prévisionnels.
     */
    private final BudgetRepository budgetRepository;

    /**
     * Dépôt de données pour les sections analytiques.
     */
    private final SectionAnalytiqueRepository sectionAnalytiqueRepository;

    /**
     * DTO représentant une requête de définition ou de mise à jour d'un budget.
     */
    @Data
    public static class BudgetRequest {
        /**
         * Exercice comptable concerné (ex: 2026).
         */
        private int annee;
        /**
         * Identifiant unique de la section analytique ciblée.
         */
        private UUID sectionId;
        /**
         * Code du compte général imputé (ex: '605200').
         */
        private String compteCode;
        /**
         * Montant prévisionnel alloué.
         */
        private BigDecimal montantBudget;
    }

    /**
     * Définit ou met à jour une enveloppe budgétaire (comportement Upsert)
     * pour un exercice, une section et un compte général donnés.
     * 
     * @param request le DTO contenant les paramètres budgétaires
     * @return la ligne de budget enregistrée ou mise à jour
     * @throws IllegalArgumentException si la section analytique n'existe pas ou est inactive
     */
    @Transactional
    public BudgetEntity definirBudget(BudgetRequest request) {
        SectionAnalytiqueEntity section = sectionAnalytiqueRepository.findById(request.getSectionId())
                .orElseThrow(() -> new IllegalArgumentException("Section analytique introuvable avec l'ID: " + request.getSectionId()));

        if (!section.getActif() || !section.getAxe().getActif()) {
            throw new IllegalArgumentException("La section analytique ou l'axe associé est inactif.");
        }

        Optional<BudgetEntity> budgetOpt = budgetRepository.findByAnneeAndSectionIdAndCompteCode(
                request.getAnnee(), request.getSectionId(), request.getCompteCode());

        BudgetEntity budget;
        if (budgetOpt.isPresent()) {
            budget = budgetOpt.get();
            budget.setMontantBudget(request.getMontantBudget());
        } else {
            budget = BudgetEntity.builder()
                    .annee(request.getAnnee())
                    .section(section)
                    .compteCode(request.getCompteCode())
                    .montantBudget(request.getMontantBudget())
                    .build();
        }

        return budgetRepository.save(budget);
    }

    /**
     * Récupère l'intégralité des enveloppes budgétaires définies pour un exercice donné.
     * 
     * @param annee l'exercice budgétaire concerné
     * @return la liste des budgets de l'exercice
     */
    @Transactional(readOnly = true)
    public List<BudgetEntity> listerBudgetsParAnnee(int annee) {
        return budgetRepository.findByAnnee(annee);
    }

    /**
     * Récupère les enveloppes budgétaires d'une section spécifique pour un exercice donné.
     * 
     * @param annee l'exercice budgétaire
     * @param sectionId l'identifiant de la section
     * @return la liste des budgets de la section
     */
    @Transactional(readOnly = true)
    public List<BudgetEntity> listerBudgetsParSection(int annee, UUID sectionId) {
        return budgetRepository.findByAnneeAndSectionId(annee, sectionId);
    }
}
