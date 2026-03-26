package com.fasol.dto.response;

import com.fasol.domain.entity.Booking;
import com.fasol.domain.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingResponse {
    private String id;
    private String scheduleId;
    private LocalDate bookingDate;
    private BookingStatus status;
    private String lessonType;
    private String teacherName;
    private String courseName;
    private String startTime;
    private String endTime;

    public static BookingResponse from(Booking b) {
        return BookingResponse.builder()
                .id(b.getId().toString())
                .scheduleId(b.getSchedule().getId().toString())
                .bookingDate(b.getBookingDate())
                .status(b.getStatus())
                .lessonType(b.getSchedule().getLessonType().name())
                .teacherName(b.getSchedule().getTeacher().getUser().getFirstName()
                        + " " + b.getSchedule().getTeacher().getUser().getLastName())
                .courseName(b.getSchedule().getCourse().getName())
                .startTime(b.getSchedule().getStartTime().toString())
                .endTime(b.getSchedule().getEndTime().toString())
                .build();
    }
}
