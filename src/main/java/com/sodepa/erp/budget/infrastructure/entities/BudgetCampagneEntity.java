package com.sodepa.erp.budget.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité matérialisant une campagne annuelle de préparation budgétaire
 * (Cahier des charges BUD - EP-BUD-02, F-BUD-004 : Campagne budgétaire et lettre-circulaire).
 */
@Entity
@Table(name = "budget_campagne")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetCampagneEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "CAMP-BUD";

    public String getPrefixeCode() {
        return this.codePrefix;
    }

    public void setPrefixeCode(String prefixeCode) {
        this.codePrefix = prefixeCode;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Exercice budgétaire concerné (ex: 2027).
     */
    @Column(nullable = false, unique = true)
    private Integer annee;

    /**
     * Intitulé de la campagne.
     */
    @Column(nullable = false, length = 200)
    private String intitule;

    /**
     * Date officielle de lancement de la campagne (signature de la lettre-circulaire).
     */
    @Column(name = "date_lancement", nullable = false)
    private LocalDate dateLancement;

    /**
     * Date limite prévisionnelle de dépôt des avant-projets par les unités opérationnelles.
     */
    @Column(name = "date_limite_depot_uo")
    private LocalDate dateLimiteDepotUo;

    /**
     * Date prévisionnelle de clôture et soumission au Conseil d'Administration.
     */
    @Column(name = "date_cloture_previsionnelle")
    private LocalDate dateCloturePrevisionnelle;

    /**
     * Statut de la campagne (OUVERTE, EN_COURS, ARBITREE, CLOTUREE).
     */
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String statut = "OUVERTE";

    /**
     * Référence officielle de la lettre-circulaire signée par le DG (ex: CIRC-2026-001/SODEPA/DG).
     */
    @Column(name = "lettre_circulaire_ref", length = 100)
    private String lettreCirculaireRef;

    /**
     * Identifiant du document archivé dans la GED (MinIO) de la lettre-circulaire signée.
     */
    @Column(name = "lettre_circulaire_ged_id")
    private UUID lettreCirculaireGedId;

    /**
     * Orientations stratégiques et directives budgétaires formulées par la Direction Générale.
     */
    @Column(name = "orientations_strategiques", columnDefinition = "TEXT")
    private String orientationsStrategiques;

    /**
     * Horodatage de création.
     */
    @Column(name = "cree_le", nullable = false)
    @Builder.Default
    private LocalDateTime creeLe = LocalDateTime.now();

    /**
     * Auteur de l'enregistrement de la campagne (DPD / CSPP / CSB).
     */
    @Column(name = "cree_par")
    private UUID creePar;
}
