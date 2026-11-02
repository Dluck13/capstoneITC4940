package com.itsupportoperationsdashboard.capstoneProject.technician;

import com.itsupportoperationsdashboard.capstoneProject.model.User;
import com.itsupportoperationsdashboard.capstoneProject.service.UserService;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(properties = "spring.jpa.open-in-view=false")
@Transactional
class TechnicianApiIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired UserService userService;
    @Autowired ObjectMapper mapper;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void unauthenticatedTechnicianListRejected() throws Exception {
        mvc.perform(get("/api/technicians")).andExpect(status().isUnauthorized());
    }

    @Test
    void nonAdminCannotListTechnicians() throws Exception {
        MockHttpSession session = login(createUser("TECHNICIAN", "Tech Viewer", true));

        mvc.perform(get("/api/technicians").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminGetsOnlyActiveTechniciansIncludingLegacyTechRole() throws Exception {
        String technicianName = "Technician " + UUID.randomUUID();
        String legacyTechName = "Legacy Tech " + UUID.randomUUID();
        String inactiveTechName = "Inactive Tech " + UUID.randomUUID();
        String endUserName = "End User " + UUID.randomUUID();
        String adminName = "Admin " + UUID.randomUUID();

        User activeTechnician = createUser("TECHNICIAN", technicianName, true);
        User activeLegacyTech = createUser("TECH", legacyTechName, true);
        createUser("TECHNICIAN", inactiveTechName, false);
        createUser("END_USER", endUserName, true);
        createUser("ADMIN", adminName, true);

        MockHttpSession adminSession = login(createUser("ADMIN", "API Admin", true));

        mvc.perform(get("/api/technicians").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItems(activeTechnician.getUserId().intValue(), activeLegacyTech.getUserId().intValue())))
                .andExpect(jsonPath("$[*].name", hasItems(technicianName, legacyTechName)))
                .andExpect(jsonPath("$[*].name").value(not(hasItems(inactiveTechName, endUserName, adminName))))
                .andExpect(jsonPath("$[*].email").doesNotExist())
                .andExpect(jsonPath("$[*].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[*].role").doesNotExist())
                .andExpect(jsonPath("$[*].active").doesNotExist())
                .andExpect(jsonPath("$[*].userId").doesNotExist());
    }

    private User createUser(String role, String name, boolean active) {
        User user = new User();
        user.setName(name);
        user.setEmail("technician-api-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("TechnicianPassword123!");
        user.setRole(role);
        user.setActive(active);
        return userService.saveUser(user);
    }

    private MockHttpSession login(User user) throws Exception {
        var result = mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of(
                                "email", user.getEmail(),
                                "password", "TechnicianPassword123!"))))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
