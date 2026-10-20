package com.mediasense.converter.upload.service;

import com.mediasense.converter.dto.JobPayload;
import com.mediasense.converter.upload.config.UploadProperties;
import com.mediasense.converter.upload.exception.FileValidationException;
import com.mediasense.converter.upload.exception.UploadFailureException;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class FileStorageService {
    private final UploadProperties properties;
    private final MinioClient minioClient;
    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${app.topic.conversion-jobs}")
    private String conversionJobsTopic;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private StorageUploader uploader;

    public FileStorageService(UploadProperties properties, MinioClient minioClient) {
        this.properties = properties;
        this.minioClient = minioClient;
    }

    public String store(MultipartFile file, String outputFormat) {
        return storeInternal(file, null, outputFormat); // default behavior
    }

    public String storeWithFixedId(MultipartFile file, String fileIdOverride, String outputFormat) {
        return storeInternal(file, fileIdOverride, outputFormat); // used only in tests
    }

    public String storeInternal(MultipartFile file, String fileIdOverride, String outputFormat) {
        String originalFilename = file.getOriginalFilename();
        String ext = getExtension(originalFilename).toLowerCase();
        String mimeType = file.getContentType();

        // Validate extension
        if(!properties.getAllowedExtensions().contains(ext)) {
            logger.warn("Rejected file: {} with unsupported extension: {} ", originalFilename, ext);
            throw new FileValidationException(originalFilename, "Unsupported file extension: " + ext);
        }

        // Validate MIME type
        if(!properties.getAllowedMimetypes().contains(mimeType)) {
            logger.warn("Rejected file: {} with unsupported MIME type: {} ", originalFilename, mimeType);
            throw new FileValidationException(originalFilename, "Unsupported MIME type: " + mimeType);
        }

        try {
            String blobName = fileIdOverride != null ? fileIdOverride : resolveBlobNameWithVersioning(originalFilename);

            if (!properties.isAllowOverwrite() && objectExists(properties.getBucketName(), blobName)) {
                logger.warn("File {} already exists in bucket {}", blobName, properties.getBucketName());
                throw new FileValidationException(originalFilename, "File " + blobName + " already exists in storage");
            }

            uploader.uploadToStorage(file, blobName, mimeType, properties.getBucketName(), minioClient);

            // Publish kafka event
            if (outputFormat != null) {
                logger.info("blobName: {}, mimeType: {}, outputFormat: {}", blobName, mimeType, outputFormat);
                JobPayload job = new JobPayload(blobName, mimeType, outputFormat);
                kafkaTemplate.send(conversionJobsTopic, blobName, job);
                logger.info("Published conversion job for {}", blobName);
            }

            return blobName;
        } catch (IOException e) {
            throw new UploadFailureException(originalFilename, "Upload failed");
        }
    }

    private boolean objectExists(String bucketName, String objectName) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            return false;
        } catch(Exception e) {
            return false;
        }
    }

    private String getExtension(String fileName) {
        return fileName.contains(".")
                ? fileName.substring(fileName.lastIndexOf('.') + 1)
                : "";
    }

    private String resolveBlobNameWithVersioning(String originalName) {
        String ext = getExtension(originalName);
        String baseName = originalName.contains(".")
                ? originalName.substring(0, originalName.lastIndexOf('.'))
                : originalName;

        String blobName = originalName;
        int version = 1;

        while (objectExists(properties.getBucketName(), blobName)) {
            version++;
            blobName = baseName + "_v" + version + (ext.isEmpty() ? "" : "." + ext);
        }

        return blobName;
    }
}
