package com.fasol.dto.response;

import com.fasol.domain.entity.Course;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseResponse {
    private String id;
    private String name;
    private String description;
    private int individualLessons;
    private int groupLessons;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private boolean active;
    /** URL PDF-договора — фронт открывает в iframe для просмотра перед оплатой */
    private String contractUrl;

    public static CourseResponse from(Course c) {
        return CourseResponse.builder()
                .id(c.getId().toString())
                .name(c.getName())
                .description(c.getDescription())
                .individualLessons(c.getIndividualLessons())
                .groupLessons(c.getGroupLessons())
                .price(c.getPrice())
                .discountPrice(c.getDiscountPrice())
                .active(c.isActive())
                .contractUrl(c.getContractUrl())
                .build();
    }
}
