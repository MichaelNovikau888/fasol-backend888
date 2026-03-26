package com.fasol.service.impl;

import com.fasol.domain.entity.Schedule;
import com.fasol.domain.enums.LessonType;
import com.fasol.dto.request.ScheduleRequest;
import com.fasol.dto.response.ScheduleResponse;
import com.fasol.exception.ResourceNotFoundException;
import com.fasol.repository.CourseRepository;
import com.fasol.repository.ScheduleRepository;
import com.fasol.repository.TeacherRepository;
import com.fasol.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getActiveSchedules() {
        return scheduleRepository.findByActiveTrue().stream().map(ScheduleResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getAllSchedules() {
        return scheduleRepository.findAll().stream().map(ScheduleResponse::from).toList();
    }

    @Override
    @Transactional
    public ScheduleResponse create(ScheduleRequest req) {
        var teacher = teacherRepository.findById(req.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Преподаватель не найден"));
        var course = courseRepository.findById(req.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Курс не найден"));

        // Бизнес-правило: group=3, individual=1
        int maxP = req.getLessonType() == LessonType.GROUP ? 3 : 1;

        Schedule s = Schedule.builder()
                .teacher(teacher).course(course)
                .dayOfWeek(req.getDayOfWeek())
                .startTime(req.getStartTime()).endTime(req.getEndTime())
                .lessonType(req.getLessonType()).maxParticipants(maxP)
                .build();
        return ScheduleResponse.from(scheduleRepository.save(s));
    }

    @Override
    @Transactional
    public ScheduleResponse update(UUID id, ScheduleRequest req) {
        Schedule s = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Расписание не найдено"));
        var teacher = teacherRepository.findById(req.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Преподаватель не найден"));
        var course = courseRepository.findById(req.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Курс не найден"));

        s.setTeacher(teacher); s.setCourse(course);
        s.setDayOfWeek(req.getDayOfWeek());
        s.setStartTime(req.getStartTime()); s.setEndTime(req.getEndTime());
        s.setLessonType(req.getLessonType());
        s.setMaxParticipants(req.getLessonType() == LessonType.GROUP ? 3 : 1);
        return ScheduleResponse.from(scheduleRepository.save(s));
    }

    @Override
    @Transactional
    public void toggleActive(UUID id) {
        Schedule s = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Расписание не найдено"));
        s.setActive(!s.isActive());
        scheduleRepository.save(s);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!scheduleRepository.existsById(id))
            throw new ResourceNotFoundException("Расписание не найдено");
        scheduleRepository.deleteById(id);
    }
}
