package com.fasol.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(KafkaTemplate.class)
public class NotificationConsumer {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.topics.booking-events}.DLT")
    private String dlqTopic;

    @KafkaListener(
            topics = "${app.kafka.topics.booking-events}",
            groupId = "notification-group"
    )
    public void handleBookingEvent(ConsumerRecord<String, String> record,
                                   Acknowledgment ack) {
        log.debug("Received event key={} offset={}", record.key(), record.offset());
        try {
            String eventType = objectMapper.readTree(record.value())
                    .path("eventType").asText("UNKNOWN");

            switch (eventType) {
                case "BOOKING_CREATED"   -> handleCreated(record.value());
                case "BOOKING_CANCELLED" -> handleCancelled(record.value());
                default -> log.warn("Unknown eventType: {}", eventType);
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process event at offset {}", record.offset(), e);
            sendToDlq(record);
            ack.acknowledge();
        }
    }

    private void handleCreated(String payload) {
        log.info("Booking created notification: {}", payload);
    }

    private void handleCancelled(String payload) {
        log.info("Booking cancelled notification: {}", payload);
    }

    private void sendToDlq(ConsumerRecord<String, String> record) {
        try {
            kafkaTemplate.send(dlqTopic, record.key(), record.value());
            log.warn("Sent to DLQ: key={}", record.key());
        } catch (Exception e) {
            log.error("CRITICAL: Failed to send to DLQ, event may be lost: key={}", record.key(), e);
        }
    }
}