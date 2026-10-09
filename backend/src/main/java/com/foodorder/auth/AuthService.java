package com.foodorder.auth;

import com.foodorder.security.JwtService;
import com.foodorder.user.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthDtos.LoginResponse register(AuthDtos.RegisterRequest request) {
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        String mobile = request.mobile().replaceAll("[\\s()-]", "");
        if (users.existsByEmail(email) || users.existsByMobile(mobile)) {
            throw new DataIntegrityViolationException("Account already exists");
        }
        var user = users.save(new UserEntity(request.firstName().trim(), request.lastName().trim(), mobile, email,
                passwordEncoder.encode(request.password()), UserRole.CUSTOMER));
        return new AuthDtos.LoginResponse(jwtService.createToken(user), AuthDtos.UserResponse.from(user));
    }

    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
            var user = ((com.foodorder.security.AppPrincipal) authentication.getPrincipal());
            var entity = users.findById(user.getId()).orElseThrow();
            return new AuthDtos.LoginResponse(jwtService.createToken(entity), AuthDtos.UserResponse.from(entity));
        } catch (AuthenticationException exception) {
            throw new org.springframework.security.authentication.BadCredentialsException("Email or password is incorrect.");
        }
    }
}
