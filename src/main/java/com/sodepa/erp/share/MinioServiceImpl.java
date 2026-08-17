package com.sodepa.erp.share;

import com.sodepa.erp.configuration.MinioProperties;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of MinIO service for object storage operations
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "minio", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MinioServiceImpl implements MinioService {
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /**
     * Allowed file extensions for security
     */
    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            "jpg", "jpeg", "png", "gif", "pdf", "doc", "docx",
            "xls", "xlsx", "ppt", "pptx", "txt", "csv", "mp4",
            "avi", "mp3", "zip", "rar");

    public MinioServiceImpl(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    @Override
    public String uploadFile(MultipartFile file, String objectName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        // Generate unique object name if not provided
        if (objectName == null || objectName.isBlank()) {
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            objectName = generateUniqueFileName(extension);
        }

        // Validate file extension
        String extension = getFileExtension(objectName);
        if (!isAllowedExtension(extension)) {
            throw new IllegalArgumentException("File type not allowed: " + extension);
        }

        // Validate MIME type
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        log.info("Uploading file to MinIO: {} (size: {} bytes, type: {})",objectName, file.getSize(), contentType);

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(contentType)
                            .build());

            log.info("File uploaded successfully: {}", objectName);
            // Retourne l'URL publique directe : {endpoint}/{bucket}/{objectName}
            String endpoint = minioProperties.getEndpoint();
            // Supprimer le slash final de l'endpoint s'il y en a un
            if (endpoint.endsWith("/")) {
                endpoint = endpoint.substring(0, endpoint.length() - 1);
            }
            return minioProperties.getFileurl() + "/" + minioProperties.getBucketName() + "/" + objectName;
        } catch (Exception e) {
            log.error("Error uploading file to MinIO: {}", objectName, e);
            throw new RuntimeException("Failed to upload file: " + objectName, e);
        }
    }

    @Override
    public InputStream downloadFile(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            throw new IllegalArgumentException("Object name cannot be null or empty");
        }

        log.info("Downloading file from MinIO: {}", objectName);

        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            log.error("Error downloading file from MinIO: {}", objectName, e);
            throw new RuntimeException("Failed to download file: " + objectName, e);
        }
    }

    @Override
    public void deleteFile(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            throw new IllegalArgumentException("Object name cannot be null or empty");
        }

        log.info("Deleting file from MinIO: {}", objectName);

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .build());

            log.info("File deleted successfully: {}", objectName);
        } catch (Exception e) {
            // log.error("Error deleting file from MinIO: {}", objectName, e);
            throw new RuntimeException("Failed to delete file: " + objectName, e);
        }
    }

    @Override
    public String getFileUrl(String objectName, Duration expiry) {
        if (objectName == null || objectName.isBlank()) {
            throw new IllegalArgumentException("Object name cannot be null or empty");
        }

        // Default expiry: 7 days
        if (expiry == null) {
            expiry = Duration.ofDays(7);
        }

        // log.info("Generating pre-signed URL for: {} (expiry: {})", objectName,
        // expiry);

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .expiry((int) expiry.getSeconds(), TimeUnit.SECONDS)
                            .build());
        } catch (Exception e) {
            log.error("Error generating pre-signed URL for: {}", objectName, e);
            throw new RuntimeException("Failed to generate URL for: " + objectName, e);
        }
    }

    @Override
    public List<String> listFiles(String prefix) {
        log.info("Listing files in MinIO with prefix: {}", prefix);

        List<String> fileNames = new ArrayList<>();

        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .prefix(prefix)
                            .recursive(true)
                            .build());

            for (Result<Item> result : results) {
                Item item = result.get();
                fileNames.add(item.objectName());
            }

            // log.info("Found {} files with prefix: {}", fileNames.size(), prefix);
            return fileNames;
        } catch (Exception e) {
            // log.error("Error listing files in MinIO", e);
            throw new RuntimeException("Failed to list files", e);
        }
    }

    @Override
    public boolean fileExists(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            return false;
        }

        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public long getFileSize(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            throw new IllegalArgumentException("Object name cannot be null or empty");
        }

        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .build());
            return stat.size();
        } catch (Exception e) {
            log.error("Error getting file size: {}", objectName, e);
            throw new RuntimeException("Failed to get file size: " + objectName, e);
        }
    }

    @Override
    public String uploadBytes(byte[] data, String objectName, String contentType) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }
        if (objectName == null || objectName.isBlank()) {
            throw new IllegalArgumentException("Object name cannot be null or empty");
        }
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        try (InputStream inputStream = new java.io.ByteArrayInputStream(data)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .stream(inputStream, data.length, -1)
                            .contentType(contentType)
                            .build());

            String endpoint = minioProperties.getEndpoint();
            if (endpoint.endsWith("/")) {
                endpoint = endpoint.substring(0, endpoint.length() - 1);
            }
            return minioProperties.getFileurl() + "/" + minioProperties.getBucketName() + "/" + objectName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload bytes: " + objectName, e);
        }
    }

    /**
     * Generate a unique file name with timestamp and UUID
     */
    private String generateUniqueFileName(String extension) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s_%s.%s", timestamp, uuid, extension);
    }

    /**
     * Extract file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * Check if file extension is allowed
     */
    private boolean isAllowedExtension(String extension) {
        return ALLOWED_EXTENSIONS.contains(extension.toLowerCase());
    }

    @Override
    public byte[] getFileTest(String fileId) {
        InputStream stream = null;
        try {
            stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fileId)
                            .build());
            return stream.readAllBytes();
        } catch (ErrorResponseException e) {
            throw new RuntimeException(e);
        } catch (InsufficientDataException e) {
            throw new RuntimeException(e);
        } catch (InternalException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (InvalidResponseException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (ServerException e) {
            throw new RuntimeException(e);
        } catch (XmlParserException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveFileTest(String fileId, byte[] content) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fileId)
                            .stream(new ByteArrayInputStream(content), content.length, -1)
                            .contentType("application/octet-stream")
                            .build());
        } catch (ErrorResponseException e) {
            throw new RuntimeException(e);
        } catch (InsufficientDataException e) {
            throw new RuntimeException(e);
        } catch (InternalException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (InvalidResponseException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (ServerException e) {
            throw new RuntimeException(e);
        } catch (XmlParserException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getFileSizeTest(String fileId) {
        StatObjectResponse stat = null;
        try {
            stat = minioClient.statObject(
                    StatObjectArgs.builder().bucket(minioProperties.getBucketName()).object(fileId).build());
        } catch (ErrorResponseException e) {
            throw new RuntimeException(e);
        } catch (InsufficientDataException e) {
            throw new RuntimeException(e);
        } catch (InternalException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (InvalidResponseException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (ServerException e) {
            throw new RuntimeException(e);
        } catch (XmlParserException e) {
            throw new RuntimeException(e);
        }
        return stat.size();
    }
}

