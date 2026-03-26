package com.fasol.service;

import com.fasol.dto.request.BookingRequest;
import com.fasol.dto.response.BookingResponse;

import java.util.List;
import java.util.UUID;

public interface BookingService {
    BookingResponse createBooking(UUID studentId, BookingRequest request);
    void cancelBooking(UUID bookingId, UUID cancelledBy);
    List<BookingResponse> getStudentBookings(UUID studentId);
    List<BookingResponse> getTeacherBookings(UUID teacherUserId, String weekStart);

    /**
     * Отмена занятия администратором — без 24-часового дедлайна.
     * penalize=true → занятие списывается с баланса (< 24ч до начала).
     * penalize=false → занятие возвращается на баланс.
     */
    void adminCancelBooking(UUID bookingId, UUID adminId, boolean penalize);

    /** Все бронирования на неделю для AdminScheduleView. teacherId — опционально. */
    List<BookingResponse> getAdminWeekBookings(java.time.LocalDate weekStart, UUID teacherId);
}
