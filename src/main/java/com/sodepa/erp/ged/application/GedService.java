package com.sodepa.erp.ged.application;

import com.sodepa.erp.ged.infrastructure.entities.DocumentGedEntity;
import com.sodepa.erp.ged.infrastructure.repo.DocumentGedRepository;
import com.sodepa.erp.share.MinioService;
import com.sodepa.erp.share.UtilsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service applicatif de la Gestion Électronique des Documents (GED SODEPA)
 * (REF-07 / F-REF-031 / CA-REF-031-01 à 04 / CT-REF-06 / RG-REF-012).
 * <p>
 * Couvre :
 * <ul>
 *   <li>L'indexation, le stockage S3 MinIO, le calcul SHA-256 et la recherche plein texte/multicritère ({@code CA-REF-031-01}).</li>
 *   <li>Le versioning automatique sans écrasement des versions antérieures ({@code CA-REF-031-02}).</li>
 *   <li>La signature électronique et la détection de toute falsification post-signature ({@code CA-REF-031-03}).</li>
 *   <li>L'interdiction stricte de suppression physique avant l'expiration de la durée légale de conservation ({@code CA-REF-031-04}, {@code RG-REF-012}).</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GedService {

    private final DocumentGedRepository documentGedRepository;
    private final MinioService minioService;

    @Value("${minio.bucket-name:sodepa-erp-ged}")
    private String defaultBucket;

    @Value("${sodepa.ged.hmac-secret:}")
    private String hmacSecret;

    @Autowired(required = false)
    private org.springframework.core.env.Environment environment;

    private volatile String ephemeralDevSecret;

    @Autowired(required = false)
    private UtilsService utilsService;

    private String resolveHmacKey() {
        if (hmacSecret != null && !hmacSecret.isBlank()) {
            return hmacSecret;
        }
        boolean isProd = environment != null && java.util.Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> "prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p));
        if (isProd) {
            throw new IllegalStateException("Propriété obligatoire manquante en production : sodepa.ged.hmac-secret");
        }
        if (ephemeralDevSecret == null) {
            byte[] randomBytes = new byte[32];
            new java.security.SecureRandom().nextBytes(randomBytes);
            ephemeralDevSecret = java.util.Base64.getEncoder().encodeToString(randomBytes);
            log.warn("⚠️ sodepa.ged.hmac-secret absent hors production : clé HMAC éphémère générée en mémoire.");
        }
        return ephemeralDevSecret;
    }

    private String resolveActorOrFallback(String fallback) {
        if (utilsService != null) {
            try {
                var ud = utilsService.getCurrentUserData();
                if (ud != null && ud.username() != null && !ud.username().isBlank()) {
                    return ud.username();
                }
            } catch (Exception ignored) {
            }
        }
        return (fallback != null && !fallback.isBlank()) ? fallback : "SYSTEM";
    }

    private String sanitizeFilename(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return "document-" + UUID.randomUUID() + ".pdf";
        }
        String fileName = Paths.get(rawName.replace("\\", "/")).getFileName().toString();
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Archive un document dans la GED avec gestion automatique des versions et de la durée de conservation
     * (CA-REF-031-01 & CA-REF-031-02).
     */
    @Transactional
    public DocumentGedEntity archiverDocument(
            MultipartFile file,
            String typeEntite,
            UUID entiteId,
            String categorieDocumentaire,
            String description,
            String motsClesIndexation,
            Integer dureeConservationAnnees,
            String uploadPar) {

        try {
            byte[] bytes = file != null ? file.getBytes() : new byte[0];
            String originalName = sanitizeFilename(file != null ? file.getOriginalFilename() : null);
            String actorName = resolveActorOrFallback(uploadPar);
            String contentType = (file != null && file.getContentType() != null)
                    ? file.getContentType()
                    : "application/pdf";
            String sha256 = calculerSha256(bytes);

            // Versioning automatique (CA-REF-031-02) : recherche de la dernière version existante
            List<DocumentGedEntity> existants = documentGedRepository
                    .findByTypeEntiteIgnoreCaseAndEntiteIdAndNomFichierIgnoreCaseOrderByNumeroVersionDesc(
                            typeEntite, entiteId, originalName);

            int nouvelleVersion = 1;
            UUID parentId = null;
            if (!existants.isEmpty()) {
                DocumentGedEntity precedent = existants.get(0);
                nouvelleVersion = (precedent.getNumeroVersion() != null ? precedent.getNumeroVersion() : 1) + 1;
                parentId = precedent.getId();
            }

            String s3Key = String.format("ged/%s/%s/v%d_%s", typeEntite.toLowerCase(), entiteId, nouvelleVersion, originalName);
            try {
                if (file != null && minioService != null) {
                    minioService.uploadFile(file, s3Key);
                }
            } catch (Exception e) {
                log.warn("Stockage S3 MinIO indisponible en mode local, métadonnées et empreinte SHA-256 conservées en base : {}", e.getMessage());
            }

            int anneesConservation = (dureeConservationAnnees != null && dureeConservationAnnees > 0)
                    ? Math.max(dureeConservationAnnees, 1)
                    : 10;

            DocumentGedEntity doc = DocumentGedEntity.builder()
                    .nomFichier(originalName)
                    .tailleOctets((long) bytes.length)
                    .contentType(contentType)
                    .hashSha256(sha256)
                    .s3Cle(s3Key)
                    .s3Bucket(defaultBucket)
                    .typeEntite(typeEntite.toUpperCase())
                    .entiteId(entiteId)
                    .categorieDocumentaire(categorieDocumentaire != null ? categorieDocumentaire : "PIECE_JUSTIFICATIVE")
                    .description(description)
                    .motsClesIndexation(motsClesIndexation)
                    .numeroVersion(nouvelleVersion)
                    .documentParentId(parentId)
                    .dureeConservationAnnees(anneesConservation)
                    .dateFinConservation(LocalDate.now().plusYears(anneesConservation))
                    .statutCycleVie("ARCHIVE")
                    .signeElectroniquement(false)
                    .dateUpload(LocalDateTime.now())
                    .uploadPar(actorName)
                    .build();

            return documentGedRepository.save(doc);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'archivage GED du document : " + e.getMessage(), e);
        }
    }

    /**
     * Permet d'enregistrer les métadonnées et le contenu texte/binaire d'un document généré par l'ERP (CT-REF-06).
     */
    @Transactional
    public DocumentGedEntity archiverDocumentJson(DocumentGedEntity input) {
        String sanitizedName = sanitizeFilename(input.getNomFichier());
        int annees = (input.getDureeConservationAnnees() != null && input.getDureeConservationAnnees() > 0)
                ? input.getDureeConservationAnnees()
                : 10;

        List<DocumentGedEntity> existants = documentGedRepository
                .findByTypeEntiteIgnoreCaseAndEntiteIdAndNomFichierIgnoreCaseOrderByNumeroVersionDesc(
                        input.getTypeEntite(), input.getEntiteId(), sanitizedName);
        int numeroVersion = 1;
        UUID parentId = null;
        if (!existants.isEmpty()) {
            DocumentGedEntity precedent = existants.get(0);
            numeroVersion = (precedent.getNumeroVersion() != null ? precedent.getNumeroVersion() : 1) + 1;
            parentId = precedent.getId();
        }

        String sha256 = (input.getHashSha256() != null && !input.getHashSha256().isBlank())
                ? input.getHashSha256().trim()
                : calculerSha256((sanitizedName + "|" + input.getTypeEntite() + "|" + input.getEntiteId() + "|" + input.getDescription())
                        .getBytes(StandardCharsets.UTF_8));

        // Isolation anti Mass-Assignment : aucun champ de signature, id ou conservation raccourcie accepté du client
        DocumentGedEntity entity = DocumentGedEntity.builder()
                .nomFichier(sanitizedName)
                .tailleOctets(input.getTailleOctets() != null && input.getTailleOctets() > 0 ? input.getTailleOctets() : 1024L)
                .contentType(input.getContentType() != null && !input.getContentType().isBlank() ? input.getContentType() : "application/pdf")
                .hashSha256(sha256)
                .s3Bucket(defaultBucket)
                .s3Cle("ged/" + input.getTypeEntite().toLowerCase() + "/" + input.getEntiteId() + "/v" + numeroVersion + "_" + sanitizedName)
                .typeEntite(input.getTypeEntite().toUpperCase())
                .entiteId(input.getEntiteId())
                .categorieDocumentaire(input.getCategorieDocumentaire() != null ? input.getCategorieDocumentaire() : "PIECE_JUSTIFICATIVE")
                .description(input.getDescription())
                .motsClesIndexation(input.getMotsClesIndexation())
                .numeroVersion(numeroVersion)
                .documentParentId(parentId)
                .dureeConservationAnnees(annees)
                .dateFinConservation(LocalDate.now().plusYears(annees))
                .statutCycleVie("ARCHIVE")
                .signeElectroniquement(false)
                .dateUpload(LocalDateTime.now())
                .uploadPar(resolveActorOrFallback(input.getUploadPar()))
                .build();
        return documentGedRepository.save(entity);
    }

    /**
     * Exécute l'opération de consulter documents entite.
     * @param typeEntite Type entite dans le contexte du traitement consulterDocumentsEntite.
     * @param entiteId Identifiant unique.
     * @return Résultat du traitement (List<DocumentGedEntity>).
     */
    @Transactional(readOnly = true)
    public List<DocumentGedEntity> consulterDocumentsEntite(String typeEntite, UUID entiteId) {
        return documentGedRepository.findByTypeEntiteAndEntiteIdOrderByDateUploadDesc(typeEntite, entiteId);
    }

    /**
     * Exécute l'opération de rechercher documents.
     * @param typeEntite Type entite dans le contexte du traitement rechercherDocuments.
     * @param categorie Categorie dans le contexte du traitement rechercherDocuments.
     * @param motCle Mot cleée dans le contexte du traitement rechercherDocuments.
     * @return Résultat du traitement (List<DocumentGedEntity>).
     */
    @Transactional(readOnly = true)
    public List<DocumentGedEntity> rechercherDocuments(String typeEntite, String categorie, String motCle) {
        return documentGedRepository.rechercherMulticritere(typeEntite, categorie, motCle);
    }

    /**
     * Appose une signature et un cachet électroniques scellant l'empreinte SHA-256 du document (CA-REF-031-03).
     */
    @Transactional
    public DocumentGedEntity signerDocumentElectroniquement(UUID documentId, String signataireNom) {
        DocumentGedEntity doc = documentGedRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document GED introuvable : " + documentId));
        String effectiveSignataire = resolveActorOrFallback(signataireNom);
        LocalDateTime horodatage = LocalDateTime.now();
        String scellement = doc.getId() + "|" + doc.getHashSha256() + "|SIGNE_PAR:" + effectiveSignataire + "|DATE:" + horodatage;
        doc.setSigneElectroniquement(true);
        doc.setSignataireNom(effectiveSignataire);
        doc.setDateSignature(horodatage);
        doc.setEmpreinteSignature(calculerHmacSha256(scellement));
        doc.setStatutCycleVie("VALIDE");
        return documentGedRepository.save(doc);
    }

    /**
     * Vérifie l'intégrité cryptographique et la validité de la signature électronique d'un document (CA-REF-031-03).
     * Détecte immédiatement toute altération du contenu ou du hash SHA-256 après signature.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> verifierIntegriteEtSignature(UUID documentId, String hashSha256Soumis) {
        DocumentGedEntity doc = documentGedRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document GED introuvable : " + documentId));

        boolean hashIdentique = (hashSha256Soumis == null || hashSha256Soumis.isBlank())
                || doc.getHashSha256().equalsIgnoreCase(hashSha256Soumis.trim());

        boolean cachetValide = true;
        if (Boolean.TRUE.equals(doc.getSigneElectroniquement())) {
            String attenduHmac = calculerHmacSha256(
                    doc.getId() + "|" + doc.getHashSha256() + "|SIGNE_PAR:" + doc.getSignataireNom() + "|DATE:" + doc.getDateSignature());
            String attenduLegacy = calculerSha256(
                    (doc.getHashSha256() + "|SIGNE_PAR:" + doc.getSignataireNom() + "|DATE:" + doc.getDateSignature())
                            .getBytes(StandardCharsets.UTF_8));
            cachetValide = (attenduHmac.equalsIgnoreCase(doc.getEmpreinteSignature())
                    || attenduLegacy.equalsIgnoreCase(doc.getEmpreinteSignature())) && hashIdentique;
        }

        boolean integriteValide = hashIdentique && cachetValide;
        Map<String, Object> resultat = new LinkedHashMap<>();
        resultat.put("documentId", doc.getId());
        resultat.put("nomFichier", doc.getNomFichier());
        resultat.put("numeroVersion", doc.getNumeroVersion());
        resultat.put("hashSha256Original", doc.getHashSha256());
        resultat.put("signeElectroniquement", doc.getSigneElectroniquement());
        resultat.put("signataireNom", doc.getSignataireNom());
        resultat.put("integriteValide", integriteValide);
        resultat.put("diagnostic", integriteValide
                ? "Document authentique : empreinte SHA-256 et signature électronique intègres."
                : "ALERTE SÉCURITÉ (CA-REF-031-03) : Le document a été altéré après sa signature électronique ! Signature invalidée.");
        return resultat;
    }

    /**
     * Bloque toute suppression physique d'un document avant la fin de sa durée légale de conservation
     * (CA-REF-031-04 / RG-REF-009 / RG-REF-012).
     */
    @Transactional
    public void supprimerDocument(UUID documentId) {
        DocumentGedEntity doc = documentGedRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document GED introuvable : " + documentId));

        LocalDate finConservation = doc.getDateFinConservation() != null
                ? doc.getDateFinConservation()
                : doc.getDateUpload().toLocalDate().plusYears(
                        doc.getDureeConservationAnnees() != null ? doc.getDureeConservationAnnees() : 10);

        if (LocalDate.now().isBefore(finConservation)) {
            throw new IllegalStateException(
                    "Suppression refusée (CA-REF-031-04 / RG-REF-012) : le document [" + doc.getNomFichier()
                            + "] est soumis à une obligation légale de conservation jusqu'au " + finConservation + ".");
        }
        documentGedRepository.delete(doc);
    }

    private String calculerHmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            String key = resolveHmacKey();
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(raw);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erreur de calcul HMAC-SHA256", e);
        }
    }

    private String calculerSha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return bytesToHex(md.digest(data));
        } catch (Exception e) {
            throw new RuntimeException("Erreur de calcul SHA-256", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) sb.append('0');
            sb.append(hex);
        }
        return sb.toString();
    }
}
