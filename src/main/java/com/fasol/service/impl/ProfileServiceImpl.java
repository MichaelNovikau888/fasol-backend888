package com.fasol.service.impl;

import com.fasol.dto.request.UpdateProfileRequest;
import com.fasol.dto.response.ProfileResponse;
import com.fasol.exception.ResourceNotFoundException;
import com.fasol.repository.UserRepository;
import com.fasol.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID userId) {
        return ProfileResponse.from(
                userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден")));
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        // Обновляем телефон только если передан; null = не трогаем
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        log.info("Profile updated for user {}", userId);
        return ProfileResponse.from(userRepository.save(user));
    }

    @Override
    @Transactional
    public ProfileResponse updateAvatar(UUID userId, String avatarUrl) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        user.setAvatarUrl(avatarUrl);
        log.info("Avatar updated for user {}", userId);
        return ProfileResponse.from(userRepository.save(user));
    }
}
