package com.fasol.controller;

import com.fasol.dto.response.StudentCourseResponse;
import com.fasol.security.CurrentUser;
import com.fasol.service.CourseService;
import com.fasol.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Контроллер платежей.
 *
 * Текущее состояние: заглушки — бизнес-логика готова к подключению Stripe.
 * Офлайн-оплата уже работает через ManagerController:
 *   POST /api/manager/payments/offline/purchase
 *   PATCH /api/manager/payments/offline/add-lessons/{id}
 *
 * Этот контроллер добавляет:
 *   1. История платежей студента        GET  /api/payments/my
 *   2. Инициация онлайн-оплаты (Stripe) POST /api/payments/checkout
 *   3. Webhook от Stripe                POST /api/payments/webhook
 *   4. История всех платежей (менеджер) GET  /api/manager/payments
 *   5. Подтверждение оплаты вручную     PATCH /api/manager/payments/{id}/confirm
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final CourseService courseService;
    private final UserAdminService userAdminService;

    // ── 1. История платежей студента ─────────────────────────────────────────

    /**
     * Все абонементы студента — видно что куплено, как оплачено (онлайн/офлайн).
     * Фронт: Dashboard.tsx → секция "Мои тарифы".
     *
     * paidOnline=true  → оплачено через Stripe
     * paidOnline=false → оплачено наличными (менеджер подтвердил)
     */
    @GetMapping("/api/payments/my")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<List<StudentCourseResponse>> myPayments(@CurrentUser UUID studentId) {
        return ResponseEntity.ok(courseService.getStudentCourses(studentId));
    }

    // ── 2. Инициация онлайн-оплаты (Stripe) ──────────────────────────────────

    /**
     * Создать Stripe Checkout Session для покупки курса.
     *
     * ЗАГЛУШКА — возвращает mock-ответ.
     * Для реального подключения нужно:
     *   1. Добавить stripe-java в pom.xml
     *   2. Прописать STRIPE_SECRET_KEY в application.yml
     *   3. Заменить тело метода на:
     *      SessionCreateParams params = SessionCreateParams.builder()
     *        .addLineItem(...)
     *        .setMode(SessionCreateParams.Mode.PAYMENT)
     *        .setSuccessUrl(successUrl)
     *        .setCancelUrl(cancelUrl)
     *        .build();
     *      Session session = Session.create(params);
     *      return ResponseEntity.ok(Map.of("checkoutUrl", session.getUrl()));
     *
     * Тело запроса: { "courseId": "uuid" }
     * Ответ: { "checkoutUrl": "https://checkout.stripe.com/..." }
     */
    @PostMapping("/api/payments/checkout")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> createCheckout(
            @CurrentUser UUID studentId,
            @RequestBody Map<String, String> body) {

        String courseId = body.get("courseId");
        if (courseId == null || courseId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "courseId обязателен"));
        }

        log.info("[STUB] Stripe checkout requested by student={} for course={}", studentId, courseId);

        // ЗАГЛУШКА — в реальном проекте здесь вызов Stripe API
        return ResponseEntity.ok(Map.of(
                "checkoutUrl", "https://checkout.stripe.com/stub/session_" + UUID.randomUUID(),
                "sessionId",   "cs_stub_" + UUID.randomUUID(),
                "status",      "stub — подключите Stripe для реальной оплаты"
        ));
    }

    // ── 3. Stripe Webhook ─────────────────────────────────────────────────────

    /**
     * Webhook от Stripe — вызывается после успешной оплаты.
     *
     * ЗАГЛУШКА — логирует payload и возвращает 200.
     * Для реального подключения нужно:
     *   1. Верифицировать подпись: Webhook.constructEvent(payload, sigHeader, endpointSecret)
     *   2. Обработать event.getType():
     *      - "checkout.session.completed" → courseService.purchaseCourse() с paidOnline=true
     *      - "payment_intent.payment_failed" → уведомить студента
     *
     * Stripe требует публичный URL — настройте через ngrok при локальной разработке:
     *   ngrok http 8080
     *   stripe listen --forward-to localhost:8080/api/payments/webhook
     */
    @PostMapping("/api/payments/webhook")
    public ResponseEntity<Map<String, String>> stripeWebhook(
            @RequestBody String payload,
            @org.springframework.web.bind.annotation.RequestHeader(
                    value = "Stripe-Signature", required = false) String signature) {

        log.info("[STUB] Stripe webhook received, signature={}, payload length={}",
                signature != null ? "present" : "absent", payload.length());

        // ЗАГЛУШКА — в реальном проекте парсим event и вызываем courseService
        return ResponseEntity.ok(Map.of("received", "true"));
    }

    // ── 4. История всех платежей (менеджер) ───────────────────────────────────

    /**
     * Все студенты с информацией об оплате — для отчётности менеджера.
     *
     * Возвращает все StudentCourse записи с флагами paidOnline и stripePaymentId.
     * Менеджер видит: кто оплатил онлайн, кто — наличными, у кого не оплачено.
     */
    @GetMapping("/api/manager/payments")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<StudentCourseResponse>> allPayments() {
        // Все абонементы всех студентов — виден флаг paidOnline и stripePaymentId
        return ResponseEntity.ok(userAdminService.getAllStudentCourses());
    }

    // ── 5. Ручное подтверждение оплаты менеджером ────────────────────────────

    /**
     * Менеджер вручную подтверждает что студент оплатил.
     * Устанавливает paidOnline=false (наличные) и фиксирует время подтверждения.
     *
     * ЗАГЛУШКА — логирует действие.
     * В реальном проекте: обновить StudentCourse.confirmedAt, отправить уведомление студенту.
     *
     * Тело: { "method": "cash" | "transfer", "note": "Оплата 22.03.2026" }
     */
    @PostMapping("/api/manager/payments/{studentCourseId}/confirm")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> confirmPayment(
            @PathVariable UUID studentCourseId,
            @RequestBody Map<String, String> body) {

        String method = body.getOrDefault("method", "cash");
        String note   = body.getOrDefault("note", "");

        log.info("[STUB] Payment confirmed: studentCourseId={} method={} note={}",
                studentCourseId, method, note);

        // ЗАГЛУШКА — в реальном проекте: обновить запись + отправить уведомление
        return ResponseEntity.ok(Map.of(
                "studentCourseId", studentCourseId.toString(),
                "method",          method,
                "confirmedAt",     LocalDateTime.now().toString(),
                "status",          "confirmed",
                "note",            note
        ));
    }
}
