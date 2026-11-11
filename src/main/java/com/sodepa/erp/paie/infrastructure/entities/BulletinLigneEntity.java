package com.sodepa.erp.paie.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entité de persistance représentant `BulletinLigneEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Entity
@Table(name = "paie_bulletin_lignes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulletinLigneEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "BL";

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
     * Champ bulletin : bulletin utilisé pour le pilotage et le suivi dans BulletinLigneEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bulletin_id", nullable = false)
    private BulletinPaieEntity bulletin;

    /**
     * Champ ordre : ordre utilisé pour le pilotage et le suivi dans BulletinLigneEntity.
     */
    @Column(nullable = false)
    private Integer ordre;

    /**
     * Champ codeRubrique : code rubrique utilisé pour le pilotage et le suivi dans BulletinLigneEntity.
     */
    @Column(name = "code_rubrique", nullable = false, length = 30)
    private String codeRubrique;

    /**
     * Libellé descriptif officiel dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, length = 150)
    private String libelle;

    /**
     * Champ sens : sens utilisé pour le pilotage et le suivi dans BulletinLigneEntity.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SensRubrique sens;

    /**
     * Champ baseCalcul : base calcul utilisé pour le pilotage et le suivi dans BulletinLigneEntity.
     */
    @Column(name = "base_calcul", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal baseCalcul = BigDecimal.ZERO;

    /**
     * Taux ou pourcentage appliqué dans le cadre des opérations SODEPA.
     */
    @Column(precision = 8, scale = 4)
    @Builder.Default
    private BigDecimal taux = BigDecimal.ZERO;

    /**
     * Champ gain : gain utilisé pour le pilotage et le suivi dans BulletinLigneEntity.
     */
    @Column(precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal gain = BigDecimal.ZERO;

    /**
     * Champ retenue : retenue utilisé pour le pilotage et le suivi dans BulletinLigneEntity.
     */
    @Column(precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal retenue = BigDecimal.ZERO;

    /**
     * Champ chargePatronale : charge patronale utilisé pour le pilotage et le suivi dans BulletinLigneEntity.
     */
    @Column(name = "charge_patronale", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal chargePatronale = BigDecimal.ZERO;
}
