package com.itsupportoperationsdashboard.capstoneProject.category;

import com.itsupportoperationsdashboard.capstoneProject.model.Category;

public final class CategoryDtos {
    private CategoryDtos() {}

    public record Response(Long id, String name) {}

    public static Response response(Category category) {
        return new Response(category.getCategoryId(), category.getCategoryName());
    }
}