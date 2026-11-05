package com.sodepa.erp.organigramme.infrastructure.entities;

import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entité matérialisant une délégation temporaire de pouvoirs managériaux / de signature.
 */
@Entity
@Table(name = "delegations_pouvoir", indexes = {
    @Index(name = "idx_del_delegant", columnList = "delegant_id"),
    @Index(name = "idx_del_delegataire", columnList = "delegataire_id"),
    @Index(name = "idx_del_statut", columnList = "statut")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DelegationPouvoirEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegant_id", nullable = false)
    private UtilisateurEntity delegant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegataire_id", nullable = false)
    private UtilisateurEntity delegataire;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_delegation", nullable = false, length = 30)
    private TypeDelegation typeDelegation;

    @Column(name = "plafond_montant", precision = 18, scale = 2)
    private BigDecimal plafondMontant;

    @Column(name = "date_debut", nullable = false)
    private Instant dateDebut;

    @Column(name = "date_fin", nullable = false)
    private Instant dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutDelegation statut = StatutDelegation.ACTIVE;
}
