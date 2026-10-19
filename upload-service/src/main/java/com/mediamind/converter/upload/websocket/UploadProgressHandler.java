package com.mediamind.converter.upload.websocket;

import com.mediamind.converter.upload.util.ByteConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class UploadProgressHandler extends TextWebSocketHandler {
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger(UploadProgressHandler.class);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    public void broadcastProgress(String filename, double progress, long totalBytesLoaded) {
        String message = filename + ":" + String.format("%.2f", progress) + "% "
                + "\n Total bytes loaded: " + ByteConverter.formatBytes(totalBytesLoaded);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    logger.warn("Failed to send progress update to WebSocket client: {}", e.getMessage());
                    // Optionally remove the session if it's broken
                    try {
                        session.close();
                    } catch (IOException closeEx) {
                        logger.error("Failed to close broken WebSocket session: {}", closeEx.getMessage());
                    }
                    sessions.remove(session);
                }
            }
        }
    }

    public void broadcastFailure(String fileName, String reason) {
        String message = "Upload failed for " + fileName + ": " + reason;
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    logger.warn("Failed to send failure update to WebSocket client: {}", e.getMessage());
                    try {
                        session.close();
                    } catch (IOException closeEx) {
                        logger.error("Failed to close broken WebSocket session(broadcast failure): {}", closeEx.getMessage());
                    }
                    sessions.remove(session);
                }
            }
        }
    }
}