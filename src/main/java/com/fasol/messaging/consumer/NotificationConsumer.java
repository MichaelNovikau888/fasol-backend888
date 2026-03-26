package com.fasol.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Обрабатывает события бронирования для отправки уведомлений.
 *
 * Manual acknowledgment — offset подтверждаем только после успешной обработки.
 * При ошибке перекладываем в DLQ (Dead Letter Queue) — не блокируем партицию.
 *
 * На собеседовании: "Без manual ack при краше consumer'а потеряем событие.
 * С manual ack при рестарте получим его снова — consumer идемпотентен."
 */
@Slf4j
@Component
@RequiredArgsConstructor
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

            ack.acknowledge(); // подтверждаем только при успехе
        } catch (Exception e) {
            log.error("Failed to process event at offset {}", record.offset(), e);
            sendToDlq(record);
            ack.acknowledge(); // подтверждаем, чтобы не застрять на одном сообщении
        }
    }

    private void handleCreated(String payload) {
        // В реальном проекте: emailService.sendBookingConfirmation(...)
        // smsService.sendReminder(bookingDate - 24h)
        log.info("Booking created notification: {}", payload);
    }

    private void handleCancelled(String payload) {
        // emailService.sendCancellationNotice(...)
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
