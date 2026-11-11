package com.sodepa.erp.comptabilite.generale.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité représentant un Exercice Comptable SYSCOHADA (F-REF-017 / CA-REF-017-03)
 * et ses 12 périodes mensuelles rattachées.
 */
@Entity
@Table(name = "compta_exercices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciceComptableEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "EXC";

    public String getPrefixeCode() {
        return this.codePrefix;
    }

    public void setPrefixeCode(String prefixeCode) {
        this.codePrefix = prefixeCode;
    }


    /**
     * Identifiant unique.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Code mnémonique unique normalisé.
     */
    @Column(nullable = false, unique = true, length = 30)
    private String code;

    /**
     * Annee dans le contexte du traitement ExerciceComptableEntity.
     */
    @Column(nullable = false, unique = true)
    private Integer annee;

    /**
     * Libellé cleéair et descriptif.
     */
    @Column(nullable = false, length = 150)
    private String libelle;

    /**
     * Date de l'opération ou de l'échéance.
     */
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    /**
     * Date de l'opération ou de l'échéance.
     */
    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    /**
     * Statut : OUVERT, CLOTURE_PROVISOIRE, CLOTURE_DEFINITIF (RG-REF-023).
     */
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String statut = "OUVERT";

    /**
     * Date de l'opération ou de l'échéance.
     */
    @Column(name = "date_cloture")
    private Instant dateCloture;

    /**
     * Cloture par dans le contexte du traitement ExerciceComptableEntity.
     */
    @Column(name = "cloture_par")
    private UUID cloturePar;

    /**
     * Collection des éléments de type List<PeriodeComptableEntity> associés à cet enregistréement.
     */
    @OneToMany(mappedBy = "exercice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("numeroMois ASC")
    @Builder.Default
    private List<PeriodeComptableEntity> periodes = new ArrayList<>();
}
