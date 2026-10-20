package com.mediasense.converter.conversion.service;

import com.mediasense.converter.conversion.config.MinioConfig;
import com.mediasense.converter.conversion.util.MimeTypeUtil;
import com.mediasense.converter.dto.JobPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ConversionJobListener {
    private static final Logger logger = LoggerFactory.getLogger(ConversionJobListener.class);

    private final MediaConversionService mediaConversionService;
    private final MinioConfig minioConfig;

    @Autowired
    public ConversionJobListener(
            MediaConversionService mediaConversionService,
            MinioConfig minioConfig) {
        this.mediaConversionService = mediaConversionService;
        this.minioConfig = minioConfig;
    }

    @KafkaListener(
            topics = "${app.topic.conversion-jobs}",
            groupId = "conversion-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void listen(JobPayload job) throws Exception {
        logger.info("Received job: {}", job);

        // Automatically resolve MIME type from format
        String mimeTypeResult = MimeTypeUtil.resolveMimeType(job.getOutputFormat(), null);
        job.setMimeType(mimeTypeResult);

        String blobName = job.getBlobName();
        String mimeType = job.getMimeType();
        String outputFormat = job.getOutputFormat();

        try {
            mediaConversionService.convertMedia(
                    0L, // estimated size placeholder
                    minioConfig.getInputBucket(),
                    minioConfig.getOutputBucket(),
                    blobName,
                    mimeType,
                    outputFormat,
                    null, // codecs
                    null  // extra args
            );
            logger.info("Conversion completed for {}", blobName);
        } catch (Exception e) {
            logger.error("Conversion failed for " + blobName + ": " + e.getMessage());
        }
    }
}
