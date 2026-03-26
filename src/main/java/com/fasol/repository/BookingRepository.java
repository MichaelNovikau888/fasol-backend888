package com.fasol.repository;

import com.fasol.domain.entity.Booking;
import com.fasol.domain.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByStudentIdOrderByBookingDateDesc(UUID studentId);

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.schedule s
        JOIN FETCH s.teacher t
        JOIN FETCH t.user
        JOIN FETCH s.course
        WHERE b.student.id = :studentId
        ORDER BY b.bookingDate DESC
        """)
    List<Booking> findByStudentIdWithDetails(@Param("studentId") UUID studentId);

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.schedule s
        JOIN FETCH s.teacher t
        JOIN FETCH t.user
        JOIN FETCH s.course
        WHERE b.student.id = :studentId
          AND b.bookingDate BETWEEN :from AND :to
          AND b.status = 'CONFIRMED'
        ORDER BY b.bookingDate, s.startTime
        """)
    List<Booking> findUpcomingByStudentId(@Param("studentId") UUID studentId,
                                           @Param("from") LocalDate from,
                                           @Param("to") LocalDate to);

    long countByStudentIdAndStatus(UUID studentId, BookingStatus status);

    /**
     * Все подтверждённые бронирования за период — для AdminScheduleView.
     * Используется когда teacherId не указан (показываем всех преподавателей).
     */
    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.student
        JOIN FETCH b.schedule s
        JOIN FETCH s.teacher t
        JOIN FETCH t.user
        JOIN FETCH s.course
        WHERE b.bookingDate BETWEEN :from AND :to
          AND b.status = 'CONFIRMED'
        ORDER BY b.bookingDate, s.startTime
        """)
    List<Booking> findAllConfirmedByDateRange(@Param("from") LocalDate from,
                                               @Param("to") LocalDate to);

    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.schedule.id = :scheduleId
          AND b.bookingDate = :date
          AND b.status = 'CONFIRMED'
        """)
    long countConfirmedByScheduleAndDate(@Param("scheduleId") UUID scheduleId,
                                          @Param("date") LocalDate date);

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.student
        JOIN FETCH b.schedule s
        JOIN FETCH s.teacher t
        JOIN FETCH t.user
        WHERE s.teacher.id = :teacherId
          AND b.bookingDate BETWEEN :from AND :to
          AND b.status = 'CONFIRMED'
        ORDER BY b.bookingDate, s.startTime
        """)
    List<Booking> findByTeacherAndDateRange(@Param("teacherId") UUID teacherId,
                                             @Param("from") LocalDate from,
                                             @Param("to") LocalDate to);
}
