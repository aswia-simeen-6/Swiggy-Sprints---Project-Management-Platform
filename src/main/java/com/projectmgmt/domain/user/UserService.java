package com.projectmgmt.domain.user;

import com.projectmgmt.common.exception.DuplicateResourceException;
import com.projectmgmt.common.exception.ResourceNotFoundException;
import com.projectmgmt.common.util.JwtUtils;
import com.projectmgmt.domain.user.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailAndDeletedAtIsNull(request.email())) {
            throw new DuplicateResourceException("User", "email", request.email());
        }

        User user = User.builder()
                .email(request.email().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName().trim())
                .role(UserRole.MEMBER)
                .build();

        User saved = userRepository.save(user);
        log.info("User registered: {} ({})", saved.getDisplayName(), saved.getEmail());

        String token = jwtUtils.generateToken(saved.getId(), saved.getEmail(),
                Map.of("role", saved.getRole().name(), "name", saved.getDisplayName()));

        return AuthResponse.of(token, toResponse(saved));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().toLowerCase().trim(), request.password()));

        User user = (User) auth.getPrincipal();
        String token = jwtUtils.generateToken(user.getId(), user.getEmail(),
                Map.of("role", user.getRole().name(), "name", user.getDisplayName()));

        return AuthResponse.of(token, toResponse(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        return getUserById(userId);
    }
    
    @Transactional(readOnly = true)
    public UserResponse findByEmail(String email) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return toResponse(user);
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}
