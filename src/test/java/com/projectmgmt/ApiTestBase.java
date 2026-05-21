package com.projectmgmt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectmgmt.common.util.JwtUtils;
import com.projectmgmt.domain.user.User;
import com.projectmgmt.domain.user.UserRepository;
import com.projectmgmt.domain.user.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

/**
 * Base class for controller integration tests.
 * Provides MockMvc, ObjectMapper, and helper methods for auth.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiTestBase extends IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtUtils jwtUtils;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected User createTestUser(String email, String displayName) {
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("password123"))
                .displayName(displayName)
                .role(UserRole.MEMBER)
                .build();
        return userRepository.save(user);
    }

    protected String tokenFor(User user) {
        return jwtUtils.generateToken(user.getId(), user.getEmail(),
                Map.of("role", user.getRole().name(), "name", user.getDisplayName()));
    }

    protected String bearerToken(User user) {
        return "Bearer " + tokenFor(user);
    }

    protected String uniqueEmail() {
        return "test-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }
}
