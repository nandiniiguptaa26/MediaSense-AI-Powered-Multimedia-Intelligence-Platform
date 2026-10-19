package com.mediamind.converter.upload.exception;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class UploadExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(UploadExceptionHandler.class);
    @Value("${spring.servlet.multipart.max-file-size}")
    private String maxFileSize;

    @ExceptionHandler(FileValidationException.class)
    public ResponseEntity<Map<String, String>> handleFileValidation(FileValidationException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("file", ex.getFileName());
        errorResponse.put("error", ex.getReason());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxSizeExceeded(MaxUploadSizeExceededException ex) {
        logger.error("Upload failed. File size exceeded the {} limit.", maxFileSize, ex);
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "File is too large");
        errorResponse.put("message", "File cannot exceed " + maxFileSize);

        // Add timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        errorResponse.put("timestamp", timestamp);
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }
}
