package com.sodepa.erp.budget.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité matérialisant un transfert ou réallocation d'enveloppe budgétaire entre deux lignes de budget,
 * conforme au cahier des charges officiel SODEPA (EP-BUD-06, F-BUD-021 à F-BUD-024, RG-BUD-014 à RG-BUD-020).
 */
@Entity
@Table(name = "budget_reallocation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetReallocationEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "BR";

    public String getPrefixeCode() {
        return this.codePrefix;
    }

    public void setPrefixeCode(String prefixeCode) {
        this.codePrefix = prefixeCode;
    }

    /**
     * Identifiant unique du transfert.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Ligne budgétaire source (qui cède le budget).
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "source_item_id", nullable = false)
    private BudgetItemEntity sourceItem;

    /**
     * Ligne budgétaire de destination (qui reçoit le budget).
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dest_item_id", nullable = false)
    private BudgetItemEntity destItem;

    /**
     * Montant transféré en FCFA.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal montant;

    /**
     * Statut du cycle de vie de la réallocation (Section 9 du CdC).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private StatutReallocation statut = StatutReallocation.EXECUTEE;

    /**
     * Type de réallocation (SIMPLE si <= 5% même UO/nature, SIGNIFICATIVE si > 5% ou investissement).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_reallocation", length = 50)
    @Builder.Default
    private TypeReallocation typeReallocation = TypeReallocation.SIMPLE;

    /**
     * Taux de réallocation calculé par rapport à la dotation initiale de la ligne cédante (RG-BUD-018).
     */
    @Column(name = "taux_reallocation", precision = 7, scale = 4)
    private BigDecimal tauxReallocation;

    /**
     * Indicateur de mesure conservatoire d'extrême urgence autorisée par le DG (RG-BUD-017 / CA-BUD-023-03).
     */
    @Column(name = "mesure_conservatoire", nullable = false)
    @Builder.Default
    private boolean mesureConservatoire = false;

    /**
     * Justification circonstanciée de l'extrême urgence sanitaire/vétérinaire/exploitation.
     */
    @Column(name = "justification_urgence", length = 500)
    private String justificationUrgence;

    /**
     * Référence de la délibération ou résolution du Conseil d'Administration régularisant l'opération.
     */
    @Column(name = "numero_resolution_ca", length = 100)
    private String numeroResolutionCa;

    /**
     * Date de la session d'approbation ou de régularisation par le CA.
     */
    @Column(name = "date_validation_ca")
    private LocalDateTime dateValidationCa;

    /**
     * Date et heure du transfert ou de son enregistrement.
     */
    @Column(name = "date_transfert", nullable = false)
    private LocalDateTime dateTransfert;

    /**
     * Identifiant de l'utilisateur (responsable / approbateur) ayant validé le transfert.
     */
    @Column(name = "valide_par")
    private UUID validePar;

    /**
     * Raison ou justification de la réallocation.
     */
    @Column(length = 255)
    private String raison;
}
