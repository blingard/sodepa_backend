package com.sodepa.erp.achats.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entité de persistance représentant `MarcheReceptionPvEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Entity
@Table(name = "marche_receptions_pv")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarcheReceptionPvEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "MRP";

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
     * Champ numeroPv : numero pv utilisé pour le pilotage et le suivi dans MarcheReceptionPvEntity.
     */
    @Column(name = "numero_pv", nullable = false, unique = true, length = 60)
    private String numeroPv;

    /**
     * Champ dossier : dossier utilisé pour le pilotage et le suivi dans MarcheReceptionPvEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id", nullable = false)
    private MarcheDossierEntity dossier;

    /**
     * Champ dateReception : date reception utilisé pour le pilotage et le suivi dans MarcheReceptionPvEntity.
     */
    @Column(name = "date_reception", nullable = false)
    private LocalDate dateReception;

    /**
     * Champ lieuReception : lieu reception utilisé pour le pilotage et le suivi dans MarcheReceptionPvEntity.
     */
    @Column(name = "lieu_reception", length = 150)
    private String lieuReception;

    /**
     * Champ resultatConformite : resultat conformite utilisé pour le pilotage et le suivi dans MarcheReceptionPvEntity.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "resultat_conformite", nullable = false, length = 40)
    @Builder.Default
    private ResultatConformitePv resultatConformite = ResultatConformitePv.CONFORME_SANS_RESERVE;

    /**
     * Champ observations : observations utilisé pour le pilotage et le suivi dans MarcheReceptionPvEntity.
     */
    @Column(columnDefinition = "TEXT")
    private String observations;

    /**
     * Champ numeroFactureDefinitive : numero facture definitive utilisé pour le pilotage et le suivi dans MarcheReceptionPvEntity.
     */
    @Column(name = "numero_facture_definitive", length = 100)
    private String numeroFactureDefinitive;

    /**
     * Valeur financière (montant facture liquide) exprimée en Francs CFA (XAF).
     */
    @Column(name = "montant_facture_liquide", precision = 18, scale = 2)
    private BigDecimal montantFactureLiquide;

    /**
     * Champ fichierUrl : fichier url utilisé pour le pilotage et le suivi dans MarcheReceptionPvEntity.
     */
    @Column(name = "fichier_url", length = 500)
    private String fichierUrl;

    /**
     * Champ liquidationValidee : liquidation validee utilisé pour le pilotage et le suivi dans MarcheReceptionPvEntity.
     */
    @Column(name = "liquidation_validee", nullable = false)
    @Builder.Default
    private boolean liquidationValidee = false;

    /**
     * Champ dateLiquidation : date liquidation utilisé pour le pilotage et le suivi dans MarcheReceptionPvEntity.
     */
    @Column(name = "date_liquidation")
    private Instant dateLiquidation;
}
