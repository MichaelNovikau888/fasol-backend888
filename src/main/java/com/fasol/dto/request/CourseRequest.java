package com.fasol.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseRequest {
    @NotBlank
    private String name;
    private String description;
    @Min(0)
    private int individualLessons;
    @Min(0)
    private int groupLessons;
    @NotNull @DecimalMin("0.0")
    private BigDecimal price;
    private BigDecimal discountPrice;
    /** URL PDF-договора — менеджер загружает файл в Supabase Storage, сюда передаёт URL */
    private String contractUrl;
}
