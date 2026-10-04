package com.coffeeshop.product.service;

import com.coffeeshop.product.dto.ProductResponse;
import com.coffeeshop.product.entity.Product;
import com.coffeeshop.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;


    @Test
    void shouldReturnActiveProducts() {

        // =========================================================
        // ARRANGE
        // =========================================================

        Product espresso = new Product();

        espresso.setId(1L);
        espresso.setSku("CF-001");
        espresso.setName("Espresso");
        espresso.setDescription("Strong black coffee");
        espresso.setPrice(new BigDecimal("30000.00"));
        espresso.setActive(true);

        /*
         * We don't want this test to access PostgreSQL.
         *
         * Instead, Mockito simulates the repository response.
         */
        when(productRepository.findByActiveTrueOrderByNameAsc())
                .thenReturn(List.of(espresso));


        // =========================================================
        // ACT
        // =========================================================

        List<ProductResponse> result =
                productService.getActiveProducts();


        // =========================================================
        // ASSERT
        // =========================================================

        assertNotNull(result);

        assertEquals(1, result.size());

        ProductResponse product = result.get(0);

        assertEquals(1L, product.id());

        assertEquals(
                "CF-001",
                product.sku()
        );

        assertEquals(
                "Espresso",
                product.name()
        );

        assertEquals(
                "Strong black coffee",
                product.description()
        );

        assertEquals(
                new BigDecimal("30000.00"),
                product.price()
        );

        assertTrue(product.active());


        // =========================================================
        // VERIFY
        // =========================================================

        /*
         * Make sure the service actually called
         * the repository exactly once.
         */
        verify(
                productRepository,
                times(1)
        ).findByActiveTrueOrderByNameAsc();
    }


    @Test
    void shouldReturnEmptyListWhenNoActiveProductsExist() {

        // ARRANGE

        when(productRepository.findByActiveTrueOrderByNameAsc())
                .thenReturn(List.of());


        // ACT

        List<ProductResponse> result =
                productService.getActiveProducts();


        // ASSERT

        assertNotNull(result);

        assertTrue(result.isEmpty());


        // VERIFY

        verify(
                productRepository,
                times(1)
        ).findByActiveTrueOrderByNameAsc();
    }
}