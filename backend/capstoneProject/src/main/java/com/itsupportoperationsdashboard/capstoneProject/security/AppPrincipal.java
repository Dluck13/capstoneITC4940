package com.itsupportoperationsdashboard.capstoneProject.security;

import com.itsupportoperationsdashboard.capstoneProject.auth.AuthDtos.UserResponse;
import com.itsupportoperationsdashboard.capstoneProject.model.User;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public final class AppPrincipal extends org.springframework.security.core.userdetails.User {
    private final UserResponse profile;

    public AppPrincipal(User user, String role) {
        super(user.getEmail(), user.getPasswordHash(), user.isActive(), true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        profile = new UserResponse(user.getUserId(), user.getName(), user.getEmail(), role);
    }

    public UserResponse profile() { return profile; }

    public static String canonicalRole(String role) {
        if (role == null) return "";
        return switch (role) {
            case "USER", "END_USER" -> "END_USER";
            case "TECH", "TECHNICIAN" -> "TECHNICIAN";
            case "ADMIN" -> "ADMIN";
            default -> "";
        };
    }
}
