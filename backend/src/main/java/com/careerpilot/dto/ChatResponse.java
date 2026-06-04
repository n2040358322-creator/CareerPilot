package com.careerpilot.dto;

public class ChatResponse {

    private String answer;
    private Long sessionId;
    private String sessionTitle;

    public ChatResponse() {
    }

    public ChatResponse(String answer) {
        this.answer = answer;
    }

    public ChatResponse(String answer, Long sessionId, String sessionTitle) {
        this.answer = answer;
        this.sessionId = sessionId;
        this.sessionTitle = sessionTitle;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionTitle() {
        return sessionTitle;
    }

    public void setSessionTitle(String sessionTitle) {
        this.sessionTitle = sessionTitle;
    }
}
