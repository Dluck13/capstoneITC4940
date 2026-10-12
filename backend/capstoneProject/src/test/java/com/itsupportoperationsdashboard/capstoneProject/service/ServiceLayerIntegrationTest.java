package com.itsupportoperationsdashboard.capstoneProject.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itsupportoperationsdashboard.capstoneProject.model.Category;
import com.itsupportoperationsdashboard.capstoneProject.model.Comment;
import com.itsupportoperationsdashboard.capstoneProject.model.Ticket;
import com.itsupportoperationsdashboard.capstoneProject.model.User;
import com.itsupportoperationsdashboard.capstoneProject.repository.CategoryRepository;
import com.itsupportoperationsdashboard.capstoneProject.repository.CommentRepository;
import com.itsupportoperationsdashboard.capstoneProject.repository.TicketRepository;
import com.itsupportoperationsdashboard.capstoneProject.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
class ServiceLayerIntegrationTest {

    private static final Long MISSING_ID = -987654321L;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CommentRepository commentRepository;

    private final List<Long> createdCommentIds = new ArrayList<>();
    private final List<Long> createdTicketIds = new ArrayList<>();
    private final List<Long> createdCategoryIds = new ArrayList<>();
    private final List<Long> createdUserIds = new ArrayList<>();

    @AfterEach
    void cleanUpCreatedRecords() {
        createdCommentIds.forEach(commentRepository::deleteById);
        createdTicketIds.forEach(ticketId -> {
            if (ticketRepository.existsById(ticketId)) {
                ticketService.deleteTicket(ticketId);
            }
        });
        createdCategoryIds.forEach(categoryRepository::deleteById);
        createdUserIds.forEach(userRepository::deleteById);
    }

    @Test
    void integrationTestsUseConfiguredMySqlDatabase() throws SQLException {
        String url;
        try (Connection connection = dataSource.getConnection()) {
            url = connection.getMetaData().getURL();
        }

        assertTrue(url.startsWith("jdbc:mysql://"));
    }

    @Test
    void createAndRetrieveUserStoresBCryptPasswordHash() {
        String plaintextPassword = "IntegrationPassword123!";
        User savedUser = createUser(plaintextPassword);

        User retrievedUser = userService.getUserById(savedUser.getUserId());

        assertEquals(savedUser.getUserId(), retrievedUser.getUserId());
        assertEquals(savedUser.getEmail(), retrievedUser.getEmail());
        assertNotEquals(plaintextPassword, retrievedUser.getPasswordHash());
        assertTrue(passwordEncoder.matches(plaintextPassword, retrievedUser.getPasswordHash()));
    }

    @Test
    void createAndRetrieveCategory() {
        Category savedCategory = createCategory();

        Category retrievedCategory = categoryService.getCategoryById(savedCategory.getCategoryId());

        assertEquals(savedCategory.getCategoryId(), retrievedCategory.getCategoryId());
        assertEquals(savedCategory.getCategoryName(), retrievedCategory.getCategoryName());
        assertEquals(savedCategory.getDescription(), retrievedCategory.getDescription());
    }

    @Test
    void createTicketReferencingExistingUserAndCategoryThenUpdateTicket() {
        User user = createUser("TicketPassword123!");
        Category category = createCategory();
        Ticket ticket = createTicket(user, category);

        Ticket retrievedTicket = ticketService.getTicketById(ticket.getTicketId());
        assertEquals(ticket.getTicketId(), retrievedTicket.getTicketId());
        assertEquals("OPEN", retrievedTicket.getStatus());
        assertEquals("MEDIUM", retrievedTicket.getPriority());

        Ticket ticketDetails = new Ticket();
        ticketDetails.setTitle("Updated integration ticket");
        ticketDetails.setDescription("Updated integration ticket description.");
        ticketDetails.setStatus("IN_PROGRESS");
        ticketDetails.setPriority("HIGH");
        ticketDetails.setRequester(user);
        ticketDetails.setAssignedTo(user);
        ticketDetails.setCategory(category);
        ticketDetails.setResolutionNotes("Still investigating.");

        Ticket updatedTicket = ticketService.updateTicket(ticket.getTicketId(), ticketDetails);

        assertEquals(ticket.getTicketId(), updatedTicket.getTicketId());
        assertEquals("Updated integration ticket", updatedTicket.getTitle());
        assertEquals("IN_PROGRESS", updatedTicket.getStatus());
        assertEquals("HIGH", updatedTicket.getPriority());
        assertEquals("Still investigating.", updatedTicket.getResolutionNotes());
    }

