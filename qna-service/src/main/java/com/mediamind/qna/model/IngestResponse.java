package com.mediamind.qna.model;

public class IngestResponse {
    private final String source;
    private final String status;
    private final int charactersIndexed;

    public IngestResponse(String source, String status, int charactersIndexed) {
        this.source = source;
        this.status = status;
        this.charactersIndexed = charactersIndexed;
    }

    public String getSource() {
        return source;
    }

    public String getStatus() {
        return status;
    }

    public int getCharactersIndexed() {
        return charactersIndexed;
    }
}
