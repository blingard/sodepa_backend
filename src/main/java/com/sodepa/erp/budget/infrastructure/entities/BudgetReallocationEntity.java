package com.sodepa.erp.budget.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité matérialisant un transfert ou réallocation d'enveloppe budgétaire entre deux lignes de budget.
 */
@Entity
@Table(name = "budget_reallocation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetReallocationEntity {

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
     * Montant transféré.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal montant;

    /**
     * Date et heure du transfert.
     */
    @Column(name = "date_transfert", nullable = false)
    private LocalDateTime dateTransfert;

    /**
     * Identifiant de l'utilisateur (responsable) ayant validé le transfert.
     */
    @Column(name = "valide_par")
    private UUID validePar;

    /**
     * Raison ou justification de la réallocation.
     */
    @Column(length = 255)
    private String raison;
}
