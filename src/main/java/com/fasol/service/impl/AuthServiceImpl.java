package com.fasol.service.impl;

import com.fasol.domain.entity.User;
import com.fasol.domain.enums.AppRole;
import com.fasol.dto.request.LoginRequest;
import com.fasol.dto.request.RegisterRequest;
import com.fasol.dto.response.AuthResponse;
import com.fasol.exception.UserAlreadyExistsException;
import com.fasol.repository.UserRepository;
import com.fasol.security.JwtTokenProvider;
import com.fasol.security.UserPrincipal;
import com.fasol.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtProvider;
    private final AuthenticationManager authManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new UserAlreadyExistsException(
                    "Пользователь с email " + req.getEmail() + " уже существует");
        }
        User user = User.builder()
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .phone(req.getPhone())
                .roles(Set.of(AppRole.STUDENT))
                .build();
        User savedUser = userRepository.save(user);
        log.info("Registered new student: {}", savedUser.getEmail());
        return buildResponse(savedUser);
    }

    @Override
    public AuthResponse login(LoginRequest req) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow();
        log.info("User logged in: {}", user.getEmail());
        return buildResponse(user);
    }

    private AuthResponse buildResponse(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(AppRole::name).collect(Collectors.toSet());
        String token = jwtProvider.generateToken(user.getId(), user.getEmail(), roleNames);
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId().toString())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(roleNames)
                .build();
    }
}
