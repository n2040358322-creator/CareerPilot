package com.careerpilot.repository;

import com.careerpilot.model.ChatMessageRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRecordRepository extends JpaRepository<ChatMessageRecord, Long> {

    List<ChatMessageRecord> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    void deleteBySessionId(Long sessionId);
}
