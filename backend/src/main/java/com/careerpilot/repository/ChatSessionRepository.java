package com.careerpilot.repository;

import com.careerpilot.model.ChatSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    List<ChatSession> findTop20ByUserIdOrderByUpdatedAtDesc(Long userId);
}
