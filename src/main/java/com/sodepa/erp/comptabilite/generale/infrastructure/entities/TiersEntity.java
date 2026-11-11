package com.sodepa.erp.comptabilite.generale.infrastructure.entities;

import com.sodepa.erp.utils.TypeTiers;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entité maîtresse représentant un Tiers / Partenaire unifié au sein de la SODEPA (REF-05).
 * <p>
 * Centralise les données administratives, fiscales, bancaires et de risque de crédit
 * pour tous les interlocuteurs externes (Clients, Fournisseurs, Éleveurs, Bailleurs, Collectivités).
 * Assure la liaison univoque avec la comptabilité auxiliaire OHADA.
 * </p>
 */
@Entity
@Table(name = "tiers", indexes = {
    @Index(name = "idx_tiers_code", columnList = "code", unique = true),
    @Index(name = "idx_tiers_niu", columnList = "numero_contribuable"),
    @Index(name = "idx_tiers_type", columnList = "type_tiers")
})
/**
 * Entité de persistance représentant `TiersEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TiersEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "TRS";

    public String getPrefixeCode() {
        return this.codePrefix;
    }

    public void setPrefixeCode(String prefixeCode) {
        this.codePrefix = prefixeCode;
    }


    /**
     * Identifiant technique unique du tiers (UUID).
     */
    @Id
    @EqualsAndHashCode.Include
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * Code unique d'immatriculation du tiers (ex: CLI-0001, FRN-0042, ELV-0012).
     */
    @Column(nullable = false, unique = true, length = 30)
    private String code;

    /**
     * Raison sociale ou Nom complet officiel du tiers (personne morale ou physique).
     */
    @Column(nullable = false, length = 255, name = "intitule")
    private String raisonSociale;

    /**
     * Numéro d'Identifiant Unique fiscal (NIU / Numéro de contribuable) délivré par la DGI.
     */
    @Column(name = "numero_contribuable", length = 50)
    private String numeroContribuable;

    /**
     * Numéro du Registre du Commerce et du Crédit Mobilier (RCCM).
     */
    @Column(length = 50)
    private String rccm;

    /**
     * Régime fiscal d'imposition (ex: REEL, SIMPLIFIE, SYNTHETIQUE, EXONERE).
     */
    @Column(name = "regime_fiscal", length = 50)
    @Builder.Default
    private String regimeFiscal = "REEL";

    /**
     * Secteur ou spécialité d'activité du tiers (ex: Bétail sur pied, Matériel frigorifique, Intrants).
     */
    @Column(length = 150)
    private String specialite;

    /**
     * Adresse postale ou géographique du siège / domicile du tiers.
     */
    @Column(length = 255, nullable = false)
    private String adresse;

    /**
     * Numéro de téléphone principal pour le contact et les relances.
     */
    @Column(length = 50, nullable = false)
    private String telephone;

    /**
     * Adresse électronique officielle pour la facturation et les avis d'échéance.
     */
    @Column(length = 100, nullable = false)
    private String email;

    /**
     * Type de tiers au sens de la nomenclature SODEPA (CLIENT, FOURNISSEUR, ELEVEUR_PARTENAIRE, etc.).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_tiers", nullable = false, length = 30)
    private TypeTiers typeTiers;

    /**
     * Établissement bancaire ou financier domiciliataire du compte.
     */
    @Column(name = "banque_nom", length = 100)
    private String banqueNom;

    /**
     * Relevé d'Identité Bancaire normalisé CEMAC (23 caractères : Code banque, guichet, compte, clé).
     */
    @Column(name = "compte_bancaire_rib", length = 50)
    private String compteBancaireRib;

    /**
     * Code du compte collectif général OHADA de rattachement (ex: 4111 Clients, 4011 Fournisseurs).
     */
    @Column(name = "compte_collectif_code", nullable = false, length = 20)
    private String compteCollectifCode;

    /**
     * Plafond maximal de crédit en FCFA autorisé avant blocage des commandes / livraisons.
     */
    @Column(name = "plafond_credit_fcfa", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal plafondCreditFcfa = BigDecimal.ZERO;

    /**
     * Mode de règlement par défaut (ex: VIREMENT, CHEQUE, ESPECES, MOBILE_MONEY).
     */
    @Column(name = "mode_paiement", length = 50)
    @Builder.Default
    private String modePaiement = "VIREMENT";

    /**
     * Délai et conditions de paiement négociés (ex: COMPTANT, 30_JOURS, 60_JOURS).
     */
    @Column(name = "conditions_paiement", length = 100)
    @Builder.Default
    private String conditionsPaiement = "COMPTANT";

    /**
     * Date de l'agrément ou de la signature de la convention partenariale avec la SODEPA.
     */
    @Column(name = "date_agrement")
    private LocalDate dateAgrement;

    /**
     * Statut de cycle de vie du tiers (ACTIF, SUSPENDU, BLOQUE, EN_CONTENTIEUX).
     */
    @Column(name = "statut_tiers", length = 30)
    @Builder.Default
    private String statutTiers = "ACTIF";

    /**
     * Indicateur d'activité du tiers (true si le tiers peut opérer des transactions).
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean actif = true;
}
