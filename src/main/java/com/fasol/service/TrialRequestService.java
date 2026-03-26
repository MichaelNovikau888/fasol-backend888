package com.fasol.service;

import com.fasol.domain.enums.TrialRequestStatus;
import com.fasol.dto.request.TrialRequestDto;
import com.fasol.dto.response.TrialRequestResponse;

import java.util.List;
import java.util.UUID;

public interface TrialRequestService {
    TrialRequestResponse create(TrialRequestDto dto);
    List<TrialRequestResponse> getAll();
    TrialRequestResponse updateStatus(UUID id, TrialRequestStatus status, String notes);
}
