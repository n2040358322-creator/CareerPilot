package com.careerpilot.service;

import com.careerpilot.dto.ChatHistoryMessageDto;
import com.careerpilot.dto.ChatMessage;
import com.careerpilot.dto.ChatRequest;
import com.careerpilot.dto.ChatResponse;
import com.careerpilot.dto.ChatSessionDetailDto;
import com.careerpilot.dto.ChatSessionDto;
import com.careerpilot.model.ChatMessageRecord;
import com.careerpilot.model.ChatSession;
import com.careerpilot.repository.ChatMessageRecordRepository;
import com.careerpilot.repository.ChatSessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ChatService {

    private final AiClient aiClient;
    private final AuthContext authContext;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRecordRepository messageRepository;

    public ChatService(
            AiClient aiClient,
            AuthContext authContext,
            ChatSessionRepository sessionRepository,
            ChatMessageRecordRepository messageRepository) {
        this.aiClient = aiClient;
        this.authContext = authContext;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public ChatResponse chat(ChatRequest request) {
        if (request.getAiConfig() == null || !aiClient.isRuntimeConfigured(request.getAiConfig())) {
            throw new IllegalArgumentException("请先配置 AI_API_KEY、AI_BASE_URL 和 AI_MODEL");
        }

        String systemPrompt = """
                你是 CareerPilot 的 AI 求职助手，擅长 Java 后端、AI 应用、简历优化和模拟面试。
                请基于用户当前简历和目标岗位 JD 回答问题。回答要具体、可执行、适合大学生求职准备。
                如果用户让你改简历，请直接给出可以复制进简历的版本。
                """;

        String context = """
                【当前简历】
                %s

                【目标岗位 JD】
                %s
                """.formatted(limit(request.getResumeText(), 8000), limit(request.getJobDescription(), 4000));

        String answer = aiClient.chat(systemPrompt, context, request.getMessages(), request.getAiConfig());
        ChatSession session = saveChatHistory(request, answer);
        if (session == null) {
            return new ChatResponse(answer);
        }
        return new ChatResponse(answer, session.getId(), session.getTitle());
    }

    public List<ChatSessionDto> sessions() {
        Long userId = authContext.requireUser().userId();
        return sessionRepository.findTop20ByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(session -> new ChatSessionDto(
                        session.getId(),
                        session.getTitle(),
                        session.getCreatedAt(),
                        session.getUpdatedAt()))
                .toList();
    }

    public ChatSessionDetailDto sessionDetail(Long id) {
        Long userId = authContext.requireUser().userId();
        ChatSession session = findOwnedSession(id, userId);
        List<ChatHistoryMessageDto> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(id)
                .stream()
                .map(message -> new ChatHistoryMessageDto(
                        message.getId(),
                        message.getRole(),
                        message.getContent(),
                        message.getCreatedAt()))
                .toList();
        return new ChatSessionDetailDto(
                session.getId(),
                session.getTitle(),
                session.getResumeText(),
                session.getJobDescription(),
                session.getCreatedAt(),
                session.getUpdatedAt(),
                messages);
    }

    @Transactional
    public void deleteSession(Long id) {
        Long userId = authContext.requireUser().userId();
        ChatSession session = findOwnedSession(id, userId);
        messageRepository.deleteBySessionId(session.getId());
        sessionRepository.delete(session);
    }

    private ChatSession saveChatHistory(ChatRequest request, String answer) {
        Long userId = authContext.currentUser().map(TokenPayload::userId).orElse(null);
        if (userId == null) {
            return null;
        }

        ChatMessage userMessage = lastUserMessage(request.getMessages());
        if (userMessage == null || !StringUtils.hasText(userMessage.getContent())) {
            return null;
        }

        ChatSession session = resolveSession(request, userId, userMessage.getContent());
        messageRepository.save(toMessageRecord(session, userId, "user", userMessage.getContent()));
        messageRepository.save(toMessageRecord(session, userId, "assistant", answer));
        session.setUpdatedAt(LocalDateTime.now());
        return sessionRepository.save(session);
    }

    private ChatSession resolveSession(ChatRequest request, Long userId, String firstMessage) {
        if (request.getSessionId() != null) {
            ChatSession session = findOwnedSession(request.getSessionId(), userId);
            session.setResumeText(limit(request.getResumeText(), 12000));
            session.setJobDescription(limit(request.getJobDescription(), 6000));
            return session;
        }

        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(buildTitle(firstMessage));
        session.setResumeText(limit(request.getResumeText(), 12000));
        session.setJobDescription(limit(request.getJobDescription(), 6000));
        return sessionRepository.save(session);
    }

    private ChatSession findOwnedSession(Long id, Long userId) {
        ChatSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("对话会话不存在"));
        if (!userId.equals(session.getUserId())) {
            throw new IllegalArgumentException("无权访问该对话会话");
        }
        return session;
    }

    private ChatMessageRecord toMessageRecord(ChatSession session, Long userId, String role, String content) {
        ChatMessageRecord record = new ChatMessageRecord();
        record.setSessionId(session.getId());
        record.setUserId(userId);
        record.setRole(role);
        record.setContent(limit(content, 12000));
        return record;
    }

    private ChatMessage lastUserMessage(List<ChatMessage> messages) {
        for (int index = messages.size() - 1; index >= 0; index--) {
            ChatMessage message = messages.get(index);
            if ("user".equalsIgnoreCase(message.getRole())) {
                return message;
            }
        }
        return null;
    }

    private String buildTitle(String content) {
        String title = content == null ? "新的 AI 对话" : content.replaceAll("\\s+", " ").trim();
        if (!StringUtils.hasText(title)) {
            return "新的 AI 对话";
        }
        return title.length() > 28 ? title.substring(0, 28) + "..." : title;
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength);
    }
}
