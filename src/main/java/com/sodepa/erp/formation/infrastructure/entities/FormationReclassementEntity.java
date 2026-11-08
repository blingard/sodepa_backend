package com.sodepa.erp.formation.infrastructure.entities;

import com.sodepa.erp.rh.infrastructure.entities.EmployeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entité de persistance représentant `FormationReclassementEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Entity
@Table(name = "rh_formation_reclassements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormationReclassementEntity {

    /**
     * Identifiant unique technique dans le cadre des opérations SODEPA.
     */
    @Id
    private UUID id;

    /**
     * Employé ou collaborateur SODEPA concerné dans le cadre des opérations SODEPA.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", nullable = false)
    private EmployeEntity employe;

    /**
     * Champ intituleDiplomeFormation : intitule diplome formation utilisé pour le pilotage et le suivi dans FormationReclassementEntity.
     */
    @Column(name = "intitule_diplome_formation", nullable = false, length = 255)
    private String intituleDiplomeFormation;

    /**
     * Champ dureeFormationMois : duree formation mois utilisé pour le pilotage et le suivi dans FormationReclassementEntity.
     */
    @Column(name = "duree_formation_mois", nullable = false)
    private Integer dureeFormationMois;

    /**
     * Champ ancienneteAnnees : anciennete annees utilisé pour le pilotage et le suivi dans FormationReclassementEntity.
     */
    @Column(name = "anciennete_annees", nullable = false)
    private Integer ancienneteAnnees;

    /**
     * Champ ancienneCategorie : ancienne categorie utilisé pour le pilotage et le suivi dans FormationReclassementEntity.
     */
    @Column(name = "ancienne_categorie", nullable = false)
    private Integer ancienneCategorie;

    /**
     * Champ ancienEchelon : ancien echelon utilisé pour le pilotage et le suivi dans FormationReclassementEntity.
     */
    @Column(name = "ancien_echelon", nullable = false, length = 5)
    private String ancienEchelon;

    /**
     * Champ nouvelleCategorie : nouvelle categorie utilisé pour le pilotage et le suivi dans FormationReclassementEntity.
     */
    @Column(name = "nouvelle_categorie", nullable = false)
    private Integer nouvelleCategorie;

    /**
     * Champ nouvelEchelon : nouvel echelon utilisé pour le pilotage et le suivi dans FormationReclassementEntity.
     */
    @Column(name = "nouvel_echelon", nullable = false, length = 5)
    private String nouvelEchelon;

    /**
     * Champ decisionDgRef : decision dg ref utilisé pour le pilotage et le suivi dans FormationReclassementEntity.
     */
    @Column(name = "decision_dg_ref", length = 100)
    private String decisionDgRef;

    /**
     * Champ dateEffet : date effet utilisé pour le pilotage et le suivi dans FormationReclassementEntity.
     */
    @Column(name = "date_effet", nullable = false)
    private LocalDate dateEffet;

    /**
     * Champ valideDg : valide dg utilisé pour le pilotage et le suivi dans FormationReclassementEntity.
     */
    @Column(name = "valide_dg", nullable = false)
    @Builder.Default
    private boolean valideDg = false;
}
