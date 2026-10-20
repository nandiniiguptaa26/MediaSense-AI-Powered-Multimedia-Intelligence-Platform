package com.mediasense.converter.download.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class DownloadService {
    private final MinioClient minioClient;
    private final String bucketName;

    public DownloadService(MinioClient minioClient,
                           @Value("${app.minio.bucket-name}") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    public ResponseEntity<InputStreamResource> download(String blobName,
                                                        String mimeType,
                                                        String outputFormat) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(blobName)
                    .build());
        } catch(ErrorResponseException e) {
            return ResponseEntity.notFound().build();
        } catch(Exception e) {
            return ResponseEntity.internalServerError().build();
        }

        try {
            InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(blobName)
                            .build()
            );

            String filename = buildFilename(blobName, outputFormat);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    mimeType != null ? mimeType : "application/octet-stream"
            ));
            headers.setContentDisposition(
                    ContentDisposition.attachment().filename(filename).build());

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .body(new InputStreamResource(inputStream));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String buildFilename(String blobName, String outputFormat) {
        if (outputFormat == null || outputFormat.isBlank()) {
            return blobName;
        }
        int dotIndex = blobName.lastIndexOf('.');
        String base = dotIndex > 0 ? blobName.substring(0, dotIndex) : blobName;
        return base + "." + outputFormat;
    }
}
