
package com.itsupportoperationsdashboard.capstoneProject;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GeneratePasswordHashTest {

    @Test
    void generateHash() {
        BCryptPasswordEncoder encoder =
                new BCryptPasswordEncoder();

        String password = "password123";

        String hashedPassword = encoder.encode(password);

        System.out.println("BCrypt Hash: " + hashedPassword);
        System.out.println("Password matches: " +
                encoder.matches(password, hashedPassword));
    }
}
