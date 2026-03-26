package com.fasol.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TrialRequestDto {
    @NotBlank
    private String firstName;
    private String lastName;
    @NotBlank @Pattern(regexp = "^\\+?[0-9]{7,15}$")
    private String phone;
    private boolean wantsWhatsapp;
}
