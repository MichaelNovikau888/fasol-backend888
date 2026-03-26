package com.fasol.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Кастомные бизнес-метрики — ключевое для observability.
 * Grafana алерт: если cancel_rate > 15% → уведомление.
 * Именно рост booking_slot_conflict_total показал нам проблему с
 * race condition под нагрузкой и заставил ввести Redis distributed lock.
 */
@Component
public class BookingMetrics {

    private final Counter bookingSuccess;
    private final Counter bookingCancellation;
    private final Counter slotConflict;
    private final Timer bookingDuration;
    private final MeterRegistry registry;

    public BookingMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.bookingSuccess = Counter.builder("booking.created.total")
                .description("Total successful bookings").register(registry);
        this.bookingCancellation = Counter.builder("booking.cancelled.total")
                .description("Total cancelled bookings").register(registry);
        this.slotConflict = Counter.builder("booking.slot.conflict.total")
                .description("Slot conflicts caught by distributed lock").register(registry);
        this.bookingDuration = Timer.builder("booking.duration")
                .description("Booking operation latency")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public void incrementBookingSuccess()    { bookingSuccess.increment(); }
    public void incrementCancellation()      { bookingCancellation.increment(); }
    public void incrementSlotConflict()      { slotConflict.increment(); }
    public Timer.Sample startTimer()         { return Timer.start(registry); }
    public void stopTimer(Timer.Sample s)    { s.stop(bookingDuration); }
}
