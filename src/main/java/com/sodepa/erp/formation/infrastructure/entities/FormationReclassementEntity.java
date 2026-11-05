package com.sodepa.erp.formation.infrastructure.entities;

import com.sodepa.erp.rh.infrastructure.entities.EmployeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "rh_formation_reclassements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormationReclassementEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", nullable = false)
    private EmployeEntity employe;

    @Column(name = "intitule_diplome_formation", nullable = false, length = 255)
    private String intituleDiplomeFormation;

    @Column(name = "duree_formation_mois", nullable = false)
    private Integer dureeFormationMois;

    @Column(name = "anciennete_annees", nullable = false)
    private Integer ancienneteAnnees;

    @Column(name = "ancienne_categorie", nullable = false)
    private Integer ancienneCategorie;

    @Column(name = "ancien_echelon", nullable = false, length = 5)
    private String ancienEchelon;

    @Column(name = "nouvelle_categorie", nullable = false)
    private Integer nouvelleCategorie;

    @Column(name = "nouvel_echelon", nullable = false, length = 5)
    private String nouvelEchelon;

    @Column(name = "decision_dg_ref", length = 100)
    private String decisionDgRef;

    @Column(name = "date_effet", nullable = false)
    private LocalDate dateEffet;

    @Column(name = "valide_dg", nullable = false)
    @Builder.Default
    private boolean valideDg = false;
}
