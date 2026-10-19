package com.itsupportoperationsdashboard.capstoneProject.security;

import com.itsupportoperationsdashboard.capstoneProject.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserRepository users;
    public DatabaseUserDetailsService(UserRepository users) { this.users = users; }

    @Override
    public UserDetails loadUserByUsername(String email) {
        var user = users.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        String role = AppPrincipal.canonicalRole(user.getRole());
        if (role.isEmpty()) throw new UsernameNotFoundException("Invalid credentials");
        return new AppPrincipal(user, role);
    }
}
