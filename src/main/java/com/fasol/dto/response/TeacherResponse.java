package com.fasol.dto.response;

import com.fasol.domain.entity.Teacher;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TeacherResponse {
    private String id;
    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String bio;
    private String specialization;
    private String avatarUrl;
    private boolean active;

    public static TeacherResponse from(Teacher t) {
        return TeacherResponse.builder()
                .id(t.getId().toString())
                .userId(t.getUser().getId().toString())
                .firstName(t.getUser().getFirstName())
                .lastName(t.getUser().getLastName())
                .email(t.getUser().getEmail())
                .bio(t.getBio())
                .specialization(t.getSpecialization())
                .avatarUrl(t.getAvatarUrl())
                .active(t.isActive())
                .build();
    }
}
