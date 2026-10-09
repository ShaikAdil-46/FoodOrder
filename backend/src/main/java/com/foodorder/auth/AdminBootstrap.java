package com.foodorder.auth;

import com.foodorder.user.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrap {
    @Bean
    ApplicationRunner createConfiguredAdmin(Environment environment, UserRepository users, PasswordEncoder encoder) {
        return args -> {
            String email = environment.getProperty("ADMIN_EMAIL");
            String password = environment.getProperty("ADMIN_PASSWORD");
            if ((email == null || email.isBlank()) && (password == null || password.isBlank())) return;
            if (email == null || email.isBlank() || password == null || password.length() < 12) {
                throw new IllegalStateException("Set both ADMIN_EMAIL and ADMIN_PASSWORD (at least 12 characters).");
            }
            String normalizedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);
            String mobile = String.format(java.util.Locale.ROOT, "+1%010d",
                    Math.abs((long) normalizedEmail.hashCode()) % 10_000_000_000L);
            users.findByEmail(normalizedEmail).orElseGet(() ->
                    users.save(new UserEntity("FoodOrder", "Admin", mobile, normalizedEmail,
                            encoder.encode(password), UserRole.ADMIN)));
        };
    }
}
