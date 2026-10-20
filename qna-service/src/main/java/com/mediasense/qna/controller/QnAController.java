package com.mediasense.qna.controller;

import com.mediasense.qna.model.AskRequest;
import com.mediasense.qna.model.AskResponse;
import com.mediasense.qna.model.IngestResponse;
import com.mediasense.qna.service.ContextStore;
import com.mediasense.qna.service.IngestService;
import com.mediasense.qna.service.QnAService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/qna")
public class QnAController {

    private final IngestService ingestService;
    private final QnAService qnAService;
    private final ContextStore contextStore;

    public QnAController(IngestService ingestService, QnAService qnAService, ContextStore contextStore) {
        this.ingestService = ingestService;
        this.qnAService = qnAService;
        this.contextStore = contextStore;
    }

    @PostMapping(value = "/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<IngestResponse> ingestFile(@RequestPart("file") MultipartFile file) {
        try {
            IngestResponse response = ingestService.ingestFile(file);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new IngestResponse(file.getOriginalFilename(), "failed: " + e.getMessage(), 0));
        }
    }

    @PostMapping(value = "/ingest-url", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IngestResponse> ingestUrl(@RequestBody Map<String, String> body) {
        String url = body.get("url");
        if(url == null || url.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new IngestResponse("", "failed: 'uri' field is required", 0));
        }
        try {
            IngestResponse response = ingestService.ingestUrl(url);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new IngestResponse(url, "failed: " + e.getMessage(), 0));
        }
    }

    @PostMapping(value = "/ask", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AskResponse> ask(@RequestBody AskRequest request) {
        if(request.getQuestion() == null || request.getQuestion().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new AskResponse("", "failed: 'Question' must not be empty", "none"));
        }
        try {
            AskResponse response = qnAService.answer(request.getQuestion());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new AskResponse(request.getQuestion(), "failed: " + e.getMessage(), "none"));
        }
    }

    @DeleteMapping("/context")
    public ResponseEntity<Map<String , String>> clearContext() {
        contextStore.clear();
        return ResponseEntity.ok(Map.of("status", "cleared", "message", "All context has been removed"));
    }

    @GetMapping("/context/status")
    public ResponseEntity<Map<String, Object>> contextStatus() {
        return ResponseEntity.ok(Map.of(
                "chunksLoaded", contextStore.size(),
                "sources", contextStore.getAll().stream()
                        .map(c -> c.getSource())
                        .toList()
        ));
    }
}
