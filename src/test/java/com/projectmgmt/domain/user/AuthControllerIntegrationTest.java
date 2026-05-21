package com.projectmgmt.domain.user;

import com.projectmgmt.ApiTestBase;
import com.projectmgmt.domain.user.dto.LoginRequest;
import com.projectmgmt.domain.user.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerIntegrationTest extends ApiTestBase {

    @Test
    void register_shouldCreateUserAndReturnToken() throws Exception {
        RegisterRequest request = new RegisterRequest(uniqueEmail(), "password123", "Test User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value(request.email()))
                .andExpect(jsonPath("$.data.user.displayName").value("Test User"));
    }

    @Test
    void register_duplicateEmail_shouldReturn409() throws Exception {
        String email = uniqueEmail();
        createTestUser(email, "First User");

        RegisterRequest request = new RegisterRequest(email, "password123", "Duplicate User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_RESOURCE"));
    }

    @Test
    void register_invalidEmail_shouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest("not-an-email", "password123", "Bad Email");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shortPassword_shouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest(uniqueEmail(), "short", "Short Pass");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_validCredentials_shouldReturnToken() throws Exception {
        String email = uniqueEmail();
        createTestUser(email, "Login User");

        LoginRequest request = new LoginRequest(email, "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value(email));
    }

    @Test
    void login_wrongPassword_shouldReturn401() throws Exception {
        String email = uniqueEmail();
        createTestUser(email, "Wrong Pass User");

        LoginRequest request = new LoginRequest(email, "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_authenticated_shouldReturnCurrentUser() throws Exception {
        User user = createTestUser(uniqueEmail(), "Me User");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.data.displayName").value("Me User"));
    }

    @Test
    void me_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
