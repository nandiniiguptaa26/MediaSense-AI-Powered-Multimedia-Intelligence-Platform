//package com.mediasense.converter.upload.service.integration;
//
//
//import com.google.cloud.storage.Storage;
//import com.mediasense.converter.upload.config.UploadProperties;
//import com.mediasense.converter.upload.exception.FileValidationException;
//import com.mediasense.converter.upload.service.FileStorageService;
//import com.mediasense.converter.upload.websocket.UploadProgressHandler;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.test.context.TestPropertySource;
//import org.springframework.web.socket.TextMessage;
//import org.springframework.web.socket.WebSocketSession;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.argThat;
//import static org.mockito.Mockito.*;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//@TestPropertySource(properties = {
//        "upload.bucketName=audio-video-converter-test-files",
//        "upload.allowedExtensions=mp4",
//        "upload.allowedMimetypes=video/mp4"
//})
//public class FileStorageServiceIntegrationTest {
//    @Mock
//    private Storage storage;
//
//    @Mock
//    private UploadProgressHandler progressHandler;
//
//    @Mock
//    private UploadProperties properties;
//
//    @Autowired
//    private FileStorageService fileStorageService;
//
//    @BeforeEach
//    void setup() {
//        MockitoAnnotations.openMocks(this);
//        when(properties.getBucketName()).thenReturn("audio-video-converter-files");
//        when(properties.getAllowedExtensions()).thenReturn(java.util.List.of("mp4"));
//        when(properties.getAllowedMimetypes()).thenReturn(java.util.List.of("video/mp4"));
//        when(properties.isAllowOverwrite()).thenReturn(false);
//    }
//
//    @Test
//    public void testUploadToGCS() throws Exception {
//        // Create a mock MP4 file
//        MockMultipartFile mockFile = new MockMultipartFile(
//                "file",
//                "test-video.mp4",
//                "video/mp4",
//                "dummy video content".getBytes()
//        );
//
//        // Attempt to store the file
//        String fileId = fileStorageService.store(mockFile, null);
//
//        // Assert that a file ID is returned
//        assertNotNull(fileId);
//        assertTrue(fileId.endsWith(".mp4"));
//
//        System.out.println("Uploaded file ID: " + fileId);
//    }
//
//    @Test
//    public void testPreventOverwriteWithFixedID() throws Exception {
//        // Create a mock MP4 file
//        MockMultipartFile mockFile = new MockMultipartFile(
//                "file",
//                "test-video.mp4",
//                "video/mp4",
//                "dummy video content".getBytes()
//        );
//
//        String fixedId = "test-fixed-id.mp4";
//
//        // First upload should succeed
//        String fileId1 = fileStorageService.storeWithFixedId(mockFile, fixedId, null);
//        assertNotNull(fileId1);
//
//        // Second upload with same ID should fail
//        assertThrows(FileValidationException.class, () -> {
//            fileStorageService.storeWithFixedId(mockFile, fixedId, null);
//        });
//    }
//
//    @Test
//    public void testVersioningWhenFileAlreadyExists() throws Exception {
//        MockMultipartFile mockFile = new MockMultipartFile(
//                "file", "versioned-video.mp4", "video/mp4", "dummy video content".getBytes()
//        );
//
//        String firstBlobName = fileStorageService.store(mockFile, null);
//        assertEquals("versioned-video.mp4", firstBlobName);
//
//        String secondBlobName = fileStorageService.store(mockFile, null);
//        assertTrue(secondBlobName.startsWith("versioned-video_v"));
//        assertTrue(secondBlobName.endsWith(".mp4"));
//        assertNotEquals(firstBlobName, secondBlobName);
//
//        System.out.println("First blob: " + firstBlobName);
//        System.out.println("Second blob: " + secondBlobName);
//    }
//
//    @Test
//    public void testOverwriteBlockedWhenFlagIsFalse() throws Exception {
//        MockMultipartFile mockFile = new MockMultipartFile(
//                "file", "overwrite-test.mp4", "video/mp4", "dummy video content".getBytes()
//        );
//
//        String blobName = fileStorageService.storeWithFixedId(mockFile, "overwrite-test.mp4", null);
//        assertEquals("overwrite-test.mp4", blobName);
//
//        assertThrows(FileValidationException.class, () -> fileStorageService.storeWithFixedId(mockFile, "overwrite-test.mp4", null));
//    }
//
//    @Test
//    void testBroadcastFailureSendsMessage() throws Exception {
//        WebSocketSession session = mock(WebSocketSession.class);
//        when(session.isOpen()).thenReturn(true);
//
//        UploadProgressHandler handler = new UploadProgressHandler();
//        handler.afterConnectionEstablished(session);
//
//        handler.broadcastFailure("video.mp4", "Upload failed");
//
//        verify(session).sendMessage(argThat(message ->
//                message instanceof TextMessage &&
//                        ((TextMessage) message).getPayload().contains("Upload failed")
//        ));
//    }
//}