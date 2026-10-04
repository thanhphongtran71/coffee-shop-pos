package com.coffeeshop.product.controller;

import com.coffeeshop.product.dto.ProductResponse;
import com.coffeeshop.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /*
     * ProductController depends on ProductService.
     *
     * This test focuses ONLY on the MVC / HTTP layer.
     *
     * Therefore, we do NOT use the real ProductService.
     *
     * @MockitoBean tells Spring:
     *
     * "Create a Mockito mock of ProductService
     * and put it into the Spring test ApplicationContext."
     *
     * ProductController will receive this mock
     * through dependency injection.
     */
    @MockitoBean
    private ProductService productService;


    @Test
    void shouldReturnActiveProducts() throws Exception {

        // =========================================================
        // ARRANGE
        // =========================================================

        ProductResponse espresso =
                new ProductResponse(
                        1L,
                        "CF-001",
                        "Espresso",
                        "Strong black coffee",
                        new BigDecimal("30000.00"),
                        true
                );

        /*
         * Define the behavior of our mocked service.
         *
         * When ProductController calls:
         *
         *     productService.getActiveProducts()
         *
         * the mock returns one Espresso product.
         */
        when(productService.getActiveProducts())
                .thenReturn(List.of(espresso));


        // =========================================================
        // ACT + ASSERT
        // =========================================================

        /*
         * MockMvc simulates an HTTP request.
         *
         * We are NOT starting a real Tomcat server.
         *
         * Instead:
         *
         * HTTP Request
         *      ↓
         * DispatcherServlet
         *      ↓
         * ProductController
         *      ↓
         * Mock ProductService
         *      ↓
         * JSON Response
         */
        mockMvc.perform(
                        get("/api/products")
                                .accept(MediaType.APPLICATION_JSON)
                )

                // HTTP status must be 200 OK.
                .andExpect(status().isOk())

                // Response must be JSON.
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )

                // Root JSON structure must be an array.
                .andExpect(
                        jsonPath("$").isArray()
                )

                // We expect one product.
                .andExpect(
                        jsonPath("$.length()").value(1)
                )

                // Verify API response contract.
                .andExpect(
                        jsonPath("$[0].id").value(1)
                )

                .andExpect(
                        jsonPath("$[0].sku")
                                .value("CF-001")
                )

                .andExpect(
                        jsonPath("$[0].name")
                                .value("Espresso")
                )

                .andExpect(
                        jsonPath("$[0].description")
                                .value("Strong black coffee")
                )

                .andExpect(
                        jsonPath("$[0].price")
                                .value(30000.00)
                )

                .andExpect(
                        jsonPath("$[0].active")
                                .value(true)
                );
    }


    @Test
    void shouldReturnEmptyArrayWhenNoProductsExist()
            throws Exception {

        // =========================================================
        // ARRANGE
        // =========================================================

        /*
         * Tell the mocked service that there are no products.
         */
        when(productService.getActiveProducts())
                .thenReturn(List.of());


        // =========================================================
        // ACT + ASSERT
        // =========================================================

        mockMvc.perform(
                        get("/api/products")
                )

                // Endpoint should still respond successfully.
                .andExpect(status().isOk())

                // Response should be a JSON array.
                .andExpect(
                        jsonPath("$").isArray()
                )

                // The array should contain zero elements.
                .andExpect(
                        jsonPath("$.length()").value(0)
                );
    }
}