package com.fasol.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "student_courses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    @Builder.Default
    private Integer individualLessonsRemaining = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer groupLessonsRemaining = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean repeatPurchase = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean paidOnline = false;

    private String stripePaymentId;

    private LocalDateTime expiresAt;

    /**
     * Ссылка на заявку на пробный урок, после которого студент купил курс.
     * NULL — если покупка без пробного (повторная, прямая продажа, офлайн без заявки).
     * Используется для подсчёта воронки: пробный → покупка.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trial_request_id")
    private TrialRequest trialRequest;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
