package com.careerpilot.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ChatSessionDetailDto {

    private Long id;
    private String title;
    private String resumeText;
    private String jobDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ChatHistoryMessageDto> messages;

    public ChatSessionDetailDto(
            Long id,
            String title,
            String resumeText,
            String jobDescription,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<ChatHistoryMessageDto> messages) {
        this.id = id;
        this.title = title;
        this.resumeText = resumeText;
        this.jobDescription = jobDescription;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.messages = messages;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getResumeText() {
        return resumeText;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<ChatHistoryMessageDto> getMessages() {
        return messages;
    }
}
