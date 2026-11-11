package com.sodepa.erp.comptabilite.generale.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Pièce administrative et fiscale obligatoire attachée à un Tiers (REF-05 / F-REF-023 / CA-REF-023-01).
 * Permet de suivre les dates de validité (Attestation de non-redevance DGI, Attestation CNPS, RCCM, NIU, RIB,
 * Agrément vétérinaire) et de décleéencher les alertes d'expiration à J-15.
 */
@Entity
@Table(name = "tiers_pieces_conformite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PieceConformiteTiersEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "PCT";

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
     * Identifiant unique.
     */
    @Column(name = "tiers_id", nullable = false)
    private UUID tiersId;

    /**
     * Code mnémonique du tiers ou partenaire enregistré dans le référentiel unifié.
     */
    @Column(name = "code_tiers", nullable = false, length = 30)
    private String codeTiers;

    /**
     * Type de pièce : ATTESTATION_NON_REDEVANCE_DGI, ATTESTATION_CNPS, EXTRAIT_RCCM,
     * CARTE_CONTRIBUABLE_NIU, ATTESTATION_DOMICILIATION_RIB, AGREMENT_SANITAIRE_MINEPIA.
     */
    @Column(name = "type_piece", nullable = false, length = 60)
    private String typePiece;

    /**
     * Numéro de référence officielle du document ou de la pièce.
     */
    @Column(name = "numero_reference", nullable = false, length = 100)
    private String numeroReference;

    /**
     * Date de l'opération ou de l'échéance.
     */
    @Column(name = "date_emission", nullable = false)
    private LocalDate dateEmission;

    /**
     * Date d'expiration de la pièce (ex: 3 mois pour l'ANR DGI et la CNPS).
     */
    @Column(name = "date_expiration")
    private LocalDate dateExpiration;

    /**
     * Référence du fichier archivé dans la GED (DocumentGedEntity).
     */
    @Column(name = "document_ged_id")
    private UUID documentGedId;

    /**
     * Indicateur booléen précisant si obligatoire est effectif ou validé.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean obligatoire = true;

    /**
     * Indicateur booléen précisant si verifiee est effectif ou validé.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean verifiee = true;

    /**
     * Indicateur booléen précisant si alerte j15 envoyee est effectif ou validé.
     */
    @Column(name = "alerte_j15_envoyee", nullable = false)
    @Builder.Default
    private boolean alerteJ15Envoyee = false;
}
