package com.sodepa.erp.pastoral.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Référentiel d'Abattage, de Transformation et des Normes de Rendement Carcasse
 * (REF-03 / F-REF-015 / CA-REF-015-02).
 */
@Entity
@Table(name = "ref_abattage_transformation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferentielAbattageTransformationEntity {

    /**
     * Identifiant unique.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Code mnémonique unique normalisé.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /**
     * Libellé cleéair et descriptif.
     */
    @Column(nullable = false, length = 200)
    private String libelle;

    /**
     * Type : CHAINE_ABATTAGE, NORME_RENDEMENT_CARCASSE, DECOUPE_STANDARD, SEUIL_PERTE_RESSUYAGE.
     */
    @Column(name = "type_norme", nullable = false, length = 50)
    private String typeNorme;

    /**
     * Code mnémonique du espèce animale du référentiel zootechnique (Bovins, Porcins, Caprins, etc.).
     */
    @Column(name = "code_espece", nullable = false, length = 30)
    @Builder.Default
    private String codeEspece = "BOVINE";

    /**
     * Code mnémonique du race animale spécifique (Goudali, Mbororo, Charolaise, etc.).
     */
    @Column(name = "code_race", length = 40)
    private String codeRace;

    /**
     * Rendement min pct dans le contexte du traitement ReferentielAbattageTransformationEntity.
     */
    @Column(name = "rendement_min_pct", precision = 5, scale = 2)
    private BigDecimal rendementMinPct;

    /**
     * Rendement cible pct dans le contexte du traitement ReferentielAbattageTransformationEntity.
     */
    @Column(name = "rendement_cible_pct", precision = 5, scale = 2)
    private BigDecimal rendementCiblePct;

    /**
     * Rendement max pct dans le contexte du traitement ReferentielAbattageTransformationEntity.
     */
    @Column(name = "rendement_max_pct", precision = 5, scale = 2)
    private BigDecimal rendementMaxPct;

    /**
     * Perte max ressuyage pct dans le contexte du traitement ReferentielAbattageTransformationEntity.
     */
    @Column(name = "perte_max_ressuyage_pct", precision = 5, scale = 2)
    private BigDecimal perteMaxRessuyagePct;

    /**
     * Temperature consigne celsius dans le contexte du traitement ReferentielAbattageTransformationEntity.
     */
    @Column(name = "temperature_consigne_celsius", precision = 5, scale = 2)
    private BigDecimal temperatureConsigneCelsius;

    /**
     * Indicateur booléen d'activation de l'élément (évite la suppression physique).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
