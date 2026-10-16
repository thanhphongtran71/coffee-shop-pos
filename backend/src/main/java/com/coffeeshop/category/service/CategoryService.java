package com.coffeeshop.category.service;

import com.coffeeshop.category.dto.CategoryCreateRequest;
import com.coffeeshop.category.dto.CategoryResponse;
import com.coffeeshop.category.dto.CategoryUpdateRequest;
import com.coffeeshop.category.entity.Category;
import com.coffeeshop.category.repository.CategoryRepository;
import com.coffeeshop.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponse> getCategories(String search) {
        List<Category> categories;

        if (search == null || search.isBlank()) {
            categories = categoryRepository.findAllByOrderByNameAsc();
        } else {
            categories = categoryRepository
                    .findByNameContainingIgnoreCaseOrderByNameAsc(search.trim());
        }

        return categories.stream().map(this::toResponse).toList();
    }

    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoryResponse getCategoryById(Long id) {
        return toResponse(findCategoryOrThrow(id));
    }

    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        String name = normalizeName(request.name());
        ensureNameAvailable(name, null);

        Category category = new Category(
                name,
                normalizeDescription(request.description())
        );

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryUpdateRequest request) {
        Category category = findCategoryOrThrow(id);
        String name = normalizeName(request.name());

        ensureNameAvailable(name, id);

        category.setName(name);
        category.setDescription(normalizeDescription(request.description()));

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse activateCategory(Long id) {
        Category category = findCategoryOrThrow(id);

        if (!category.isActive()) {
            category.setActive(true);
            category = categoryRepository.save(category);
        }

        return toResponse(category);
    }

    @Transactional
    public CategoryResponse deactivateCategory(Long id) {
        Category category = findCategoryOrThrow(id);

        if (category.isActive()) {
            category.setActive(false);
            category = categoryRepository.save(category);
        }

        return toResponse(category);
    }

    private Category findCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found with id: " + id
                        ));
    }

    private void ensureNameAvailable(String name, Long currentId) {
        boolean exists = currentId == null
                ? categoryRepository.existsByNameIgnoreCase(name)
                : categoryRepository.existsByNameIgnoreCaseAndIdNot(name, currentId);

        if (exists) {
            throw new com.coffeeshop.common.exception.DuplicateCategoryException(name);
        }
    }

    private String normalizeName(String name) {
        return name.trim();
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank()
                ? null
                : description.trim();
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive()
        );
    }
}