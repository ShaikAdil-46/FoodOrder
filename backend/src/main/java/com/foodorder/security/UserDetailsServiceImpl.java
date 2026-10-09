package com.foodorder.security;

import com.foodorder.user.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository users;

    public UserDetailsServiceImpl(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return users.findByEmail(username)
                .map(AppPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    }
}
