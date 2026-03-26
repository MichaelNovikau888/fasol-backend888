package com.fasol.unit.service;

import com.fasol.domain.entity.Course;
import com.fasol.domain.entity.Schedule;
import com.fasol.domain.entity.StudentCourse;
import com.fasol.domain.entity.Teacher;
import com.fasol.domain.entity.User;
import com.fasol.domain.enums.BookingStatus;
import com.fasol.domain.enums.LessonType;
import com.fasol.dto.response.DashboardResponse;
import com.fasol.repository.BookingRepository;
import com.fasol.repository.StudentCourseRepository;
import com.fasol.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock StudentCourseRepository studentCourseRepository;
    @Mock BookingRepository bookingRepository;

    @InjectMocks DashboardServiceImpl dashboardService;

    private UUID studentId;
    private StudentCourse activeCourse;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();

        User student = User.builder().id(studentId).email("s@fasol.ru").build();
        User teacherUser = User.builder().id(UUID.randomUUID())
                .firstName("Анна").lastName("И").build();
        Teacher teacher = Teacher.builder().id(UUID.randomUUID()).user(teacherUser).build();

        Course course = Course.builder().id(UUID.randomUUID()).name("Вокал")
                .price(BigDecimal.valueOf(5000))
                .individualLessons(4).groupLessons(8).build();

        activeCourse = StudentCourse.builder().id(UUID.randomUUID())
                .student(student).course(course)
                .individualLessonsRemaining(3).groupLessonsRemaining(7).build();
    }

    @Test
    @DisplayName("getSummary — суммирует остатки занятий по всем активным абонементам")
    void getSummary_sumsTotalLessons() {
        when(studentCourseRepository.findActiveByStudentId(studentId))
                .thenReturn(List.of(activeCourse));
        when(bookingRepository.findUpcomingByStudentId(eq(studentId), any(), any()))
                .thenReturn(List.of());
        when(bookingRepository.countByStudentIdAndStatus(studentId, BookingStatus.COMPLETED))
                .thenReturn(5L);

        DashboardResponse result = dashboardService.getSummary(studentId);

        assertThat(result.getStats().getTotalIndividualRemaining()).isEqualTo(3);
        assertThat(result.getStats().getTotalGroupRemaining()).isEqualTo(7);
        assertThat(result.getStats().getCompletedLessons()).isEqualTo(5);
    }

    @Test
    @DisplayName("getSummary — hasLowBalance=false при нормальном балансе")
    void getSummary_normalBalance_noLowBalanceFlag() {
        // 3 из 4 индивидуальных — это 75%, не низкий баланс
        when(studentCourseRepository.findActiveByStudentId(studentId))
                .thenReturn(List.of(activeCourse));
        when(bookingRepository.findUpcomingByStudentId(eq(studentId), any(), any()))
                .thenReturn(List.of());
        when(bookingRepository.countByStudentIdAndStatus(any(), any())).thenReturn(0L);

        DashboardResponse result = dashboardService.getSummary(studentId);

        assertThat(result.getStats().isHasLowBalance()).isFalse();
    }

    @Test
    @DisplayName("getSummary — пустой дашборд для студента без абонементов")
    void getSummary_noActiveCourses_emptyDashboard() {
        when(studentCourseRepository.findActiveByStudentId(studentId))
                .thenReturn(List.of());
        when(bookingRepository.findUpcomingByStudentId(eq(studentId), any(), any()))
                .thenReturn(List.of());
        when(bookingRepository.countByStudentIdAndStatus(any(), any())).thenReturn(0L);

        DashboardResponse result = dashboardService.getSummary(studentId);

        assertThat(result.getActiveCourses()).isEmpty();
        assertThat(result.getUpcomingBookings()).isEmpty();
        assertThat(result.getStats().getTotalIndividualRemaining()).isZero();
        assertThat(result.getStats().isHasLowBalance()).isFalse();
    }
}
