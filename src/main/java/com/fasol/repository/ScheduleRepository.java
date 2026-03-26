package com.fasol.repository;

import com.fasol.domain.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {

    List<Schedule> findByActiveTrue();

    List<Schedule> findByTeacherIdAndActiveTrue(UUID teacherId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.schedule.id = :scheduleId AND b.bookingDate = :date AND b.status = 'CONFIRMED'")
    long countConfirmedByScheduleAndDate(@Param("scheduleId") UUID scheduleId,
                                         @Param("date") java.time.LocalDate date);
}
