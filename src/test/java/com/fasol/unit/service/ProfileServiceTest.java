package com.fasol.unit.service;

import com.fasol.domain.entity.User;
import com.fasol.domain.enums.AppRole;
import com.fasol.dto.request.UpdateProfileRequest;
import com.fasol.dto.response.ProfileResponse;
import com.fasol.exception.ResourceNotFoundException;
import com.fasol.repository.UserRepository;
import com.fasol.service.impl.ProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock UserRepository userRepository;

    @InjectMocks ProfileServiceImpl profileService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("user@fasol.ru")
                .firstName("Иван")
                .lastName("Иванов")
                .phone("+79001234567")
                .roles(Set.of(AppRole.STUDENT))
                .active(true)
                .build();
    }

    @Test
    @DisplayName("getProfile — возвращает профиль по userId")
    void getProfile_returnsProfile() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        ProfileResponse result = profileService.getProfile(userId);

        assertThat(result.getEmail()).isEqualTo("user@fasol.ru");
        assertThat(result.getFirstName()).isEqualTo("Иван");
        assertThat(result.getRoles()).contains("STUDENT");
    }

    @Test
    @DisplayName("getProfile — бросает ResourceNotFoundException если не найден")
    void getProfile_notFound_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getProfile(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateProfile — обновляет имя, фамилию и телефон")
    void updateProfile_updatesFields() {
        UpdateProfileRequest req = UpdateProfileRequest.builder()
                .firstName("Пётр")
                .lastName("Петров")
                .phone("+79009999999")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        ProfileResponse result = profileService.updateProfile(userId, req);

        assertThat(user.getFirstName()).isEqualTo("Пётр");
        assertThat(user.getLastName()).isEqualTo("Петров");
        assertThat(user.getPhone()).isEqualTo("+79009999999");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("updateProfile — не меняет телефон если передан null")
    void updateProfile_nullPhone_keepsOldPhone() {
        UpdateProfileRequest req = UpdateProfileRequest.builder()
                .firstName("Пётр")
                .lastName("Петров")
                .phone(null)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        profileService.updateProfile(userId, req);

        assertThat(user.getPhone()).isEqualTo("+79001234567"); // не изменился
    }

    @Test
    @DisplayName("updateAvatar — сохраняет URL аватара")
    void updateAvatar_savesUrl() {
        String avatarUrl = "https://storage.fasol.ru/avatars/user.jpg";

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        profileService.updateAvatar(userId, avatarUrl);

        assertThat(user.getAvatarUrl()).isEqualTo(avatarUrl);
        verify(userRepository).save(user);
    }
}
