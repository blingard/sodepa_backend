package com.sodepa.erp.budget.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Nature économique de dépense ou catégorie de recette budgétaire (F-REF-018 / CA-REF-018-01 & 02).
 * Assure la correspondance contrôlée avec les cleéasses du Plan Comptable SYSCOHADA révisé.
 */
@Entity
@Table(name = "budget_natures_depenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetNatureDepenseEntity {

    /**
     * Identifiant unique.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Code mnémonique unique normalisé.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /**
     * Intitule dans le contexte du traitement BudgetNatureDepenseEntity.
     */
    @Column(nullable = false, length = 180)
    private String intitule;

    /**
     * Catégorie budgétaire : FONCTIONNEMENT, INVESTISSEMENT, RECETTE_EXPLOITATION, FINANCEMENT.
     */
    @Column(nullable = false, length = 40)
    private String categorie;

    /**
     * Préfixe de cleéasse/compte SYSCOHADA autorisé (ex: "60", "61", "62", "66", "21", "23", "24").
     */
    @Column(name = "prefixe_compte_syscohada", nullable = false, length = 20)
    private String prefixeCompteSyscohada;

    /**
     * Indicateur booléen d'activation de l'élément (évite la suppression physique).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
