package com.fasol.security;

import com.fasol.domain.entity.User;
import com.fasol.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * loadUserByUsername вызывается двумя путями:
 * 1. При логине — передаётся email через AuthenticationManager
 * 2. При каждом запросе через JwtFilter — передаётся userId (UUID string)
 * Метод пробует оба варианта.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String value) throws UsernameNotFoundException {
        User user = tryById(value);
        if (user == null) {
            user = userRepository.findByEmail(value)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + value));
        }
        return new UserPrincipal(user);
    }

    private User tryById(String value) {
        try {
            return userRepository.findById(UUID.fromString(value)).orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
