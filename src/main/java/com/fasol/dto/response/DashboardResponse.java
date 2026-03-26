package com.fasol.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Сводка для главного экрана студента (Dashboard.tsx).
 *
 * Один объект содержит всё необходимое — frontend не делает
 * дополнительных запросов после загрузки дашборда.
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardResponse {

    /** Активные абонементы (где остались занятия или ещё не истёк срок) */
    private List<StudentCourseResponse> activeCourses;

    /** Подтверждённые бронирования на ближайшие 7 дней */
    private List<BookingResponse> upcomingBookings;

    /** Статистика */
    private Stats stats;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Stats {
        /** Суммарно оставшихся индивидуальных занятий по всем абонементам */
        private int totalIndividualRemaining;
        /** Суммарно оставшихся групповых занятий по всем абонементам */
        private int totalGroupRemaining;
        /** Всего пройдено занятий (статус COMPLETED) */
        private long completedLessons;
        /**
         * true если хотя бы один абонемент с низким балансом (≤10%).
         * Dashboard.tsx показывает предупреждение «Занятия заканчиваются».
         */
        private boolean hasLowBalance;
    }
}
