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

    /**
     * Identifiant unique technique dans le cadre des opérations SODEPA.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Champ typeProcedure : type procedure utilisé pour le pilotage et le suivi dans AchatSeuilConfigEntity.
     */
    @Column(name = "type_procedure", nullable = false, unique = true, length = 50)
    private String typeProcedure;

    /**
     * Libellé descriptif officiel dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, length = 150)
    private String libelle;

    /**
     * Valeur financière (montant min ttc) exprimée en Francs CFA (XAF).
     */
    @Column(name = "montant_min_ttc", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantMinTtc;

    /**
     * Valeur financière (montant max ttc) exprimée en Francs CFA (XAF).
     */
    @Column(name = "montant_max_ttc", precision = 15, scale = 2)
    private BigDecimal montantMaxTtc;

    /**
     * Champ nbProformasRequis : nb proformas requis utilisé pour le pilotage et le suivi dans AchatSeuilConfigEntity.
     */
    @Column(name = "nb_proformas_requis", nullable = false)
    @Builder.Default
    private Integer nbProformasRequis = 0;

    /**
     * Champ commissionRequise : commission requise utilisé pour le pilotage et le suivi dans AchatSeuilConfigEntity.
     */
    @Column(name = "commission_requise", nullable = false)
    @Builder.Default
    private Boolean commissionRequise = false;

    /**
     * Indicateur d'activation opérationnelle (true si actif, false si archivé/désactivé) dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean actif = true;
}
