package com.foodorder.auth;

import com.foodorder.security.AppPrincipal;
import com.foodorder.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final UserRepository users;

    public ProfileController(UserRepository users) {
        this.users = users;
    }

    @PutMapping
    public AuthDtos.UserResponse update(@AuthenticationPrincipal AppPrincipal principal,
                                        @Valid @RequestBody AuthDtos.ProfileRequest request) {
        var user = users.findById(principal.getId()).orElseThrow();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setMobile(request.mobile().replaceAll("[\\s()-]", ""));
        return AuthDtos.UserResponse.from(user);
    }
}
