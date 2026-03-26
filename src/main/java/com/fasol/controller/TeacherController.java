package com.fasol.controller;

import com.fasol.dto.response.BookingResponse;
import com.fasol.dto.response.TeacherResponse;
import com.fasol.security.CurrentUser;
import com.fasol.service.BookingService;
import com.fasol.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherController {
    private final BookingService bookingService;
    private final TeacherService teacherService;

    /** Расписание преподавателя на неделю */
    @GetMapping("/schedule")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<BookingResponse>> schedule(
            @CurrentUser UUID userId,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().toString()}") String weekStart) {
        return ResponseEntity.ok(bookingService.getTeacherBookings(userId, weekStart));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<TeacherResponse>> getTeachers() {
        return ResponseEntity.ok(teacherService.getActiveTeachers());
    }
}
