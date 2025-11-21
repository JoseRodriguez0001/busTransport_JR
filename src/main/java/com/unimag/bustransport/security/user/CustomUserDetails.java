package com.unimag.bustransport.security.user;

import com.unimag.bustransport.domain.entities.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

     private final Long userId;
    private final String email;
    private final String name;

    private final String password; // Password hasheado (BCrypt)
    private final String role;
    private final boolean isActive;

     public static CustomUserDetails fromUser(User user) {
        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPasswordHash(),
                user.getRole().name(),  // Convierte enum a String: ROLE_PASSENGER
                user.getStatus() == User.Status.ACTIVE
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

     @Override
    public String getPassword() {
        return password;
    }

     @Override
    public String getUsername() {
        return email;
    }

     @Override
    public boolean isAccountNonExpired() {
        return true;
    }

     @Override
    public boolean isAccountNonLocked() {
        return true;
    }

     @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

     @Override
    public boolean isEnabled() {
        return isActive;
    }
}
