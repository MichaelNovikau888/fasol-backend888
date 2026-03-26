package com.fasol.service.impl;

import com.fasol.domain.enums.BookingStatus;
import com.fasol.dto.response.BookingResponse;
import com.fasol.dto.response.DashboardResponse;
import com.fasol.dto.response.DashboardResponse.Stats;
import com.fasol.dto.response.StudentCourseResponse;
import com.fasol.repository.BookingRepository;
import com.fasol.repository.StudentCourseRepository;
import com.fasol.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final StudentCourseRepository studentCourseRepository;
    private final BookingRepository bookingRepository;

    /**
     * Собираем сводку в одной транзакции — три запроса к БД, ноль N+1.
     *
     * Dashboard.tsx раньше делал три отдельных запроса в useEffect.
     * Здесь всё агрегируется на бэкенде: фронт получает готовый объект.
     */
    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getSummary(UUID studentId) {

        // 1. Активные абонементы (индивидуальные или групповые занятия ещё есть)
        List<StudentCourseResponse> activeCourses = studentCourseRepository
                .findActiveByStudentId(studentId)
                .stream()
                .map(StudentCourseResponse::from)
                .toList();

        // 2. Ближайшие 7 дней — подтверждённые бронирования
        LocalDate today = LocalDate.now();
        List<BookingResponse> upcoming = bookingRepository
                .findUpcomingByStudentId(studentId, today, today.plusDays(7))
                .stream()
                .map(BookingResponse::from)
                .toList();

        // 3. Статистика — считаем прямо по загруженным данным, без лишних запросов
        int totalInd = activeCourses.stream()
                .mapToInt(StudentCourseResponse::getIndividualLessonsRemaining).sum();
        int totalGrp = activeCourses.stream()
                .mapToInt(StudentCourseResponse::getGroupLessonsRemaining).sum();
        boolean hasLowBalance = activeCourses.stream()
                .anyMatch(sc -> sc.isIndividualLowBalance() || sc.isGroupLowBalance());

        long completed = bookingRepository
                .countByStudentIdAndStatus(studentId, BookingStatus.COMPLETED);

        Stats stats = Stats.builder()
                .totalIndividualRemaining(totalInd)
                .totalGroupRemaining(totalGrp)
                .completedLessons(completed)
                .hasLowBalance(hasLowBalance)
                .build();

        return DashboardResponse.builder()
                .activeCourses(activeCourses)
                .upcomingBookings(upcoming)
                .stats(stats)
                .build();
    }
}
