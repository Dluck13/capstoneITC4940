package com.itsupportoperationsdashboard.capstoneProject.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.itsupportoperationsdashboard.capstoneProject.model.User;
import com.itsupportoperationsdashboard.capstoneProject.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class UserServiceTest {

    private BCryptPasswordEncoder passwordEncoder;
    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void saveUserStoresPlaintextPasswordAsBCryptHash() {
        User user = buildUser(null, "plainPassword123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User savedUser = userService.saveUser(user);

        assertNotEquals("plainPassword123", savedUser.getPasswordHash());
        assertTrue(savedUser.getPasswordHash().startsWith("$2"));
    }

    @Test
    void savedBCryptHashMatchesOriginalPassword() {
        User user = buildUser(null, "plainPassword123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User savedUser = userService.saveUser(user);

        assertTrue(passwordEncoder.matches("plainPassword123", savedUser.getPasswordHash()));
    }

    @Test
    void updateUserWithoutChangingPasswordPreservesExistingHash() {
        String existingHash = passwordEncoder.encode("oldPassword123");
        User existingUser = buildUser(1L, existingHash);
        existingUser.setCreatedAt(LocalDateTime.now());
        User userDetails = buildUser(null, null);
        userDetails.setName("Updated User");
        userDetails.setEmail("updated@example.com");
        userDetails.setRole("TECH");
        userDetails.setActive(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updatedUser = userService.updateUser(1L, userDetails);

        assertEquals(existingHash, updatedUser.getPasswordHash());
        assertEquals("Updated User", updatedUser.getName());
    }

    @Test
    void updateUserPasswordStoresNewValidBCryptHash() {
        String existingHash = passwordEncoder.encode("oldPassword123");
        User existingUser = buildUser(1L, existingHash);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updatedUser = userService.updateUserPassword(1L, "newPassword123");

        assertNotEquals(existingHash, updatedUser.getPasswordHash());
        assertTrue(passwordEncoder.matches("newPassword123", updatedUser.getPasswordHash()));
    }

    private User buildUser(Long userId, String passwordHash) {
        User user = new User();
        user.setUserId(userId);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPasswordHash(passwordHash);
        user.setRole("USER");
        user.setActive(true);
        return user;
    }
}
