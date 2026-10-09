package com.foodorder.auth;

import com.foodorder.security.AppPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthDtos.LoginResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthDtos.LoginResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public AuthDtos.UserResponse me(@AuthenticationPrincipal AppPrincipal user) {
        return AuthDtos.UserResponse.from(user);
    }
}
