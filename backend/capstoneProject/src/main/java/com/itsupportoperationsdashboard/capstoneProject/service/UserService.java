package com.itsupportoperationsdashboard.capstoneProject.service;

import com.itsupportoperationsdashboard.capstoneProject.model.User;
import com.itsupportoperationsdashboard.capstoneProject.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User saveUser(User user) {
        user.setUserId(null);
        user.setEmail(normalizeEmail(user.getEmail()));
        user.setPasswordHash(hashPlaintextPassword(user.getPasswordHash()));
        return userRepository.save(user);
    }

    @Transactional
    public User registerEndUser(String name, String email, String plaintextPassword) {
        return createManagedUser(name, email, plaintextPassword, "END_USER", true);
    }

    @Transactional
    public User createSupportAccount(String name, String email, String plaintextPassword, String role) {
        String normalizedRole = normalizeAdminCreatableRole(role);
        return createManagedUser(name, email, plaintextPassword, normalizedRole, true);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getActiveTechnicians() {
        return userRepository.findByActiveTrueAndRoleInOrderByNameAsc(List.of("TECHNICIAN", "TECH"));
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));
    }

    @Transactional
    public User updateUser(Long userId, User userDetails) {
        User existingUser = getUserById(userId);

        existingUser.setName(userDetails.getName());
        existingUser.setEmail(userDetails.getEmail());
        existingUser.setRole(userDetails.getRole());
        existingUser.setActive(userDetails.isActive());

        return userRepository.save(existingUser);
    }

    @Transactional
    public User updateUserPassword(Long userId, String plaintextPassword) {
        User existingUser = getUserById(userId);
        existingUser.setPasswordHash(hashPlaintextPassword(plaintextPassword));
        return userRepository.save(existingUser);
    }

    private String hashPlaintextPassword(String plaintextPassword) {
        if (plaintextPassword == null || plaintextPassword.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (plaintextPassword.startsWith("$2a$")
                || plaintextPassword.startsWith("$2b$")
                || plaintextPassword.startsWith("$2y$")) {
            throw new IllegalArgumentException("Password must be provided as plaintext.");
        }
        return passwordEncoder.encode(plaintextPassword);
    }

    private User createManagedUser(String name, String email, String plaintextPassword, String role, boolean active) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateEmailException("Email address is already in use");
        }

        User user = new User();
        user.setName(name.trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(hashPlaintextPassword(plaintextPassword));
        user.setRole(role);
        user.setActive(active);

        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException("Email address is already in use", exception);
        }
    }

    public static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeAdminCreatableRole(String role) {
        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }

        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "TECHNICIAN", "ADMIN" -> normalized;
            default -> throw new IllegalArgumentException("Role must be TECHNICIAN or ADMIN");
        };
    }
}
