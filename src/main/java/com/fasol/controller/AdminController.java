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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final SiteContentService siteContentService;
    private final TrialRequestService trialRequestService;
    private final BookingService bookingService;
    private final ScheduleService scheduleService;
    private final UserAdminService userAdminService;
    private final Optional<OutboxEventPublisher> outboxPublisher;

    @Autowired
    public AdminController(SiteContentService siteContentService,
                           TrialRequestService trialRequestService,
                           BookingService bookingService,
                           ScheduleService scheduleService,
                           UserAdminService userAdminService,
                           Optional<OutboxEventPublisher> outboxPublisher) {
        this.siteContentService = siteContentService;
        this.trialRequestService = trialRequestService;
        this.bookingService = bookingService;
        this.scheduleService = scheduleService;
        this.userAdminService = userAdminService;
        this.outboxPublisher = outboxPublisher;
    }

    // ── 2. Заявки (Trial Requests) ────────────────────────────────────────────

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

    @GetMapping("/site-content")
    public ResponseEntity<List<SiteContent>> getContent() {
        return ResponseEntity.ok(siteContentService.getActiveContent());
    }

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

    @GetMapping("/schedules")
    public ResponseEntity<List<ScheduleResponse>> schedules() {
        return ResponseEntity.ok(scheduleService.getAllSchedules());
    }

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

    @DeleteMapping("/bookings/{bookingId}")
    public ResponseEntity<Void> adminCancelBooking(
            @PathVariable UUID bookingId,
            @CurrentUser UUID adminId,
            @RequestBody(required = false) Map<String, Boolean> body) {
        boolean penalize = body != null && Boolean.TRUE.equals(body.get("penalize"));
        bookingService.adminCancelBooking(bookingId, adminId, penalize);
        return ResponseEntity.noContent().build();
    }

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
     * Недоступен если Kafka отключена (профиль railway без Kafka).
     */
    @PostMapping("/outbox/requeue")
    public ResponseEntity<Map<String, Object>> requeueDeadLetters() {
        return outboxPublisher
                .map(publisher -> {
                    int count = publisher.requeueDeadLetterEvents();
                    return ResponseEntity.ok(Map.<String, Object>of("requeued", count));
                })
                .orElse(ResponseEntity.ok(
                        Map.of("requeued", 0, "message", "Kafka не подключена")));
    }
}