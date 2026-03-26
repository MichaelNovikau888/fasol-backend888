package com.fasol.service.impl;

import com.fasol.domain.entity.Booking;
import com.fasol.domain.entity.OutboxEvent;
import com.fasol.domain.entity.Schedule;
import com.fasol.domain.entity.StudentCourse;
import com.fasol.domain.entity.Teacher;
import com.fasol.domain.entity.User;
import com.fasol.domain.enums.BookingStatus;
import com.fasol.domain.enums.LessonType;
import com.fasol.dto.request.BookingRequest;
import com.fasol.dto.response.BookingResponse;
import com.fasol.event.BookingCancelledEvent;
import com.fasol.event.BookingCreatedEvent;
import com.fasol.exception.BookingCancellationTooLateException;
import com.fasol.exception.BookingConflictException;
import com.fasol.exception.InsufficientLessonsException;
import com.fasol.exception.ResourceNotFoundException;
import com.fasol.exception.SlotFullException;
import com.fasol.metrics.BookingMetrics;
import com.fasol.metrics.SalesMetrics;
import com.fasol.repository.BookingRepository;
import com.fasol.repository.OutboxRepository;
import com.fasol.repository.ScheduleRepository;
import com.fasol.repository.StudentCourseRepository;
import com.fasol.repository.TeacherRepository;
import com.fasol.repository.UserRepository;
import com.fasol.service.BookingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ScheduleRepository scheduleRepository;
    private final StudentCourseRepository studentCourseRepository;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final OutboxRepository outboxRepository;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final BookingMetrics metrics;
    private final SalesMetrics salesMetrics;

    @Value("${app.booking.cancel-deadline-hours:24}")
    private int cancelDeadlineHours;

    @Value("${app.booking.group-max-participants:3}")
    private int groupMaxParticipants;

    @Override
    @Transactional
    public BookingResponse createBooking(UUID studentId, BookingRequest request) {
        String lockKey = "slot:" + request.getScheduleId() + ":" + request.getBookingDate();
        RLock lock = redissonClient.getLock(lockKey);
        Timer.Sample sample = metrics.startTimer();
        try {
            boolean acquired = lock.tryLock(3, 5, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BookingConflictException("Слот временно недоступен, попробуйте ещё раз");
            }
            try {
                return doCreate(studentId, request);
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BookingConflictException("Операция прервана");
        } finally {
            metrics.stopTimer(sample);
        }
    }

    private BookingResponse doCreate(UUID studentId, BookingRequest request) {
        Schedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Расписание не найдено"));
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Студент не найден"));

        if (schedule.getLessonType() == LessonType.GROUP) {
            long taken = bookingRepository.countConfirmedByScheduleAndDate(
                    schedule.getId(), request.getBookingDate());
            if (taken >= groupMaxParticipants) {
                metrics.incrementSlotConflict();
                throw new SlotFullException("Групповое занятие заполнено (макс. " + groupMaxParticipants + " чел.)");
            }
        }

        StudentCourse sc = studentCourseRepository
                .findByStudentIdAndCourseId(studentId, schedule.getCourse().getId())
                .orElseThrow(() -> new InsufficientLessonsException("Нет активного абонемента на этот курс"));

        decrementBalance(sc, schedule.getLessonType());

        Booking booking = Booking.builder()
                .student(student).schedule(schedule).studentCourse(sc)
                .bookingDate(request.getBookingDate()).status(BookingStatus.CONFIRMED)
                .build();
        booking = bookingRepository.save(booking);

        saveOutbox(booking, "BOOKING_CREATED");
        log.info("Booking created: {} student={} date={}", booking.getId(), studentId, request.getBookingDate());
        metrics.incrementBookingSuccess();
        salesMetrics.incrementDailyBooking();
        return BookingResponse.from(booking);
    }

    @Override
    @Transactional
    public void cancelBooking(UUID bookingId, UUID cancelledBy) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Бронирование не найдено"));
        if (booking.getStatus() != BookingStatus.CONFIRMED)
            throw new IllegalStateException("Нельзя отменить бронирование со статусом: " + booking.getStatus());

        LocalDateTime lessonStart = booking.getBookingDate().atTime(booking.getSchedule().getStartTime());
        if (LocalDateTime.now().isAfter(lessonStart.minusHours(cancelDeadlineHours)))
            throw new BookingCancellationTooLateException("Отмена невозможна: до занятия менее " + cancelDeadlineHours + " ч.");

        User canceller = userRepository.findById(cancelledBy)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledBy(canceller);
        booking.setCancelledAt(LocalDateTime.now());

        if (booking.getStudentCourse() != null)
            incrementBalance(booking.getStudentCourse(), booking.getSchedule().getLessonType());

        saveOutbox(booking, "BOOKING_CANCELLED");
        log.info("Booking {} cancelled by {}", bookingId, cancelledBy);
        metrics.incrementCancellation();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getStudentBookings(UUID studentId) {
        return bookingRepository.findByStudentIdWithDetails(studentId).stream().map(BookingResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getTeacherBookings(UUID teacherUserId, String weekStartStr) {
        Teacher teacher = teacherRepository.findByUserId(teacherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Преподаватель не найден"));
        LocalDate from = LocalDate.parse(weekStartStr);
        return bookingRepository.findByTeacherAndDateRange(teacher.getId(), from, from.plusDays(6))
                .stream().map(BookingResponse::from).toList();
    }

    private void decrementBalance(StudentCourse sc, LessonType type) {
        if (type == LessonType.INDIVIDUAL) {
            if (sc.getIndividualLessonsRemaining() <= 0) throw new InsufficientLessonsException("Не осталось индивидуальных занятий");
            sc.setIndividualLessonsRemaining(sc.getIndividualLessonsRemaining() - 1);
        } else {
            if (sc.getGroupLessonsRemaining() <= 0) throw new InsufficientLessonsException("Не осталось групповых занятий");
            sc.setGroupLessonsRemaining(sc.getGroupLessonsRemaining() - 1);
        }
        studentCourseRepository.save(sc);
    }

    private void incrementBalance(StudentCourse sc, LessonType type) {
        if (type == LessonType.INDIVIDUAL) sc.setIndividualLessonsRemaining(sc.getIndividualLessonsRemaining() + 1);
        else sc.setGroupLessonsRemaining(sc.getGroupLessonsRemaining() + 1);
        studentCourseRepository.save(sc);
    }

    @Override
    @Transactional
    public void adminCancelBooking(UUID bookingId, UUID adminId, boolean penalize) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Бронирование не найдено"));

        if (booking.getStatus() != BookingStatus.CONFIRMED)
            throw new IllegalStateException("Нельзя отменить бронирование со статусом: " + booking.getStatus());

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledBy(admin);
        booking.setCancelledAt(LocalDateTime.now());

        if (!penalize && booking.getStudentCourse() != null) {
            incrementBalance(booking.getStudentCourse(), booking.getSchedule().getLessonType());
        }

        bookingRepository.save(booking);
        saveOutbox(booking, "BOOKING_CANCELLED");
        log.info("Booking {} admin-cancelled by {} penalize={}", bookingId, adminId, penalize);
        metrics.incrementCancellation();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getAdminWeekBookings(LocalDate weekStart, UUID teacherId) {
        LocalDate weekEnd = weekStart.plusDays(6);
        if (teacherId != null) {
            Teacher teacher = teacherRepository.findByUserId(teacherId)
                    .orElseThrow(() -> new ResourceNotFoundException("Преподаватель не найден"));
            return bookingRepository.findByTeacherAndDateRange(teacher.getId(), weekStart, weekEnd)
                    .stream().map(BookingResponse::from).toList();
        }
        return bookingRepository.findAllConfirmedByDateRange(weekStart, weekEnd)
                .stream().map(BookingResponse::from).toList();
    }

    private void saveOutbox(Booking b, String eventType) {
        try {
            Object payload = switch (eventType) {
                case "BOOKING_CREATED" -> BookingCreatedEvent.builder()
                        .bookingId(b.getId().toString()).studentId(b.getStudent().getId().toString())
                        .studentEmail(b.getStudent().getEmail()).scheduleId(b.getSchedule().getId().toString())
                        .bookingDate(b.getBookingDate().toString()).lessonType(b.getSchedule().getLessonType().name())
                        .teacherName(b.getSchedule().getTeacher().getUser().getFirstName() + " " + b.getSchedule().getTeacher().getUser().getLastName())
                        .courseName(b.getSchedule().getCourse().getName()).build();
                case "BOOKING_CANCELLED" -> BookingCancelledEvent.builder()
                        .bookingId(b.getId().toString()).studentId(b.getStudent().getId().toString())
                        .studentEmail(b.getStudent().getEmail()).cancelledAt(b.getCancelledAt().toString())
                        .lessonType(b.getSchedule().getLessonType().name()).build();
                default -> throw new IllegalArgumentException("Unknown event: " + eventType);
            };
            outboxRepository.save(OutboxEvent.builder()
                    .aggregateType("Booking").aggregateId(b.getId().toString())
                    .eventType(eventType).payload(objectMapper.writeValueAsString(payload)).build());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox event", e);
        }
    }
}
