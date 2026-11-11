package com.sodepa.erp.fiscalite.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entité du Référentiel Fiscal SODEPA (F-REF-020 / CA-REF-020-01 / RG-REF-007).
 * Gère les impôts, taxes, retenues à la source (TVA, AIR, CAC, TSR, taxes vétérinaires)
 * avec date d'effet et historisation pour appliquer le taux en vigueur à la date d'opération.
 */
@Entity
@Table(name = "ref_fiscalite_taxes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxeImpotsEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "TAX";

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
    @Column(nullable = false, length = 50)
    private String code;

    /**
     * Libellé cleéair et descriptif.
     */
    @Column(nullable = false, length = 180)
    private String libelle;

    /**
     * Catégorie fiscale : TVA, RETENUE_SOURCE_AIR, IMPOT_SALARIAL, TAXE_PARAFISCALE_PASTORALE, DROIT_ENREGISTREMENT.
     */
    @Column(name = "categorie_fiscale", nullable = false, length = 50)
    private String categorieFiscale;

    /**
     * Taux en pourcentage (ex: 19.25 pour la TVA Cameroun, 2.20 ou 5.50 pour l'AIR).
     */
    @Column(name = "taux_pourcentage", precision = 8, scale = 4)
    private BigDecimal tauxPourcentage;

    /**
     * Montant forfaitaire en FCFA si la taxe est fixe (ex: taxe d'inspection sanitaire par tête abattue).
     */
    @Column(name = "montant_fixe_fcfa", precision = 15, scale = 2)
    private BigDecimal montantFixeFcfa;

    /**
     * Base de calcul : HT, TTC, BRUT_TAXABLE, TETE_ABATTUE, FORFAIT.
     */
    @Column(name = "base_calcul", nullable = false, length = 40)
    @Builder.Default
    private String baseCalcul = "HT";

    /**
     * Compte SYSCOHADA de taxe collectée / retenue à reverser (ex: 4431, 4472).
     */
    @Column(name = "compte_comptable_collecte", length = 20)
    private String compteComptableCollecte;

    /**
     * Compte SYSCOHADA de taxe déductible / récupérable (ex: 4452).
     */
    @Column(name = "compte_comptable_deductible", length = 20)
    private String compteComptableDeductible;

    /**
     * Périodicité décleéarative DGI : MENSUELLE_J15, TRIMESTRIELLE, ANNUELLE.
     */
    @Column(name = "periodicite_decleéaration", length = 40)
    @Builder.Default
    private String periodiciteDecleéaration = "MENSUELLE_J15";

    /**
     * Indicateur booléen précisant si exonere export est effectif ou validé.
     */
    @Column(name = "exonere_export", nullable = false)
    @Builder.Default
    private boolean exonereExport = false;

    /**
     * Date d'entrée en vigueur du taux (RG-REF-007 / CA-REF-020-01).
     */
    @Column(name = "date_effet", nullable = false)
    private LocalDate dateEffet;

    /**
     * Date de fin de validité (renseignée automatiquement lorsqu'une nouvelle loi de finances remplace le taux).
     */
    @Column(name = "date_fin")
    private LocalDate dateFin;

    /**
     * Numéro de référence officielle du document ou de la pièce.
     */
    @Column(name = "reference_texte_loi", length = 180)
    private String referenceTexteLoi;

    /**
     * Indicateur booléen d'activation de l'élément (évite la suppression physique).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
