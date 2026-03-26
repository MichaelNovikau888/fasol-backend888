package com.fasol.repository;

import com.fasol.domain.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query(value = """
        SELECT * FROM outbox_events
        WHERE processed = false AND retry_count < :maxRetries
        ORDER BY created_at ASC
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<OutboxEvent> findUnprocessedBatchForUpdate(
            @Param("limit") int limit,
            @Param("maxRetries") int maxRetries);

    long countByProcessedFalseAndRetryCountLessThan(int maxRetries);

    @Query(value = """
        SELECT * FROM outbox_events
        WHERE processed = false AND retry_count >= :maxRetries
        ORDER BY created_at ASC
        LIMIT 50
        """, nativeQuery = true)
    List<OutboxEvent> findDeadLetterEvents(@Param("maxRetries") int maxRetries);
}
