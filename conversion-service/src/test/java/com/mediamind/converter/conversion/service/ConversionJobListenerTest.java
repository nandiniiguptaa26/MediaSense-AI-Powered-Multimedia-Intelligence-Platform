//package com.mediamind.converter.conversion.service;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.mediamind.converter.conversion.config.GCSConfig;
//import com.mediamind.converter.dto.JobPayload;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.ArgumentCaptor;
//
//import static org.mockito.Mockito.*;
//
//class ConversionJobListenerTest {
//    private MediaConversionService mediaConversionService;
//    private ObjectMapper objectMapper;
//    private GCSConfig gcsConfig;
//    private ConversionJobListener listener;
//
//    @BeforeEach
//    void setup() {
//        mediaConversionService = mock(MediaConversionService.class);
//        objectMapper = new ObjectMapper();
//        gcsConfig = mock(GCSConfig.class);
//
//        when(gcsConfig.getInputBucket()).thenReturn("input-bucket");
//        when(gcsConfig.getOutputBucket()).thenReturn("output-bucket");
//
//        listener = new ConversionJobListener(
//                mediaConversionService,
//                objectMapper,
//                null,
//                gcsConfig
//        );
//    }
//
//    @Test
//    void testSuccessfulConversion() throws Exception {
//        JobPayload job = new JobPayload();
//        job.setBlobName("video.mp4");
//        job.setOutputFormat("mp4");
//
//        listener.listen(job);
//
//        ArgumentCaptor<Long> estSize = ArgumentCaptor.forClass(Long.class);
//        ArgumentCaptor<String> inputBucket = ArgumentCaptor.forClass(String.class);
//        ArgumentCaptor<String> outputBucket = ArgumentCaptor.forClass(String.class);
//        ArgumentCaptor<String> blobName = ArgumentCaptor.forClass(String.class);
//        ArgumentCaptor<String> mimeType = ArgumentCaptor.forClass(String.class);
//        ArgumentCaptor<String> format = ArgumentCaptor.forClass(String.class);
//
//        verify(mediaConversionService, times(1)).convertMedia(
//                estSize.capture(),
//                inputBucket.capture(),
//                outputBucket.capture(),
//                blobName.capture(),
//                mimeType.capture(),
//                format.capture(),
//                isNull(),
//                isNull()
//        );
//
//        // Assertions
//        assert estSize.getValue() == 0L;
//        assert inputBucket.getValue().equals("input-bucket");
//        assert outputBucket.getValue().equals("output-bucket");
//        assert blobName.getValue().equals("video.mp4");
//        assert mimeType.getValue().equals("video/mp4"); // Resolved by MimeTypeUtil class
//        assert format.getValue().equals("mp4");
//    }
//
//    @Test
//    void testConversionFailureIsHandled() throws Exception {
//        JobPayload job = new JobPayload();
//        job.setBlobName("badfile.avi");
//        job.setOutputFormat("mp4");
//
//        doThrow(new RuntimeException("FFmpeg failed"))
//                .when(mediaConversionService)
//                .convertMedia(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString(), any(), any());
//
//        listener.listen(job);
//
//        // Listener should NOT rethrow the exception
//        verify(mediaConversionService, times(1)).convertMedia(
//                anyLong(), anyString(), anyString(), anyString(), anyString(), anyString(), any(), any()
//        );
//    }
//
//    @Test
//    void testMimeTypeIsResolved() throws Exception {
//        JobPayload job = new JobPayload();
//        job.setBlobName("clip.mov");
//        job.setOutputFormat("mov");
//
//        listener.listen(job);
//
//        ArgumentCaptor<String> mimeType = ArgumentCaptor.forClass(String.class);
//
//        verify(mediaConversionService).convertMedia(
//                anyLong(),
//                anyString(),
//                anyString(),
//                anyString(),
//                mimeType.capture(),
//                anyString(),
//                any(),
//                any()
//        );
//
//        assert mimeType.getValue().equals("video/quicktime");
//    }
//
//    @Test
//    void testMissingFieldsDoNotCrashListener() throws Exception {
//        JobPayload job = new JobPayload(); // empty Payload
//
//        listener.listen(job);
//
//        // convertMedia should still be called with nulls
//        verify(mediaConversionService).convertMedia(
//                anyLong(),
//                anyString(),
//                anyString(),
//                isNull(),
//                anyString(),
//                isNull(),
//                any(),
//                any()
//        );
//    }
//}
