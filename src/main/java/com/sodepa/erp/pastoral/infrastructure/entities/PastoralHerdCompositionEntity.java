package com.sodepa.erp.pastoral.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entité de persistance représentant `PastoralHerdCompositionEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Entity
@Table(name = "pastoral_herd_compositions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PastoralHerdCompositionEntity {

    /**
     * Identifiant unique technique dans le cadre des opérations SODEPA.
     */
    @Id
    private UUID id;

    /**
     * Champ anneeExercice : annee exercice utilisé pour le pilotage et le suivi dans PastoralHerdCompositionEntity.
     */
    @Column(name = "annee_exercice", nullable = false)
    private Integer anneeExercice;

    /**
     * Ranch d'élevage ou station pastorale de la SODEPA dans le cadre des opérations SODEPA.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ranch_id", nullable = false)
    private PastoralRanchEntity ranch;

    /**
     * Champ dateInventaire : date inventaire utilisé pour le pilotage et le suivi dans PastoralHerdCompositionEntity.
     */
    @Column(name = "date_inventaire", nullable = false)
    private LocalDate dateInventaire;

    /**
     * Champ effectifPhysiqueTotal : effectif physique total utilisé pour le pilotage et le suivi dans PastoralHerdCompositionEntity.
     */
    @Column(name = "effectif_physique_total", nullable = false)
    private Integer effectifPhysiqueTotal;

    /**
     * Champ effectifVeaux : effectif veaux utilisé pour le pilotage et le suivi dans PastoralHerdCompositionEntity.
     */
    @Column(name = "effectif_veaux")
    @Builder.Default
    private Integer effectifVeaux = 0;

    /**
     * Champ effectifGenissesTaurillons : effectif genisses taurillons utilisé pour le pilotage et le suivi dans PastoralHerdCompositionEntity.
     */
    @Column(name = "effectif_genisses_taurillons")
    @Builder.Default
    private Integer effectifGenissesTaurillons = 0;

    /**
     * Champ effectifReproducteurs : effectif reproducteurs utilisé pour le pilotage et le suivi dans PastoralHerdCompositionEntity.
     */
    @Column(name = "effectif_reproducteurs")
    @Builder.Default
    private Integer effectifReproducteurs = 0;

    /**
     * Champ effectifEmboucheReforme : effectif embouche reforme utilisé pour le pilotage et le suivi dans PastoralHerdCompositionEntity.
     */
    @Column(name = "effectif_embouche_reforme")
    @Builder.Default
    private Integer effectifEmboucheReforme = 0;

    /**
     * Champ valeurTotaleCheptel : valeur totale cheptel utilisé pour le pilotage et le suivi dans PastoralHerdCompositionEntity.
     */
    @Column(name = "valeur_totale_cheptel", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal valeurTotaleCheptel = BigDecimal.ZERO;

    /**
     * Champ statutRapprochement : statut rapprochement utilisé pour le pilotage et le suivi dans PastoralHerdCompositionEntity.
     */
    @Column(name = "statut_rapprochement", nullable = false, length = 40)
    @Builder.Default
    private String statutRapprochement = "CONFORME";

    /**
     * Champ observations : observations utilisé pour le pilotage et le suivi dans PastoralHerdCompositionEntity.
     */
    @Column(columnDefinition = "TEXT")
    private String observations;
}
