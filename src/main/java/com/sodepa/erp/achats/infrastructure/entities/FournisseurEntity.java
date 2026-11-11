package com.sodepa.erp.achats.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entité de persistance représentant `FournisseurEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Entity
@Table(name = "achat_fournisseurs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FournisseurEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "FOUR";

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
     * Code de référence unique normalisé dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, unique = true, length = 30)
    private String code;

    /**
     * Champ raisonSociale : raison sociale utilisé pour le pilotage et le suivi dans FournisseurEntity.
     */
    @Column(name = "raison_sociale", nullable = false, length = 200)
    private String raisonSociale;

    /**
     * Champ numeroContribuable : numero contribuable utilisé pour le pilotage et le suivi dans FournisseurEntity.
     */
    @Column(name = "numero_contribuable", length = 50)
    private String numeroContribuable;

    /**
     * Champ rccm : rccm utilisé pour le pilotage et le suivi dans FournisseurEntity.
     */
    @Column(length = 50)
    private String rccm;

    /**
     * Champ regimeFiscal : regime fiscal utilisé pour le pilotage et le suivi dans FournisseurEntity.
     */
    @Column(name = "regime_fiscal", length = 50)
    @Builder.Default
    private String regimeFiscal = "REEL";

    /**
     * Champ specialite : specialite utilisé pour le pilotage et le suivi dans FournisseurEntity.
     */
    @Column(length = 150)
    private String specialite;

    /**
     * Adresse géographique ou postale dans le cadre des opérations SODEPA.
     */
    @Column(length = 255)
    private String adresse;

    /**
     * Numéro de téléphone de contact dans le cadre des opérations SODEPA.
     */
    @Column(length = 50)
    private String telephone;

    /**
     * Adresse électronique professionnelle de contact dans le cadre des opérations SODEPA.
     */
    @Column(length = 100)
    private String email;

    /**
     * Nom ou désignation textuelle de banque pour l'affichage et la traçabilité.
     */
    @Column(name = "banque_nom", length = 100)
    private String banqueNom;

    /**
     * Champ compteBancaireRib : compte bancaire rib utilisé pour le pilotage et le suivi dans FournisseurEntity.
     */
    @Column(name = "compte_bancaire_rib", length = 50)
    private String compteBancaireRib;

    /**
     * Champ agreeSodepa : agree sodepa utilisé pour le pilotage et le suivi dans FournisseurEntity.
     */
    @Column(name = "agree_sodepa", nullable = false)
    @Builder.Default
    private boolean agreeSodepa = true;

    /**
     * Champ dateAgrement : date agrement utilisé pour le pilotage et le suivi dans FournisseurEntity.
     */
    @Column(name = "date_agrement")
    private LocalDate dateAgrement;

    /**
     * Indicateur d'activation opérationnelle (true si actif, false si archivé/désactivé) dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
