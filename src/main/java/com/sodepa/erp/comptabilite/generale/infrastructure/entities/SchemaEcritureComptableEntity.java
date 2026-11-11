package com.sodepa.erp.comptabilite.generale.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Référentiel des schémas d'écritures comptables automatiques SYSCOHADA (BF-REF-025 / F-REF-017).
 */
@Entity
@Table(name = "compta_schemas_ecritures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemaEcritureComptableEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "SEC";

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
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /**
     * Libellé cleéair et descriptif.
     */
    @Column(nullable = false, length = 180)
    private String libelle;

    /**
     * Code mnémonique unique normalisé.
     */
    @Column(name = "code_journal", nullable = false, length = 20)
    private String codeJournal;

    /**
     * Type d'événement décleéencheur : FACTURE_ACHAT, VENTE_BOVIN, VENTE_CARCASSE, PAIE_MENSUELLE, NAISSANCE_BOVIN, APPRO_REGIE.
     */
    @Column(name = "type_operation", nullable = false, length = 50)
    private String typeOperation;

    /**
     * Code mnémonique du compte comptable conforme au plan comptable OHADA révisé.
     */
    @Column(name = "compte_debit_code", nullable = false, length = 20)
    private String compteDebitCode;

    /**
     * Code mnémonique du compte comptable conforme au plan comptable OHADA révisé.
     */
    @Column(name = "compte_credit_code", nullable = false, length = 20)
    private String compteCreditCode;

    /**
     * Code mnémonique du compte comptable conforme au plan comptable OHADA révisé.
     */
    @Column(name = "compte_taxe_code", length = 20)
    private String compteTaxeCode;

    /**
     * Code mnémonique du compte comptable conforme au plan comptable OHADA révisé.
     */
    @Column(name = "compte_retenue_air_code", length = 20)
    private String compteRetenueAirCode;

    /**
     * Indicateur booléen d'activation de l'élément (évite la suppression physique).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
