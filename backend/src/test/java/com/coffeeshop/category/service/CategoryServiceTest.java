package com.coffeeshop.category.service;

import com.coffeeshop.category.dto.CategoryCreateRequest;
import com.coffeeshop.category.dto.CategoryUpdateRequest;
import com.coffeeshop.category.entity.Category;
import com.coffeeshop.category.repository.CategoryRepository;
import com.coffeeshop.common.exception.DuplicateCategoryException;
import com.coffeeshop.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository repository;

    @InjectMocks
    private CategoryService service;

    private Category active;
    private Category inactive;

    @BeforeEach
    void setUp() {
        active = new Category("Coffee", "Coffee drinks");
        inactive = new Category("Tea", "Tea drinks");
        inactive.setActive(false);
    }

    @Test
    void getActiveCategoriesReturnsActiveOnly() {
        when(repository.findByActiveTrueOrderByNameAsc())
                .thenReturn(List.of(active));

        var result = service.getActiveCategories();

        assertEquals(1, result.size());
        assertEquals("Coffee", result.get(0).name());
        assertTrue(result.get(0).active());
    }

    @Test
    void getCategoriesWithoutSearchReturnsAllCategories() {
        when(repository.findAllByOrderByNameAsc())
                .thenReturn(List.of(active, inactive));

        var result = service.getCategories(null);

        assertEquals(2, result.size());
    }

    @Test
    void getCategoriesWithSearchFiltersByName() {
        when(repository.findByNameContainingIgnoreCaseOrderByNameAsc("cof"))
                .thenReturn(List.of(active));

        var result = service.getCategories(" cof ");

        assertEquals(1, result.size());
        assertEquals("Coffee", result.get(0).name());
    }

    @Test
    void getCategoryByIdReturnsCategory() {
        when(repository.findById(1L)).thenReturn(Optional.of(active));

        assertEquals("Coffee", service.getCategoryById(1L).name());
    }

    @Test
    void getCategoryByIdThrowsWhenMissing() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.getCategoryById(404L)
        );
    }

    @Test
    void createCategoryTrimsNameAndDescription() {
        when(repository.existsByNameIgnoreCase("Coffee")).thenReturn(false);
        when(repository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            return category;
        });

        var result = service.createCategory(
                new CategoryCreateRequest(" Coffee ", " Hot drinks ")
        );

        assertEquals("Coffee", result.name());
        assertEquals("Hot drinks", result.description());
        assertTrue(result.active());
    }

    @Test
    void createCategoryNormalizesBlankDescriptionToNull() {
        when(repository.existsByNameIgnoreCase("Coffee")).thenReturn(false);
        when(repository.save(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.createCategory(
                new CategoryCreateRequest("Coffee", "  ")
        );

        assertNull(result.description());
    }

    @Test
    void createCategoryRejectsDuplicateName() {
        // Arrange: Giả lập repository phát hiện tên danh mục đã tồn tại.
        when(repository.existsByNameIgnoreCase("Coffee"))
                .thenReturn(true);

        CategoryCreateRequest request =
                new CategoryCreateRequest("Coffee", "Coffee drinks");

        // Act & Assert: Service phải ném DuplicateCategoryException.
        assertThrows(
                DuplicateCategoryException.class,
                () -> service.createCategory(request)
        );

        // Verify: Repository được gọi để kiểm tra tên trùng.
        verify(repository).existsByNameIgnoreCase("Coffee");

        // Verify: Không lưu category nếu tên đã tồn tại.
        verify(repository, never()).save(any(Category.class));
    }

    @Test
    void updateCategoryChangesFields() {
        when(repository.findById(1L)).thenReturn(Optional.of(active));
        when(repository.existsByNameIgnoreCaseAndIdNot("Coffee Bar", 1L))
                .thenReturn(false);
        when(repository.save(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.updateCategory(
                1L,
                new CategoryUpdateRequest(" Coffee Bar ", " New description ")
        );

        assertEquals("Coffee Bar", result.name());
        assertEquals("New description", result.description());
    }

    @Test
    void updateCategoryRejectsDuplicateName() {
        when(repository.findById(1L)).thenReturn(Optional.of(active));
        when(repository.existsByNameIgnoreCaseAndIdNot("Tea", 1L))
                .thenReturn(true);

        assertThrows(
                DuplicateCategoryException.class,
                () -> service.updateCategory(
                        1L,
                        new CategoryUpdateRequest("Tea", null)
                )
        );

        verify(repository, never()).save(any(Category.class));
    }

    @Test
    void updateMissingCategoryThrows() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.updateCategory(
                        404L,
                        new CategoryUpdateRequest("Coffee", null)
                )
        );
    }

    @Test
    void activateInactiveCategory() {
        when(repository.findById(2L)).thenReturn(Optional.of(inactive));
        when(repository.save(inactive)).thenReturn(inactive);

        assertTrue(service.activateCategory(2L).active());
        verify(repository).save(inactive);
    }

    @Test
    void activateAlreadyActiveCategoryDoesNotSave() {
        when(repository.findById(1L)).thenReturn(Optional.of(active));

        assertTrue(service.activateCategory(1L).active());
        verify(repository, never()).save(any(Category.class));
    }

    @Test
    void deactivateActiveCategory() {
        when(repository.findById(1L)).thenReturn(Optional.of(active));
        when(repository.save(active)).thenReturn(active);

        assertFalse(service.deactivateCategory(1L).active());
        verify(repository).save(active);
    }

    @Test
    void deactivateAlreadyInactiveCategoryDoesNotSave() {
        when(repository.findById(2L)).thenReturn(Optional.of(inactive));

        assertFalse(service.deactivateCategory(2L).active());
        verify(repository, never()).save(any(Category.class));
    }
}