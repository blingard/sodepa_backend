package com.sodepa.erp.comptabilite.analytique.infrastructure.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entité représentant un détail (section et pourcentage) associé à une clé de répartition.
 */
@Entity
@Table(name = "details_cle_repartition")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetailCleRepartitionEntity {

    /**
     * Identifiant unique du détail.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Clé de répartition parente à laquelle ce détail est rattaché.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cle_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private CleRepartitionEntity cle;

    /**
     * Section analytique destinataire de l'imputation.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "section_id", nullable = false)
    private SectionAnalytiqueEntity section;

    /**
     * Pourcentage de répartition à appliquer (ex: 30.00 pour 30%).
     */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal pourcentage;
}
