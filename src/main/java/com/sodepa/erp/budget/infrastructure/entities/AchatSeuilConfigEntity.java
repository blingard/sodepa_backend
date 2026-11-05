package com.sodepa.erp.budget.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Configuration des seuils d'acquisition et passation de commandes / marchés SODEPA
 * (Manuel des Marchés et du Patrimoine SODEPA).
 */
@Entity
@Table(name = "achat_seuils_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AchatSeuilConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "type_procedure", nullable = false, unique = true, length = 50)
    private String typeProcedure;

    @Column(nullable = false, length = 150)
    private String libelle;

    @Column(name = "montant_min_ttc", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantMinTtc;

    @Column(name = "montant_max_ttc", precision = 15, scale = 2)
    private BigDecimal montantMaxTtc;

    @Column(name = "nb_proformas_requis", nullable = false)
    @Builder.Default
    private Integer nbProformasRequis = 0;

    @Column(name = "commission_requise", nullable = false)
    @Builder.Default
    private Boolean commissionRequise = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean actif = true;
}
