package com.coffeeshop.category.controller;

import com.coffeeshop.category.dto.CategoryCreateRequest;
import com.coffeeshop.category.dto.CategoryResponse;
import com.coffeeshop.category.dto.CategoryUpdateRequest;
import com.coffeeshop.category.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Returns active categories for product forms and operational screens.
     */
    @GetMapping
    public List<CategoryResponse> getActiveCategories() {
        return categoryService.getActiveCategories();
    }

    /**
     * Returns all categories for management, optionally filtered by name.
     */
    @GetMapping("/management")
    public List<CategoryResponse> getCategoriesForManagement(
            @RequestParam(required = false) String search) {
        return categoryService.getCategories(search);
    }

    @GetMapping("/{id}")
    public CategoryResponse getCategoryById(@PathVariable Long id) {
        return categoryService.getCategoryById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(
            @Valid @RequestBody CategoryCreateRequest request) {
        return categoryService.createCategory(request);
    }

    @PutMapping("/{id}")
    public CategoryResponse updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryUpdateRequest request) {
        return categoryService.updateCategory(id, request);
    }

    @PatchMapping("/{id}/activate")
    public CategoryResponse activateCategory(@PathVariable Long id) {
        return categoryService.activateCategory(id);
    }

    @PatchMapping("/{id}/deactivate")
    public CategoryResponse deactivateCategory(@PathVariable Long id) {
        return categoryService.deactivateCategory(id);
    }
}