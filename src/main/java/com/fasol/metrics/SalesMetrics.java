package com.fasol.metrics;

import com.fasol.domain.enums.TrialRequestStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * CRM-метрики воронки продаж школы вокала Fa Sol.
 *
 * Воронка:
 *   trial.request.total          — подали заявку на пробный урок
 *   trial.status.contacted.total — менеджер позвонил
 *   trial.status.scheduled.total — урок назначен
 *   trial.status.completed.total — урок состоялся
 *   course.purchased.total       — купили курс (конверсия)
 *   payment.received.total       — оплата поступила
 *
 * Конверсия в Grafana:
 *   course.purchased.total / trial.status.completed.total * 100
 */
@Component
public class SalesMetrics {

    // ── Воронка ───────────────────────────────────────────────────────────────

    /** Новая заявка на пробный урок с лендинга */
    private final Counter trialRequestNew;

    /** Менеджер позвонил — статус CONTACTED */
    private final Counter trialContactedTotal;

    /** Пробный урок назначен — статус SCHEDULED */
    private final Counter trialScheduledTotal;

    /** Пробный урок состоялся — статус COMPLETED */
    private final Counter trialCompletedTotal;

    /** Заявка отменена — статус CANCELLED */
    private final Counter trialCancelledTotal;

    // ── Продажи ───────────────────────────────────────────────────────────────

    /** Куплен курс (первичная или повторная покупка) */
    private final Counter coursePurchasedTotal;

    /** Повторная покупка курса (лояльный клиент) */
    private final Counter courseRepeatPurchaseTotal;

    /**
     * Покупка курса ПОСЛЕ пробного урока — точная метрика конверсии воронки.
     * Отличие от coursePurchasedTotal: здесь только те, кто пришёл через заявку,
     * отходил на пробный урок (COMPLETED) и затем купил курс.
     */
    private final Counter trialConversionTotal;

    /** Оплата получена (онлайн или офлайн) */
    private final Counter paymentReceivedTotal;

    /** Офлайн-оплата (менеджер принял наличными) */
    private final Counter paymentOfflineTotal;

    // ── Текущие остатки (Gauge — реальное значение, не накопительный счётчик) ─

    @Getter private volatile long trialQueueSize = 0;   // необработанных заявок (NEW)
    @Getter private volatile long activeStudents  = 0;   // студентов с активным абонементом

    // ── Бронирования ─────────────────────────────────────────────────────────

    /** Занятий забронировано за день */
    private final Counter bookingsDailyTotal;

    public SalesMetrics(MeterRegistry registry) {

        // Воронка
        trialRequestNew = Counter.builder("trial.request.total")
                .description("Новые заявки на пробный урок")
                .register(registry);

        trialContactedTotal = Counter.builder("trial.status.contacted.total")
                .description("Заявки: менеджер позвонил")
                .register(registry);

        trialScheduledTotal = Counter.builder("trial.status.scheduled.total")
                .description("Заявки: урок назначен")
                .register(registry);

        trialCompletedTotal = Counter.builder("trial.status.completed.total")
                .description("Заявки: пробный урок состоялся")
                .register(registry);

        trialCancelledTotal = Counter.builder("trial.status.cancelled.total")
                .description("Заявки: отменены")
                .register(registry);

        // Продажи
        coursePurchasedTotal = Counter.builder("course.purchased.total")
                .description("Куплено курсов")
                .register(registry);

        courseRepeatPurchaseTotal = Counter.builder("course.repeat.purchase.total")
                .description("Повторных покупок курсов")
                .register(registry);

        trialConversionTotal = Counter.builder("trial.conversion.total")
                .description("Покупок курса после пробного урока (точная конверсия воронки)")
                .register(registry);

        paymentReceivedTotal = Counter.builder("payment.received.total")
                .description("Оплат получено")
                .register(registry);

        paymentOfflineTotal = Counter.builder("payment.offline.total")
                .description("Офлайн-оплат (наличными через менеджера)")
                .register(registry);

        // Бронирования
        bookingsDailyTotal = Counter.builder("bookings.daily.total")
                .description("Занятий забронировано (нарастающий итог)")
                .register(registry);

        // Gauges — текущее состояние очереди заявок и активных студентов
        Gauge.builder("trial.queue.size", this, SalesMetrics::getTrialQueueSize)
                .description("Необработанных заявок (статус NEW) в очереди")
                .register(registry);

        Gauge.builder("students.active.count", this, SalesMetrics::getActiveStudents)
                .description("Студентов с активным абонементом")
                .register(registry);
    }

    // ── Публичный API ─────────────────────────────────────────────────────────

    /** Вызывать при создании новой заявки */
    public void incrementTrialRequest() { trialRequestNew.increment(); }

    /**
     * Вызывать при смене статуса заявки.
     * Автоматически выбирает нужный счётчик по статусу.
     */
    public void trackTrialStatusChange(TrialRequestStatus newStatus) {
        switch (newStatus) {
            case CONTACTED  -> trialContactedTotal.increment();
            case SCHEDULED  -> trialScheduledTotal.increment();
            case COMPLETED  -> trialCompletedTotal.increment();
            case CANCELLED  -> trialCancelledTotal.increment();
            default         -> { /* NEW — уже посчитан при создании */ }
        }
    }

    /** Вызывать при покупке курса */
    public void incrementCoursePurchased(boolean isRepeat) {
        coursePurchasedTotal.increment();
        if (isRepeat) courseRepeatPurchaseTotal.increment();
    }

    /** Вызывать при покупке курса ПОСЛЕ пробного урока (точная конверсия) */
    public void incrementTrialConversion() {
        trialConversionTotal.increment();
    }

    /** Вызывать при получении оплаты */
    public void incrementPaymentReceived(boolean isOffline) {
        paymentReceivedTotal.increment();
        if (isOffline) paymentOfflineTotal.increment();
    }

    /** Вызывать при создании бронирования */
    public void incrementDailyBooking() { bookingsDailyTotal.increment(); }

    /** Обновлять периодически (например, в @Scheduled задаче) */
    public void setTrialQueueSize(long size) { this.trialQueueSize = size; }

    /** Обновлять периодически */
    public void setActiveStudents(long count) { this.activeStudents = count; }
}
