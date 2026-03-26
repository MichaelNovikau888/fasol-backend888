package com.fasol.controller;

import com.fasol.domain.enums.AppRole;
import com.fasol.metrics.SalesMetrics;
import com.fasol.exception.ResourceNotFoundException;
import com.fasol.domain.enums.TrialRequestStatus;
import com.fasol.dto.request.CourseRequest;
import com.fasol.dto.request.ScheduleRequest;
import com.fasol.dto.response.CourseResponse;
import com.fasol.dto.response.ScheduleResponse;
import com.fasol.dto.response.StudentCourseResponse;
import com.fasol.dto.response.TeacherResponse;
import com.fasol.dto.response.TrialRequestResponse;
import com.fasol.dto.response.UserResponse;
import com.fasol.service.CourseService;
import com.fasol.service.ScheduleService;
import com.fasol.service.TeacherService;
import com.fasol.service.TrialRequestService;
import com.fasol.service.UserAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
public class ManagerController {

    private final TrialRequestService trialRequestService;
    private final CourseService courseService;
    private final TeacherService teacherService;
    private final ScheduleService scheduleService;
    private final UserAdminService userAdminService;
    private final SalesMetrics salesMetrics;

    // ── 1. Заявки (Trial requests) ────────────────────────────────────────────

    @GetMapping("/trial-requests")
    public ResponseEntity<List<TrialRequestResponse>> trialRequests() {
        return ResponseEntity.ok(trialRequestService.getAll());
    }

