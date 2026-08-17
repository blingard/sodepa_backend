package com.sodepa.erp.configuration;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Validated
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /**
     * MinIO server endpoint (e.g., http://localhost:9000)
     */
    @NotBlank(message = "MinIO endpoint is required")
    private String endpoint;
    @NotBlank(message = "MinIO endpoint fil is required")
    private String fileurl;

    /**
     * Access key for MinIO authentication
     */
    @NotBlank(message = "MinIO access key is required")
    private String accessKey;

    /**
     * Secret key for MinIO authentication
     */
    @NotBlank(message = "MinIO secret key is required")
    private String secretKey;

    /**
     * Default bucket name for storing files
     */
    @NotBlank(message = "MinIO bucket name is required")
    private String bucketName;

    /**
     * Region for the MinIO server
     * Default: us-east-1
     */
    private String region = "us-east-1";

    /**
     * Enable/disable MinIO integration
     * Default: true
     */
    private boolean enabled = true;

    public MinioProperties() {
    }

    public MinioProperties(String endpoint, String fileurl, String accessKey, String secretKey, String bucketName, String region, boolean enabled) {
        this.endpoint = endpoint;
        this.fileurl = fileurl;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.bucketName = bucketName;
        this.region = region;
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFileurl() {
        return fileurl;
    }

    public void setFileurl(String fileurl) {
        this.fileurl = fileurl;
    }
}

