package com.mediamind.converter.conversion.controller;

import com.mediamind.converter.conversion.config.MinioConfig;
import com.mediamind.converter.conversion.service.*;
import com.mediamind.converter.conversion.util.MimeTypeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;

@RestController
@RequestMapping("/convert")
public class ConversionController {
    @Autowired
    private MediaConversionService conversionService;
    @Autowired
    private GcsHelper gcsHelper;
    @Autowired
    private MinioConfig minioConfig;
    @Autowired
    private WaveformVideoService waveformVideoService;
    @Autowired
    private CaptionVideoService captionVideoService;
    @Autowired
    private TranscriptionService transcriptionService;

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    public ResponseEntity<StreamingResponseBody> convertFromGCS(
            @RequestPart("blobName") String blobName,
            @RequestPart("format") String format,
            @RequestPart(value = "mimeType", required = false) String mimeType,
            @RequestPart(value = "codecs", required = false) Map<String, String> codecs,
            @RequestPart(value = "arguments", required = false) Map<String, String> arguments
    ) {
        String convertedBlobName = blobName + "_converted." + format;

        // Automatically resolve MIME type from format
        String mimeTypeResult = MimeTypeUtil.resolveMimeType(format, mimeType);

        StreamingResponseBody responseBody = outputStream -> {
            conversionService.convertMedia(
                    1L,
                    minioConfig.getInputBucket(),
                    minioConfig.getOutputBucket(),
                    blobName,
                    mimeTypeResult,
                    format,
                    codecs,
                    arguments
            );

            // Always read the final converted file from GCS
            try (InputStream gcsStream =
                         gcsHelper.fetchFromGCS(minioConfig.getOutputBucket(), convertedBlobName)) {
                gcsStream.transferTo(outputStream);
            }
        };
        return ResponseEntity.ok()
                .header(
                        "Content-Disposition",
                        "attachment; filename=\"" + convertedBlobName + "\""
                )
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(responseBody);
    }

    @PostMapping(
            value = "/waveform",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    public ResponseEntity<StreamingResponseBody> generateWaveform(
            @RequestPart("blobName") String blobName,
            @RequestPart(value = "backgroundImage", required = false) MultipartFile backgroundImage
    ) {
        String outputBlobName = blobName + "_waveform.mp4";

        StreamingResponseBody responseBody = outputStream -> {
            try {
                waveformVideoService.generateWaveformVideo(
                        minioConfig.getInputBucket(),
                        minioConfig.getOutputBucket(),
                        blobName,
                        backgroundImage
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            try (InputStream gcsStrem =
                    gcsHelper.fetchFromGCS(minioConfig.getOutputBucket(), outputBlobName)) {
                gcsStrem.transferTo(outputStream);
            } catch (java.io.IOException e) {
                if(e.getMessage() != null && (e.getMessage().contains("aborted") || e.getMessage().contains("Broken pipe") || e.getMessage().contains("Connection reset"))) {
                    return;
                }
                throw e;
            }
        };

        return ResponseEntity.ok()
                .header(
                        "Content-Disposition",
                        "attachment; filename=\"" + outputBlobName + "\""
                )
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(responseBody);
    }

    @PostMapping(
            value = "/caption",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    public ResponseEntity<StreamingResponseBody> generateCaptionVideo(
            @RequestPart("blobName") String blobName
    ) {
        String outputBlobName = blobName + "_captioned.mp4";

        StreamingResponseBody responseBody = outputStream -> {
            try {
                captionVideoService.generateCaptionVideo(
                        minioConfig.getInputBucket(),
                        minioConfig.getOutputBucket(),
                        blobName
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            try(InputStream gcsStream = gcsHelper.fetchFromGCS(minioConfig.getOutputBucket(), outputBlobName)) {
                gcsStream.transferTo(outputStream);
            }
        };

        return ResponseEntity.ok()
                .header(
                        "Content-Disposition",
                        "attachment; filename=\"" + outputBlobName + "\""
                )
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(responseBody);
    }

    @PostMapping(
            value = "/transcribe",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public ResponseEntity<StreamingResponseBody> transcribe(
        @RequestPart("blobName") String blobName
    ) {
        String outputBlobName = blobName + "transcription.txt";

        StreamingResponseBody responseBody = outputStream -> {
            try {
                byte[] txtContent = transcriptionService.transcribe(
                        minioConfig.getInputBucket(),
                        minioConfig.getOutputBucket(),
                        blobName
                );
                outputStream.write(txtContent);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        return ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename=\"" + outputBlobName + "\""
                )
                .contentType(MediaType.TEXT_PLAIN)
                .body(responseBody);
    }
}
