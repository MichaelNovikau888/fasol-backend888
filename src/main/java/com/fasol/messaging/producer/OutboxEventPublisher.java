package com.fasol.messaging.producer;

import com.fasol.domain.entity.OutboxEvent;
import com.fasol.metrics.OutboxMetrics;
import com.fasol.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Outbox relay — публикует события из БД в Kafka.
 *
 * Аннотация @ConditionalOnBean(KafkaTemplate.class) означает, что весь бин
 * не создаётся если KafkaAutoConfiguration исключён (профиль railway).
 * Scheduled-задача просто не запускается — outbox-записи остаются в БД,
 * но приложение не падает при старте.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(KafkaTemplate.class)
public class OutboxEventPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxMetrics metrics;

    @Value("${app.kafka.topics.booking-events}")
    private String bookingTopic;

    @Value("${app.kafka.topics.notification-events}")
    private String notificationTopic;

    @Value("${app.outbox.batch-size:100}")
    private int batchSize;

    @Value("${app.outbox.max-retries:3}")
    private int maxRetries;

    @Value("${app.outbox.kafka-timeout-seconds:5}")
    private int kafkaTimeoutSeconds;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:2000}")
    public void publishPendingEvents() {
        List<OutboxEvent> events = fetchBatch();
        if (events.isEmpty()) {
            updateQueueMetric();
            return;
        }
        log.debug("Outbox relay: processing {} events", events.size());
        OutboxMetrics.Sample sample = metrics.startTimer();
        for (OutboxEvent event : events) {
            publishOne(event);
        }
        metrics.stopTimer(sample);
        updateQueueMetric();
    }

    @Transactional
    protected List<OutboxEvent> fetchBatch() {
        return outboxRepository.findUnprocessedBatchForUpdate(batchSize, maxRetries);
    }

    private void publishOne(OutboxEvent event) {
        String topic = resolveTopic(event.getEventType());
        try {
            kafkaTemplate
                    .send(topic, event.getAggregateId(), event.getPayload())
                    .get(kafkaTimeoutSeconds, TimeUnit.SECONDS);
            markProcessed(event.getId());
            metrics.incrementPublished();
            log.debug("Published event id={} type={} topic={}", event.getId(), event.getEventType(), topic);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Outbox publish interrupted for event {}", event.getId());
        } catch (TimeoutException e) {
            markFailed(event.getId(), event.getRetryCount());
            metrics.incrementFailed();
            log.warn("Kafka timeout {}s for event {}, retry {}/{}", kafkaTimeoutSeconds,
                    event.getId(), event.getRetryCount() + 1, maxRetries);
        } catch (ExecutionException e) {
            markFailed(event.getId(), event.getRetryCount());
            metrics.incrementFailed();
            log.error("Kafka send failed for event {} type={}: {}",
                    event.getId(), event.getEventType(), e.getCause().getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessed(UUID eventId) {
        outboxRepository.findById(eventId).ifPresent(event -> {
            event.setProcessed(true);
            event.setProcessedAt(LocalDateTime.now());
            outboxRepository.save(event);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID eventId, int currentRetryCount) {
        outboxRepository.findById(eventId).ifPresent(event -> {
            int newCount = currentRetryCount + 1;
            event.setRetryCount(newCount);
            outboxRepository.save(event);
            if (newCount >= maxRetries) {
                metrics.incrementDeadLetter();
                log.error("DEAD LETTER: event {} type={} exceeded {} retries",
                        eventId, event.getEventType(), maxRetries);
            }
        });
    }

    @Transactional
    public int requeueDeadLetterEvents() {
        List<OutboxEvent> dead = outboxRepository.findDeadLetterEvents(maxRetries);
        dead.forEach(e -> {
            e.setRetryCount(0);
            outboxRepository.save(e);
        });
        log.info("Requeued {} dead-letter events", dead.size());
        return dead.size();
    }

    private void updateQueueMetric() {
        try {
            long size = outboxRepository.countByProcessedFalseAndRetryCountLessThan(maxRetries);
            metrics.setQueueSize(size);
            if (size > 500) {
                log.warn("Outbox queue size={} — проверьте доступность Kafka", size);
            }
        } catch (Exception e) {
            log.warn("Failed to update outbox queue metric", e);
        }
    }

    private String resolveTopic(String eventType) {
        return switch (eventType) {
            case "BOOKING_CREATED", "BOOKING_CANCELLED", "BOOKING_COMPLETED" -> bookingTopic;
            default -> notificationTopic;
        };
    }
}