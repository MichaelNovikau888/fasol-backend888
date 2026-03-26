package com.fasol.controller;

import com.fasol.dto.request.UpdateProfileRequest;
import com.fasol.dto.response.ProfileResponse;
import com.fasol.security.CurrentUser;
import com.fasol.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Профиль текущего пользователя.
 *
 * Фронт (Profile.tsx) делает:
 *   GET  /api/profile        — загрузить данные
 *   PATCH /api/profile        — обновить имя / телефон
 *   PATCH /api/profile/avatar — сохранить URL аватара после загрузки в S3/Supabase Storage
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ProfileResponse> get(@CurrentUser UUID userId) {
        return ResponseEntity.ok(profileService.getProfile(userId));
    }

    @PatchMapping
    public ResponseEntity<ProfileResponse> update(
            @CurrentUser UUID userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(userId, request));
    }

    /**
     * Фронт загружает файл напрямую в Supabase Storage / S3,
     * получает публичный URL и присылает его сюда.
     * Бэкенд только сохраняет URL — не занимается файлами.
     */
    @PatchMapping("/avatar")
    public ResponseEntity<ProfileResponse> updateAvatar(
            @CurrentUser UUID userId,
            @RequestBody Map<String, String> body) {
        String avatarUrl = body.get("avatarUrl");
        if (avatarUrl == null || avatarUrl.isBlank()) {
            throw new IllegalArgumentException("avatarUrl не может быть пустым");
        }
        return ResponseEntity.ok(profileService.updateAvatar(userId, avatarUrl));
    }
}
