package com.itsupportoperationsdashboard.capstoneProject.config;

import com.itsupportoperationsdashboard.capstoneProject.repository.UserRepository;
import com.itsupportoperationsdashboard.capstoneProject.service.DuplicateEmailException;
import com.itsupportoperationsdashboard.capstoneProject.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminBootstrapConfig {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapConfig.class);

    @Bean
    ApplicationRunner adminBootstrapRunner(
            UserRepository users,
            UserService userService,
            @Value("${app.bootstrap.admin.name:TicketCoop Admin}") String adminName,
            @Value("${app.bootstrap.admin.email:}") String adminEmail,
            @Value("${app.bootstrap.admin.password:}") String adminPassword) {
        return args -> {
            if (users.existsByRoleIgnoreCaseAndActiveTrue("ADMIN")) {
                return;
            }

            if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
                log.warn("No active ADMIN account exists and bootstrap admin credentials are not configured. "
                        + "Set app.bootstrap.admin.email and app.bootstrap.admin.password to create one on startup.");
                return;
            }

            try {
                userService.createSupportAccount(adminName, adminEmail, adminPassword, "ADMIN");
                log.info("Bootstrapped initial ADMIN account for email {}", UserService.normalizeEmail(adminEmail));
            } catch (DuplicateEmailException duplicateEmailException) {
                log.info("Bootstrap admin email already exists: {}", UserService.normalizeEmail(adminEmail));
            }
        };
    }
}