    @PatchMapping("/trial-requests/{id}/status")
    public ResponseEntity<TrialRequestResponse> updateTrialStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        var status = TrialRequestStatus.valueOf(body.get("status").toUpperCase());
        return ResponseEntity.ok(trialRequestService.updateStatus(id, status, body.get("notes")));
    }

    // ── 2. Тарифы (Courses) ───────────────────────────────────────────────────

    @GetMapping("/courses")
    public ResponseEntity<List<CourseResponse>> courses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @PostMapping("/courses")
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CourseRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.createCourse(req));
    }

    @PutMapping("/courses/{id}")
    public ResponseEntity<CourseResponse> updateCourse(
            @PathVariable UUID id, @Valid @RequestBody CourseRequest req) {
        return ResponseEntity.ok(courseService.updateCourse(id, req));
    }

    @PatchMapping("/courses/{id}/toggle")
    public ResponseEntity<Void> toggleCourse(@PathVariable UUID id) {
        courseService.toggleActive(id);
        return ResponseEntity.noContent().build();
    }

    // ── 3. Преподаватели (Teachers) ───────────────────────────────────────────

    @GetMapping("/teachers")
    public ResponseEntity<List<TeacherResponse>> teachers() {
        return ResponseEntity.ok(teacherService.getAllTeachers());
    }

    @PostMapping("/teachers")
    public ResponseEntity<TeacherResponse> createTeacher(@RequestBody Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                teacherService.createTeacher(
                        UUID.fromString(body.get("userId")),
                        body.get("bio"),
                        body.get("specialization")));
    }

    @PatchMapping("/teachers/{id}/toggle")
    public ResponseEntity<Void> toggleTeacher(@PathVariable UUID id) {
        teacherService.toggleActive(id);
        return ResponseEntity.noContent().build();
    }

    // ── 4. Расписание (Schedules) ─────────────────────────────────────────────

    @GetMapping("/schedules")
    public ResponseEntity<List<ScheduleResponse>> schedules() {
        return ResponseEntity.ok(scheduleService.getAllSchedules());
    }

    @PostMapping("/schedules")
    public ResponseEntity<ScheduleResponse> createSchedule(@Valid @RequestBody ScheduleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.create(req));
    }

    @PutMapping("/schedules/{id}")
    public ResponseEntity<ScheduleResponse> updateSchedule(
            @PathVariable UUID id, @Valid @RequestBody ScheduleRequest req) {
        return ResponseEntity.ok(scheduleService.update(id, req));
    }

    @PatchMapping("/schedules/{id}/toggle")
    public ResponseEntity<Void> toggleSchedule(@PathVariable UUID id) {
        scheduleService.toggleActive(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/schedules/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable UUID id) {
        scheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── 5. Роли (Role Management) ─────────────────────────────────────────────
    //
    // RoleManagement.tsx читает profiles + user_roles напрямую через Supabase.
    // Здесь даём REST-альтернативу: список пользователей с ролями и точечное
    // переключение одной роли (toggle), чтобы не перезаписывать весь набор.
    //
    // Менеджер может назначать и снимать любые роли включая ADMIN — как во фронте.

    /** Все пользователи с их ролями — таблица в RoleManagement.tsx */
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(userAdminService.getAllUsers());
    }

    /**
     * Переключить одну роль у пользователя (добавить если нет, убрать если есть).
     * Тело: { "role": "ADMIN" }
     *
     * Менеджер может устанавливать и снимать любые роли включая ADMIN.
     * Полная замена всех ролей доступна только через /api/admin/users/{id}/roles.
     */
    @PatchMapping("/users/{userId}/roles/toggle")
    public ResponseEntity<UserResponse> toggleRole(
            @PathVariable UUID userId,
            @RequestBody Map<String, String> body) {

        String rawRole = body.get("role");
        if (rawRole == null || rawRole.isBlank()) {
            throw new IllegalArgumentException("Поле 'role' обязательно");
        }

        AppRole role;
        try {
            role = AppRole.valueOf(rawRole.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Неизвестная роль: " + rawRole);
        }

        // Читаем текущие роли и переключаем
        UserResponse current = userAdminService.getAllUsers().stream()
                .filter(u -> u.getId().equals(userId.toString()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        Set<AppRole> currentRoles = current.getRoles().stream()
                .map(r -> AppRole.valueOf(r.toUpperCase()))
                .collect(Collectors.toSet());

        if (currentRoles.contains(role)) {
            // Нельзя оставить пользователя совсем без ролей
            if (currentRoles.size() == 1) {
                throw new IllegalArgumentException("Нельзя снять последнюю роль пользователя");
            }
            currentRoles.remove(role);
        } else {
            currentRoles.add(role);
        }

        return ResponseEntity.ok(userAdminService.updateRoles(userId, currentRoles));
    }

    // ── 6. Ученики (Student Management) ──────────────────────────────────────
    //
    // StudentManagement.tsx показывает всех студентов с тарифами.
    // Данные приходят из student_courses + profiles + trial_requests.

    /**
     * Все студенты с абонементами — таблица в StudentManagement.tsx.
     * trial_requests фронт пока получает отдельно через /api/manager/trial-requests.
     */
    @GetMapping("/students")
    public ResponseEntity<List<StudentCourseResponse>> allStudentCourses() {
        return ResponseEntity.ok(userAdminService.getAllStudentCourses());
    }

    /** Абонементы конкретного студента — детальный дровер */
    @GetMapping("/students/{userId}/courses")
    public ResponseEntity<List<StudentCourseResponse>> studentCourses(@PathVariable UUID userId) {
        return ResponseEntity.ok(userAdminService.getStudentCourses(userId));
    }

    /** Деактивировать / активировать студента */
    @PatchMapping("/users/{userId}/toggle")
    public ResponseEntity<UserResponse> toggleUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(userAdminService.toggleActive(userId));
    }

    // ── 7. Офлайн-оплата (Offline Payments) ──────────────────────────────────
    //
    // Менеджер принял оплату наличными → нужно вручную добавить занятия студенту.
    // Два сценария:
    //   A) Студент покупает новый тариф наличными → purchaseCourse + paidOnline=false
    //   B) Менеджер докидывает занятия к существующему абонементу вручную

    /**
     * Выдать тариф студенту при офлайн-оплате.
     * Тело: { "studentId": "uuid", "courseId": "uuid" }
     *
     * Создаёт StudentCourse с paidOnline=false.
     * Применяет скидку если студент уже покупал этот курс (repeatPurchase).
     */
    @PostMapping("/payments/offline/purchase")
    public ResponseEntity<StudentCourseResponse> offlinePurchase(
            @RequestBody Map<String, String> body) {
        UUID studentId = UUID.fromString(body.get("studentId"));
        UUID courseId  = UUID.fromString(body.get("courseId"));
        StudentCourseResponse result = courseService.purchaseCourse(studentId, courseId);
        salesMetrics.incrementPaymentReceived(true);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Студент пришёл на пробный урок и купил курс.
     * Тело: { "studentId": "uuid", "courseId": "uuid", "trialRequestId": "uuid" }
     *
     * Сохраняет связь trial_request → student_course в БД — это даёт точный
     * подсчёт конверсии воронки в Grafana:
     *   trial.conversion.total / trial.status.completed.total × 100 = конверсия %
     */
    @PostMapping("/payments/offline/purchase-after-trial")
    public ResponseEntity<StudentCourseResponse> purchaseAfterTrial(
            @RequestBody Map<String, String> body) {
        UUID studentId      = UUID.fromString(body.get("studentId"));
        UUID courseId       = UUID.fromString(body.get("courseId"));
        UUID trialRequestId = UUID.fromString(body.get("trialRequestId"));
        StudentCourseResponse result = courseService.purchaseCourseAfterTrial(
                studentId, courseId, trialRequestId);
        salesMetrics.incrementPaymentReceived(true);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Вручную добавить занятия к существующему абонементу.
     * Используется когда студент доплатил за дополнительные занятия.
     *
     * Тело: { "individualLessons": 2, "groupLessons": 0 }
     */
    @PatchMapping("/payments/offline/add-lessons/{studentCourseId}")
    public ResponseEntity<StudentCourseResponse> addLessonsManually(
            @PathVariable UUID studentCourseId,
            @RequestBody Map<String, Integer> body) {
        int individual = body.getOrDefault("individualLessons", 0);
        int group      = body.getOrDefault("groupLessons", 0);
        if (individual < 0 || group < 0) {
            throw new IllegalArgumentException("Количество занятий не может быть отрицательным");
        }
        if (individual == 0 && group == 0) {
            throw new IllegalArgumentException("Укажите хотя бы одно занятие для добавления");
        }
        return ResponseEntity.ok(courseService.addLessonsManually(studentCourseId, individual, group));
    }
}
