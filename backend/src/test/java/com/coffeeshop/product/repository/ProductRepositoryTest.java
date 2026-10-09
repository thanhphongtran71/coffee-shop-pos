package com.coffeeshop.product.repository;

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

    @Test
    @DisplayName("Nên trả về danh sách sản phẩm active và sắp xếp theo tên A-Z")
    void shouldReturnOnlyActiveProductsOrderedByName() {
        // Given
        Product latte = createProduct(
                "CF-002",
                "Latte",
                "45000",
                true
        );

        Product espresso = createProduct(
                "CF-001",
                "Espresso",
                "30000",
                true
        );

        Product mocha = createProduct(
                "CF-003",
                "Mocha",
                "50000",
                false
        );

        productRepository.saveAll(List.of(latte, espresso, mocha));

        // When
        List<Product> result = productRepository.findByActiveTrueOrderByNameAsc();

        // Then
        assertEquals(2, result.size());

        assertTrue(result.stream().allMatch(Product::isActive));

        assertEquals("Espresso", result.get(0).getName());
        assertEquals("Latte", result.get(1).getName());
    }

    private Product createProduct(
            String sku,
            String name,
            String price,
            boolean active
    ) {
        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        product.setActive(active);
        return product;
    }
}