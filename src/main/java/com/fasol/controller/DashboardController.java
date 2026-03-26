package com.fasol.controller;

import com.fasol.dto.response.DashboardResponse;
import com.fasol.security.CurrentUser;
import com.fasol.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Сводная страница студента (Dashboard.tsx).
 *
 * Один запрос возвращает всё нужное для главного экрана:
 *   - активные абонементы с остатком занятий
 *   - ближайшие подтверждённые бронирования (7 дней)
 *   - статистика (всего занятий пройдено, осталось дней до истечения)
 *
 * Почему один endpoint, а не три отдельных:
 *   Dashboard.tsx грузит страницу один раз — три параллельных запроса
 *   создают waterfall и race condition на UI. Один запрос = один spinner.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<DashboardResponse> summary(@CurrentUser UUID studentId) {
        return ResponseEntity.ok(dashboardService.getSummary(studentId));
    }
}
