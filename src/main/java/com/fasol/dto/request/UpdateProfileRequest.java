package com.fasol.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateProfileRequest {

    @NotBlank(message = "Имя обязательно")
    @Size(max = 100, message = "Имя не более 100 символов")
    private String firstName;

    @NotBlank(message = "Фамилия обязательна")
    @Size(max = 100, message = "Фамилия не более 100 символов")
    private String lastName;

    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Некорректный формат телефона")
    private String phone;
}
