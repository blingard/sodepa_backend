package com.sodepa.erp.budget.infrastructure.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entité représentant une échéance de remboursement au sein du plan d'amortissement d'un financement.
 */
@Entity
@Table(name = "echeance_financement")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcheanceFinancementEntity {

    /**
     * Identifiant unique de l'échéance.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Financement parent auquel est rattachée cette échéance.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ligne_financement_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private LigneFinancementEntity ligneFinancement;

    /**
     * Date de paiement prévue pour l'échéance.
     */
    @Column(name = "date_echeance", nullable = false)
    private LocalDate dateEcheance;

    /**
     * Part du capital remboursé (principal).
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal principal;

    /**
     * Part des intérêts payés.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal interets;

    /**
     * Capital restant dû après ce paiement.
     */
    @Column(name = "solde_restant_du", nullable = false, precision = 19, scale = 4)
    private BigDecimal soldeRestantDu;

    /**
     * Statut de l'échéance : 'A_PAYER' ou 'PAYE'.
     */
    @Column(nullable = false, length = 50)
    private String statut;
}
