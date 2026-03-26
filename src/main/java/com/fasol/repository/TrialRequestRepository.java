package com.fasol.repository;

import com.fasol.domain.entity.TrialRequest;
import com.fasol.domain.enums.TrialRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TrialRequestRepository extends JpaRepository<TrialRequest, UUID> {

    List<TrialRequest> findByStatusOrderByCreatedAtDesc(TrialRequestStatus status);

    List<TrialRequest> findAllByOrderByCreatedAtDesc();

    /** Для метрики trial.queue.size — сколько заявок с данным статусом */
    long countByStatus(TrialRequestStatus status);
}
