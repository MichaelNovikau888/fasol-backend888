package com.fasol.unit.service;

import com.fasol.domain.entity.User;
import com.fasol.domain.enums.AppRole;
import com.fasol.dto.request.LoginRequest;
import com.fasol.dto.request.RegisterRequest;
import com.fasol.dto.response.AuthResponse;
import com.fasol.exception.UserAlreadyExistsException;
import com.fasol.repository.UserRepository;
import com.fasol.security.JwtTokenProvider;
import com.fasol.security.UserPrincipal;
import com.fasol.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtProvider;
    @Mock AuthenticationManager authManager;

    @InjectMocks AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .email("test@fasol.ru")
                .password("password123")
                .firstName("Иван")
                .lastName("Иванов")
                .phone("+79001234567")
                .build();

        savedUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@fasol.ru")
                .passwordHash("hashed")
                .firstName("Иван")
                .lastName("Иванов")
                .roles(Set.of(AppRole.STUDENT))
                .build();
    }

    @Test
    @DisplayName("Регистрация — создаёт пользователя с ролью STUDENT")
    void register_success_createsStudentRole() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(savedUser);
        when(jwtProvider.generateToken(any(), anyString(), any())).thenReturn("jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("test@fasol.ru");
        assertThat(response.getRoles()).contains("STUDENT");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Регистрация — бросает UserAlreadyExistsException если email занят")
    void register_emailTaken_throws() {
        when(userRepository.existsByEmail("test@fasol.ru")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("test@fasol.ru");
    }

    @Test
    @DisplayName("Логин — возвращает токен при верных данных")
    void login_success_returnsToken() {
        UserPrincipal principal = new UserPrincipal(savedUser);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(savedUser));
        when(jwtProvider.generateToken(any(), anyString(), any())).thenReturn("jwt-token");

        LoginRequest loginRequest = LoginRequest.builder()
                .email("test@fasol.ru")
                .password("password123")
                .build();

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getFirstName()).isEqualTo("Иван");
    }
}
