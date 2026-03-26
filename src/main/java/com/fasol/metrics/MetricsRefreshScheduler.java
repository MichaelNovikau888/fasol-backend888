package com.fasol.metrics;

import com.fasol.domain.enums.TrialRequestStatus;
import com.fasol.repository.StudentCourseRepository;
import com.fasol.repository.TrialRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Периодически синхронизирует Gauge-метрики с реальными данными БД.
 *
 * Counters (booking.created.total, trial.request.total и т.д.) обновляются
 * инкрементально в момент события — они точные.
 *
 * Gauges (очередь заявок, активные студенты) — это «снапшот» текущего
 * состояния, поэтому обновляем их из БД каждые 30 секунд.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsRefreshScheduler {

    private final SalesMetrics salesMetrics;
    private final TrialRequestRepository trialRequestRepository;
    private final StudentCourseRepository studentCourseRepository;

    @Scheduled(fixedDelay = 30_000)
    public void refreshGauges() {
        try {
            // Сколько необработанных заявок ждут в очереди
            long newTrials = trialRequestRepository.countByStatus(TrialRequestStatus.NEW);
            salesMetrics.setTrialQueueSize(newTrials);

            // Сколько студентов с ненулевым балансом занятий (активный абонемент)
            long activeStudents = studentCourseRepository.countActiveStudents();
            salesMetrics.setActiveStudents(activeStudents);

        } catch (Exception e) {
            log.warn("Failed to refresh gauge metrics: {}", e.getMessage());
        }
    }
}
