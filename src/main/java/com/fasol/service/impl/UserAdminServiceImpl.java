package com.fasol.service.impl;

import com.fasol.domain.enums.AppRole;
import com.fasol.dto.response.StudentCourseResponse;
import com.fasol.dto.response.UserResponse;
import com.fasol.exception.ResourceNotFoundException;
import com.fasol.repository.StudentCourseRepository;
import com.fasol.repository.UserRepository;
import com.fasol.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository userRepository;
    private final StudentCourseRepository studentCourseRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentCourseResponse> getAllStudentCourses() {
        return studentCourseRepository.findAllWithDetails()
                .stream()
                .map(StudentCourseResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentCourseResponse> getStudentCourses(UUID userId) {
        // Проверяем, что пользователь существует
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Пользователь не найден: " + userId);
        }
        return studentCourseRepository.findByStudentId(userId)
                .stream()
                .map(StudentCourseResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public UserResponse toggleActive(UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден: " + userId));
        user.setActive(!user.isActive());
        log.info("User {} active={}", userId, user.isActive());
        return UserResponse.from(userRepository.save(user));
    }

    /**
     * Полная замена ролей — безопаснее чем add/remove по одной:
     * фронт отправляет итоговый набор ролей, бэкенд заменяет целиком.
     *
     * Минимальное ограничение: нельзя оставить пользователя без ролей.
     * Минимум одна роль (обычно STUDENT) должна остаться.
     */
    @Override
    @Transactional
    public UserResponse updateRoles(UUID userId, Set<AppRole> newRoles) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден: " + userId));

        // Очищаем и добавляем заново — Hibernate обновит user_roles через @ElementCollection
        user.getRoles().clear();
        user.getRoles().addAll(newRoles);

        log.info("Roles updated for user {}: {}", userId, newRoles);
        return UserResponse.from(userRepository.save(user));
    }
}
