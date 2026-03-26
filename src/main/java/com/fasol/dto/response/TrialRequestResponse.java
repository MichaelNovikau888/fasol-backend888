package com.fasol.dto.response;

import com.fasol.domain.entity.TrialRequest;
import com.fasol.domain.enums.TrialRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TrialRequestResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String phone;
    private boolean wantsWhatsapp;
    private TrialRequestStatus status;
    private String notes;
    private LocalDateTime createdAt;

    public static TrialRequestResponse from(TrialRequest r) {
        return TrialRequestResponse.builder()
                .id(r.getId().toString())
                .firstName(r.getFirstName())
                .lastName(r.getLastName())
                .phone(r.getPhone())
                .wantsWhatsapp(r.isWantsWhatsapp())
                .status(r.getStatus())
                .notes(r.getNotes())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
