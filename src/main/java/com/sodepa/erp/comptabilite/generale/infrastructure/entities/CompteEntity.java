package com.sodepa.erp.comptabilite.generale.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Représente un compte du plan comptable général ou analytique (SYSCOHADA).
 * Cette entité stocke les comptes de différents niveaux (Classe, Rubrique, Sous-compte, Auxiliaire).
 */
@Entity
@Table(name = "comptes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompteEntity {

    /**
     * Identifiant unique du compte (UUID).
     */
    @Id
    @EqualsAndHashCode.Include
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * Code unique du compte (ex: '10', '101', '41110001').
     */
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    /**
     * Libellé ou intitulé du compte.
     */
    @Column(nullable = false, length = 255)
    private String intitule;

    /**
     * Code du compte parent de niveau supérieur.
     */
    @Column(name = "parent_code", length = 20)
    private String parentCode;

    /**
     * Niveau hiérarchique du compte (1 = Classe, 2 = Rubrique, 3 = Sous-compte, >=4 = Auxiliaire).
     */
    @Column(nullable = false)
    private Integer niveau;

    /**
     * Type analytique du compte (optionnel, utilisé pour catégoriser les comptes analytiques).
     */
    @Column(name = "type_analytique", length = 50)
    private String typeAnalytique;

    /**
     * Nature comptable du compte (ex: CHARGE, PRODUIT, ACTIF, PASSIF).
     */
    @Column(length = 50)
    private String nature;

    /**
     * Indique si le compte est un compte auxiliaire (généralement créé dynamiquement).
     */
    @Column(name = "is_auxiliaire", nullable = false)
    @Builder.Default
    private Boolean isAuxiliaire = false;

    /**
     * Date et heure de création de l'enregistrement.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Date et heure de la dernière mise à jour de l'enregistrement.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
