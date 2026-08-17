package com.sodepa.erp.budget.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entité représentant une proposition de ligne budgétaire faite par un département ou service (workflow Bottom-Up).
 */
@Entity
@Table(name = "budget_demande")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetDemandeEntity {

    /**
     * Identifiant unique de la demande budgétaire.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Identifiant du département ou service émetteur de la demande.
     */
    @Column(name = "departement_id", nullable = false)
    private UUID departementId;

    /**
     * Exercice comptable concerné par la demande (ex: 2026).
     */
    @Column(nullable = false)
    private Integer annee;

    /**
     * Code du compte comptable SYSCOHADA (classe 6 ou 7).
     */
    @Column(name = "compte_code", nullable = false, length = 20)
    private String compteCode;

    /**
     * Identifiant de la section analytique associée (axe analytique facultatif).
     */
    @Column(name = "section_id")
    private UUID sectionId;

    /**
     * Montant initialement demandé par le département.
     */
    @Column(name = "montant_demande", nullable = false, precision = 19, scale = 4)
    private BigDecimal montantDemande;

    /**
     * Statut de la demande dans le workflow collaboratif (ex: 'DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED').
     */
    @Column(nullable = false, length = 50)
    private String statut;

    /**
     * Commentaires ou justifications du demandeur ou de la direction financière.
     */
    @Column(length = 1000)
    private String commentaires;
}
