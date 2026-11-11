package com.sodepa.erp.achats.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entité de persistance représentant `MarcheDossierLigneEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Entity
@Table(name = "marche_dossier_lignes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarcheDossierLigneEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "MDL";

    public String getPrefixeCode() {
        return this.codePrefix;
    }

    public void setPrefixeCode(String prefixeCode) {
        this.codePrefix = prefixeCode;
    }


    /**
     * Identifiant unique technique dans le cadre des opérations SODEPA.
     */
    @Id
    private UUID id;

    /**
     * Champ dossier : dossier utilisé pour le pilotage et le suivi dans MarcheDossierLigneEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id", nullable = false)
    private MarcheDossierEntity dossier;

    /**
     * Champ ordre : ordre utilisé pour le pilotage et le suivi dans MarcheDossierLigneEntity.
     */
    @Column(nullable = false)
    private Integer ordre;

    /**
     * Désignation complète du produit, service ou élément dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, length = 255)
    private String designation;

    /**
     * Quantité ou volume exprimé dans l'unité de mesure de référence dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantite;

    /**
     * Unité de mesure réglementaire dans le cadre des opérations SODEPA.
     */
    @Column(name = "unite_mesure", length = 30)
    @Builder.Default
    private String uniteMesure = "U";

    /**
     * Valeur financière (prix unitaire ht) exprimée en Francs CFA (XAF).
     */
    @Column(name = "prix_unitaire_ht", nullable = false, precision = 18, scale = 2)
    private BigDecimal prixUnitaireHt;

    /**
     * Montant hors taxes en Francs CFA (XAF) dans le cadre des opérations SODEPA.
     */
    @Column(name = "montant_ht", nullable = false, precision = 18, scale = 2)
    private BigDecimal montantHt;

    /**
     * Taux de taxe sur la valeur ajoutée (TVA) dans le cadre des opérations SODEPA.
     */
    @Column(name = "taux_tva", precision = 6, scale = 4)
    @Builder.Default
    private BigDecimal tauxTva = new BigDecimal("0.1925");

    /**
     * Montant toutes taxes comprises en Francs CFA (XAF) dans le cadre des opérations SODEPA.
     */
    @Column(name = "montant_ttc", nullable = false, precision = 18, scale = 2)
    private BigDecimal montantTtc;
}
