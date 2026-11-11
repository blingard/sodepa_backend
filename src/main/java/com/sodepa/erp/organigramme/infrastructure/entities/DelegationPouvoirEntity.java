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
/**
 * Entité de persistance représentant `DelegationPouvoirEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DelegationPouvoirEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "DP";

    public String getPrefixeCode() {
        return this.codePrefix;
    }

    public void setPrefixeCode(String prefixeCode) {
        this.codePrefix = prefixeCode;
    }


    /**
     * Identifiant unique technique dans le cadre des opérations SODEPA.
     */
    @Id
    private UUID id;

    /**
     * Champ delegant : delegant utilisé pour le pilotage et le suivi dans DelegationPouvoirEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegant_id", nullable = false)
    private UtilisateurEntity delegant;

    /**
     * Champ delegataire : delegataire utilisé pour le pilotage et le suivi dans DelegationPouvoirEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegataire_id", nullable = false)
    private UtilisateurEntity delegataire;

    /**
     * Champ typeDelegation : type delegation utilisé pour le pilotage et le suivi dans DelegationPouvoirEntity.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_delegation", nullable = false, length = 30)
    private TypeDelegation typeDelegation;

    /**
     * Valeur financière (plafond montant) exprimée en Francs CFA (XAF).
     */
    @Column(name = "plafond_montant", precision = 18, scale = 2)
    private BigDecimal plafondMontant;

    /**
     * Date de début d'effet ou de validité dans le cadre des opérations SODEPA.
     */
    @Column(name = "date_debut", nullable = false)
    private Instant dateDebut;

    /**
     * Date de fin d'effet ou échéance dans le cadre des opérations SODEPA.
     */
    @Column(name = "date_fin", nullable = false)
    private Instant dateFin;

    /**
     * Statut dans le cycle de vie de gestion au sein de la SODEPA dans le cadre des opérations SODEPA.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutDelegation statut = StatutDelegation.ACTIVE;
}
