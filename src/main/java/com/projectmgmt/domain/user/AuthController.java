package com.projectmgmt.domain.user;

import com.projectmgmt.common.model.ApiResponse;
import com.projectmgmt.common.util.SecurityUtils;
import com.projectmgmt.domain.user.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration and login")
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user")
    public ResponseEntity<ApiResponse<UserResponse>> me() {
        UserResponse response = userService.getCurrentUser(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/search")
    @Operation(summary = "Search users by email (exact match)")
    public ResponseEntity<ApiResponse<UserResponse>> searchByEmail(@RequestParam String email) {
        UserResponse response = userService.findByEmail(email);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

}
