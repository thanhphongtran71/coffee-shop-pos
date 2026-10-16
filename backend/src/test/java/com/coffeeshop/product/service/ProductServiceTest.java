package com.coffeeshop.product.service;

import com.coffeeshop.category.entity.Category;
import com.coffeeshop.category.repository.CategoryRepository;
import com.coffeeshop.common.exception.DuplicateSkuException;
import com.coffeeshop.common.exception.InactiveCategoryException;
import com.coffeeshop.common.exception.ResourceNotFoundException;
import com.coffeeshop.product.dto.ProductCreateRequest;
import com.coffeeshop.product.dto.ProductResponse;
import com.coffeeshop.product.dto.ProductUpdateRequest;
import com.coffeeshop.product.entity.Product;
import com.coffeeshop.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final Long ACTIVE_CATEGORY_ID = 10L;
    private static final Long INACTIVE_CATEGORY_ID = 20L;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private ProductService service;

    private Category activeCategory;
    private Category inactiveCategory;

    private Product active;
    private Product inactive;


    @BeforeEach
    void setup() {
        activeCategory = category(
                ACTIVE_CATEGORY_ID,
                "Coffee",
                true
        );

        inactiveCategory = category(
                INACTIVE_CATEGORY_ID,
                "Seasonal",
                false
        );

        active = product(
                1L,
                "CF-001",
                "Espresso",
                true,
                activeCategory
        );

        inactive = product(
                2L,
                "CF-002",
                "Latte",
                false,
                activeCategory
        );
    }

    // ---------------------------------------------------------
    // Read Product
    // ---------------------------------------------------------

    @Test
    void activeListMapsProducts() {
        when(repository.findByActiveTrueOrderByNameAsc())
                .thenReturn(List.of(active));

        List<ProductResponse> result = service.getActiveProducts();

        assertEquals(1, result.size());
        assertEquals("CF-001", result.get(0).sku());
        assertEquals("Espresso", result.get(0).name());
        assertEquals(ACTIVE_CATEGORY_ID, result.get(0).categoryId());
        assertEquals("Coffee", result.get(0).categoryName());
    }

    @Test
    void activeListCanBeEmpty() {
        when(repository.findByActiveTrueOrderByNameAsc())
                .thenReturn(List.of());

        assertTrue(service.getActiveProducts().isEmpty());
    }

    @Test
    void managementListIncludesInactiveProducts() {
        when(repository.findAllByOrderByNameAsc())
                .thenReturn(List.of(active, inactive));

        List<ProductResponse> result = service.getProductsForManagement();

        assertEquals(2, result.size());
        assertTrue(result.get(0).active());
        assertFalse(result.get(1).active());
    }

    @Test
    void getByIdReturnsProductAndCategory() {
        when(repository.findById(1L))
                .thenReturn(Optional.of(active));

        ProductResponse result = service.getProductById(1L);

        assertEquals("Espresso", result.name());
        assertEquals(ACTIVE_CATEGORY_ID, result.categoryId());
        assertEquals("Coffee", result.categoryName());
    }

    @Test
    void getByIdThrowsWhenProductDoesNotExist() {
        when(repository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.getProductById(99L)
        );
    }

    // ---------------------------------------------------------
    // Create Product
    // ---------------------------------------------------------

    @Test
    void createNormalizesSkuNameAndDescription() {
        when(repository.existsBySku("CF-003"))
                .thenReturn(false);

        when(categoryRepository.findById(ACTIVE_CATEGORY_ID))
                .thenReturn(Optional.of(activeCategory));

        when(repository.save(any(Product.class)))
                .thenAnswer(invocation -> {
                    Product product = invocation.getArgument(0);
                    product.setId(3L);
                    return product;
                });

        ProductResponse result = service.createProduct(
                new ProductCreateRequest(
                        " cf-003 ",
                        " Cappuccino ",
                        " Milk ",
                        new BigDecimal("42000.00"),
                        ACTIVE_CATEGORY_ID
                )
        );

        assertEquals("CF-003", result.sku());
        assertEquals("Cappuccino", result.name());
        assertEquals("Milk", result.description());
        assertEquals(new BigDecimal("42000.00"), result.price());
        assertTrue(result.active());
        assertEquals(ACTIVE_CATEGORY_ID, result.categoryId());
        assertEquals("Coffee", result.categoryName());

        verify(repository).save(any(Product.class));
    }

    @Test
    void createNormalizesBlankDescriptionToNull() {
        when(repository.existsBySku("CF-004"))
                .thenReturn(false);

        when(categoryRepository.findById(ACTIVE_CATEGORY_ID))
                .thenReturn(Optional.of(activeCategory));

        when(repository.save(any(Product.class)))
                .thenAnswer(invocation -> {
                    Product product = invocation.getArgument(0);
                    product.setId(4L);
                    return product;
                });

        ProductResponse result = service.createProduct(
                new ProductCreateRequest(
                        "CF-004",
                        "Americano",
                        " ",
                        new BigDecimal("35000"),
                        ACTIVE_CATEGORY_ID
                )
        );

        assertNull(result.description());
        assertEquals(ACTIVE_CATEGORY_ID, result.categoryId());
    }

    @Test
    void createRejectsDuplicateSkuWithoutSaving() {
        when(repository.existsBySku("CF-001"))
                .thenReturn(true);

        assertThrows(
                DuplicateSkuException.class,
                () -> service.createProduct(
                        new ProductCreateRequest(
                                "cf-001",
                                "Duplicate",
                                null,
                                new BigDecimal("30000"),
                                ACTIVE_CATEGORY_ID
                        )
                )
        );

        verify(repository, never()).save(any(Product.class));

        // Duplicate SKU is detected before the Category lookup.
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void createRejectsMissingCategory() {
        when(repository.existsBySku("CF-005"))
                .thenReturn(false);

        when(categoryRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.createProduct(
                        new ProductCreateRequest(
                                "CF-005",
                                "Mocha",
                                null,
                                new BigDecimal("45000"),
                                999L
                        )
                )
        );

        verify(repository, never()).save(any(Product.class));
    }

    @Test
    void createRejectsInactiveCategory() {
        when(repository.existsBySku("CF-006"))
                .thenReturn(false);

        when(categoryRepository.findById(INACTIVE_CATEGORY_ID))
                .thenReturn(Optional.of(inactiveCategory));

        assertThrows(
                InactiveCategoryException.class,
                () -> service.createProduct(
                        new ProductCreateRequest(
                                "CF-006",
                                "Seasonal Drink",
                                null,
                                new BigDecimal("50000"),
                                INACTIVE_CATEGORY_ID
                        )
                )
        );

        verify(repository, never()).save(any(Product.class));
    }

    // ---------------------------------------------------------
    // Update Product
    // ---------------------------------------------------------

    @Test
    void updateCanKeepOwnSkuAndChangesFields() {
        when(repository.findById(1L))
                .thenReturn(Optional.of(active));

        when(repository.existsBySkuAndIdNot("CF-001", 1L))
                .thenReturn(false);

        when(repository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse result = service.updateProduct(
                1L,
                new ProductUpdateRequest(
                        "cf-001",
                        "Espresso Double",
                        null,
                        new BigDecimal("40000"),
                        ACTIVE_CATEGORY_ID
                )
        );

        assertEquals("CF-001", result.sku());
        assertEquals("Espresso Double", result.name());
        assertNull(result.description());
        assertEquals(new BigDecimal("40000"), result.price());
        assertEquals(ACTIVE_CATEGORY_ID, result.categoryId());
        assertEquals("Coffee", result.categoryName());

        // Keeping the current Category should not require another lookup.
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void updateCanChangeToAnotherActiveCategory() {
        when(repository.findById(1L))
                .thenReturn(Optional.of(active));

        when(repository.existsBySkuAndIdNot("CF-001", 1L))
                .thenReturn(false);

        Category anotherActiveCategory = category(
                30L,
                "Tea",
                true
        );

        when(categoryRepository.findById(30L))
                .thenReturn(Optional.of(anotherActiveCategory));

        when(repository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse result = service.updateProduct(
                1L,
                new ProductUpdateRequest(
                        "CF-001",
                        "Espresso",
                        "Updated description",
                        new BigDecimal("40000"),
                        30L
                )
        );

        assertEquals(30L, result.categoryId());
        assertEquals("Tea", result.categoryName());

        verify(categoryRepository).findById(30L);
        verify(repository).save(active);
    }

    @Test
    void updateRejectsChangingToMissingCategory() {
        when(repository.findById(1L))
                .thenReturn(Optional.of(active));

        when(repository.existsBySkuAndIdNot("CF-001", 1L))
                .thenReturn(false);

        when(categoryRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.updateProduct(
                        1L,
                        new ProductUpdateRequest(
                                "CF-001",
                                "Espresso",
                                null,
                                new BigDecimal("30000"),
                                999L
                        )
                )
        );

        verify(repository, never()).save(any(Product.class));
    }

    @Test
    void updateRejectsChangingToInactiveCategory() {
        when(repository.findById(1L))
                .thenReturn(Optional.of(active));

        when(repository.existsBySkuAndIdNot("CF-001", 1L))
                .thenReturn(false);

        when(categoryRepository.findById(INACTIVE_CATEGORY_ID))
                .thenReturn(Optional.of(inactiveCategory));

        assertThrows(
                InactiveCategoryException.class,
                () -> service.updateProduct(
                        1L,
                        new ProductUpdateRequest(
                                "CF-001",
                                "Espresso",
                                null,
                                new BigDecimal("30000"),
                                INACTIVE_CATEGORY_ID
                        )
                )
        );

        verify(repository, never()).save(any(Product.class));
    }

    @Test
    void updateCanKeepExistingInactiveCategoryWithoutChangingIt() {
        // Business rule: an inactive Category cannot be newly assigned,
        // but an existing Product may retain that Category when editing
        // unrelated Product fields.
        Product productWithInactiveCategory = product(
                3L,
                "CF-003",
                "Seasonal Latte",
                true,
                inactiveCategory
        );

        when(repository.findById(3L))
                .thenReturn(Optional.of(productWithInactiveCategory));

        when(repository.existsBySkuAndIdNot("CF-003", 3L))
                .thenReturn(false);

        when(repository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse result = service.updateProduct(
                3L,
                new ProductUpdateRequest(
                        "CF-003",
                        "Seasonal Latte Updated",
                        "Updated description",
                        new BigDecimal("39000"),
                        INACTIVE_CATEGORY_ID
                )
        );

        assertEquals("Seasonal Latte Updated", result.name());
        assertEquals(INACTIVE_CATEGORY_ID, result.categoryId());
        assertEquals("Seasonal", result.categoryName());

        verifyNoInteractions(categoryRepository);
        verify(repository).save(productWithInactiveCategory);
    }

    @Test
    void updateRejectsSkuOfAnotherProduct() {
        when(repository.findById(1L))
                .thenReturn(Optional.of(active));

        when(repository.existsBySkuAndIdNot("CF-002", 1L))
                .thenReturn(true);

        assertThrows(
                DuplicateSkuException.class,
                () -> service.updateProduct(
                        1L,
                        new ProductUpdateRequest(
                                "CF-002",
                                "Espresso",
                                null,
                                new BigDecimal("30000"),
                                ACTIVE_CATEGORY_ID
                        )
                )
        );

        verify(repository, never()).save(any(Product.class));
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void updateMissingProductThrows() {
        when(repository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.updateProduct(
                        99L,
                        new ProductUpdateRequest(
                                "CF-099",
                                "Missing",
                                null,
                                new BigDecimal("10000"),
                                ACTIVE_CATEGORY_ID
                        )
                )
        );

        verify(repository, never()).save(any(Product.class));
        verifyNoInteractions(categoryRepository);
    }

    // ---------------------------------------------------------
    // Activate / Deactivate Product
    // ---------------------------------------------------------

    @Test
    void activateInactiveProduct() {
        when(repository.findById(2L))
                .thenReturn(Optional.of(inactive));

        when(repository.save(inactive))
                .thenReturn(inactive);

        ProductResponse result = service.activateProduct(2L);

        assertTrue(result.active());
        verify(repository).save(inactive);
    }

    @Test
    void activateAlreadyActiveProductIsIdempotent() {
        when(repository.findById(1L))
                .thenReturn(Optional.of(active));

        ProductResponse result = service.activateProduct(1L);

        assertTrue(result.active());
        verify(repository, never()).save(any(Product.class));
    }

    @Test
    void deactivateActiveProduct() {
        when(repository.findById(1L))
                .thenReturn(Optional.of(active));

        when(repository.save(active))
                .thenReturn(active);

        ProductResponse result = service.deactivateProduct(1L);

        assertFalse(result.active());
        verify(repository).save(active);
    }

    @Test
    void deactivateAlreadyInactiveProductIsIdempotent() {
        when(repository.findById(2L))
                .thenReturn(Optional.of(inactive));

        ProductResponse result = service.deactivateProduct(2L);

        assertFalse(result.active());
        verify(repository, never()).save(any(Product.class));
    }

    @Test
    void lifecycleOperationsThrowWhenProductDoesNotExist() {
        when(repository.findById(404L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.activateProduct(404L)
        );

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.deactivateProduct(404L)
        );
    }

    // ---------------------------------------------------------
    // Test fixtures
    // ---------------------------------------------------------

    private Category category(Long id, String name, boolean activeFlag) {
        Category category = new Category(name, "Test category");
        category.setActive(activeFlag);

        // A real Category receives its ID from the database.
        // Unit tests must assign the ID explicitly to simulate a persisted Category.
        ReflectionTestUtils.setField(category, "id", id);

        return category;
    }

    private Product product(
            Long id,
            String sku,
            String name,
            boolean activeFlag,
            Category category
    ) {
        Product product = new Product();

        product.setId(id);
        product.setSku(sku);
        product.setName(name);
        product.setDescription("Coffee");
        product.setPrice(new BigDecimal("30000"));
        product.setActive(activeFlag);
        product.setCategory(category);

        return product;
    }
}
