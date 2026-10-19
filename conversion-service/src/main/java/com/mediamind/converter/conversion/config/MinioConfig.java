package com.mediamind.converter.conversion.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {
    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accesskey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${buckets.input-bucket}")
    private String inputBucket;

    @Value("${buckets.output-bucket}")
    private String outputBucket;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accesskey, secretKey)
                .build();
    }

    public String getInputBucket() {
        return inputBucket;
    }

    public String getOutputBucket() {
        return outputBucket;
    }
}
