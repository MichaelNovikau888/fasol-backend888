package com.fasol.service;

import com.fasol.dto.response.DashboardResponse;

import java.util.UUID;

public interface DashboardService {
    DashboardResponse getSummary(UUID studentId);
}
