package com.fasol.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BookingCreatedEvent {
    private String bookingId;
    private String studentId;
    private String studentEmail;
    private String scheduleId;
    private String bookingDate;
    private String lessonType;
    private String teacherName;
    private String courseName;
    @Builder.Default
    private String eventType = "BOOKING_CREATED";
}
