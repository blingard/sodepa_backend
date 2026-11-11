package com.sodepa.erp.ged.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité représentant un document archivé dans le système de Gestion Électronique des Documents (GED)
 * de la SODEPA (REF-07 / MinIO / S3).
 * <p>
 * Assure le stockage des métadonnées, le versioning automatique, l'horodatage, le scellement
 * par empreinte SHA-256 et la signature électronique avec respect des durées légales de conservation.
 * </p>
 */
@Entity
@Table(name = "ged_documents", indexes = {
    @Index(name = "idx_ged_entite", columnList = "type_entite, entite_id"),
    @Index(name = "idx_ged_sha256", columnList = "hash_sha256"),
    @Index(name = "idx_ged_statut", columnList = "statut_cycle_vie")
})
/**
 * Entité de persistance représentant `DocumentGedEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentGedEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "GED";

    public String getPrefixeCode() {
        return this.codePrefix;
    }

    public void setPrefixeCode(String prefixeCode) {
        this.codePrefix = prefixeCode;
    }


    /**
     * Identifiant technique unique du document GED (UUID).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Nom original du fichier uploadé ou généré (ex: 'bon_commande_BC-2026-001.pdf').
     */
    @Column(name = "nom_fichier", nullable = false)
    private String nomFichier;

    /**
     * Taille du fichier en octets.
     */
    @Column(name = "taille_octets", nullable = false)
    private Long tailleOctets;

    /**
     * Type MIME officiel du contenu (ex: 'application/pdf', 'image/jpeg').
     */
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    /**
     * Empreinte cryptographique SHA-256 garantissant l'intégrité absolue du fichier.
     */
    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    /**
     * Clé de localisation du fichier sur le stockage objet S3 / MinIO.
     */
    @Column(name = "s3_cle", nullable = false, length = 500)
    private String s3Cle;

    /**
     * Nom du bucket S3 cible (ex: 'sodepa-ged-pieces').
     */
    @Column(name = "s3_bucket", nullable = false, length = 100)
    private String s3Bucket;

    /**
     * Type d'entité métier propriétaire (ex: COMMANDE, FACTURE, TIERS, EMPLOYE, VACCINATION).
     */
    @Column(name = "type_entite", nullable = false, length = 50)
    private String typeEntite;

    /**
     * Identifiant UUID de l'entité métier rattachée.
     */
    @Column(name = "entite_id", nullable = false)
    private UUID entiteId;

    /**
     * Catégorie documentaire (ex: CONTRAT, PIECE_JUSTIFICATIVE, CERTIFICAT_SANITAIRE, FACTURE).
     */
    @Column(name = "categorie_documentaire", length = 50)
    @Builder.Default
    private String categorieDocumentaire = "PIECE_JUSTIFICATIVE";

    /**
     * Description ou résumé contextuel du document.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Mots-clés d'indexation pour la recherche plein texte dans la GED.
     */
    @Column(name = "mots_cles_indexation", length = 500)
    private String motsClesIndexation;

    /**
     * Numéro incrémental de version du document (ex: 1, 2, 3).
     */
    @Column(name = "numero_version", columnDefinition = "integer default 1")
    @Builder.Default
    private Integer numeroVersion = 1;

    /**
     * Identifiant du document parent de version antérieure.
     */
    @Column(name = "document_parent_id")
    private UUID documentParentId;

    /**
     * Durée légale ou réglementaire de conservation en années (défaut : 10 ans selon droit OHADA/fiscal).
     */
    @Column(name = "duree_conservation_annees", columnDefinition = "integer default 10")
    @Builder.Default
    private Integer dureeConservationAnnees = 10;

    /**
     * Date d'échéance de la période légale de conservation (inviolable avant cette date).
     */
    @Column(name = "date_fin_conservation")
    private LocalDate dateFinConservation;

    /**
     * Statut dans le cycle de vie du document (BROUILLON, ARCHIVE, VALIDE, OBSOLETE, DETRUIT).
     */
    @Column(name = "statut_cycle_vie", length = 30, columnDefinition = "varchar(30) default 'ARCHIVE'")
    @Builder.Default
    private String statutCycleVie = "ARCHIVE";

    /**
     * Indicateur d'apposition d'une signature électronique qualifiée.
     */
    @Column(name = "signe_electroniquement", columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean signeElectroniquement = false;

    /**
     * Identité complète du signataire habilité ayant apposé le visa numérique.
     */
    @Column(name = "signataire_nom", length = 150)
    private String signataireNom;

    /**
     * Date et heure précises de la signature électronique et de l'horodatage.
     */
    @Column(name = "date_signature")
    private LocalDateTime dateSignature;

    /**
     * Empreinte cryptographique du cachet scellant la signature et le document.
     */
    @Column(name = "empreinte_signature", length = 128)
    private String empreinteSignature;

    /**
     * Date et heure d'enregistrement initial dans la GED.
     */
    @Column(name = "date_upload", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime dateUpload = LocalDateTime.now();

    /**
     * Matricule ou nom de l'agent ayant procédé au téléversement.
     */
    @Column(name = "upload_par", nullable = false, length = 100)
    private String uploadPar;
}
