package com.foodorder.security;

import com.foodorder.user.UserEntity;
import com.foodorder.user.UserRole;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AppPrincipal implements UserDetails {
    private final UserEntity user;

    public AppPrincipal(UserEntity user) {
        this.user = user;
    }

    public Long getId() { return user.getId(); }
    public String getFirstName() { return user.getFirstName(); }
    public String getLastName() { return user.getLastName(); }
    public String getEmail() { return user.getEmail(); }
    public String getMobile() { return user.getMobile(); }
    public UserRole getRole() { return user.getRole(); }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() { return user.getPassword(); }

    @Override
    public String getUsername() { return user.getEmail(); }
}
