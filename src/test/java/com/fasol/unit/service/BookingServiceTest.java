package com.fasol.unit.service;

import com.fasol.domain.entity.Booking;
import com.fasol.domain.entity.Course;
import com.fasol.domain.entity.Schedule;
import com.fasol.domain.entity.StudentCourse;
import com.fasol.domain.entity.Teacher;
import com.fasol.domain.entity.User;
import com.fasol.domain.enums.BookingStatus;
import com.fasol.domain.enums.LessonType;
import com.fasol.dto.request.BookingRequest;
import com.fasol.exception.BookingCancellationTooLateException;
import com.fasol.exception.InsufficientLessonsException;
import com.fasol.exception.SlotFullException;
import com.fasol.metrics.BookingMetrics;
import com.fasol.repository.BookingRepository;
import com.fasol.repository.OutboxRepository;
import com.fasol.repository.ScheduleRepository;
import com.fasol.repository.StudentCourseRepository;
import com.fasol.repository.TeacherRepository;
import com.fasol.repository.UserRepository;
import com.fasol.service.impl.BookingServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock ScheduleRepository scheduleRepository;
    @Mock StudentCourseRepository studentCourseRepository;
    @Mock UserRepository userRepository;
    @Mock TeacherRepository teacherRepository;
    @Mock OutboxRepository outboxRepository;
    @Mock RedissonClient redissonClient;
    @Mock ObjectMapper objectMapper;
    @Mock BookingMetrics metrics;
    @Mock RLock rLock;
    @Mock Timer.Sample timerSample;

    @InjectMocks
    BookingServiceImpl bookingService;

    private User student;
    private Teacher teacher;
    private Course course;
    private Schedule groupSchedule;
    private StudentCourse studentCourse;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(bookingService, "cancelDeadlineHours", 24);
        ReflectionTestUtils.setField(bookingService, "groupMaxParticipants", 3);

        student = User.builder().id(UUID.randomUUID())
                .email("s@test.com").firstName("Иван").lastName("П").build();

        User teacherUser = User.builder().id(UUID.randomUUID())
                .email("t@test.com").firstName("Анна").lastName("И").build();

        teacher = Teacher.builder().id(UUID.randomUUID()).user(teacherUser).build();

        course = Course.builder().id(UUID.randomUUID()).name("Вокал")
                .price(BigDecimal.valueOf(300)).individualLessons(4).groupLessons(8).build();

        groupSchedule = Schedule.builder().id(UUID.randomUUID())
                .teacher(teacher).course(course).dayOfWeek(1)
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0))
                .lessonType(LessonType.GROUP).maxParticipants(3).build();

        studentCourse = StudentCourse.builder().id(UUID.randomUUID())
                .student(student).course(course)
                .individualLessonsRemaining(4).groupLessonsRemaining(4).build();
    }

    @Test
    @DisplayName("Успешное бронирование — списывает групповые занятия")
    void createBooking_success_decrementsGroupBalance() throws Exception {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(metrics.startTimer()).thenReturn(timerSample);

        BookingRequest req = BookingRequest.builder()
                .scheduleId(groupSchedule.getId())
                .bookingDate(LocalDate.now().plusDays(3)).build();

        when(scheduleRepository.findById(groupSchedule.getId())).thenReturn(Optional.of(groupSchedule));
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(bookingRepository.countConfirmedByScheduleAndDate(any(), any())).thenReturn(1L);
        when(studentCourseRepository.findByStudentIdAndCourseId(any(), any()))
                .thenReturn(Optional.of(studentCourse));
        when(bookingRepository.save(any())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            ReflectionTestUtils.setField(b, "id", UUID.randomUUID());
            return b;
        });
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        bookingService.createBooking(student.getId(), req);

        assertThat(studentCourse.getGroupLessonsRemaining()).isEqualTo(3);
        assertThat(studentCourse.getIndividualLessonsRemaining()).isEqualTo(4);
        verify(outboxRepository).save(argThat(e -> "BOOKING_CREATED".equals(e.getEventType())));
        verify(metrics).incrementBookingSuccess();
    }

    @Test
    @DisplayName("SlotFullException когда все 3 места заняты")
    void createBooking_slotFull_throws() throws Exception {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(metrics.startTimer()).thenReturn(timerSample);
        when(scheduleRepository.findById(any())).thenReturn(Optional.of(groupSchedule));
        when(userRepository.findById(any())).thenReturn(Optional.of(student));
        when(bookingRepository.countConfirmedByScheduleAndDate(any(), any())).thenReturn(3L);

        assertThatThrownBy(() -> bookingService.createBooking(student.getId(),
                BookingRequest.builder().scheduleId(groupSchedule.getId())
                        .bookingDate(LocalDate.now().plusDays(3)).build()))
                .isInstanceOf(SlotFullException.class);

        verify(bookingRepository, never()).save(any());
        verify(metrics).incrementSlotConflict();
    }

    @Test
    @DisplayName("InsufficientLessonsException при нулевом балансе")
    void createBooking_noLessons_throws() throws Exception {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(metrics.startTimer()).thenReturn(timerSample);
        studentCourse.setGroupLessonsRemaining(0);

        when(scheduleRepository.findById(any())).thenReturn(Optional.of(groupSchedule));
        when(userRepository.findById(any())).thenReturn(Optional.of(student));
        when(bookingRepository.countConfirmedByScheduleAndDate(any(), any())).thenReturn(0L);
        when(studentCourseRepository.findByStudentIdAndCourseId(any(), any()))
                .thenReturn(Optional.of(studentCourse));

        assertThatThrownBy(() -> bookingService.createBooking(student.getId(),
                BookingRequest.builder().scheduleId(groupSchedule.getId())
                        .bookingDate(LocalDate.now().plusDays(3)).build()))
                .isInstanceOf(InsufficientLessonsException.class);
    }

    @Test
    @DisplayName("BookingCancellationTooLateException при отмене менее чем за 24 ч")
    void cancelBooking_tooLate_throws() {
        Schedule tightSchedule = Schedule.builder().id(UUID.randomUUID())
                .teacher(teacher).course(course).dayOfWeek(1)
                .startTime(LocalTime.now().plusHours(12))
                .endTime(LocalTime.now().plusHours(13))
                .lessonType(LessonType.GROUP).maxParticipants(3).build();

        Booking booking = Booking.builder().id(UUID.randomUUID())
                .student(student).schedule(tightSchedule)
                .bookingDate(LocalDate.now()).status(BookingStatus.CONFIRMED).build();

        when(bookingRepository.findById(any())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(booking.getId(), student.getId()))
                .isInstanceOf(BookingCancellationTooLateException.class);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Успешная отмена — возвращает занятие на баланс (compensation)")
    void cancelBooking_success_restoresBalance() throws Exception {
        studentCourse.setGroupLessonsRemaining(3);

        Booking booking = Booking.builder().id(UUID.randomUUID())
                .student(student).schedule(groupSchedule).studentCourse(studentCourse)
                .bookingDate(LocalDate.now().plusDays(3)).status(BookingStatus.CONFIRMED).build();

        when(bookingRepository.findById(any())).thenReturn(Optional.of(booking));
        when(userRepository.findById(any())).thenReturn(Optional.of(student));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        bookingService.cancelBooking(booking.getId(), student.getId());

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getCancelledAt()).isNotNull();
        assertThat(studentCourse.getGroupLessonsRemaining()).isEqualTo(4);
        verify(outboxRepository).save(argThat(e -> "BOOKING_CANCELLED".equals(e.getEventType())));
        verify(metrics).incrementCancellation();
    }
}
