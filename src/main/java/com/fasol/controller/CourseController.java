package com.fasol.controller;

import com.fasol.dto.response.CourseResponse;
import com.fasol.dto.response.StudentCourseResponse;
import com.fasol.security.CurrentUser;
import com.fasol.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService courseService;

    @GetMapping
    public ResponseEntity<List<CourseResponse>> getActive() {
        return ResponseEntity.ok(courseService.getActiveCourses());
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<StudentCourseResponse>> myCourses(@CurrentUser UUID studentId) {
        return ResponseEntity.ok(courseService.getStudentCourses(studentId));
    }

    @PostMapping("/{courseId}/purchase")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentCourseResponse> purchase(
            @PathVariable UUID courseId, @CurrentUser UUID studentId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courseService.purchaseCourse(studentId, courseId));
    }
}
