package com.sodepa.erp.budget.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entité représentant un contrat de couverture de change à terme (Hedging).
 * Utilisé pour limiter les risques de fluctuation des taux de change sur les flux financiers.
 */
@Entity
@Table(name = "contrat_couverture")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContratCouvertureEntity {

    /**
     * Identifiant unique du contrat de couverture.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Référence unique du contrat fourni par la banque (ex: 'HEDGE-2026-003').
     */
    @Column(nullable = false, length = 100)
    private String reference;

    /**
     * Devise étrangère ciblée par la couverture (ex: 'USD', 'EUR').
     */
    @Column(name = "devise_cible", nullable = false, length = 10)
    private String deviseCible;

    /**
     * Montant couvert en devise étrangère.
     */
    @Column(name = "montant_devise", nullable = false, precision = 19, scale = 4)
    private BigDecimal montantDevise;

    /**
     * Cours de change garanti négocié avec la banque (par rapport à la devise locale XOF).
     */
    @Column(name = "cours_garanti", nullable = false, precision = 19, scale = 4)
    private BigDecimal coursGaranti;

    /**
     * Date de début de validité du contrat de couverture.
     */
    @Column(name = "date_effet", nullable = false)
    private LocalDate dateEffet;

    /**
     * Date d'échéance finale du contrat de couverture de change.
     */
    @Column(name = "date_echeance", nullable = false)
    private LocalDate dateEcheance;

    /**
     * Statut de la couverture (ex: 'ACTIF', 'REALISE', 'DENONCE').
     */
    @Column(nullable = false, length = 50)
    private String statut;
}
