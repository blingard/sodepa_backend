package com.sodepa.erp.share;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;

public interface MinioService {
    /**
     * Upload a file to MinIO
     *
     * @param file       the file to upload
     * @param objectName the name of the object in MinIO (optional, generated if
     *                   null)
     * @return the public direct URL to access the file (e.g.
     *         http://minio-host:9000/bucket/objectName)
     */
    String uploadFile(MultipartFile file, String objectName) throws Exception;

    /**
     * Download a file from MinIO
     *
     * @param objectName the name of the object in MinIO
     * @return InputStream of the file
     */
    InputStream downloadFile(String objectName) throws Exception;

    /**
     * Delete a file from MinIO
     *
     * @param objectName the name of the object to delete
     */
    void deleteFile(String objectName) throws Exception;

    /**
     * Generate a pre-signed URL for temporary access to a file
     *
     * @param objectName the name of the object
     * @param expiry     expiration duration for the URL
     * @return pre-signed URL
     */
    String getFileUrl(String objectName, Duration expiry) throws Exception;

    /**
     * List all files in the bucket with optional prefix filter
     *
     * @param prefix optional prefix to filter objects
     * @return list of object names
     */
    List<String> listFiles(String prefix) throws Exception;

    /**
     * Check if a file exists in MinIO
     *
     * @param objectName the name of the object
     * @return true if exists, false otherwise
     */
    boolean fileExists(String objectName) throws Exception;

    /**
     * Get file metadata
     *
     * @param objectName the name of the object
     * @return file size in bytes
     */
    long getFileSize(String objectName) throws Exception;

    /**
     * Upload raw bytes to MinIO (used for base64-decoded image data)
     *
     * @param data        the byte array to upload
     * @param objectName  the name of the object in MinIO
     * @param contentType the MIME type of the data
     * @return the public direct URL to access the file
     */
    String uploadBytes(byte[] data, String objectName, String contentType) throws Exception;

    byte[] getFileTest(String fileId) throws Exception;

    void saveFileTest(String fileId, byte[] content) throws Exception;

    long getFileSizeTest(String fileId) throws Exception;
}
