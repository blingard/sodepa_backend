package com.sodepa.erp.pastoral.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entité de persistance représentant `PastoralCategorieCheptelEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Entity
@Table(name = "pastoral_categories_cheptel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PastoralCategorieCheptelEntity {

    /**
     * Identifiant unique technique dans le cadre des opérations SODEPA.
     */
    @Id
    private UUID id;

    /**
     * Code de référence unique normalisé dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, unique = true, length = 40)
    private String code;

    /**
     * Libellé descriptif officiel dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, length = 150)
    private String libelle;

    /**
     * Champ ageMinMois : age min mois utilisé pour le pilotage et le suivi dans PastoralCategorieCheptelEntity.
     */
    @Column(name = "age_min_mois")
    private Integer ageMinMois;

    /**
     * Champ ageMaxMois : age max mois utilisé pour le pilotage et le suivi dans PastoralCategorieCheptelEntity.
     */
    @Column(name = "age_max_mois")
    private Integer ageMaxMois;

    /**
     * Champ compteActifSyscohada : compte actif syscohada utilisé pour le pilotage et le suivi dans PastoralCategorieCheptelEntity.
     */
    @Column(name = "compte_actif_syscohada", nullable = false, length = 30)
    private String compteActifSyscohada;

    /**
     * Champ compteVariationSyscohada : compte variation syscohada utilisé pour le pilotage et le suivi dans PastoralCategorieCheptelEntity.
     */
    @Column(name = "compte_variation_syscohada", nullable = false, length = 30)
    private String compteVariationSyscohada;

    /**
     * Champ valeurForfaitaireBareme : valeur forfaitaire bareme utilisé pour le pilotage et le suivi dans PastoralCategorieCheptelEntity.
     */
    @Column(name = "valeur_forfaitaire_bareme", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal valeurForfaitaireBareme = BigDecimal.ZERO;
}
