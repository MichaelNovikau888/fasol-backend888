package com.fasol.dto.request;

import com.fasol.domain.enums.LessonType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ScheduleRequest {
    @NotNull
    private UUID teacherId;
    @NotNull
    private UUID courseId;
    @NotNull @Min(0) @Max(6)
    private Integer dayOfWeek;
    @NotNull
    private LocalTime startTime;
    @NotNull
    private LocalTime endTime;
    @NotNull
    private LessonType lessonType;
}
