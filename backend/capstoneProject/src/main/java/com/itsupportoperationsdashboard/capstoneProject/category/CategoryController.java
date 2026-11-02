package com.itsupportoperationsdashboard.capstoneProject.category;

import com.itsupportoperationsdashboard.capstoneProject.service.CategoryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categories;

    public CategoryController(CategoryService categories) {
        this.categories = categories;
    }

    @GetMapping
    public List<CategoryDtos.Response> list() {
        return categories.getActiveCategories().stream().map(CategoryDtos::response).toList();
    }
}