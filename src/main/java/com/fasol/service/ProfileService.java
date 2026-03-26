package com.fasol.service;

import com.fasol.dto.request.UpdateProfileRequest;
import com.fasol.dto.response.ProfileResponse;

import java.util.UUID;

public interface ProfileService {
    ProfileResponse getProfile(UUID userId);
    ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);
    ProfileResponse updateAvatar(UUID userId, String avatarUrl);
}
