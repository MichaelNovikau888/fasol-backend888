package com.fasol.dto.response;

import com.fasol.domain.entity.StudentCourse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentCourseResponse {
    private String id;
    private String courseId;
    private String courseName;
    private int individualLessonsRemaining;
    private int groupLessonsRemaining;
    private boolean repeatPurchase;
    private boolean paidOnline;
    private LocalDateTime expiresAt;
    /** true если осталось ≤10% индивидуальных занятий — предупреждение на фронте */
    private boolean individualLowBalance;
    /** true если осталось ≤10% групповых занятий */
    private boolean groupLowBalance;

    public static StudentCourseResponse from(StudentCourse sc) {
        int totalInd = sc.getCourse().getIndividualLessons();
        int totalGrp = sc.getCourse().getGroupLessons();
        boolean indLow  = totalInd > 0
                && sc.getIndividualLessonsRemaining() > 0
                && sc.getIndividualLessonsRemaining() <= Math.ceil(totalInd * 0.1);
        boolean grpLow  = totalGrp > 0
                && sc.getGroupLessonsRemaining() > 0
                && sc.getGroupLessonsRemaining() <= Math.ceil(totalGrp * 0.1);

        return StudentCourseResponse.builder()
                .id(sc.getId().toString())
                .courseId(sc.getCourse().getId().toString())
                .courseName(sc.getCourse().getName())
                .individualLessonsRemaining(sc.getIndividualLessonsRemaining())
                .groupLessonsRemaining(sc.getGroupLessonsRemaining())
                .repeatPurchase(sc.isRepeatPurchase())
                .paidOnline(sc.isPaidOnline())
                .expiresAt(sc.getExpiresAt())
                .individualLowBalance(indLow)
                .groupLowBalance(grpLow)
                .build();
    }
}
