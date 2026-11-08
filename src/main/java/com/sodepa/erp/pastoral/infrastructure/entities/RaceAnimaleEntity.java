package com.sodepa.erp.pastoral.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Référentiel des races et lignées animales rattachées à une espèce (REF-03 / F-REF-013 / CA-REF-013-01 & 02).
 */
@Entity
@Table(name = "ref_zoo_races")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RaceAnimaleEntity {

    /**
     * Identifiant unique.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Code mnémonique unique normalisé.
     */
    @Column(nullable = false, unique = true, length = 40)
    private String code;

    /**
     * Nom dans le contexte du traitement RaceAnimaleEntity.
     */
    @Column(nullable = false, length = 150)
    private String nom;

    /**
     * Code obligatoire de l'espèce de rattachement (CA-REF-013-02).
     */
    @Column(name = "code_espece", nullable = false, length = 30)
    private String codeEspece;

    /**
     * Origine geographique dans le contexte du traitement RaceAnimaleEntity.
     */
    @Column(name = "origine_geographique", length = 120)
    private String origineGeographique;

    /**
     * Aptitude : BOUCHERIE_VIANDE, LAITIERE, MIXTE, RUSTIQUE_TRYPANOTOLERANTE, CROISEMENT_AMELIORATEUR.
     */
    @Column(name = "aptitude_principale", nullable = false, length = 50)
    private String aptitudePrincipale;

    /**
     * Poids moyen adulte kg dans le contexte du traitement RaceAnimaleEntity.
     */
    @Column(name = "poids_moyen_adulte_kg", precision = 8, scale = 2)
    private BigDecimal poidsMoyenAdulteKg;

    /**
     * Rendement carcasse reference pct dans le contexte du traitement RaceAnimaleEntity.
     */
    @Column(name = "rendement_carcasse_reference_pct", precision = 5, scale = 2)
    private BigDecimal rendementCarcasseReferencePct;

    /**
     * Schema croisement autorise dans le contexte du traitement RaceAnimaleEntity.
     */
    @Column(name = "schema_croisement_autorise", length = 200)
    private String schemaCroisementAutorise;

    /**
     * Indicateur booléen d'activation de l'élément (évite la suppression physique).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
