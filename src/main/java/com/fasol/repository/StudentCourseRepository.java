package com.fasol.repository;

import com.fasol.domain.entity.StudentCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentCourseRepository extends JpaRepository<StudentCourse, UUID> {

    List<StudentCourse> findByStudentId(UUID studentId);

    Optional<StudentCourse> findByStudentIdAndCourseId(UUID studentId, UUID courseId);

    /** Проверяем, была ли уже покупка — для применения скидки */
    boolean existsByStudentIdAndCourseId(UUID studentId, UUID courseId);

    @Query("""
        SELECT sc FROM StudentCourse sc
        JOIN FETCH sc.course
        WHERE sc.student.id = :studentId
          AND (sc.individualLessonsRemaining > 0 OR sc.groupLessonsRemaining > 0)
        """)
    List<StudentCourse> findActiveByStudentId(@Param("studentId") UUID studentId);

    /** Для менеджера — все ученики с курсами */
    @Query("""
        SELECT sc FROM StudentCourse sc
        JOIN FETCH sc.student
        JOIN FETCH sc.course
        ORDER BY sc.createdAt DESC
        """)
    List<StudentCourse> findAllWithDetails();

    /** Для метрики students.active.count — студенты с ненулевым балансом */
    @Query("""
        SELECT COUNT(DISTINCT sc.student.id) FROM StudentCourse sc
        WHERE sc.individualLessonsRemaining > 0 OR sc.groupLessonsRemaining > 0
        """)
    long countActiveStudents();
}
