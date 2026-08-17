package com.sodepa.erp.share;

import com.sodepa.erp.utils.MyFileNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Service de stockage de fichiers utilisant MinIO comme backend.
 * Remplace l'ancienne implémentation basée sur le système de fichiers local.
 */
@Slf4j
@Service
public class FileStorageService {
    private final MinioService minioService;

    // Préfixes MinIO par type de fichier
    private static final String PREFIX_DEFAULT = "uploads/";

    public FileStorageService(MinioService minioService) {
        this.minioService = minioService;
    }

    // -------------------------------------------------------------------------
    // Upload
    // -------------------------------------------------------------------------

    /**
     * Upload un fichier générique vers MinIO.
     *
     * @param file fichier à uploader
     * @return URL publique directe du fichier (ex:
     *         http://minio:9000/bucket/uploads/fichier.jpg)
     */
    public String storeFile(MultipartFile file)  {
        String fileName = validateAndGetName(file);
        String objectName = PREFIX_DEFAULT + fileName;
        log.info("Stockage du fichier dans MinIO : {}", objectName);
        try {
            return minioService.uploadFile(file, objectName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Upload une image de paragraphe vers un sous-préfixe MinIO.
     *
     * @param file        fichier image
     * @param fileSubPath préfixe/sous-répertoire dans le bucket
     * @return URL publique directe du fichier uploadé
     */
    public String storeFileImage(MultipartFile file, String fileSubPath)  {
        String fileName = validateAndGetName(file);
        String prefix = fileSubPath != null && !fileSubPath.isBlank()
                ? fileSubPath.replaceAll("^/", "") + "/"
                : PREFIX_DEFAULT;
        String objectName = prefix + fileName;
        log.info("Stockage de l'image paragraphe dans MinIO : {}", objectName);
        try {
            return minioService.uploadFile(file, objectName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // -------------------------------------------------------------------------
    // Lecture / Visualisation
    // -------------------------------------------------------------------------

    /**
     * Récupère un flux d'entrée pour un fichier stocké dans MinIO.
     * À utiliser dans le controller pour streamer la réponse vers le client.
     *
     * @param objectName chemin de l'objet dans MinIO
     * @return InputStream du fichier
     */
    public InputStream getFileInputStream(String objectName)  {
        if (objectName == null || objectName.isBlank()) {
            throw new MyFileNotFoundException("Nom de fichier manquant");
        }
        try {
            log.info("Récupération du flux MinIO pour : {}", objectName);
            return minioService.downloadFile(objectName);
        } catch (Exception e) {
            throw new MyFileNotFoundException("Fichier introuvable dans MinIO : " + objectName, e);
        }
    }

    /**
     * Génère une URL pré-signée MinIO donnant un accès temporaire à un fichier.
     *
     * @param objectName    chemin de l'objet dans MinIO
     * @param durationHours durée de validité en heures (0 → défaut 24 h)
     * @return URL pré-signée
     */
    public String getPresignedUrl(String objectName, long durationHours)  {
        Duration expiry = durationHours > 0 ? Duration.ofHours(durationHours) : Duration.ofHours(24);
        log.info("Génération URL pré-signée pour {} (expiry: {}h)", objectName, expiry.toHours());
        try {
            return minioService.getFileUrl(objectName, expiry);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    // -------------------------------------------------------------------------
    // Suppression
    // -------------------------------------------------------------------------

    /**
     * Supprime un fichier du bucket MinIO.
     *
     * @param objectName chemin de l'objet à supprimer
     */
    public void deleteFile(String objectName)  {
        log.info("Suppression du fichier MinIO : {}", objectName);
        try {
            minioService.deleteFile(objectName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // -------------------------------------------------------------------------
    // Utilitaires privés
    // -------------------------------------------------------------------------

    /**
     * Valide le fichier et génère un nom unique horodaté.
     */
    private String validateAndGetName(MultipartFile file) {
        String original = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        if (original.contains("..")) {
            throw new RuntimeException("Nom de fichier invalide : " + original);
        }
        String ext = getExtension(original);
        return System.currentTimeMillis() + (ext.isBlank() ? "" : "." + ext);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains("."))
            return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    public Map<String, String> saveFIle(MultipartFile file)  {
        Map<String, String> data = new HashMap<>();
        String fileName = validateAndGetName(file);
        String objectName = PREFIX_DEFAULT + fileName;
        data.put("name", objectName);
        String[] extensions = objectName.split("\\.");
        data.put("extension", extensions[extensions.length-1]);
        String path = null;
        try {
            path = minioService.uploadFile(file, objectName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        data.put("path", path);
        return data;
    }
}
