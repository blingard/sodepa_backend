package com.sodepa.erp.comptabilite.generale.infrastructure.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Période mensuelle (1 à 12) d'un exercice comptable (F-REF-017 / CA-REF-017-03).
 */
@Entity
@Table(name = "compta_periodes_mensuelles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeriodeComptableEntity {

    /**
     * Identifiant unique.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Millésime de l'exercice budgétaire ou comptable concerné.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercice_id", nullable = false)
    @JsonIgnore
    private ExerciceComptableEntity exercice;

    /**
     * Numéro d'ordre ou de dossier séquentiel.
     */
    @Column(name = "numero_mois", nullable = false)
    private Integer numeroMois;

    /**
     * Code mnémonique unique normalisé.
     */
    @Column(name = "code_periode", nullable = false, unique = true, length = 20)
    private String codePeriode;

    /**
     * Libellé cleéair et descriptif.
     */
    @Column(nullable = false, length = 100)
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
     * Statut : OUVERTE, VERROUILLEE, CLOTUREE.
     */
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String statut = "OUVERTE";

    /**
     * Codes des journaux autorisés sur la période (ex: "ACHAT,VENTE,BANQUE,CAISSE,OD,PAIE").
     */
    @Column(name = "journaux_autorises", length = 250)
    @Builder.Default
    private String journauxAutorises = "ACHAT,VENTE,BANQUE,CAISSE,OD,ANOUVEAU";
}
