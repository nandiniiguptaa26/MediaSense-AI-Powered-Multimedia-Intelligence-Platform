//package com.mediamind.converter.upload.service;
//
//import com.google.cloud.storage.Storage;
//import com.mediamind.converter.upload.config.UploadProperties;
//import com.mediamind.converter.upload.websocket.UploadProgressHandler;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.springframework.web.socket.TextMessage;
//import org.springframework.web.socket.WebSocketSession;
//
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.argThat;
//import static org.mockito.Mockito.*;
//
//public class FileStorageServiceUnitTest {
//
//    @Mock
//    private Storage storage;
//
//    @Mock
//    private UploadProgressHandler progressHandler;
//
//    @Mock
//    private UploadProperties properties;
//
//    @InjectMocks
//    private FileStorageService storageService;
//
//    @BeforeEach
//    void setup() {
//        MockitoAnnotations.openMocks(this);
//        when(properties.getBucketName()).thenReturn("audio-video-converter-test-files");
//        when(properties.getAllowedExtensions()).thenReturn(List.of("mp4"));
//        when(properties.getAllowedMimetypes()).thenReturn(List.of("video/mp4"));
//        when(properties.isAllowOverwrite()).thenReturn(false);
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
