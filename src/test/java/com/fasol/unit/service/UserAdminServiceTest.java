package com.fasol.unit.service;

import com.fasol.domain.entity.Course;
import com.fasol.domain.entity.StudentCourse;
import com.fasol.domain.entity.User;
import com.fasol.domain.enums.AppRole;
import com.fasol.dto.response.StudentCourseResponse;
import com.fasol.dto.response.UserResponse;
import com.fasol.exception.ResourceNotFoundException;
import com.fasol.repository.StudentCourseRepository;
import com.fasol.repository.UserRepository;
import com.fasol.service.impl.UserAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock UserRepository userRepository;
    @Mock StudentCourseRepository studentCourseRepository;

    @InjectMocks UserAdminServiceImpl service;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("user@fasol.ru")
                .firstName("Иван")
                .lastName("Иванов")
                .active(true)
                .roles(new HashSet<>(Set.of(AppRole.STUDENT)))
                .build();
    }

    @Test
    @DisplayName("getAllUsers — возвращает список всех пользователей")
    void getAllUsers_returnsList() {
        when(userRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(user));

        List<UserResponse> result = service.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("user@fasol.ru");
        assertThat(result.get(0).getRoles()).contains("STUDENT");
    }

    @Test
    @DisplayName("toggleActive — деактивирует активного пользователя")
    void toggleActive_deactivatesUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        UserResponse result = service.toggleActive(userId);

        assertThat(user.isActive()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("toggleActive — активирует неактивного пользователя")
    void toggleActive_activatesUser() {
        user.setActive(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        service.toggleActive(userId);

        assertThat(user.isActive()).isTrue();
    }

    @Test
    @DisplayName("updateRoles — полностью заменяет набор ролей")
    void updateRoles_replacesRoles() {
        Set<AppRole> newRoles = Set.of(AppRole.TEACHER, AppRole.STUDENT);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        UserResponse result = service.updateRoles(userId, newRoles);

        assertThat(user.getRoles()).containsExactlyInAnyOrder(AppRole.TEACHER, AppRole.STUDENT);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("updateRoles — бросает ResourceNotFoundException если пользователь не найден")
    void updateRoles_notFound_throws() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateRoles(userId, Set.of(AppRole.STUDENT)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getStudentCourses — бросает ResourceNotFoundException если пользователь не найден")
    void getStudentCourses_userNotFound_throws() {
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> service.getStudentCourses(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getAllStudentCourses — возвращает все абонементы студентов")
    void getAllStudentCourses_returnsList() {
        Course course = Course.builder().id(UUID.randomUUID()).name("Вокал")
                .price(BigDecimal.valueOf(5000)).individualLessons(4).groupLessons(8).build();

        StudentCourse sc = StudentCourse.builder()
                .id(UUID.randomUUID()).student(user).course(course)
                .individualLessonsRemaining(4).groupLessonsRemaining(8).build();

        when(studentCourseRepository.findAllWithDetails()).thenReturn(List.of(sc));

        List<StudentCourseResponse> result = service.getAllStudentCourses();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCourseName()).isEqualTo("Вокал");
    }
}
