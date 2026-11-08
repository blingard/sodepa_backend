package com.sodepa.erp.dashboard.infrastructure.entities;

import com.sodepa.erp.utils.StatutCycleVieReferentiel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Famille de référentiels du socleée MDM SODEPA (REF-01 / F-REF-001 & F-REF-003 / RG-REF-014, RG-REF-025, RG-REF-027).
 */
@Entity
@Table(name = "ref_mdm_familles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilleReferentielEntity {

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
     * Libellé cleéair et descriptif.
     */
    @Column(nullable = false, length = 180)
    private String libelle;

    /**
     * Sous-domaine : REF-01, REF-02, REF-03, REF-04, REF-05, REF-06, REF-07.
     */
    @Column(name = "sous_domaine", nullable = false, length = 20)
    private String sousDomaine;

    /**
     * Propriétaire métier obligatoire (CA-REF-001-02 / RG-REF-014) : ex. DG, DAF, DPA, DC, DRH.
     */
    @Column(name = "proprietaire_metier", nullable = false, length = 60)
    private String proprietaireMetier;

    /**
     * Gestionnaire technique (CA-REF-001-04 / RG-REF-027) : CELLULE_INFORMATIQUE (sans droit de validation métier).
     */
    @Column(name = "gestionnaire_technique", nullable = false, length = 60)
    @Builder.Default
    private String gestionnaireTechnique = "CELLULE_INFORMATIQUE";

    /**
     * Code de la famille parente pour gérer les dépendances hiérarchiques inter-référentiels (CA-REF-001-03).
     */
    @Column(name = "code_famille_parente", length = 40)
    private String codeFamilleParente;

    /**
     * Masque de codification automatique (CA-REF-003-01) : ex. FRN-{000000}, CLI-{000000}.
     */
    @Column(name = "masque_codification", length = 60)
    private String masqueCodification;

    /**
     * Expression régulière de contrôle du format des codes (CA-REF-003-03).
     */
    @Column(name = "regex_validation_code", length = 120)
    private String regexValidationCode;

    /**
     * Compte comptable conforme au plan comptable OHADA révisé.
     */
    @Column(name = "compteur_courant", nullable = false)
    @Builder.Default
    private Long compteurCourant = 0L;

    /**
     * Numéro incrémental de version du document ou de la fiche.
     */
    @Column(name = "version_nomencleéature", nullable = false)
    @Builder.Default
    private Integer versionNomencleéature = 1;

    /**
     * Date de l'opération ou de l'échéance.
     */
    @Column(name = "date_effet", nullable = false)
    @Builder.Default
    private LocalDate dateEffet = LocalDate.now();

    /**
     * Statut actuel dans le cycleée de vie métier.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "statut_cycleée_vie", nullable = false, length = 30)
    @Builder.Default
    private StatutCycleVieReferentiel statutCycleVie = StatutCycleVieReferentiel.ACTIF;
}
