package com.sodepa.erp.paie.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entité de persistance représentant `PrimeConfigEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Entity
@Table(name = "paie_primes_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrimeConfigEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "PC";

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
     * Libellé descriptif officiel dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, length = 150)
    private String libelle;

    /**
     * Champ typeRubrique : type rubrique utilisé pour le pilotage et le suivi dans PrimeConfigEntity.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_rubrique", nullable = false, length = 40)
    private TypeRubrique typeRubrique;

    /**
     * Champ imposable : imposable utilisé pour le pilotage et le suivi dans PrimeConfigEntity.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean imposable = true;

    /**
     * Champ cotisableCnps : cotisable cnps utilisé pour le pilotage et le suivi dans PrimeConfigEntity.
     */
    @Column(name = "cotisable_cnps", nullable = false)
    @Builder.Default
    private boolean cotisableCnps = true;

    /**
     * Valeur financière (montant defaut) exprimée en Francs CFA (XAF).
     */
    @Column(name = "montant_defaut", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal montantDefaut = BigDecimal.ZERO;

    /**
     * Indicateur d'activation opérationnelle (true si actif, false si archivé/désactivé) dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
