package com.sodepa.erp.authorisation.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Règle de Séparation des Tâches (Matrice SoD — Segregation of Duties) (REF-06 / F-REF-027 / CA-REF-027-01 & 02 / RG-REF-021, 022, 027).
 * Définit deux permissions ou deux profils incompatibles ne pouvant jamais être cumulés par un même agent.
 */
@Entity
@Table(name = "iam_matrice_sod")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegleSodEntity {

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
    private String codeRegle;

    /**
     * Libellé cleéair et descriptif.
     */
    @Column(nullable = false, length = 200)
    private String libelle;

    /**
     * Type d'incompatibilité : PERMISSION_VS_PERMISSION, PROFIL_VS_PROFIL.
     */
    @Column(name = "type_incompatibilite", nullable = false, length = 40)
    @Builder.Default
    private String typeIncompatibilite = "PERMISSION_VS_PERMISSION";

    /**
     * Element a dans le contexte du traitement RegleSodEntity.
     */
    @Column(name = "element_a", nullable = false, length = 100)
    private String elementA;

    /**
     * Element b dans le contexte du traitement RegleSodEntity.
     */
    @Column(name = "element_b", nullable = false, length = 100)
    private String elementB;

    /**
     * Référence à la règle de gestion (ex: RG-REF-020, RG-REF-021, RG-REF-022, RG-REF-027).
     */
    @Column(name = "regle_gestion_ref", nullable = false, length = 30)
    private String regleGestionRef;

    /**
     * Risque metier dans le contexte du traitement RegleSodEntity.
     */
    @Column(name = "risque_metier", length = 300)
    private String risqueMetier;

    /**
     * Indicateur booléen précisant si bloquant est effectif ou validé.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean bloquant = true;

    /**
     * Indicateur booléen d'activation de l'élément (évite la suppression physique).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
