package com.fasol.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BookingCancelledEvent {
    private String bookingId;
    private String studentId;
    private String studentEmail;
    private String cancelledAt;
    private String lessonType;
    @Builder.Default
    private String eventType = "BOOKING_CANCELLED";
}
