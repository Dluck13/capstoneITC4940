package com.itsupportoperationsdashboard.capstoneProject.service;

import com.itsupportoperationsdashboard.capstoneProject.model.User;
import com.itsupportoperationsdashboard.capstoneProject.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
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
        user.setPasswordHash(hashPlaintextPassword(user.getPasswordHash()));
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
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
}
