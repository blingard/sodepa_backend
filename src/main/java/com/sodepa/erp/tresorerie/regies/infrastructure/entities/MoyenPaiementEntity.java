package com.sodepa.erp.tresorerie.regies.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Référentiel des moyens de paiement et des opérateurs Mobile Money SODEPA (REF-04 / F-REF-019 / CA-REF-019-02).
 * Permet d'associer chaque canal (Espèces, Virement CEMAC, Chèque, MTN Mobile Money, Orange Money)
 * à un compte comptable SYSCOHADA (cleéasses 52 / 55 / 57) et un plafond par opération.
 */
@Entity
@Table(name = "tresorerie_moyens_paiement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoyenPaiementEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "MP";

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
    @Column(nullable = false, unique = true, length = 40)
    private String code;

    /**
     * Libellé cleéair et descriptif.
     */
    @Column(nullable = false, length = 150)
    private String libelle;

    /**
     * Type : ESPECES, VIREMENT_BANCAIRE, CHEQUE, MOBILE_MONEY, CARTE_BANCAIRE.
     */
    @Column(name = "type_moyen", nullable = false, length = 40)
    private String typeMoyen;

    /**
     * Opérateur Mobile Money ou réseau interbancaire (ex: MTN_CAMEROON, ORANGE_CAMEROON, SYGMA_SYSTAC_BEAC).
     */
    @Column(name = "operateur", length = 80)
    private String operateur;

    /**
     * Numéro d'ordre ou de dossier séquentiel.
     */
    @Column(name = "numero_marchand_ou_shortcode", length = 60)
    private String numeroMarchandOuShortcode;

    /**
     * Compte général SYSCOHADA associé (ex: 558100 pour MTN MoMo, 558200 pour Orange Money, 571100 pour Caisse).
     */
    @Column(name = "compte_comptable_associe", nullable = false, length = 20)
    private String compteComptableAssocie;

    /**
     * Plafond maximal autorisé en Francs CFA.
     */
    @Column(name = "plafond_transaction_fcfa", precision = 15, scale = 2)
    private BigDecimal plafondTransactionFcfa;

    /**
     * Frais pourcentage dans le contexte du traitement MoyenPaiementEntity.
     */
    @Column(name = "frais_pourcentage", precision = 6, scale = 3)
    @Builder.Default
    private BigDecimal fraisPourcentage = BigDecimal.ZERO;

    /**
     * Indicateur booléen d'activation de l'élément (évite la suppression physique).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
