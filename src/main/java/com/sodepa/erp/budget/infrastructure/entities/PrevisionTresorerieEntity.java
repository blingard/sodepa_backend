package com.sodepa.erp.budget.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entité représentant une prévision d'encaissement ou de décaissement de trésorerie.
 * Alimente le plan de trésorerie (cash-flow prévisionnel).
 */
@Entity
@Table(name = "prevision_tresorerie")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrevisionTresorerieEntity {

    /**
     * Identifiant unique de la prévision.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Date d'échéance prévue pour le mouvement de fonds.
     */
    @Column(name = "date_echeance", nullable = false)
    private LocalDate dateEcheance;

    /**
     * Type de mouvement : 'ENCAISSEMENT' ou 'DECAISSEMENT'.
     */
    @Column(nullable = false, length = 50)
    private String type;

    /**
     * Origine de la prévision (ex: 'CLIENT', 'FOURNISSEUR', 'SALAIRE', 'FINANCEMENT', 'IMPOT', 'AUTRE').
     */
    @Column(nullable = false, length = 50)
    private String source;

    /**
     * Libellé ou description explicative.
     */
    @Column(nullable = false, length = 255)
    private String libelle;

    /**
     * Montant prévisionnel en devise locale.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal montant;
}
