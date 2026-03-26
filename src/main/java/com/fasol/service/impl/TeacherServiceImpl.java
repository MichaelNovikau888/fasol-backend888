package com.fasol.service.impl;

import com.fasol.domain.entity.Teacher;
import com.fasol.domain.enums.AppRole;
import com.fasol.dto.response.TeacherResponse;
import com.fasol.exception.ResourceNotFoundException;
import com.fasol.repository.TeacherRepository;
import com.fasol.repository.UserRepository;
import com.fasol.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TeacherResponse> getActiveTeachers() {
        return teacherRepository.findByActiveTrue().stream().map(TeacherResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherResponse> getAllTeachers() {
        return teacherRepository.findAll().stream().map(TeacherResponse::from).toList();
    }

    @Override
    @Transactional
    public TeacherResponse createTeacher(UUID userId, String bio, String specialization) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        // Добавляем роль TEACHER если ещё нет
        user.getRoles().add(AppRole.TEACHER);
        userRepository.save(user);

        Teacher teacher = Teacher.builder()
                .user(user).bio(bio).specialization(specialization).build();
        return TeacherResponse.from(teacherRepository.save(teacher));
    }

    @Override
    @Transactional
    public void toggleActive(UUID teacherId) {
        Teacher t = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Преподаватель не найден"));
        t.setActive(!t.isActive());
        teacherRepository.save(t);
    }
}
