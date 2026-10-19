package com.mediamind.converter.upload.service;


import com.mediamind.converter.upload.exception.UploadFailureException;
import com.mediamind.converter.upload.websocket.UploadProgressHandler;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Component
public class StorageUploader {

    @Autowired
    private UploadProgressHandler progressHandler;

    private static final Logger logger = LoggerFactory.getLogger(StorageUploader.class);

    @Retryable(
            retryFor = { IOException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void uploadToStorage(MultipartFile file, String objectName, String contentType,
                                String bucketName, MinioClient minioClient) throws IOException {
        long fileSize = file.getSize();
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, fileSize, -1)
                            .contentType(contentType)
                            .build()
            );
            progressHandler.broadcastProgress(file.getOriginalFilename(), 100.0, fileSize);
            logger.info("Uploaded {} to MinIO bucket {}", objectName, bucketName);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("MinIO Upload failed: " +  e.getMessage(), e);
        }
    }

    @Recover
    public void recover(IOException e, MultipartFile file, String objectName, String contentType,
                                        String bucketName, MinioClient minioClient) {
        String filename = file.getOriginalFilename();
        logger.error("Upload failed after retries for file: {}", filename, e);
        progressHandler.broadcastFailure(filename, "Upload failed after multiple attempts");
        throw new UploadFailureException(filename, "Upload failed after multiple attempts");
    }
}
