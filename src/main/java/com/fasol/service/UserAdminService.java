package com.fasol.service;

import com.fasol.domain.enums.AppRole;
import com.fasol.dto.response.StudentCourseResponse;
import com.fasol.dto.response.UserResponse;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface UserAdminService {
    List<UserResponse> getAllUsers();
    List<StudentCourseResponse> getStudentCourses(UUID userId);
    List<StudentCourseResponse> getAllStudentCourses();
    UserResponse toggleActive(UUID userId);
    UserResponse updateRoles(UUID userId, Set<AppRole> roles);
}
