package com.sodepa.erp.budget.infrastructure.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité représentant un engagement de dépenses (bon d'engagement / réservation budgétaire)
 * conforme au cahier des charges officiel SODEPA (EP-BUD-03, F-BUD-010 à F-BUD-016).
 */
@Entity
@Table(name = "budget_engagement")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetEngagementEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "BE";

    public String getPrefixeCode() {
        return this.codePrefix;
    }

    public void setPrefixeCode(String prefixeCode) {
        this.codePrefix = prefixeCode;
    }

    /**
     * Identifiant unique de l'engagement.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Ligne budgétaire sur laquelle s'impute l'engagement.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_item_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private BudgetItemEntity budgetItem;

    /**
     * Numéro d'engagement unique (format normé ex: BE-2026-RNCH-FARO-0001).
     */
    @Column(name = "numero_engagement", nullable = false, unique = true, length = 100)
    private String numeroEngagement;

    /**
     * Description ou motif de la dépense engagée.
     */
    @Column(length = 255)
    private String description;

    /**
     * Montant de la dépense engagée en FCFA TTC.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal montant;

    /**
     * Date et heure de l'engagement budgétaire.
     */
    @Column(name = "date_engagement", nullable = false)
    private LocalDateTime dateEngagement;

    /**
     * Circuit budgétaire déterminé par le seuil de 500 000 FCFA TTC (F-BUD-012) :
     * "MENUE_DEPENSE" (< 500 000 FCFA TTC) ou "GROSSE_DEPENSE" (>= 500 000 FCFA TTC).
     */
    @Column(length = 50)
    private String circuit;

    /**
     * Circuit spécifique le cas échéant (F-BUD-015) :
     * "STANDARD", "MISSION", "ENTRETIEN_VEHICULE", "ETUDES_CONSULTANT".
     */
    @Column(name = "type_depense_specifique", length = 50)
    @Builder.Default
    private String typeDepenseSpecifique = "STANDARD";

    /**
     * Visa budgétaire du Chef Service du Budget (SB) ou CSAFC en unité (F-BUD-013).
     */
    @Column(name = "visa_budgetaire", length = 50)
    private String visaBudgetaire;

    /**
     * Date et heure d'apposition du visa budgétaire.
     */
    @Column(name = "date_visa")
    private LocalDateTime dateVisa;

    /**
     * Identifiant de l'agent ayant apposé le visa (CSB / CSAFC).
     */
    @Column(name = "vise_par")
    private UUID visePar;

    /**
     * Indicateur de conformité de la liasse documentaire lors du contrôle a priori (F-BUD-018).
     */
    @Column(name = "pieces_jointes_valides")
    private Boolean piecesJointesValides;

    /**
     * Identifiant du Bon de Commande lié (obligatoire pour les grosses dépenses >= 500 000 FCFA TTC, F-BUD-014).
     */
    @Column(name = "bon_commande_id")
    private UUID bonCommandeId;

    /**
     * Unité Organisationnelle émettrice de l'engagement (cloisonnement ABAC multi-entités).
     */
    @Column(name = "unite_organisationnelle_id")
    private UUID uniteOrganisationnelleId;

    /**
     * Statut de l'engagement (ex: 'ENGAGED', 'CONVERTED_TO_REAL', 'CANCELLED', 'LIQUIDATED', 'PAID').
     */
    @Column(nullable = false, length = 50)
    private String statut;

    /**
     * Statut de workflow d'approbation (ex: 'PRE_ENGAGEMENT', 'APPROVED_BY_CHEF', 'APPROVED_BY_DF', 'APPROVED_BY_DG').
     */
    @Column(name = "statut_workflow", length = 100)
    private String statutWorkflow;

    /**
     * Rôle de l'approbateur courant (ex: 'CHEF_SERVICE', 'DIRECTEUR_FINANCIER', 'DIRECTEUR_GENERAL').
     */
    @Column(name = "approbateur_courant_role", length = 100)
    private String approbateurCourantRole;

    /**
     * Motif d'annulation de l'engagement en cas de renoncement ou restitution de crédit (F-BUD-016).
     */
    @Column(name = "motif_annulation", length = 500)
    private String motifAnnulation;
}
