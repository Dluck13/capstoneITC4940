package com.itsupportoperationsdashboard.capstoneProject.account;

import com.itsupportoperationsdashboard.capstoneProject.model.User;
import com.itsupportoperationsdashboard.capstoneProject.repository.UserRepository;
import com.itsupportoperationsdashboard.capstoneProject.service.UserService;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(properties = "spring.jpa.open-in-view=false")
@Transactional
class AccountManagementApiIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired BCryptPasswordEncoder encoder;
    @Autowired ObjectMapper mapper;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void publicRegistrationCreatesEndUserWithBCryptHashAndAllowsLogin() throws Exception {
        String rawEmail = "New.End.User." + UUID.randomUUID() + "@Example.com";
        String password = "RegistrationPass123!";

        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of(
                                "name", "New End User",
                                "email", rawEmail,
                                "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New End User"))
                .andExpect(jsonPath("$.email").value(rawEmail.toLowerCase()))
                .andExpect(jsonPath("$.role").value("END_USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        User saved = users.findByEmailIgnoreCase(rawEmail).orElseThrow();
        assertEquals("END_USER", saved.getRole());
        assertTrue(saved.isActive());
        assertNotEquals(password, saved.getPasswordHash());
        assertTrue(saved.getPasswordHash().startsWith("$2"));
        assertTrue(encoder.matches(password, saved.getPasswordHash()));

        mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of(
                                "email", rawEmail,
                                "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("END_USER"));
    }

    @Test
    void publicRegistrationRejectsDuplicateEmail() throws Exception {
        String email = "duplicate." + UUID.randomUUID() + "@example.com";
        createUser("END_USER", email, "DuplicatePass123!", true);

        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of(
                                "name", "Duplicate User",
                                "email", email.toUpperCase(),
                                "password", "DuplicatePass123!"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email address is already in use"));
    }

    @Test
    void publicRegistrationRejectsInvalidEmailAndWeakPassword() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of(
                                "name", "Invalid Registration",
                                "email", "invalid-email",
                                "password", "short"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publicRegistrationRequiresCsrf() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of(
                                "name", "Missing Csrf",
                                "email", "missing.csrf." + UUID.randomUUID() + "@example.com",
                                "password", "MissingCsrfPass123!"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicRegistrationIgnoresRoleInjectionAndForcesEndUserRole() throws Exception {
        String email = "role.injection." + UUID.randomUUID() + "@example.com";

        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
                        .content("""
                                {
                                  "name": "Injected User",
                                  "email": "%s",
                                  "password": "InjectedPass123!",
                                  "role": "ADMIN",
                                  "active": false
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("END_USER"));

        User saved = users.findByEmailIgnoreCase(email).orElseThrow();
        assertEquals("END_USER", saved.getRole());
        assertTrue(saved.isActive());
    }

    @Test
    void adminCanCreateTechnicianAndNewTechnicianCanLogin() throws Exception {
        MockHttpSession adminSession = login(createUser("ADMIN", randomEmail("admin"), "AdminPass123!", true), "AdminPass123!");
        String email = randomEmail("new-tech");
        String password = "TechPass123!";

        mvc.perform(post("/api/admin/users").session(adminSession).with(csrf()).contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of(
                                "name", "New Technician",
                                "email", email,
                                "password", password,
                                "role", "TECHNICIAN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TECHNICIAN"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        User saved = users.findByEmailIgnoreCase(email).orElseThrow();
        assertEquals("TECHNICIAN", saved.getRole());
        assertTrue(saved.isActive());
        assertNotEquals(password, saved.getPasswordHash());
        assertTrue(encoder.matches(password, saved.getPasswordHash()));

        mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TECHNICIAN"));
    }

    @Test
    void adminCanCreateAdminAndNewAdminCanLogin() throws Exception {
        MockHttpSession adminSession = login(createUser("ADMIN", randomEmail("admin"), "AdminPass123!", true), "AdminPass123!");
        String email = randomEmail("new-admin");
        String password = "AnotherAdminPass123!";

        mvc.perform(post("/api/admin/users").session(adminSession).with(csrf()).contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of(
                                "name", "New Admin",
                                "email", email,
                                "password", password,
                                "role", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        User saved = users.findByEmailIgnoreCase(email).orElseThrow();
        assertEquals("ADMIN", saved.getRole());
        assertFalse(saved.getPasswordHash().equals(password));
        assertTrue(encoder.matches(password, saved.getPasswordHash()));

        mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void endUserAndTechnicianCannotCreateAdminUsers() throws Exception {
        MockHttpSession endUserSession = login(createUser("END_USER", randomEmail("end"), "EndPass123!", true), "EndPass123!");
        MockHttpSession techSession = login(createUser("TECHNICIAN", randomEmail("tech"), "TechPass123!", true), "TechPass123!");

        String body = mapper.writeValueAsString(Map.of(
                "name", "Forbidden",
                "email", randomEmail("forbidden"),
                "password", "ForbiddenPass123!",
                "role", "TECHNICIAN"));

        mvc.perform(post("/api/admin/users").session(endUserSession).with(csrf()).contentType("application/json").content(body))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/admin/users").session(techSession).with(csrf()).contentType("application/json").content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedAdminCreateRequestRejected() throws Exception {
        mvc.perform(post("/api/admin/users").with(csrf()).contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of(
                                "name", "Unauthenticated",
                                "email", randomEmail("anon"),
                                "password", "AnonPass123!",
                                "role", "TECHNICIAN"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCreateRejectsInvalidRoleAndDuplicateEmail() throws Exception {
        MockHttpSession adminSession = login(createUser("ADMIN", randomEmail("admin"), "AdminPass123!", true), "AdminPass123!");
        String duplicateEmail = randomEmail("duplicate-admin-create");
        createUser("TECHNICIAN", duplicateEmail, "ExistingPass123!", true);

        mvc.perform(post("/api/admin/users").session(adminSession).with(csrf()).contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of(
                                "name", "Invalid Role",
                                "email", randomEmail("invalid-role"),
                                "password", "InvalidRolePass123!",
                                "role", "END_USER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Role must be TECHNICIAN or ADMIN"));

        mvc.perform(post("/api/admin/users").session(adminSession).with(csrf()).contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of(
                                "name", "Duplicate",
                                "email", duplicateEmail.toUpperCase(),
                                "password", "DuplicatePass123!",
                                "role", "TECHNICIAN"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email address is already in use"));
    }

    private User createUser(String role, String email, String password, boolean active) {
        User user = new User();
        user.setName("Account API User " + UUID.randomUUID());
        user.setEmail(email);
        user.setPasswordHash(password);
        user.setRole(role);
        user.setActive(active);
        return userService.saveUser(user);
    }

    private MockHttpSession login(User user, String password) throws Exception {
        var result = mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of(
                                "email", user.getEmail(),
                                "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String randomEmail(String prefix) {
        return prefix + "." + UUID.randomUUID() + "@example.com";
    }
}

