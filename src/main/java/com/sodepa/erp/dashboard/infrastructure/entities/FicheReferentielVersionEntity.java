package com.sodepa.erp.dashboard.infrastructure.entities;

import com.sodepa.erp.utils.StatutCycleVieReferentiel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fiche de donnée de référence versionnée avec snapshot avant/après, dates d'effet et détection de conflits
 * (REF-01 / F-REF-002, F-REF-004, F-REF-005, F-REF-006, F-REF-032).
 */
@Entity
@Table(name = "ref_mdm_fiches_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FicheReferentielVersionEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "FRV";

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
    @Column(name = "code_famille", nullable = false, length = 40)
    private String codeFamille;

    /**
     * Code mnémonique unique normalisé.
     */
    @Column(name = "code_fiche", nullable = false, length = 60)
    private String codeFiche;

    /**
     * Libellé cleéair et descriptif.
     */
    @Column(nullable = false, length = 200)
    private String libelle;

    /**
     * Numéro d'ordre ou de dossier séquentiel.
     */
    @Column(name = "numero_version", nullable = false)
    @Builder.Default
    private Integer numeroVersion = 1;

    /**
     * Statut actuel dans le cycleée de vie métier.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "statut_cycleée_vie", nullable = false, length = 30)
    @Builder.Default
    private StatutCycleVieReferentiel statutCycleVie = StatutCycleVieReferentiel.BROUILLON;

    /**
     * Date de l'opération ou de l'échéance.
     */
    @Column(name = "date_effet_debut", nullable = false)
    @Builder.Default
    private LocalDate dateEffetDebut = LocalDate.now();

    /**
     * Date de l'opération ou de l'échéance.
     */
    @Column(name = "date_effet_fin")
    private LocalDate dateEffetFin;

    /**
     * Montant financier en Francs CFA (XAF) pour valeur avant json.
     */
    @Column(name = "valeur_avant_json", columnDefinition = "TEXT")
    private String valeurAvantJson;

    /**
     * Montant financier en Francs CFA (XAF) pour valeur apres json.
     */
    @Column(name = "valeur_apres_json", columnDefinition = "TEXT")
    private String valeurApresJson;

    /**
     * Code mnémonique unique normalisé.
     */
    @Column(name = "code_unite_perimetre", length = 50)
    private String codeUnitePerimetre;

    /**
     * Auteur username dans le contexte du traitement FicheReferentielVersionEntity.
     */
    @Column(name = "auteur_username", length = 100)
    private String auteurUsername;

    /**
     * Agent validateur ayant instruit la décision.
     */
    @Column(name = "valideur_metier_username", length = 100)
    private String valideurMetierUsername;

    /**
     * Par delegation de dans le contexte du traitement FicheReferentielVersionEntity.
     */
    @Column(name = "par_delegation_de", length = 100)
    private String parDelegationDe;

    /**
     * Motif ou justification explicative de l'opération.
     */
    @Column(name = "motif_rejet_ou_suspension", length = 500)
    private String motifRejetOuSuspension;

    /**
     * Nombre de transactions ouvertes/non soldées rattachées à cette fiche (CA-REF-002-03 & CA-REF-002-04).
     */
    @Column(name = "transactions_non_soldees_count", nullable = false)
    @Builder.Default
    private Integer transactionsNonSoldeesCount = 0;

    /**
     * En cas de fusion de doublon (CA-REF-004-02), pointe vers le code de la fiche maître conservée.
     */
    @Column(name = "fusionnee_vers_code_fiche", length = 60)
    private String fusionneeVersCodeFiche;

    /**
     * Horodatage modification dans le contexte du traitement FicheReferentielVersionEntity.
     */
    @Column(name = "horodatage_modification", nullable = false)
    @Builder.Default
    private LocalDateTime horodatageModification = LocalDateTime.now();

    /**
     * Numéro incrémental de version du document ou de la fiche.
     */
    @Version
    @Column(name = "lock_version")
    private Long lockVersion;
}
