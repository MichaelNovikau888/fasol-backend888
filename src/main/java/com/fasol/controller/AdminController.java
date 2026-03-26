package com.fasol.controller;

import com.fasol.domain.enums.AppRole;
import com.fasol.domain.enums.TrialRequestStatus;
import com.fasol.domain.entity.SiteContent;
import com.fasol.dto.response.BookingResponse;
import com.fasol.dto.response.ScheduleResponse;
import com.fasol.dto.response.TrialRequestResponse;
import com.fasol.dto.response.UserResponse;
import com.fasol.messaging.producer.OutboxEventPublisher;
import com.fasol.security.CurrentUser;
import com.fasol.service.BookingService;
import com.fasol.service.ScheduleService;
import com.fasol.service.SiteContentService;
import com.fasol.service.TrialRequestService;
import com.fasol.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Контроллер администратора.
 *
 * Покрывает все вкладки AdminDashboard.tsx:
 *
 *  1. Обзор         — навигационные карточки, данных не требует
 *  2. Заявки        — GET/PATCH trial-requests (те же что у менеджера)
 *  3. Контент сайта — GET/PUT site-content (карточки, галереи, видео)
 *  4. Расписание    — GET schedules + bookings на неделю + admin-cancel
 *  5. Преподаватели — те же schedules/bookings через teacher-фильтр + admin-cancel
 *
 * Отличие от менеджера:
 *  - Доступно только ADMIN
 *  - adminCancelBooking обходит 24-часовой дедлайн (с явным флагом penalize)
 *    и сам управляет балансом студента — как делает фронт в AdminScheduleView
 *    и AdminTeachersView
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final SiteContentService siteContentService;
    private final TrialRequestService trialRequestService;
    private final BookingService bookingService;
    private final ScheduleService scheduleService;
    private final OutboxEventPublisher outboxPublisher;
    private final UserAdminService userAdminService;

    // ── 2. Заявки (Trial Requests) ────────────────────────────────────────────
    // Используется в AdminDashboard вкладка "Заявки" через TrialRequestsManagement

    @GetMapping("/trial-requests")
    public ResponseEntity<List<TrialRequestResponse>> trialRequests() {
        return ResponseEntity.ok(trialRequestService.getAll());
    }

    @PatchMapping("/trial-requests/{id}/status")
    public ResponseEntity<TrialRequestResponse> updateTrialStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        var status = TrialRequestStatus.valueOf(body.get("status").toUpperCase());
        return ResponseEntity.ok(
                trialRequestService.updateStatus(id, status, body.get("notes")));
    }

    // ── 3. Контент сайта (ContentManagement.tsx) ──────────────────────────────
    //
    // ContentManagement работает с секциями:
    //   Карточки: feature_rating, feature_method, feature_teachers, feature_schedule
    //   Галереи:  mission_gallery, studios_gallery
    //   Видео:    events_concerts, events_reports, events_outdoor,
    //             events_masterclass, students_videos
    //
    // Все операции через upsert: title, description, imageUrl, content (JSON).
    // Файлы загружаются напрямую в Supabase Storage — бэкенд получает только URL.

    @GetMapping("/site-content")
    public ResponseEntity<List<SiteContent>> getContent() {
        return ResponseEntity.ok(siteContentService.getActiveContent());
    }

    /**
     * Обновить секцию контента.
     * Тело: { "title": "...", "description": "...", "imageUrl": "...", "content": "{...}" }
     *
     * Поле content — JSON-строка, хранится как JSONB.
     * Для карточек: { "yandex_url": "..." }
     * Для галерей:  { "images": ["url1", "url2"] }
     * Для видео:    { "videos": [{"url":"...", "title":"...", "name":"..."}] }
     */
    @PutMapping("/site-content/{sectionKey}")
    public ResponseEntity<SiteContent> upsertContent(
            @PathVariable String sectionKey,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(siteContentService.upsert(
                sectionKey,
                body.get("title"),
                body.get("description"),
                body.get("imageUrl"),
                body.get("content")));
    }

    // ── 4. Расписание (AdminScheduleView.tsx) ─────────────────────────────────
    //
    // Показывает все слоты всех преподавателей на выбранную неделю.
    // Фильтр по преподавателю на фронте — фронт фильтрует локально,
    // но для эффективности добавляем опциональный параметр teacherId.

    /**
     * Все расписания — для заполнения таблицы и фильтра по преподавателям.
     */
    @GetMapping("/schedules")
    public ResponseEntity<List<ScheduleResponse>> schedules() {
        return ResponseEntity.ok(scheduleService.getAllSchedules());
    }

    /**
     * Все подтверждённые бронирования на неделю (для AdminScheduleView).
     *
     * Параметры:
     *   weekStart — начало недели в формате yyyy-MM-dd (по умолчанию — текущая неделя)
     *   teacherId — UUID преподавателя (опционально, для фильтра)
     */
    @GetMapping("/bookings/week")
    public ResponseEntity<List<BookingResponse>> weekBookings(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam(required = false) UUID teacherId) {
        LocalDate from = weekStart != null ? weekStart : LocalDate.now()
                .with(DayOfWeek.MONDAY);
        return ResponseEntity.ok(
                bookingService.getAdminWeekBookings(from, teacherId));
    }

    /**
     * Отмена занятия администратором.
     *
     * Отличие от студенческой отмены (/api/bookings/{id}):
     *  - Нет ограничения 24 часа — админ может отменить любое занятие
     *  - Параметр penalize управляет возвратом баланса:
     *      penalize=true  → занятие НЕ возвращается (штраф, < 24ч)
     *      penalize=false → занятие возвращается на баланс студента
     *
     * Тело: { "penalize": true }
     *
     * Это воспроизводит логику AdminScheduleView.tsx handleAdminCancel()
     * и AdminTeachersView.tsx handleCancel() — оба делали то же самое
     * напрямую в Supabase двумя запросами без транзакции.
     * Здесь — атомарно в одной транзакции.
     */
    @DeleteMapping("/bookings/{bookingId}")
    public ResponseEntity<Void> adminCancelBooking(
            @PathVariable UUID bookingId,
            @CurrentUser UUID adminId,
            @RequestBody(required = false) Map<String, Boolean> body) {
        boolean penalize = body != null && Boolean.TRUE.equals(body.get("penalize"));
        bookingService.adminCancelBooking(bookingId, adminId, penalize);
        return ResponseEntity.noContent().build();
    }

    // ── 5. Преподаватели (AdminTeachersView.tsx) ──────────────────────────────
    //
    // Список преподавателей — через /api/teacher/me (публичный для authenticated)
    // Расписание конкретного преподавателя — через /api/admin/bookings/week?teacherId=...
    // Отмена занятий — через /api/admin/bookings/{id} с penalize флагом
    // (см. выше — один endpoint для обеих вкладок)

    // ── Пользователи (RoleManagement.tsx) ────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> listAllUsers() {
        return ResponseEntity.ok(userAdminService.getAllUsers());
    }

    @PatchMapping("/users/{userId}/roles")
    public ResponseEntity<UserResponse> updateRoles(
            @PathVariable UUID userId,
            @RequestBody Map<String, List<String>> body) {
        List<String> rawRoles = body.get("roles");
        if (rawRoles == null || rawRoles.isEmpty())
            throw new IllegalArgumentException("Список ролей не может быть пустым");
        Set<AppRole> roles = rawRoles.stream()
                .map(r -> {
                    try { return AppRole.valueOf(r.toUpperCase()); }
                    catch (IllegalArgumentException e) { throw new IllegalArgumentException("Неизвестная роль: " + r); }
                })
                .collect(Collectors.toSet());
        return ResponseEntity.ok(userAdminService.updateRoles(userId, roles));
    }

    // ── Outbox / Dead Letter (служебное) ─────────────────────────────────────

    /**
     * Переотправка dead-letter событий из outbox.
     * POST /api/admin/outbox/requeue → { "requeued": 42 }
     */
    @PostMapping("/outbox/requeue")
    public ResponseEntity<Map<String, Integer>> requeueDeadLetters() {
        int count = outboxPublisher.requeueDeadLetterEvents();
        return ResponseEntity.ok(Map.of("requeued", count));
    }
}
