package com.fasol.service.impl;

import com.fasol.domain.entity.Course;
import com.fasol.domain.entity.StudentCourse;
import com.fasol.domain.entity.User;
import com.fasol.dto.request.CourseRequest;
import com.fasol.dto.response.CourseResponse;
import com.fasol.dto.response.StudentCourseResponse;
import com.fasol.exception.ResourceNotFoundException;
import com.fasol.metrics.SalesMetrics;
import com.fasol.repository.CourseRepository;
import com.fasol.repository.TrialRequestRepository;
import com.fasol.repository.StudentCourseRepository;
import com.fasol.repository.UserRepository;
import com.fasol.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final StudentCourseRepository studentCourseRepository;
    private final UserRepository userRepository;
    private final SalesMetrics salesMetrics;
    private final TrialRequestRepository trialRequestRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getActiveCourses() {
        return courseRepository.findByActiveTrue().stream().map(CourseResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getAllCourses() {
        return courseRepository.findAll().stream().map(CourseResponse::from).toList();
    }

    @Override
    @Transactional
    public CourseResponse createCourse(CourseRequest req) {
        Course course = Course.builder()
                .name(req.getName()).description(req.getDescription())
                .individualLessons(req.getIndividualLessons())
                .groupLessons(req.getGroupLessons())
                .price(req.getPrice()).discountPrice(req.getDiscountPrice())
                .contractUrl(req.getContractUrl())
                .build();
        return CourseResponse.from(courseRepository.save(course));
    }

    @Override
    @Transactional
    public CourseResponse updateCourse(UUID id, CourseRequest req) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Курс не найден: " + id));
        course.setName(req.getName());
        course.setDescription(req.getDescription());
        course.setIndividualLessons(req.getIndividualLessons());
        course.setGroupLessons(req.getGroupLessons());
        course.setPrice(req.getPrice());
        course.setDiscountPrice(req.getDiscountPrice());
        // contractUrl может быть null если менеджер удалил договор
        course.setContractUrl(req.getContractUrl());
        return CourseResponse.from(courseRepository.save(course));
    }

    @Override
    @Transactional
    public void toggleActive(UUID id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Курс не найден: " + id));
        course.setActive(!course.isActive());
        courseRepository.save(course);
    }

    /**
     * Покупка курса студентом.
     *
     * Бизнес-правило: если студент уже покупал этот курс — применяем discountPrice
     * и ставим флаг repeatPurchase = true.
     * Это воспроизводит логику из Dashboard.tsx (проверка hasDiscount).
     */
    @Override
    @Transactional
    public StudentCourseResponse purchaseCourse(UUID studentId, UUID courseId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Студент не найден"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Курс не найден"));

        boolean isRepeat = studentCourseRepository.existsByStudentIdAndCourseId(studentId, courseId);

        StudentCourse sc = StudentCourse.builder()
                .student(student).course(course)
                .individualLessonsRemaining(course.getIndividualLessons())
                .groupLessonsRemaining(course.getGroupLessons())
                .repeatPurchase(isRepeat)
                // paidOnline = false — менеджер подтвердит оплату вручную или через Stripe
                .build();

        log.info("Course {} purchased by student {} (repeat={})", courseId, studentId, isRepeat);
        StudentCourseResponse response = StudentCourseResponse.from(studentCourseRepository.save(sc));
        salesMetrics.incrementCoursePurchased(isRepeat);
        return response;
    }

    /**
     * Покупка курса после пробного урока — сохраняет связь trial → purchase.
     * Вызывается менеджером когда студент пришёл на пробный и решил купить курс.
     * Именно эта связь даёт точную конверсию в воронке Grafana.
     */
    @Override
    @Transactional
    public StudentCourseResponse purchaseCourseAfterTrial(UUID studentId, UUID courseId, UUID trialRequestId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Студент не найден"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Курс не найден"));
        var trial = trialRequestRepository.findById(trialRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка на пробный урок не найдена"));

        boolean isRepeat = studentCourseRepository.existsByStudentIdAndCourseId(studentId, courseId);

        StudentCourse sc = StudentCourse.builder()
                .student(student).course(course)
                .individualLessonsRemaining(course.getIndividualLessons())
                .groupLessonsRemaining(course.getGroupLessons())
                .repeatPurchase(isRepeat)
                .trialRequest(trial)   // ← связь для воронки
                .build();

        log.info("Course {} purchased after trial {} by student {} (repeat={})",
                courseId, trialRequestId, studentId, isRepeat);
        StudentCourseResponse response = StudentCourseResponse.from(studentCourseRepository.save(sc));
        salesMetrics.incrementCoursePurchased(isRepeat);
        salesMetrics.incrementTrialConversion(); // воронка: пробный → покупка
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentCourseResponse> getStudentCourses(UUID studentId) {
        return studentCourseRepository.findByStudentId(studentId)
                .stream().map(StudentCourseResponse::from).toList();
    }

    /**
     * Вручную добавить занятия к абонементу при офлайн-оплате.
     * Менеджер принял деньги наличными и докидывает занятия.
     */
    @Override
    @Transactional
    public StudentCourseResponse addLessonsManually(UUID studentCourseId, int individual, int group) {
        StudentCourse sc = studentCourseRepository.findById(studentCourseId)
                .orElseThrow(() -> new ResourceNotFoundException("Абонемент не найден: " + studentCourseId));
        sc.setIndividualLessonsRemaining(sc.getIndividualLessonsRemaining() + individual);
        sc.setGroupLessonsRemaining(sc.getGroupLessonsRemaining() + group);
        log.info("Manual lessons added to studentCourse {}: ind+{} grp+{}", studentCourseId, individual, group);
        return StudentCourseResponse.from(studentCourseRepository.save(sc));
    }
}
