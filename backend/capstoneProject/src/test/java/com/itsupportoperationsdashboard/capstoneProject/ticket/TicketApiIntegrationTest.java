package com.itsupportoperationsdashboard.capstoneProject.ticket;

import com.itsupportoperationsdashboard.capstoneProject.model.*;
import com.itsupportoperationsdashboard.capstoneProject.repository.*;
import com.itsupportoperationsdashboard.capstoneProject.service.UserService;
import jakarta.persistence.EntityManager;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(properties = "spring.jpa.open-in-view=false")
@Transactional
class TicketApiIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired UserService userService;
    @Autowired CategoryRepository categories;
    @Autowired TicketRepository tickets;
    @Autowired EntityManager entityManager;
    @Autowired ObjectMapper mapper;
    MockMvc mvc;
    User owner;
    User other;
    Category category;

    @BeforeEach
    void setup() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
        owner = createUser("END_USER");
        other = createUser("END_USER");
        category = new Category();
        category.setCategoryName("Ticket API " + UUID.randomUUID());
        category = categories.save(category);
    }

    @Test
    void createUsesSessionIdentityAndServerDefaultsDespiteSpoofedFields() throws Exception {
        var session = login(owner);
        String body = mapper.writeValueAsString(Map.of(
                "title", " Printer problem ", "description", "Cannot print", "priority", "HIGH",
                "categoryId", category.getCategoryId(), "requesterId", other.getUserId(),
                "requester", Map.of("userId", other.getUserId()), "status", "CLOSED",
                "assignedToId", other.getUserId(), "id", 12345));
        var result = mvc.perform(post("/api/tickets").session(session).with(csrf())
                .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requester.id").value(owner.getUserId()))
                .andExpect(jsonPath("$.title").value("Printer problem"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.assignedTechnician").value(nullValue()))
                .andExpect(jsonPath("$.category.id").value(category.getCategoryId()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn();
        long id = mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        assertEquals("/api/tickets/" + id, result.getResponse().getHeader("Location"));
        entityManager.flush();
        entityManager.clear();
        Ticket persisted = tickets.findById(id).orElseThrow();
        assertEquals(owner.getUserId(), persisted.getRequester().getUserId());
        assertEquals("OPEN", persisted.getStatus());
        assertNull(persisted.getAssignedTo());
        assertSafe(result.getResponse().getContentAsString());
    }

    @Test
    void invalidCategoryRejected() throws Exception {
        mvc.perform(post("/api/tickets").session(login(owner)).with(csrf())
                .contentType("application/json").content(body(Long.MAX_VALUE)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void inactiveCategoryRejected() throws Exception {
        category.setActive(false);
        categories.saveAndFlush(category);
        mvc.perform(post("/api/tickets").session(login(owner)).with(csrf())
                .contentType("application/json").content(body(category.getCategoryId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthenticatedRequestsRejected() throws Exception {
        mvc.perform(get("/api/tickets")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/tickets/1")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/tickets").with(csrf()).contentType("application/json")
                .content(body(category.getCategoryId()))).andExpect(status().isUnauthorized());
    }

    @Test
    void creationRequiresValidCsrf() throws Exception {
        var session = login(owner);
        mvc.perform(post("/api/tickets").session(session).contentType("application/json")
                .content(body(category.getCategoryId()))).andExpect(status().isForbidden());
        mvc.perform(post("/api/tickets").session(session).with(csrf().useInvalidToken())
                .contentType("application/json").content(body(category.getCategoryId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void endUserListsOnlyOwnTicketsAndCanReadOwnDetail() throws Exception {
        Ticket own = ticket(owner);
        ticket(other);
        var session = login(owner);
        entityManager.flush();
        entityManager.clear();
        var list = mvc.perform(get("/api/tickets").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(own.getTicketId()))
                .andExpect(jsonPath("$[0].description").doesNotExist()).andReturn();
        assertSafe(list.getResponse().getContentAsString());
        mvc.perform(get("/api/tickets/" + own.getTicketId()).session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.description").value("Ticket details"));
    }

    @Test
    void endUserCannotReadAnotherUsersTicket() throws Exception {
        Ticket someoneElses = ticket(other);
        mvc.perform(get("/api/tickets/" + someoneElses.getTicketId()).session(login(owner)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void technicianCanListAndReadSupportTickets() throws Exception {
        verifySupportAccess("TECHNICIAN");
    }

    @Test
    void administratorCanListAndReadAllTickets() throws Exception {
        verifySupportAccess("ADMIN");
    }

    @Test
    void missingTicketReturns404() throws Exception {
        mvc.perform(get("/api/tickets/" + Long.MAX_VALUE).session(login(owner)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void invalidFieldsAndMalformedRequestsRejected() throws Exception {
        var session = login(owner);
        for (String body : new String[]{"{}", "{", body(category.getCategoryId()).replace("MEDIUM", "INVALID"),
                mapper.writeValueAsString(Map.of("title", " ", "description", "x", "priority", "LOW",
                        "categoryId", category.getCategoryId())),
                mapper.writeValueAsString(Map.of("title", "x".repeat(151), "description", "x",
                        "priority", "LOW", "categoryId", category.getCategoryId())),
                mapper.writeValueAsString(Map.of("title", "x", "description", "", "priority", "LOW",
                        "categoryId", category.getCategoryId())),
                body(-1L)}) {
            mvc.perform(post("/api/tickets").session(session).with(csrf()).contentType("application/json")
                    .content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/tickets/not-a-number").session(session)).andExpect(status().isBadRequest());
    }

    @Test
    void futureWriteOperationsRemainDenied() throws Exception {
        var session = login(createUser("ADMIN"));
        mvc.perform(put("/api/tickets/1").session(session).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/tickets/1").session(session).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/api/tickets/1/assign").session(session).with(csrf())).andExpect(status().isForbidden());
    }

    private void verifySupportAccess(String role) throws Exception {
        Ticket first = ticket(owner);
        Ticket second = ticket(other);
        second.setAssignedTo(owner);
        tickets.saveAndFlush(second);
        var session = login(createUser(role));
        entityManager.clear();
        var list = mvc.perform(get("/api/tickets").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItems(first.getTicketId().intValue(), second.getTicketId().intValue())))
                .andReturn();
        assertSafe(list.getResponse().getContentAsString());
        var detail = mvc.perform(get("/api/tickets/" + second.getTicketId()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTechnician.id").value(owner.getUserId()))
                .andExpect(jsonPath("$.category.name").value(category.getCategoryName())).andReturn();
        assertSafe(detail.getResponse().getContentAsString());
    }

    private User createUser(String role) {
        User user = new User();
        user.setName("Ticket API User");
        user.setEmail("ticket-api-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("TicketApiPassword123!");
        user.setRole(role);
        return userService.saveUser(user);
    }

    private MockHttpSession login(User user) throws Exception {
        var result = mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
                .content(mapper.writeValueAsString(Map.of("email", user.getEmail(), "password", "TicketApiPassword123!"))))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private Ticket ticket(User requester) {
        Ticket ticket = new Ticket();
        ticket.setTitle("Test ticket");
        ticket.setDescription("Ticket details");
        ticket.setStatus("OPEN");
        ticket.setPriority("MEDIUM");
        ticket.setRequester(requester);
        ticket.setCategory(category);
        return tickets.saveAndFlush(ticket);
    }


    @Autowired com.itsupportoperationsdashboard.capstoneProject.service.TicketService ticketService;
    @Autowired UserRepository users;

    @Test
    void adminAssignsOpenTicketAndTimestampIsUpdated() throws Exception {
        Ticket ticket = ticket(owner);
        var before = ticket.getUpdatedAt();
        User technician = createUser("TECHNICIAN");
        var result = assign(ticket, technician, login(createUser("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.assignedTechnician.id").value(technician.getUserId())).andReturn();
        assertSafe(result.getResponse().getContentAsString());
        var timestamp = java.time.LocalDateTime.parse(
                mapper.readTree(result.getResponse().getContentAsString()).get("updatedAt").asString());
        assertTrue(timestamp.isAfter(before));
        entityManager.clear();
        Ticket saved = tickets.findById(ticket.getTicketId()).orElseThrow();
        assertEquals("ASSIGNED", saved.getStatus());
        assertEquals(technician.getUserId(), saved.getAssignedTo().getUserId());
    }

    @Test
    void technicianAssignsOnlySelf() throws Exception {
        User technician = createUser("TECHNICIAN");
        User another = createUser("TECHNICIAN");
        var session = login(technician);
        Ticket ticket = ticket(owner);
        assign(ticket, another, session).andExpect(status().isForbidden());
        assertNull(ticket.getAssignedTo());
        assign(ticket, technician, session).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }

    @Test
    void endUserCannotAssignOrUpdateStatus() throws Exception {
        var session = login(owner);
        Ticket ticket = ticket(owner);
        assign(ticket, createUser("TECHNICIAN"), session).andExpect(status().isForbidden());
        changeStatus(ticket, "IN_PROGRESS", session).andExpect(status().isForbidden());
        assertEquals("OPEN", ticket.getStatus());
        assertNull(ticket.getAssignedTo());
    }

    @Test
    void invalidAssignmentTargetsRejected() throws Exception {
        var session = login(createUser("ADMIN"));
        Ticket ticket = ticket(owner);
        User inactive = createUser("TECHNICIAN");
        inactive.setActive(false);
        users.saveAndFlush(inactive);
        assign(ticket, inactive, session).andExpect(status().isBadRequest());
        assign(ticket, other, session).andExpect(status().isBadRequest());
        assign(ticket, createUser("ADMIN"), session).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/tickets/" + ticket.getTicketId() + "/assign").session(session).with(csrf())
                .contentType("application/json").content("{\"technicianId\":9223372036854775807}"))
                .andExpect(status().isBadRequest());
        assertEquals("OPEN", ticket.getStatus());
        assertNull(ticket.getAssignedTo());
    }

    @Test
    void legacyTechnicianRoleIsValidTarget() throws Exception {
        User technician = createUser("TECH");
        assign(ticket(owner), technician, login(technician)).andExpect(status().isOk());
    }

    @Test
    void assignedTicketCanStartProgressForBothSupportRoles() throws Exception {
        for (String role : new String[]{"TECHNICIAN", "ADMIN"}) {
            User technician = createUser("TECHNICIAN");
            var session = login(role.equals("TECHNICIAN") ? technician : createUser("ADMIN"));
            Ticket ticket = ticket(owner);
            assign(ticket, technician, session).andExpect(status().isOk());
            var before = ticket.getUpdatedAt();
            var response = changeStatus(ticket, "IN_PROGRESS", session).andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS")).andReturn();
            assertTrue(ticket.getUpdatedAt().isAfter(before));
            assertSafe(response.getResponse().getContentAsString());
            entityManager.clear();
            assertEquals("IN_PROGRESS", tickets.findById(ticket.getTicketId()).orElseThrow().getStatus());
        }
    }

    @Test
    void generalStatusCannotSkipOrReverseLifecycle() throws Exception {
        var session = login(createUser("ADMIN"));
        Ticket ticket = ticket(owner);
        for (String next : new String[]{"ASSIGNED", "IN_PROGRESS", "RESOLVED", "CLOSED", "UNKNOWN"}) {
            changeStatus(ticket, next, session).andExpect(status().isBadRequest());
        }
        User technician = createUser("TECHNICIAN");
        assign(ticket, technician, session).andExpect(status().isOk());
        for (String next : new String[]{"OPEN", "ASSIGNED", "RESOLVED", "CLOSED"}) {
            changeStatus(ticket, next, session).andExpect(status().isBadRequest());
        }
        changeStatus(ticket, "IN_PROGRESS", session).andExpect(status().isOk());
        for (String next : new String[]{"OPEN", "ASSIGNED", "IN_PROGRESS", "RESOLVED", "CLOSED"}) {
            changeStatus(ticket, next, session).andExpect(status().isBadRequest());
        }
        assertEquals("IN_PROGRESS", ticket.getStatus());
        assertNull(ticket.getResolvedAt());
        assertNull(ticket.getResolutionNotes());
    }

    @Test
    void inconsistentUnassignedTicketCannotStartProgress() throws Exception {
        Ticket ticket = ticket(owner);
        ticket.setStatus("ASSIGNED");
        tickets.saveAndFlush(ticket);
        changeStatus(ticket, "IN_PROGRESS", login(createUser("ADMIN"))).andExpect(status().isBadRequest());
        assertEquals("ASSIGNED", ticket.getStatus());
    }

    @Test
    void resolvedAndClosedTicketsCannotBeModified() throws Exception {
        var session = login(createUser("ADMIN"));
        User technician = createUser("TECHNICIAN");
        for (String terminal : new String[]{"RESOLVED", "CLOSED"}) {
            Ticket ticket = ticket(owner);
            ticket.setStatus(terminal);
            tickets.saveAndFlush(ticket);
            assign(ticket, technician, session).andExpect(status().isBadRequest());
            for (String next : new String[]{"OPEN", "ASSIGNED", "IN_PROGRESS", "RESOLVED", "CLOSED"}) {
                changeStatus(ticket, next, session).andExpect(status().isBadRequest());
            }
            assertEquals(terminal, ticket.getStatus());
            assertNull(ticket.getAssignedTo());
        }
    }

    @Test
    void technicianCannotCloseAndCannotBypassResolutionNotes() throws Exception {
        User technician = createUser("TECHNICIAN");
        var session = login(technician);
        Ticket ticket = ticket(owner);
        assign(ticket, technician, session).andExpect(status().isOk());
        changeStatus(ticket, "IN_PROGRESS", session).andExpect(status().isOk());
        changeStatus(ticket, "RESOLVED", session).andExpect(status().isBadRequest());
        changeStatus(ticket, "CLOSED", session).andExpect(status().isForbidden());
        assertEquals("IN_PROGRESS", ticket.getStatus());
    }

    @Test
    void reassignmentPreservesAssignedAndInProgressStatus() throws Exception {
        var session = login(createUser("ADMIN"));
        User first = createUser("TECHNICIAN");
        User second = createUser("TECHNICIAN");
        Ticket ticket = ticket(owner);
        assign(ticket, first, session).andExpect(status().isOk());
        assign(ticket, second, session).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
        changeStatus(ticket, "IN_PROGRESS", session).andExpect(status().isOk());
        assign(ticket, first, session).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void patchRequiresAuthenticationAndCsrf() throws Exception {
        var session = login(createUser("ADMIN"));
        for (String endpoint : new String[]{"assign", "status"}) {
            String body = endpoint.equals("assign") ? "{\"technicianId\":1}" : "{\"status\":\"IN_PROGRESS\"}";
            String url = "/api/tickets/1/" + endpoint;
            mvc.perform(patch(url).with(csrf()).contentType("application/json").content(body))
                    .andExpect(status().isUnauthorized());
            mvc.perform(patch(url).session(session).contentType("application/json").content(body))
                    .andExpect(status().isForbidden());
            mvc.perform(patch(url).session(session).with(csrf().useInvalidToken())
                    .contentType("application/json").content(body)).andExpect(status().isForbidden());
        }
    }

    @Test
    void missingTicketsAndInvalidPatchBodiesRejected() throws Exception {
        var session = login(createUser("ADMIN"));
        for (String endpoint : new String[]{"assign", "status"}) {
            for (String body : new String[]{"{}", "{", endpoint.equals("assign")
                    ? "{\"technicianId\":-1}" : "{\"status\":\" \"}"}) {
                mvc.perform(patch("/api/tickets/1/" + endpoint).session(session).with(csrf())
                        .contentType("application/json").content(body)).andExpect(status().isBadRequest());
            }
            String body = endpoint.equals("assign") ? "{\"technicianId\":1}" : "{\"status\":\"IN_PROGRESS\"}";
            mvc.perform(patch("/api/tickets/9223372036854775807/" + endpoint).session(session).with(csrf())
                    .contentType("application/json").content(body)).andExpect(status().isNotFound());
        }
    }

    @Test
    void directServiceCallsEnforceRoleAndSelfAssignment() throws Exception {
        Ticket ticket = ticket(owner);
        User technician = createUser("TECHNICIAN");
        User another = createUser("TECHNICIAN");
        try {
            var session = login(owner);
            var context = (org.springframework.security.core.context.SecurityContext)
                    session.getAttribute("SPRING_SECURITY_CONTEXT");
            org.springframework.security.core.context.SecurityContextHolder.setContext(context);
            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> ticketService.updateTicket(ticket.getTicketId(), ticket));
            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> ticketService.saveTicket(ticket));
            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> ticketService.deleteTicket(ticket.getTicketId()));
            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> ticketService.assignForCurrentUser(ticket.getTicketId(), technician.getUserId()));
            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> ticketService.updateStatusForCurrentUser(ticket.getTicketId(), "IN_PROGRESS"));
            session = login(technician);
            context = (org.springframework.security.core.context.SecurityContext)
                    session.getAttribute("SPRING_SECURITY_CONTEXT");
            org.springframework.security.core.context.SecurityContextHolder.setContext(context);
            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> ticketService.assignForCurrentUser(ticket.getTicketId(), another.getUserId()));
            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> ticketService.updateStatusForCurrentUser(ticket.getTicketId(), "CLOSED"));
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
        assertNull(ticket.getAssignedTo());
        assertEquals("OPEN", ticket.getStatus());
    }

    @Test
    void patchCorsAllowsOnlyConfiguredReactOrigin() throws Exception {
        mvc.perform(options("/api/tickets/1/assign").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "PATCH")
                .header("Access-Control-Request-Headers", "content-type,x-csrf-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options("/api/tickets/1/assign").header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "PATCH")).andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.ResultActions assign(
            Ticket ticket, User technician, MockHttpSession session) throws Exception {
        return mvc.perform(patch("/api/tickets/" + ticket.getTicketId() + "/assign").session(session).with(csrf())
                .contentType("application/json").content(mapper.writeValueAsString(
                        Map.of("technicianId", technician.getUserId()))));
    }

    private org.springframework.test.web.servlet.ResultActions changeStatus(
            Ticket ticket, String next, MockHttpSession session) throws Exception {
        return mvc.perform(patch("/api/tickets/" + ticket.getTicketId() + "/status").session(session).with(csrf())
                .contentType("application/json").content(mapper.writeValueAsString(Map.of("status", next))));
    }


    @Autowired com.itsupportoperationsdashboard.capstoneProject.service.CommentService commentService;
    @Autowired CommentRepository comments;

    @Test
    void ownerCreatesAndReadsCommentWithSessionAuthorDespiteSpoofing() throws Exception {
        Ticket ticket = ticket(owner);
        var session = login(owner);
        var result = mvc.perform(post(commentUrl(ticket)).session(session).with(csrf())
                .contentType("application/json").content(mapper.writeValueAsString(Map.of(
                        "text", "Please help", "userId", other.getUserId(), "authorId", other.getUserId(),
                        "user", Map.of("userId", other.getUserId())))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Please help"))
                .andExpect(jsonPath("$.authorId").value(owner.getUserId()))
                .andExpect(jsonPath("$.authorName").value(owner.getName()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty()).andReturn();
        assertSafe(result.getResponse().getContentAsString());
        long id = mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        entityManager.flush();
        entityManager.clear();
        assertEquals(owner.getUserId(), comments.findById(id).orElseThrow().getUser().getUserId());
        var list = mvc.perform(get(commentUrl(ticket)).session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id)).andReturn();
        assertSafe(list.getResponse().getContentAsString());
    }

    @Test
    void ownerCannotReadOrWriteAnotherTicketsComments() throws Exception {
        Ticket ticket = ticket(other);
        var session = login(owner);
        mvc.perform(get(commentUrl(ticket)).session(session)).andExpect(status().isForbidden());
        addComment(ticket, session, "Not mine").andExpect(status().isForbidden());
        assertTrue(comments.findByTicket_TicketId(ticket.getTicketId()).isEmpty());
    }

    @Test
    void technicianCanReadAndWriteSupportComments() throws Exception {
        verifySupportComments("TECHNICIAN");
    }

    @Test
    void adminCanReadAndWriteComments() throws Exception {
        verifySupportComments("ADMIN");
    }

    @Test
    void invalidCommentTextRejected() throws Exception {
        Ticket ticket = ticket(owner);
        var session = login(owner);
        for (String body : new String[]{"{}", "{", "{\"text\":null}", "{\"text\":\"  \\n\\t\"}",
                mapper.writeValueAsString(Map.of("text", "x".repeat(10001)))}) {
            mvc.perform(post(commentUrl(ticket)).session(session).with(csrf())
                    .contentType("application/json").content(body)).andExpect(status().isBadRequest());
        }
        assertTrue(comments.findByTicket_TicketId(ticket.getTicketId()).isEmpty());
    }

    @Test
    void missingTicketCommentRequestsReturn404() throws Exception {
        var session = login(owner);
        String url = "/api/tickets/9223372036854775807/comments";
        mvc.perform(get(url).session(session)).andExpect(status().isNotFound());
        mvc.perform(post(url).session(session).with(csrf()).contentType("application/json")
                .content("{\"text\":\"Hello\"}")).andExpect(status().isNotFound());
    }

    @Test
    void commentsSortByTimestampThenIdAndExcludeOtherTickets() throws Exception {
        Ticket ticket = ticket(owner);
        Comment newer = seedComment(ticket, "newer");
        Comment older = seedComment(ticket, "older");
        Comment sameTime = seedComment(ticket, "same-time");
        var early = java.time.LocalDateTime.of(2026, 1, 1, 12, 0);
        newer.setCreatedAt(early.plusDays(1));
        older.setCreatedAt(early);
        sameTime.setCreatedAt(early);
        seedComment(ticket(other), "not included");
        comments.flush();
        entityManager.clear();
        mvc.perform(get(commentUrl(ticket)).session(login(owner))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(older.getCommentId()))
                .andExpect(jsonPath("$[1].id").value(sameTime.getCommentId()))
                .andExpect(jsonPath("$[2].id").value(newer.getCommentId()));
    }

    @Test
    void closedTicketsRejectNewCommentsButKeepHistoryForEveryRole() throws Exception {
        Ticket ticket = ticket(owner);
        seedComment(ticket, "Before closure");
        ticket.setStatus("CLOSED");
        tickets.saveAndFlush(ticket);
        for (User actor : new User[]{owner, createUser("TECHNICIAN"), createUser("ADMIN")}) {
            var session = login(actor);
            addComment(ticket, session, "Too late").andExpect(status().isBadRequest());
            mvc.perform(get(commentUrl(ticket)).session(session)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].text").value("Before closure"));
        }
        assertEquals("CLOSED", ticket.getStatus());
    }

    @Test
    void commentsRequireAuthenticationAndPostCsrf() throws Exception {
        Ticket ticket = ticket(owner);
        mvc.perform(get(commentUrl(ticket))).andExpect(status().isUnauthorized());
        mvc.perform(post(commentUrl(ticket)).with(csrf()).contentType("application/json")
                .content("{\"text\":\"Hello\"}")).andExpect(status().isUnauthorized());
        var session = login(owner);
        mvc.perform(post(commentUrl(ticket)).session(session).contentType("application/json")
                .content("{\"text\":\"Hello\"}")).andExpect(status().isForbidden());
        mvc.perform(post(commentUrl(ticket)).session(session).with(csrf().useInvalidToken())
                .contentType("application/json").content("{\"text\":\"Hello\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void emptyCommentListReturnsEmptyArray() throws Exception {
        mvc.perform(get(commentUrl(ticket(owner))).session(login(owner)))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void directCommentServiceCallsCannotBypassOwnershipOrValidation() throws Exception {
        Ticket own = ticket(owner);
        Ticket someoneElses = ticket(other);
        var session = login(owner);
        try {
            org.springframework.security.core.context.SecurityContextHolder.setContext(
                    (org.springframework.security.core.context.SecurityContext)
                            session.getAttribute("SPRING_SECURITY_CONTEXT"));
            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> commentService.listForCurrentUser(someoneElses.getTicketId()));
            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> commentService.createForCurrentUser(someoneElses.getTicketId(), "No"));
            assertThrows(IllegalArgumentException.class,
                    () -> commentService.createForCurrentUser(own.getTicketId(), " "));
            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> commentService.saveComment(new Comment()));
            own.setStatus("CLOSED");
            tickets.saveAndFlush(own);
            assertThrows(IllegalArgumentException.class,
                    () -> commentService.createForCurrentUser(own.getTicketId(), "No"));
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    void commentsDoNotChangeTicketLifecycleAndResolvedTicketsRemainCommentable() throws Exception {
        Ticket ticket = ticket(owner);
        ticket.setStatus("RESOLVED");
        tickets.saveAndFlush(ticket);
        var before = ticket.getUpdatedAt();
        addComment(ticket, login(owner), "Thank you").andExpect(status().isCreated());
        entityManager.flush();
        assertEquals("RESOLVED", ticket.getStatus());
        assertEquals(before, ticket.getUpdatedAt());
    }

    private void verifySupportComments(String role) throws Exception {
        Ticket ticket = ticket(owner);
        User actor = createUser(role);
        var session = login(actor);
        var result = addComment(ticket, session, "Support reply").andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorId").value(actor.getUserId())).andReturn();
        assertSafe(result.getResponse().getContentAsString());
        var list = mvc.perform(get(commentUrl(ticket)).session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].text").value("Support reply")).andReturn();
        assertSafe(list.getResponse().getContentAsString());
    }

    private Comment seedComment(Ticket ticket, String text) {
        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setUser(owner);
        comment.setCommentText(text);
        return comments.saveAndFlush(comment);
    }

    private String commentUrl(Ticket ticket) {
        return "/api/tickets/" + ticket.getTicketId() + "/comments";
    }

    private org.springframework.test.web.servlet.ResultActions addComment(
            Ticket ticket, MockHttpSession session, String text) throws Exception {
        return mvc.perform(post(commentUrl(ticket)).session(session).with(csrf())
                .contentType("application/json").content(mapper.writeValueAsString(Map.of("text", text))));
    }


    @Test
    void technicianResolvesWithNotesAndTimestamps() throws Exception {
        verifyResolution("TECHNICIAN");
    }

    @Test
    void administratorResolvesWithNotesAndTimestamps() throws Exception {
        verifyResolution("ADMIN");
    }

    @Test
    void invalidResolutionNotesRejected() throws Exception {
        var session = login(createUser("ADMIN"));
        Ticket ticket = progressTicket();
        for (String body : new String[]{"{}", "{", "{\"resolutionNotes\":null}",
                "{\"resolutionNotes\":\"  \\t\\n\"}",
                mapper.writeValueAsString(Map.of("resolutionNotes", "x".repeat(10001)))}) {
            mvc.perform(patch("/api/tickets/" + ticket.getTicketId() + "/resolve")
                    .session(session).with(csrf()).contentType("application/json").content(body))
                    .andExpect(status().isBadRequest());
        }
        assertEquals("IN_PROGRESS", ticket.getStatus());
        assertNull(ticket.getResolvedAt());
        assertNull(ticket.getResolutionNotes());
    }

    @Test
    void resolveRejectsEveryOtherStatus() throws Exception {
        var session = login(createUser("ADMIN"));
        for (String initial : new String[]{"OPEN", "ASSIGNED", "RESOLVED", "CLOSED"}) {
            Ticket ticket = ticket(owner);
            ticket.setStatus(initial);
            tickets.saveAndFlush(ticket);
            resolve(ticket, session, "Fixed").andExpect(status().isBadRequest());
            assertEquals(initial, ticket.getStatus());
            assertNull(ticket.getResolvedAt());
        }
    }

    @Test
    void adminClosurePreservesResolutionAndRejectsRepeatedOperations() throws Exception {
        var session = login(createUser("ADMIN"));
        Ticket ticket = progressTicket();
        resolve(ticket, session, "Restored network settings").andExpect(status().isOk());
        entityManager.clear();
        Ticket persisted = tickets.findById(ticket.getTicketId()).orElseThrow();
        var resolvedAt = persisted.getResolvedAt();
        var updatedAt = persisted.getUpdatedAt();
        resolve(persisted, session, "Overwrite attempt").andExpect(status().isBadRequest());
        var result = close(persisted, session).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.resolutionNotes").value("Restored network settings")).andReturn();
        assertSafe(result.getResponse().getContentAsString());
        assertEquals(resolvedAt, persisted.getResolvedAt());
        assertTrue(persisted.getUpdatedAt().isAfter(updatedAt));
        close(persisted, session).andExpect(status().isBadRequest());
        resolve(persisted, session, "No").andExpect(status().isBadRequest());
        for (String next : new String[]{"OPEN", "ASSIGNED", "IN_PROGRESS", "RESOLVED"}) {
            changeStatus(persisted, next, session).andExpect(status().isBadRequest());
        }
        entityManager.clear();
        Ticket saved = tickets.findById(ticket.getTicketId()).orElseThrow();
        assertEquals("CLOSED", saved.getStatus());
        assertEquals(resolvedAt, saved.getResolvedAt());
        assertEquals("Restored network settings", saved.getResolutionNotes());
    }

    @Test
    void closureRequiresResolvedStateAndCompleteResolutionEvidence() throws Exception {
        var session = login(createUser("ADMIN"));
        for (String initial : new String[]{"OPEN", "ASSIGNED", "IN_PROGRESS", "CLOSED"}) {
            Ticket ticket = ticket(owner);
            ticket.setStatus(initial);
            ticket.setResolutionNotes("Notes alone cannot skip states");
            ticket.setResolvedAt(java.time.LocalDateTime.now());
            tickets.saveAndFlush(ticket);
            close(ticket, session).andExpect(status().isBadRequest());
            assertEquals(initial, ticket.getStatus());
        }
        for (String notes : new String[]{null, "", "  ", "Valid notes"}) {
            Ticket ticket = ticket(owner);
            ticket.setStatus("RESOLVED");
            ticket.setResolutionNotes(notes);
            ticket.setResolvedAt("Valid notes".equals(notes) ? null : java.time.LocalDateTime.now());
            tickets.saveAndFlush(ticket);
            close(ticket, session).andExpect(status().isBadRequest());
            assertEquals("RESOLVED", ticket.getStatus());
        }
    }

    @Test
    void resolutionAndClosureEnforceRolesAndDirectServicePermissions() throws Exception {
        Ticket ticket = progressTicket();
        for (User actor : new User[]{owner, createUser("TECHNICIAN")}) {
            var session = login(actor);
            close(ticket, session).andExpect(status().isForbidden());
            if ("END_USER".equals(actor.getRole())) {
                resolve(ticket, session, "Fixed").andExpect(status().isForbidden());
            }
            try {
                org.springframework.security.core.context.SecurityContextHolder.setContext(
                        (org.springframework.security.core.context.SecurityContext)
                                session.getAttribute("SPRING_SECURITY_CONTEXT"));
                assertThrows(org.springframework.security.access.AccessDeniedException.class,
                        () -> ticketService.closeForCurrentUser(ticket.getTicketId()));
                if ("END_USER".equals(actor.getRole())) {
                    assertThrows(org.springframework.security.access.AccessDeniedException.class,
                            () -> ticketService.resolveForCurrentUser(ticket.getTicketId(), "Fixed"));
                } else {
                    assertThrows(IllegalArgumentException.class,
                            () -> ticketService.resolveForCurrentUser(ticket.getTicketId(), " "));
                }
                assertThrows(org.springframework.security.access.AccessDeniedException.class,
                        () -> ticketService.updateTicket(ticket.getTicketId(), ticket));
            } finally {
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
            }
        }
    }

    @Test
    void resolveAndCloseRequireAuthenticationCsrfAndExistingTicket() throws Exception {
        var session = login(createUser("ADMIN"));
        for (String action : new String[]{"resolve", "close"}) {
            String url = "/api/tickets/9223372036854775807/" + action;
            String body = action.equals("resolve") ? "{\"resolutionNotes\":\"Fixed\"}" : "{}";
            mvc.perform(patch(url).with(csrf()).contentType("application/json").content(body))
                    .andExpect(status().isUnauthorized());
            mvc.perform(patch(url).session(session).contentType("application/json").content(body))
                    .andExpect(status().isForbidden());
            mvc.perform(patch(url).session(session).with(csrf().useInvalidToken())
                    .contentType("application/json").content(body)).andExpect(status().isForbidden());
            mvc.perform(patch(url).session(session).with(csrf()).contentType("application/json").content(body))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void fullLifecycleKeepsCommentsReadableAndPreventsBypasses() throws Exception {
        User technician = createUser("TECHNICIAN");
        var techSession = login(technician);
        var adminSession = login(createUser("ADMIN"));
        var ownerSession = login(owner);
        var created = mvc.perform(post("/api/tickets").session(ownerSession).with(csrf())
                .contentType("application/json").content(body(category.getCategoryId())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("OPEN")).andReturn();
        long id = mapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        Ticket ticket = tickets.findById(id).orElseThrow();
        addComment(ticket, ownerSession, "Original comment").andExpect(status().isCreated());
        assign(ticket, technician, techSession).andExpect(status().isOk());
        changeStatus(ticket, "IN_PROGRESS", techSession).andExpect(status().isOk());
        changeStatus(ticket, "RESOLVED", adminSession).andExpect(status().isBadRequest());
        resolve(ticket, techSession, "Replaced faulty cable").andExpect(status().isOk());
        assign(ticket, technician, adminSession).andExpect(status().isBadRequest());
        changeStatus(ticket, "CLOSED", adminSession).andExpect(status().isBadRequest());
        close(ticket, adminSession).andExpect(status().isOk());
        assign(ticket, technician, adminSession).andExpect(status().isBadRequest());
        addComment(ticket, ownerSession, "Too late").andExpect(status().isBadRequest());
        mvc.perform(get(commentUrl(ticket)).session(ownerSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("Original comment"));
        mvc.perform(get("/api/tickets/" + id).session(ownerSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    private Ticket progressTicket() {
        Ticket ticket = ticket(owner);
        ticket.setAssignedTo(createUser("TECHNICIAN"));
        ticket.setStatus("IN_PROGRESS");
        return tickets.saveAndFlush(ticket);
    }

    private void verifyResolution(String role) throws Exception {
        Ticket ticket = progressTicket();
        var before = ticket.getUpdatedAt();
        var earliest = java.time.LocalDateTime.now();
        var result = resolve(ticket, login(createUser(role)), "Fixed the network cable")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolutionNotes").value("Fixed the network cable"))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty()).andReturn();
        assertSafe(result.getResponse().getContentAsString());
        assertFalse(ticket.getResolvedAt().isBefore(earliest));
        assertFalse(ticket.getResolvedAt().isAfter(java.time.LocalDateTime.now()));
        assertTrue(ticket.getUpdatedAt().isAfter(before));
        var response = mapper.readTree(result.getResponse().getContentAsString());
        assertEquals(ticket.getUpdatedAt(), java.time.LocalDateTime.parse(response.get("updatedAt").asString()));
        entityManager.clear();
        Ticket saved = tickets.findById(ticket.getTicketId()).orElseThrow();
        assertEquals("RESOLVED", saved.getStatus());
        assertEquals("Fixed the network cable", saved.getResolutionNotes());
        assertNotNull(saved.getResolvedAt());
    }

    private org.springframework.test.web.servlet.ResultActions resolve(
            Ticket ticket, MockHttpSession session, String notes) throws Exception {
        return mvc.perform(patch("/api/tickets/" + ticket.getTicketId() + "/resolve").session(session).with(csrf())
                .contentType("application/json").content(mapper.writeValueAsString(Map.of("resolutionNotes", notes))));
    }

    private org.springframework.test.web.servlet.ResultActions close(
            Ticket ticket, MockHttpSession session) throws Exception {
        return mvc.perform(patch("/api/tickets/" + ticket.getTicketId() + "/close").session(session).with(csrf()));
    }

    private String body(Long categoryId) {
        return mapper.writeValueAsString(Map.of("title", "Printer problem", "description", "Cannot print",
                "priority", "MEDIUM", "categoryId", categoryId));
    }

    private void assertSafe(String json) {
        assertFalse(json.contains("password"));
        assertFalse(json.contains("$2"));
        assertFalse(json.contains("email"));
    }
}
