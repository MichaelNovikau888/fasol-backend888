package com.fasol.service;

import com.fasol.dto.response.TeacherResponse;
import java.util.List;
import java.util.UUID;

public interface TeacherService {
    List<TeacherResponse> getActiveTeachers();
    List<TeacherResponse> getAllTeachers();
    TeacherResponse createTeacher(UUID userId, String bio, String specialization);
    void toggleActive(UUID teacherId);
}
