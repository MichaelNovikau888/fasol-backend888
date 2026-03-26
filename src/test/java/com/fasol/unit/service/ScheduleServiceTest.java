package com.fasol.unit.service;

import com.fasol.domain.entity.Course;
import com.fasol.domain.entity.Schedule;
import com.fasol.domain.entity.Teacher;
import com.fasol.domain.entity.User;
import com.fasol.domain.enums.LessonType;
import com.fasol.dto.request.ScheduleRequest;
import com.fasol.dto.response.ScheduleResponse;
import com.fasol.exception.ResourceNotFoundException;
import com.fasol.repository.CourseRepository;
import com.fasol.repository.ScheduleRepository;
import com.fasol.repository.TeacherRepository;
import com.fasol.service.impl.ScheduleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock ScheduleRepository scheduleRepository;
    @Mock TeacherRepository teacherRepository;
    @Mock CourseRepository courseRepository;

    @InjectMocks ScheduleServiceImpl service;

    private Teacher teacher;
    private Course course;
    private Schedule schedule;

    @BeforeEach
    void setUp() {
        User teacherUser = User.builder().id(UUID.randomUUID())
                .firstName("Анна").lastName("Иванова").email("t@fasol.ru").build();

        teacher = Teacher.builder().id(UUID.randomUUID()).user(teacherUser).build();

        course = Course.builder().id(UUID.randomUUID()).name("Вокал")
                .price(BigDecimal.valueOf(5000)).build();

        schedule = Schedule.builder().id(UUID.randomUUID())
                .teacher(teacher).course(course).dayOfWeek(1)
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0))
                .lessonType(LessonType.GROUP).maxParticipants(3).active(true).build();
    }

    @Test
    @DisplayName("create — групповое занятие получает maxParticipants=3")
    void create_group_maxParticipants3() {
        ScheduleRequest req = ScheduleRequest.builder()
                .teacherId(teacher.getId()).courseId(course.getId())
                .dayOfWeek(1).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0))
                .lessonType(LessonType.GROUP).build();

        when(teacherRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(scheduleRepository.save(any())).thenAnswer(inv -> {
            Schedule s = inv.getArgument(0);
            return Schedule.builder().id(UUID.randomUUID())
                    .teacher(s.getTeacher()).course(s.getCourse())
                    .dayOfWeek(s.getDayOfWeek()).startTime(s.getStartTime())
                    .endTime(s.getEndTime()).lessonType(s.getLessonType())
                    .maxParticipants(s.getMaxParticipants()).active(true).build();
        });

        ScheduleResponse result = service.create(req);

        assertThat(result.getMaxParticipants()).isEqualTo(3);
        assertThat(result.getLessonType()).isEqualTo(LessonType.GROUP);
    }

    @Test
    @DisplayName("create — индивидуальное занятие получает maxParticipants=1")
    void create_individual_maxParticipants1() {
        ScheduleRequest req = ScheduleRequest.builder()
                .teacherId(teacher.getId()).courseId(course.getId())
                .dayOfWeek(2).startTime(LocalTime.of(12, 0)).endTime(LocalTime.of(13, 0))
                .lessonType(LessonType.INDIVIDUAL).build();

        when(teacherRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(scheduleRepository.save(any())).thenAnswer(inv -> {
            Schedule s = inv.getArgument(0);
            return Schedule.builder().id(UUID.randomUUID())
                    .teacher(s.getTeacher()).course(s.getCourse())
                    .dayOfWeek(s.getDayOfWeek()).startTime(s.getStartTime())
                    .endTime(s.getEndTime()).lessonType(s.getLessonType())
                    .maxParticipants(s.getMaxParticipants()).active(true).build();
        });

        ScheduleResponse result = service.create(req);

        assertThat(result.getMaxParticipants()).isEqualTo(1);
    }

    @Test
    @DisplayName("toggleActive — инвертирует флаг")
    void toggleActive_invertsFlag() {
        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any())).thenReturn(schedule);

        service.toggleActive(schedule.getId());

        assertThat(schedule.isActive()).isFalse();
    }

    @Test
    @DisplayName("delete — удаляет расписание")
    void delete_callsRepository() {
        when(scheduleRepository.existsById(schedule.getId())).thenReturn(true);

        service.delete(schedule.getId());

        verify(scheduleRepository).deleteById(schedule.getId());
    }

    @Test
    @DisplayName("delete — бросает ResourceNotFoundException если не найдено")
    void delete_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(scheduleRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getActiveSchedules — возвращает только активные")
    void getActiveSchedules_returnsOnlyActive() {
        when(scheduleRepository.findByActiveTrue()).thenReturn(List.of(schedule));

        List<ScheduleResponse> result = service.getActiveSchedules();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isActive()).isTrue();
    }
}
