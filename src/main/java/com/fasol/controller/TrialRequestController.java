package com.fasol.controller;

import com.fasol.dto.request.TrialRequestDto;
import com.fasol.dto.response.TrialRequestResponse;
import com.fasol.service.TrialRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trial-requests")
@RequiredArgsConstructor
public class TrialRequestController {

    private final TrialRequestService trialRequestService;

    /** Публичный endpoint — форма с лендинга */
    @PostMapping
    public ResponseEntity<TrialRequestResponse> submit(@Valid @RequestBody TrialRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trialRequestService.create(dto));
    }
}
