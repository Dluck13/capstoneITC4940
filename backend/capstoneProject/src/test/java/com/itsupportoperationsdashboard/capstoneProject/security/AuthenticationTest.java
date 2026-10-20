package com.itsupportoperationsdashboard.capstoneProject.security;

import com.itsupportoperationsdashboard.capstoneProject.auth.*;
import com.itsupportoperationsdashboard.capstoneProject.config.*;
import com.itsupportoperationsdashboard.capstoneProject.model.User;
import com.itsupportoperationsdashboard.capstoneProject.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringJUnitWebConfig(AuthenticationTest.Config.class)
class AuthenticationTest {
    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, PasswordConfig.class, DatabaseUserDetailsService.class,
            AuthController.class, AuthExceptionHandler.class, TicketAccess.class})
    static class Config {
        @Bean UserRepository users() { return mock(UserRepository.class); }
        @Bean ObjectMapper mapper() { return new ObjectMapper(); }
        @Bean ProtectedOperation protectedOperation() { return new ProtectedOperation(); }
    }

    static class ProtectedOperation {
        @PreAuthorize("@ticketAccess.canClose(authentication)")
        public String closePermission() { return "allowed"; }
    }

    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired BCryptPasswordEncoder encoder;
    @Autowired ObjectMapper mapper;
    @Autowired TicketAccess access;
    @Autowired ProtectedOperation operation;
    MockMvc mvc;
    User user;

    @BeforeEach
    void setup() {
        reset(users);
        SecurityContextHolder.clearContext();
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
        user = new User();
        user.setUserId(7L);
        user.setName("Test User");
        user.setEmail("user@example.com");
        user.setPasswordHash(encoder.encode("TestPassword123!"));
        user.setRole("END_USER");
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    @Test
    void loginAndCurrentUserReturnOnlyPublicProfile() throws Exception {
        var result = mvc.perform(post("/api/auth/login").with(csrf())
                .contentType("application/json").content(loginJson("user@example.com", "TestPassword123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("END_USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist()).andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
        var saved = (org.springframework.security.core.context.SecurityContext)
                session.getAttribute("SPRING_SECURITY_CONTEXT");
        assertNull(((AppPrincipal) saved.getAuthentication().getPrincipal()).getPassword());
    }

    @Test void incorrectPassword() throws Exception { rejected("user@example.com", "incorrect"); }
    @Test void unknownEmail() throws Exception { rejected("unknown@example.com", "TestPassword123!"); }
    @Test void inactiveUser() throws Exception {
        user.setActive(false);
        rejected("user@example.com", "TestPassword123!");
    }
    @Test void unknownRole() throws Exception {
        user.setRole("SUPERUSER");
        rejected("user@example.com", "TestPassword123!");
    }

    @Test
    void anonymousCurrentUserIsUnauthorized() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void missingCsrfIsForbidden() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(loginJson(user.getEmail(), "TestPassword123!")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        verifyNoInteractions(users);
    }

    @Test
    void browserCsrfFlowRotatesSessionAndLogsOut() throws Exception {
        var tokenResult = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        var token = mapper.readTree(tokenResult.getResponse().getContentAsString());
        var session = (MockHttpSession) tokenResult.getRequest().getSession(false);
        String originalId = session.getId();
        mvc.perform(post("/api/auth/login").session(session)
                .header(token.get("headerName").asString(), token.get("token").asString())
                .contentType("application/json").content(loginJson(user.getEmail(), "TestPassword123!")))
                .andExpect(status().isOk());
        assertNotEquals(originalId, session.getId());
        mvc.perform(post("/api/auth/logout").session(session)
                .header(token.get("headerName").asString(), token.get("token").asString()))
                .andExpect(status().isForbidden());
        var freshResult = mvc.perform(get("/api/auth/csrf").session(session)).andReturn();
        var fresh = mapper.readTree(freshResult.getResponse().getContentAsString());
        mvc.perform(post("/api/auth/logout").session(session)
                .header(fresh.get("headerName").asString(), fresh.get("token").asString()))
                .andExpect(status().isNoContent());
        assertTrue(session.isInvalid());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void corsAllowsOnlyReactOrigin() throws Exception {
        mvc.perform(options("/api/auth/login").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,x-csrf-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(options("/api/auth/login").header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "POST")).andExpect(status().isForbidden());
    }

    @Test
    void invalidJsonAndInvalidFieldsAreBadRequests() throws Exception {
        for (String body : new String[]{"{", "{}", loginJson("invalid", "password")}) {
            mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        }
    }

    @Test
    void entitiesCannotSerializePasswordHashes() {
        String json = mapper.writeValueAsString(user);
        assertFalse(json.contains("password"));
        assertFalse(json.contains(user.getPasswordHash()));
    }

    @Test
    void roleAndOwnershipMatrix() {
        var endUser = authentication("END_USER");
        assertTrue(access.canCreate(endUser));
        assertTrue(access.canView(endUser, 7L));
        assertTrue(access.canComment(endUser, 7L));
        assertFalse(access.canView(endUser, 8L));
        assertFalse(access.canComment(endUser, 8L));
        assertFalse(access.canAssign(endUser, 7L));
        assertFalse(access.canUpdateStatus(endUser));
        assertFalse(access.canResolve(endUser));
        assertFalse(access.canClose(endUser));
        var tech = authentication("TECHNICIAN");
        assertTrue(access.canView(tech, 8L));
        assertTrue(access.canComment(tech, 8L));
        assertTrue(access.canAssign(tech, 7L));
        assertFalse(access.canAssign(tech, 8L));
        assertTrue(access.canUpdateStatus(tech));
        assertTrue(access.canResolve(tech));
        assertFalse(access.canClose(tech));
        var admin = authentication("ADMIN");
        assertTrue(access.canView(admin, 8L));
        assertTrue(access.canAssign(admin, 8L));
        assertTrue(access.canUpdateStatus(admin));
        assertTrue(access.canResolve(admin));
        assertTrue(access.canClose(admin));
        assertFalse(access.canCreate(null));
        assertThrows(AccessDeniedException.class, () -> access.currentUserId(null));
    }

    @Test
    void methodSecurityEnforcesRolePredicates() {
        try {
            SecurityContextHolder.getContext().setAuthentication(authentication("END_USER"));
            assertThrows(AccessDeniedException.class, operation::closePermission);
            SecurityContextHolder.getContext().setAuthentication(authentication("TECHNICIAN"));
            assertThrows(AccessDeniedException.class, operation::closePermission);
            SecurityContextHolder.getContext().setAuthentication(authentication("ADMIN"));
            assertEquals("allowed", operation.closePermission());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void unimplementedRoutesAreDeniedEvenForAuthenticatedUsers() throws Exception {
        var result = mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
                .content(loginJson(user.getEmail(), "TestPassword123!"))).andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(post("/api/tickets/1/assign").session(session).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/register").session(session).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void legacyRolesAreMappedWithoutChangingStoredRole() throws Exception {
        for (String role : new String[]{"USER", "TECH", "ADMIN"}) {
            user.setRole(role);
            mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
                    .content(loginJson(user.getEmail(), "TestPassword123!")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value(AppPrincipal.canonicalRole(role)));
            assertEquals(role, user.getRole());
        }
    }

    private UsernamePasswordAuthenticationToken authentication(String role) {
        var principal = new AppPrincipal(user, role);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }

    private void rejected(String email, String password) throws Exception {
        var result = mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
                .content(loginJson(email, password))).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist()).andReturn();
        var session = result.getRequest().getSession(false);
        assertTrue(session == null || session.getAttribute("SPRING_SECURITY_CONTEXT") == null);
    }

    private String loginJson(String email, String password) {
        return mapper.writeValueAsString(java.util.Map.of("email", email, "password", password));
    }
}
