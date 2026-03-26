package com.fasol.service;

import com.fasol.dto.request.LoginRequest;
import com.fasol.dto.request.RegisterRequest;
import com.fasol.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
