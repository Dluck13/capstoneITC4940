package com.itsupportoperationsdashboard.capstoneProject.repository;

import com.itsupportoperationsdashboard.capstoneProject.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
}
