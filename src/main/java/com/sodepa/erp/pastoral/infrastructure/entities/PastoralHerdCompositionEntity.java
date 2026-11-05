package com.sodepa.erp.pastoral.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pastoral_herd_compositions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PastoralHerdCompositionEntity {

    @Id
    private UUID id;

    @Column(name = "annee_exercice", nullable = false)
    private Integer anneeExercice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ranch_id", nullable = false)
    private PastoralRanchEntity ranch;

    @Column(name = "date_inventaire", nullable = false)
    private LocalDate dateInventaire;

    @Column(name = "effectif_physique_total", nullable = false)
    private Integer effectifPhysiqueTotal;

    @Column(name = "effectif_veaux")
    @Builder.Default
    private Integer effectifVeaux = 0;

    @Column(name = "effectif_genisses_taurillons")
    @Builder.Default
    private Integer effectifGenissesTaurillons = 0;

    @Column(name = "effectif_reproducteurs")
    @Builder.Default
    private Integer effectifReproducteurs = 0;

    @Column(name = "effectif_embouche_reforme")
    @Builder.Default
    private Integer effectifEmboucheReforme = 0;

    @Column(name = "valeur_totale_cheptel", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal valeurTotaleCheptel = BigDecimal.ZERO;

    @Column(name = "statut_rapprochement", nullable = false, length = 40)
    @Builder.Default
    private String statutRapprochement = "CONFORME";

    @Column(columnDefinition = "TEXT")
    private String observations;
}
