package com.sodepa.erp.budget.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entité représentant une ligne de crédit ou facilité de découvert bancaire.
 */
@Entity
@Table(name = "ligne_decouvert")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LigneDecouvertEntity {

    /**
     * Identifiant unique de la ligne de découvert.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Identifiant unique de la banque partenaire octroyant la ligne.
     */
    @Column(name = "banque_id", nullable = false)
    private UUID banqueId;

    /**
     * Libellé ou intitulé de l'autorisation de découvert.
     */
    @Column(nullable = false, length = 255)
    private String intitule;

    /**
     * Montant maximum autorisé (plafond).
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal plafond;

    /**
     * Taux d'intérêt annuel appliqué sur le découvert utilisé (ex: 8.50 pour 8.5%).
     */
    @Column(name = "taux_interet", nullable = false, precision = 5, scale = 2)
    private BigDecimal tauxInteret;

    /**
     * Solde actuellement débiteur (utilisé).
     */
    @Column(name = "solde_utilise", nullable = false, precision = 19, scale = 4)
    private BigDecimal soldeUtilise;

    /**
     * Date de début de validité de la ligne.
     */
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    /**
     * Date de fin de validité de la ligne.
     */
    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    /**
     * Pourcentage d'utilisation à partir duquel une alerte de liquidité est déclenchée (ex: 80.00).
     */
    @Column(name = "alerte_seuil_pourcent", nullable = false, precision = 5, scale = 2)
    private BigDecimal alerteSeuilPourcent;
}
