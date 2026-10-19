package com.mediamind.qna.service;

import com.mediamind.qna.model.ContextChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Component
public class ContextStore {

    private final List<ContextChunk> chunks = new ArrayList<>();

    public void add(ContextChunk chunk) {
        chunks.add(chunk);
    }

    public void clear() {
        chunks.clear();
    }

    public int size() {
        return chunks.size();
    }

    public List<ContextChunk> getAll() {
        return Collections.unmodifiableList(chunks);
    }

    public String buildContext(int maxChars) {
        StringBuilder sb = new StringBuilder();
        for (ContextChunk chunk : chunks) {
            sb.append("--- Source: ").append(chunk.getSource()).append(" ---\n");
            sb.append(chunk.getText()).append("\n\n");
            if (sb.length() >= maxChars) break;
        }
        String result = sb.toString();
        return result.length() > maxChars ? result.substring(0, maxChars) : result;
    }
}
