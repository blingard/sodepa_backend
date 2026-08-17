package com.sodepa.erp.budget.infrastructure.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entité représentant une ligne de relevé bancaire (mouvement de fonds constaté).
 */
@Entity(name = "BudgetLigneReleveBancaireEntity")
@Table(name = "ligne_releve_bancaire")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LigneReleveBancaireEntity {

    /**
     * Identifiant unique de la ligne de relevé.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Le relevé bancaire auquel est rattaché cette ligne de transaction.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "releve_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private ReleveBancaireEntity releveBancaire;

    /**
     * Date de valeur de la transaction.
     */
    @Column(name = "date_valeur", nullable = false)
    private LocalDate dateValeur;

    /**
     * Libellé ou motif de la transaction figurant sur le relevé.
     */
    @Column(nullable = false, length = 500)
    private String libelle;

    /**
     * Montant de l'opération (valeur positive pour un crédit, négative pour un débit).
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal montant;

    /**
     * Statut du rapprochement bancaire de cette ligne (ex: 'NON_RAPPROCHE', 'RAPPROCHE').
     */
    @Column(name = "statut_rapprochement", nullable = false, length = 50)
    private String statutRapprochement;

    /**
     * Identifiant de la pièce d'écriture comptable correspondante si rapproché (optionnel).
     */
    @Column(name = "ecriture_id")
    private UUID ecritureId;
}
