package com.mediamind.converter.download.controller;

import com.mediamind.converter.download.service.DownloadService;
import com.mediamind.converter.dto.JobPayload;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/download")
public class DownloadController {

    private final DownloadService downloadService;

    public DownloadController (DownloadService downloadService) {
        this.downloadService = downloadService;
    }

    @PostMapping
    public ResponseEntity<InputStreamResource> download(@RequestBody JobPayload payload) {
        return downloadService.download(
                payload.getBlobName(),
                payload.getMimeType(),
                payload.getOutputFormat()
        );
    }
}
