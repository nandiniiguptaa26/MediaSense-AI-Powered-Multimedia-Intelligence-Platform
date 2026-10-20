package com.mediasense.qna.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mediasense.qna.model.AskResponse;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class QnAService {

    private static final Logger logger = LoggerFactory.getLogger(QnAService.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int MAX_CONTENT_CHARS = 4_000;

    @Value("${ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${ollama.model}")
    private String ollamaModel;

    @Value("${huggingface.api-url}")
    private String huggingFaceApiUrl;

    private final ContextStore contextStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public QnAService(ContextStore contextStore) {
        this.contextStore = contextStore;
    }

    public AskResponse answer(String question) {
        if(contextStore.size() == 0) {
            return new AskResponse(question,
                    "No Context has been ingested yet. Please call POST/qna/ingest first.",
                    "none");
        }
        String context = contextStore.buildContext(MAX_CONTENT_CHARS);

        try {
            String answer = askOllama(question, context);
            logger.info("Answered via ollama");
            return new AskResponse(question, answer, "ollama/" + ollamaModel);
        } catch (Exception ollamaEx) {
            logger.warn("Ollama Unavailable ({}), falling back to HuggingFace", ollamaEx.getMessage());
        }

        try {
            String answer = askHuggingFace(question, context);
            logger.info("Answered via HuggingFace");
            return new AskResponse(question, answer, "huggingface/deepset-roberta-base-squad2");
        } catch (Exception hfEx) {
            logger.error("huggingface also failed: {}", hfEx.getMessage());
            return new AskResponse(question,
                    "Both ollama and huggingface unavailable",
                    "error");
        }
    }

    private String askOllama(String question, String context) throws IOException {
        String prompt = buildPrompt(question, context);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", ollamaModel);
        body.put("prompt", prompt);
        body.put("stream", false);

        Request request = new Request.Builder()
                .url(ollamaBaseUrl + "/api/generate")
                .post(RequestBody.create(objectMapper.writeValueAsString(body), JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if(!response.isSuccessful()) {
                throw new IOException("ollama return HTTP " + response.code());
            }

            String responseBody = response.body().string();
            JsonNode json = objectMapper.readTree(responseBody);
            return json.path("response").asText("No response from Ollama");
        }
    }

    private String askHuggingFace(String question, String context) throws IOException {
        String trimmedContext = context.length() > 2000 ? context.substring(0, 2000) : context;

        ObjectNode inputs = objectMapper.createObjectNode();
        inputs.put("question", question);
        inputs.put("context", trimmedContext);

        ObjectNode body = objectMapper.createObjectNode();
        body.set("inputs", inputs);

        Request request = new Request.Builder()
                .url(huggingFaceApiUrl)
                .post(RequestBody.create(objectMapper.writeValueAsString(body), JSON))
                .header("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if(!response.isSuccessful()) {
                throw new IOException("HuggingFace return HTTP " + response.code());
            }

            String responseBody = response.body().string();
            JsonNode json = objectMapper.readTree(responseBody);
            return json.path("answer").asText("No answer found in context");
        }
    }

    private String buildPrompt(String question, String context) {
        return "You are a helpful assistant. Answer the question based only on the context provided below. " +
                "If the answer is not in the context, say 'I could not find the answer in the provider context.'\n\n" +
                "CONTEXT:\n" + context + "\n\n" +
                "QUESTION:\n" + question + "\n\n" +
                "ANSWER:";
    }

}
