package com.fasol.unit.service;

import com.fasol.domain.entity.Course;
import com.fasol.domain.entity.StudentCourse;
import com.fasol.domain.entity.User;
import com.fasol.dto.request.CourseRequest;
import com.fasol.dto.response.CourseResponse;
import com.fasol.dto.response.StudentCourseResponse;
import com.fasol.exception.ResourceNotFoundException;
import com.fasol.repository.CourseRepository;
import com.fasol.repository.StudentCourseRepository;
import com.fasol.repository.UserRepository;
import com.fasol.service.impl.CourseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock CourseRepository courseRepository;
    @Mock StudentCourseRepository studentCourseRepository;
    @Mock UserRepository userRepository;

    @InjectMocks CourseServiceImpl courseService;

    private Course course;
    private User student;

    @BeforeEach
    void setUp() {
        course = Course.builder()
                .id(UUID.randomUUID())
                .name("Вокал базовый")
                .price(BigDecimal.valueOf(5000))
                .individualLessons(4)
                .groupLessons(8)
                .active(true)
                .build();

        student = User.builder()
                .id(UUID.randomUUID())
                .email("student@fasol.ru")
                .build();
    }

    @Test
    @DisplayName("getActiveCourses — возвращает только активные")
    void getActiveCourses_returnsOnlyActive() {
        when(courseRepository.findByActiveTrue()).thenReturn(List.of(course));

        List<CourseResponse> result = courseService.getActiveCourses();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Вокал базовый");
        assertThat(result.get(0).isActive()).isTrue();
    }

    @Test
    @DisplayName("createCourse — сохраняет и возвращает DTO")
    void createCourse_savesAndReturns() {
        CourseRequest req = CourseRequest.builder()
                .name("Новый курс")
                .individualLessons(2)
                .groupLessons(4)
                .price(BigDecimal.valueOf(3000))
                .build();

        when(courseRepository.save(any())).thenReturn(
                Course.builder().id(UUID.randomUUID()).name("Новый курс")
                        .individualLessons(2).groupLessons(4)
                        .price(BigDecimal.valueOf(3000)).active(true).build());

        CourseResponse result = courseService.createCourse(req);

        assertThat(result.getName()).isEqualTo("Новый курс");
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    @DisplayName("toggleActive — инвертирует флаг активности")
    void toggleActive_invertFlag() {
        course.setActive(true);
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(courseRepository.save(any())).thenReturn(course);

        courseService.toggleActive(course.getId());

        assertThat(course.isActive()).isFalse();
        verify(courseRepository).save(course);
    }

    @Test
    @DisplayName("toggleActive — бросает ResourceNotFoundException если курс не найден")
    void toggleActive_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(courseRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.toggleActive(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("purchaseCourse — первая покупка без скидки, repeatPurchase=false")
    void purchaseCourse_firstTime_noDiscount() {
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(studentCourseRepository.existsByStudentIdAndCourseId(any(), any())).thenReturn(false);
        when(studentCourseRepository.save(any())).thenAnswer(inv -> {
            StudentCourse sc = inv.getArgument(0);
            ReflectionTestUtils.setField(sc, "id", UUID.randomUUID());
            return sc;
        });

        StudentCourseResponse result = courseService.purchaseCourse(student.getId(), course.getId());

        assertThat(result.isRepeatPurchase()).isFalse();
        assertThat(result.getIndividualLessonsRemaining()).isEqualTo(4);
        assertThat(result.getGroupLessonsRemaining()).isEqualTo(8);
    }

    @Test
    @DisplayName("purchaseCourse — повторная покупка устанавливает repeatPurchase=true")
    void purchaseCourse_repeat_setsFlag() {
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(studentCourseRepository.existsByStudentIdAndCourseId(any(), any())).thenReturn(true);
        when(studentCourseRepository.save(any())).thenAnswer(inv -> {
            StudentCourse sc = inv.getArgument(0);
            ReflectionTestUtils.setField(sc, "id", UUID.randomUUID());
            return sc;
        });

        StudentCourseResponse result = courseService.purchaseCourse(student.getId(), course.getId());

        assertThat(result.isRepeatPurchase()).isTrue();
    }

    @Test
    @DisplayName("addLessonsManually — увеличивает баланс занятий")
    void addLessonsManually_increasesBalance() {
        StudentCourse sc = StudentCourse.builder()
                .id(UUID.randomUUID()).student(student).course(course)
                .individualLessonsRemaining(2).groupLessonsRemaining(1).build();

        when(studentCourseRepository.findById(sc.getId())).thenReturn(Optional.of(sc));
        when(studentCourseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StudentCourseResponse result = courseService.addLessonsManually(sc.getId(), 3, 2);

        assertThat(result.getIndividualLessonsRemaining()).isEqualTo(5);
        assertThat(result.getGroupLessonsRemaining()).isEqualTo(3);
    }
}
