package com.sodepa.erp.budget.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entité représentant un engagement hors-bilan contracté par l'entreprise (cautionnements, garanties, leasings).
 * Requis pour la conformité avec l'article 34-1 de l'acte uniforme OHADA.
 */
@Entity
@Table(name = "engagement_hors_bilan")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EngagementHorsBilanEntity {

    /**
     * Identifiant unique de l'engagement hors-bilan.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Type d'engagement : 'LEASING', 'GARANTIE_BANCAIRE', 'CAUTIONNEMENT', 'AUTRE'.
     */
    @Column(nullable = false, length = 50)
    private String type;

    /**
     * Intitulé ou libellé du contrat d'engagement.
     */
    @Column(nullable = false, length = 255)
    private String intitule;

    /**
     * Tiers (client, fournisseur, bailleur) bénéficiaire ou émetteur.
     */
    @Column(name = "tiers_id", nullable = false)
    private UUID tiersId;

    /**
     * Montant financier couvert par l'engagement.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal montant;

    /**
     * Date de début de prise d'effet.
     */
    @Column(name = "date_effet", nullable = false)
    private LocalDate dateEffet;

    /**
     * Date de fin ou d'expiration de l'engagement.
     */
    @Column(name = "date_echeance", nullable = false)
    private LocalDate dateEcheance;

    /**
     * Statut de l'engagement : 'ACTIF' ou 'EXPIRE'.
     */
    @Column(nullable = false, length = 50)
    private String statut;
}
