
package com.coffeeshop.product.repository;

import com.coffeeshop.category.entity.Category;
import com.coffeeshop.category.repository.CategoryRepository;
import com.coffeeshop.product.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("Returns active products ordered by name and keeps their Category")
    void shouldReturnOnlyActiveProductsOrderedByName() {
        // Given: Every Product must reference a persisted Category.
        Category coffeeCategory = categoryRepository.save(
                new Category("Coffee", "Coffee drinks")
        );

        Product latte = createProduct(
                "CF-002",
                "Latte",
                "45000",
                true,
                coffeeCategory
        );

        Product espresso = createProduct(
                "CF-001",
                "Espresso",
                "30000",
                true,
                coffeeCategory
        );

        Product mocha = createProduct(
                "CF-003",
                "Mocha",
                "50000",
                false,
                coffeeCategory
        );

        productRepository.saveAll(List.of(latte, espresso, mocha));
        productRepository.flush();

        // When
        List<Product> result =
                productRepository.findByActiveTrueOrderByNameAsc();

        // Then: Inactive products are excluded and active products are ordered A-Z.
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(Product::isActive));
        assertEquals("Espresso", result.get(0).getName());
        assertEquals("Latte", result.get(1).getName());

        // The Product-to-Category relationship is persisted.
        assertNotNull(result.get(0).getCategory());
        assertEquals(
                coffeeCategory.getId(),
                result.get(0).getCategory().getId()
        );
        assertEquals(
                "Coffee",
                result.get(0).getCategory().getName()
        );
    }

    private Product createProduct(
            String sku,
            String name,
            String price,
            boolean active,
            Category category
    ) {
        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        product.setActive(active);
        product.setCategory(category);

        return product;
    }
}
