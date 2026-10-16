
package com.coffeeshop.product.controller;

import com.coffeeshop.common.exception.DuplicateSkuException;
import com.coffeeshop.common.exception.GlobalExceptionHandler;
import com.coffeeshop.common.exception.InactiveCategoryException;
import com.coffeeshop.common.exception.ResourceNotFoundException;
import com.coffeeshop.product.dto.ProductResponse;
import com.coffeeshop.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProductService service;

    @Test
    void getActiveListReturnsProductsWithCategory() throws Exception {
        when(service.getActiveProducts()).thenReturn(List.of(product(1L, true)));

        mvc.perform(get("/api/products").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].categoryId").value(10))
                .andExpect(jsonPath("$[0].categoryName").value("Coffee"));
    }

    @Test
    void getActiveListCanBeEmpty() throws Exception {
        when(service.getActiveProducts()).thenReturn(List.of());

        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void managementListIncludesInactiveProducts() throws Exception {
        when(service.getProductsForManagement())
                .thenReturn(List.of(product(1L, true), product(2L, false)));

        mvc.perform(get("/api/products/management"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].active").value(false))
                .andExpect(jsonPath("$[1].categoryId").value(10))
                .andExpect(jsonPath("$[1].categoryName").value("Coffee"));
    }

    @Test
    void getMissingProductReturns404() throws Exception {
        when(service.getProductById(44L))
                .thenThrow(new ResourceNotFoundException("Product not found"));

        mvc.perform(get("/api/products/44"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void createReturns201WithCategory() throws Exception {
        when(service.createProduct(any())).thenReturn(product(3L, true));

        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "CF-003",
                                  "name": "Latte",
                                  "price": 45000,
                                  "categoryId": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.categoryId").value(10))
                .andExpect(jsonPath("$.categoryName").value("Coffee"));
    }

    @Test
    void createWithoutCategoryReturns400WithoutCallingService() throws Exception {
        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "CF-003",
                                  "name": "Latte",
                                  "price": 45000
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.categoryId")
                        .value("Category is required"));

        verify(service, never()).createProduct(any());
    }

    @Test
    void blankSkuReturns400WithoutCallingService() throws Exception {
        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": " ",
                                  "name": "Latte",
                                  "price": 45000,
                                  "categoryId": 10
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.sku")
                        .value("SKU is required"));

        verify(service, never()).createProduct(any());
    }

    @Test
    void zeroPriceReturns400() throws Exception {
        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "CF-003",
                                  "name": "Latte",
                                  "price": 0,
                                  "categoryId": 10
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.price")
                        .value("Price must be greater than 0"));

        verify(service, never()).createProduct(any());
    }

    @Test
    void duplicateSkuReturns409() throws Exception {
        when(service.createProduct(any()))
                .thenThrow(new DuplicateSkuException("CF-001"));

        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "CF-001",
                                  "name": "Espresso",
                                  "price": 30000,
                                  "categoryId": 10
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void assigningInactiveCategoryReturns409() throws Exception {
        when(service.createProduct(any()))
                .thenThrow(new InactiveCategoryException(20L));

        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "CF-020",
                                  "name": "Seasonal Drink",
                                  "price": 50000,
                                  "categoryId": 20
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void updateReturns200WithCategory() throws Exception {
        when(service.updateProduct(eq(1L), any()))
                .thenReturn(product(1L, true));

        mvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "CF-001",
                                  "name": "Espresso",
                                  "price": 32000,
                                  "categoryId": 10
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.categoryId").value(10))
                .andExpect(jsonPath("$.categoryName").value("Coffee"));
    }

    @Test
    void updateWithoutCategoryReturns400WithoutCallingService() throws Exception {
        mvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "CF-001",
                                  "name": "Espresso",
                                  "price": 32000
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.categoryId")
                        .value("Category is required"));

        verify(service, never()).updateProduct(eq(1L), any());
    }

    @Test
    void deactivateReturnsInactiveProduct() throws Exception {
        when(service.deactivateProduct(1L)).thenReturn(product(1L, false));

        mvc.perform(patch("/api/products/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void activateReturnsActiveProduct() throws Exception {
        when(service.activateProduct(1L)).thenReturn(product(1L, true));

        mvc.perform(patch("/api/products/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    private ProductResponse product(Long id, boolean active) {
        return new ProductResponse(
                id,
                "CF-00" + id,
                "Espresso",
                "Coffee",
                new BigDecimal("30000.00"),
                active,
                10L,
                "Coffee"
        );
    }
}
