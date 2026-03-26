package com.fasol.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
public class OutboxMetrics {

    private final Counter published;
    private final Counter failed;
    private final Counter deadLetter;
    private final Timer publishDuration;
    @Getter
    private volatile long queueSize = 0;

    public OutboxMetrics(MeterRegistry registry) {
        this.published = Counter.builder("outbox.publish.success.total")
                .description("Events successfully published to Kafka")
                .register(registry);
        this.failed = Counter.builder("outbox.publish.failed.total")
                .description("Events failed to publish (will be retried)")
                .register(registry);
        this.deadLetter = Counter.builder("outbox.dead.letter.total")
                .description("Events that exceeded max retries and moved to dead letter")
                .register(registry);
        this.publishDuration = Timer.builder("outbox.publish.duration")
                .description("Time to publish a batch of outbox events")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
        Gauge.builder("outbox.queue.size", this, OutboxMetrics::getQueueSize)
                .description("Current number of unprocessed outbox events")
                .register(registry);
    }

    public void incrementPublished()    { published.increment(); }
    public void incrementFailed()       { failed.increment(); }
    public void incrementDeadLetter()   { deadLetter.increment(); }
    public void setQueueSize(long size) { this.queueSize = size; }

    public static class Sample {
        private final Timer.Sample inner;
        Sample(Timer.Sample inner) { this.inner = inner; }
        void stop(Timer timer) { inner.stop(timer); }
    }

    public Sample startTimer()        { return new Sample(Timer.start()); }
    public void stopTimer(Sample s)   { s.stop(publishDuration); }
}
