package com.fasol.service.impl;

import com.fasol.domain.entity.TrialRequest;
import com.fasol.domain.enums.TrialRequestStatus;
import com.fasol.dto.request.TrialRequestDto;
import com.fasol.dto.response.TrialRequestResponse;
import com.fasol.exception.ResourceNotFoundException;
import com.fasol.metrics.SalesMetrics;
import com.fasol.repository.TrialRequestRepository;
import com.fasol.service.TrialRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrialRequestServiceImpl implements TrialRequestService {

    private final TrialRequestRepository repository;
    private final SalesMetrics salesMetrics;

    @Override
    @Transactional
    public TrialRequestResponse create(TrialRequestDto dto) {
        TrialRequest req = TrialRequest.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .phone(dto.getPhone())
                .wantsWhatsapp(dto.isWantsWhatsapp())
                .build();
        log.info("New trial request from {} {}", dto.getFirstName(), dto.getPhone());
        TrialRequestResponse response = TrialRequestResponse.from(repository.save(req));
        salesMetrics.incrementTrialRequest();
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrialRequestResponse> getAll() {
        return repository.findAllByOrderByCreatedAtDesc()
                .stream().map(TrialRequestResponse::from).toList();
    }

    @Override
    @Transactional
    public TrialRequestResponse updateStatus(UUID id, TrialRequestStatus status, String notes) {
        TrialRequest req = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена: " + id));
        req.setStatus(status);
        if (notes != null) req.setNotes(notes);
        salesMetrics.trackTrialStatusChange(status);
        return TrialRequestResponse.from(repository.save(req));
    }
}
