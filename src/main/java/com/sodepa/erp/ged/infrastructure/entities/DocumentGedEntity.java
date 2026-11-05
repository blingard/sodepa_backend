package com.sodepa.erp.ged.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité représentant un document archivé dans la GED SODEPA (MinIO / S3).
 */
@Entity
@Table(name = "ged_documents", indexes = {
    @Index(name = "idx_ged_entite", columnList = "type_entite, entite_id"),
    @Index(name = "idx_ged_sha256", columnList = "hash_sha256")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentGedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nom_fichier", nullable = false)
    private String nomFichier;

    @Column(name = "taille_octets", nullable = false)
    private Long tailleOctets;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @Column(name = "s3_cle", nullable = false, length = 500)
    private String s3Cle;

    @Column(name = "s3_bucket", nullable = false, length = 100)
    private String s3Bucket;

    @Column(name = "type_entite", nullable = false, length = 50)
    private String typeEntite;

    @Column(name = "entite_id", nullable = false)
    private UUID entiteId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "date_upload", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime dateUpload = LocalDateTime.now();

    @Column(name = "upload_par", nullable = false, length = 100)
    private String uploadPar;
}
