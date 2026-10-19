package com.mediamind.converter.upload.controller;

import com.mediamind.converter.upload.exception.FileValidationException;
import com.mediamind.converter.upload.exception.UploadFailureException;
import com.mediamind.converter.upload.service.FileStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/upload")
public class UploadController {
    private final FileStorageService storageService;

    public UploadController(FileStorageService storageService){
        this.storageService = storageService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("outputFormat") String outputFormat){
        Map<String, Object> response = new HashMap<>();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        try {
            response.put("uploaded", storageService.store(file, outputFormat));
            response.put("timestamp", timestamp);
        } catch (FileValidationException | UploadFailureException e) {
            Map<String, Object> failedDetails = new HashMap<>();
            failedDetails.put("fileName", e instanceof FileValidationException
                    ? ((FileValidationException) e).getFileName()
                    : ((UploadFailureException) e).getFileName());
            failedDetails.put("reason", e instanceof FileValidationException
                    ? ((FileValidationException) e).getReason()
                    : ((UploadFailureException) e).getReason());
            response.put("failed", failedDetails);
            response.put("timestamp", timestamp);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> uploadMultipleFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("outputFormat") String outputFormat) {
        List<String> uploadedFileIds = new ArrayList<>();
        List<Map<String, String>> failedFiles = new ArrayList<>();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        for (MultipartFile file : files) {
            try {
                uploadedFileIds.add(storageService.store(file, outputFormat));
            } catch (FileValidationException | UploadFailureException e) {
                Map<String, String> errorDetails = new HashMap<>();
                errorDetails.put("file", e instanceof FileValidationException
                        ? ((FileValidationException) e).getFileName()
                        : ((UploadFailureException) e).getFileName());
                errorDetails.put("error", e instanceof FileValidationException
                        ? ((FileValidationException) e).getReason()
                        : ((UploadFailureException) e).getReason());
                failedFiles.add(errorDetails);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("uploaded", uploadedFileIds);
        response.put("failed", failedFiles);
        response.put("timestamp", timestamp);

        return ResponseEntity.ok(response);
    }
}