    @Test
    void createRetrieveAndDeleteCommentAssociatedWithExistingTicketAndUser() {
        User user = createUser("CommentPassword123!");
        Category category = createCategory();
        Ticket ticket = createTicket(user, category);

        Comment savedComment = createComment(ticket, user);
        Comment retrievedComment = commentService.getCommentById(savedComment.getCommentId());

        assertEquals(savedComment.getCommentId(), retrievedComment.getCommentId());
        assertEquals("Integration test comment.", retrievedComment.getCommentText());
        assertEquals(1, commentService.getCommentsByTicketId(ticket.getTicketId()).size());

        commentService.deleteComment(savedComment.getCommentId());
        createdCommentIds.remove(savedComment.getCommentId());

        assertThrows(EntityNotFoundException.class,
                () -> commentService.getCommentById(savedComment.getCommentId()));
    }

    @Test
    void invalidRelationshipReferencesAreRejected() {
        Ticket invalidTicket = new Ticket();
        invalidTicket.setTitle("Invalid relationship ticket");
        invalidTicket.setDescription("This ticket should not be saved.");
        invalidTicket.setStatus("OPEN");
        invalidTicket.setPriority("LOW");
        User missingUser = new User();
        missingUser.setUserId(MISSING_ID);
        invalidTicket.setRequester(missingUser);

        assertThrows(EntityNotFoundException.class, () -> ticketService.saveTicket(invalidTicket));

        Comment invalidComment = new Comment();
        invalidComment.setCommentText("This comment should not be saved.");
        Ticket missingTicket = new Ticket();
        missingTicket.setTicketId(MISSING_ID);
        invalidComment.setTicket(missingTicket);
        invalidComment.setUser(missingUser);

        assertThrows(EntityNotFoundException.class, () -> commentService.saveComment(invalidComment));
    }

    private User createUser(String plaintextPassword) {
        String uniqueValue = UUID.randomUUID().toString();
        User user = new User();
        user.setName("Integration User " + uniqueValue);
        user.setEmail("integration-" + uniqueValue + "@example.com");
        user.setPasswordHash(plaintextPassword);
        user.setRole("USER");
        user.setActive(true);

        User savedUser = userService.saveUser(user);
        createdUserIds.add(savedUser.getUserId());
        return savedUser;
    }

    private Category createCategory() {
        String uniqueValue = UUID.randomUUID().toString();
        Category category = new Category();
        category.setCategoryName("Integration Category " + uniqueValue);
        category.setDescription("Integration test category.");
        category.setActive(true);

        Category savedCategory = categoryService.saveCategory(category);
        createdCategoryIds.add(savedCategory.getCategoryId());
        return savedCategory;
    }

    private Ticket createTicket(User user, Category category) {
        Ticket ticket = new Ticket();
        ticket.setTitle("Integration ticket " + UUID.randomUUID());
        ticket.setDescription("Integration ticket description.");
        ticket.setStatus("OPEN");
        ticket.setPriority("MEDIUM");
        ticket.setRequester(user);
        ticket.setCategory(category);

        Ticket savedTicket = ticketService.saveTicket(ticket);
        createdTicketIds.add(savedTicket.getTicketId());
        return savedTicket;
    }

    private Comment createComment(Ticket ticket, User user) {
        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setUser(user);
        comment.setCommentText("Integration test comment.");

        Comment savedComment = commentService.saveComment(comment);
        createdCommentIds.add(savedComment.getCommentId());
        return savedComment;
    }
}
