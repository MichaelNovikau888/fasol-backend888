package com.fasol.service;

import com.fasol.dto.request.CourseRequest;
import com.fasol.dto.response.CourseResponse;
import com.fasol.dto.response.StudentCourseResponse;

import java.util.List;
import java.util.UUID;

public interface CourseService {
    List<CourseResponse> getActiveCourses();
    List<CourseResponse> getAllCourses();
    CourseResponse createCourse(CourseRequest request);
    CourseResponse updateCourse(UUID id, CourseRequest request);
    void toggleActive(UUID id);
    /** Купить курс. Применяет скидку при повторной покупке. */
    StudentCourseResponse purchaseCourse(UUID studentId, UUID courseId);

    /**
     * Купить курс после пробного урока.
     * Сохраняет ссылку на заявку для отслеживания воронки: пробное → покупка.
     */
    StudentCourseResponse purchaseCourseAfterTrial(UUID studentId, UUID courseId, UUID trialRequestId);
    List<StudentCourseResponse> getStudentCourses(UUID studentId);
    /** Вручную добавить занятия к существующему абонементу (офлайн-оплата). */
    StudentCourseResponse addLessonsManually(UUID studentCourseId, int individual, int group);
}
