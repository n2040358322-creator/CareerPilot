package com.careerpilot.controller;

import com.careerpilot.dto.ApiResponse;
import com.careerpilot.dto.ChatRequest;
import com.careerpilot.dto.ChatResponse;
import com.careerpilot.dto.ChatSessionDetailDto;
import com.careerpilot.dto.ChatSessionDto;
import com.careerpilot.service.ChatService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ApiResponse.ok(chatService.chat(request));
    }

    @GetMapping("/sessions")
    public ApiResponse<List<ChatSessionDto>> sessions() {
        return ApiResponse.ok(chatService.sessions());
    }

    @GetMapping("/sessions/{id}")
    public ApiResponse<ChatSessionDetailDto> sessionDetail(@PathVariable Long id) {
        return ApiResponse.ok(chatService.sessionDetail(id));
    }

    @DeleteMapping("/sessions/{id}")
    public ApiResponse<Void> deleteSession(@PathVariable Long id) {
        chatService.deleteSession(id);
        return ApiResponse.ok(null);
    }
}
