package com.itsupportoperationsdashboard.capstoneProject.category;

import com.itsupportoperationsdashboard.capstoneProject.model.Category;
import com.itsupportoperationsdashboard.capstoneProject.model.User;
import com.itsupportoperationsdashboard.capstoneProject.repository.CategoryRepository;
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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(properties = "spring.jpa.open-in-view=false")
@Transactional
class CategoryApiIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired UserService userService;
    @Autowired CategoryRepository categories;
    @Autowired ObjectMapper mapper;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void unauthenticatedCategoryListRejected() throws Exception {
        mvc.perform(get("/api/categories")).andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserGetsOnlyActiveSafeCategoryDtos() throws Exception {
        String accountingName = "Accounting " + UUID.randomUUID();
        String networkName = "Network " + UUID.randomUUID();
        String inactiveName = "Inactive Category " + UUID.randomUUID();

        Category activeA = createCategory(accountingName, true);
        Category activeB = createCategory(networkName, true);
        createCategory(inactiveName, false);

        MockHttpSession session = login(createUser("END_USER"));

        mvc.perform(get("/api/categories").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$[*].id", hasItems(activeA.getCategoryId().intValue(), activeB.getCategoryId().intValue())))
                .andExpect(jsonPath("$[*].name", hasItems(accountingName, networkName)))
                .andExpect(jsonPath("$[*].name", hasItems(accountingName, networkName)))
                .andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.not(hasItems(inactiveName))))
                .andExpect(jsonPath("$[*].description").doesNotExist())
                .andExpect(jsonPath("$[*].active").doesNotExist())
                .andExpect(jsonPath("$[*].categoryName").doesNotExist())
                .andExpect(jsonPath("$[*].categoryId").doesNotExist());
    }

    @Test
    void supportRolesCanAccessCategories() throws Exception {
        createCategory("Hardware " + UUID.randomUUID(), true);

        for (String role : new String[]{"TECHNICIAN", "ADMIN"}) {
            mvc.perform(get("/api/categories").session(login(createUser(role))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));
        }
    }

    private Category createCategory(String name, boolean active) {
        Category category = new Category();
        category.setCategoryName(name);
        category.setActive(active);
        return categories.saveAndFlush(category);
    }

    private User createUser(String role) {
        User user = new User();
        user.setName("Category API User");
        user.setEmail("category-api-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("CategoryPassword123!");
        user.setRole(role);
        return userService.saveUser(user);
    }

    private MockHttpSession login(User user) throws Exception {
        var result = mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of(
                                "email", user.getEmail(),
                                "password", "CategoryPassword123!"))))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
