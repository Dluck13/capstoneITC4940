package com.itsupportoperationsdashboard.capstoneProject.service;

import com.itsupportoperationsdashboard.capstoneProject.model.Category;
import com.itsupportoperationsdashboard.capstoneProject.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category saveCategory(Category category) {
        category.setCategoryId(null);
        return categoryRepository.save(category);
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + categoryId));
    }

    public Category getCategoryByName(String categoryName) {
        return categoryRepository.findByCategoryName(categoryName)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with name: " + categoryName));
    }

    @Transactional
    public Category updateCategory(Long categoryId, Category categoryDetails) {
        Category existingCategory = getCategoryById(categoryId);

        existingCategory.setCategoryName(categoryDetails.getCategoryName());
        existingCategory.setDescription(categoryDetails.getDescription());
        existingCategory.setActive(categoryDetails.isActive());

        return categoryRepository.save(existingCategory);
    }
}
