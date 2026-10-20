package com.mediasense.qna.model;

public class AskResponse {
    private final String question;
    private final String answer;
    private final String moduleUsed;

    public AskResponse(String question, String answer, String moduleUsed) {
        this.question = question;
        this.answer = answer;
        this.moduleUsed = moduleUsed;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public String getModuleUsed() {
        return moduleUsed;
    }
}
