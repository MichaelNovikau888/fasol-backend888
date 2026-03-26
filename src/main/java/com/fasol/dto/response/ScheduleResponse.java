package com.fasol.dto.response;

import com.fasol.domain.entity.Schedule;
import com.fasol.domain.enums.LessonType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ScheduleResponse {
    private String id;
    private String teacherId;
    private String teacherName;
    private String courseId;
    private String courseName;
    private int dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private LessonType lessonType;
    private int maxParticipants;
    private boolean active;

    public static ScheduleResponse from(Schedule s) {
        return ScheduleResponse.builder()
                .id(s.getId().toString())
                .teacherId(s.getTeacher().getId().toString())
                .teacherName(s.getTeacher().getUser().getFirstName()
                        + " " + s.getTeacher().getUser().getLastName())
                .courseId(s.getCourse().getId().toString())
                .courseName(s.getCourse().getName())
                .dayOfWeek(s.getDayOfWeek())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .lessonType(s.getLessonType())
                .maxParticipants(s.getMaxParticipants())
                .active(s.isActive())
                .build();
    }
}
