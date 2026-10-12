package com.itsupportoperationsdashboard.capstoneProject.repository;

import com.itsupportoperationsdashboard.capstoneProject.model.Category;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByCategoryName(String categoryName);
}
