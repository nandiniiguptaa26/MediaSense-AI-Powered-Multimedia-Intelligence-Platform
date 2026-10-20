package com.mediasense.converter.conversion.service;

import io.minio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

@Component
public class GcsHelper {

    @Autowired
    private MinioClient minioClient;

    public InputStream fetchFromGCS(String bucketName, String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Object not found in MinIO", e);
        }
    }

    public OutputStream prepareOutputStream(final String bucketName, final String objectName, final String contentType) {
        return new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                super.close();
                byte[] data = this.toByteArray();
                try {
                    minioClient.putObject(
                            PutObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(objectName)
                                    .stream(new ByteArrayInputStream(data), data.length, -1)
                                    .contentType(contentType != null ? contentType : "application/octet-stream")
                                    .build()
                    );
                } catch (Exception e) {
                    throw new RuntimeException("Fialed to upload to MinIO: " + objectName, e);
                }
            }
        };
    }
}
