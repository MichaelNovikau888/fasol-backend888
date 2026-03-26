package com.fasol.service;

import com.fasol.dto.request.ScheduleRequest;
import com.fasol.dto.response.ScheduleResponse;

import java.util.List;
import java.util.UUID;

public interface ScheduleService {
    List<ScheduleResponse> getActiveSchedules();
    List<ScheduleResponse> getAllSchedules();
    ScheduleResponse create(ScheduleRequest request);
    ScheduleResponse update(UUID id, ScheduleRequest request);
    void toggleActive(UUID id);
    void delete(UUID id);
}
