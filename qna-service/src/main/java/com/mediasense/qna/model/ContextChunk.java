package com.mediasense.qna.model;

public class ContextChunk {
    private final String source;
    private final String text;

    public ContextChunk(String source, String text) {
        this.source = source;
        this.text = text;
    }

    public String getSource() {
        return source;
    }

    public String getText() {
        return text;
    }
}
