package com.foodorder.auth;

import com.foodorder.security.AppPrincipal;
import com.foodorder.user.UserEntity;
import com.foodorder.user.UserRole;
import jakarta.validation.constraints.*;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Size(max = 60) String firstName,
            @NotBlank @Size(max = 60) String lastName,
            @NotBlank @Pattern(regexp = "^\\+?[0-9 ()-]{7,20}$", message = "Enter a valid mobile number") String mobile,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72)
            @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                    message = "Use at least 8 characters with an uppercase letter, a lowercase letter, and a number")
            String password) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record LoginResponse(String token, UserResponse user) {}
    public record UserResponse(Long id, String firstName, String lastName, String mobile, String email, UserRole role) {
        public static UserResponse from(UserEntity user) {
            return new UserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getMobile(),
                    user.getEmail(), user.getRole());
        }
        public static UserResponse from(AppPrincipal user) {
            return new UserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getMobile(),
                    user.getEmail(), user.getRole());
        }
    }

    public record ProfileRequest(@NotBlank @Size(max = 60) String firstName,
                                 @NotBlank @Size(max = 60) String lastName,
                                 @NotBlank @Pattern(regexp = "^\\+?[0-9 ()-]{7,20}$") String mobile) {}
}
