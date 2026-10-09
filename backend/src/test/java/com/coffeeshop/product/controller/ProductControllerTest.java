package com.coffeeshop.product.controller;

import com.coffeeshop.common.exception.DuplicateSkuException;
import com.coffeeshop.common.exception.GlobalExceptionHandler;
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
    @Autowired MockMvc mvc;
    @MockitoBean ProductService service;

    @Test void getActiveListReturnsJson() throws Exception {
        when(service.getActiveProducts()).thenReturn(List.of(product(1L, true)));
        mvc.perform(get("/api/products").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1));
    }

    @Test void getActiveListCanBeEmpty() throws Exception {
        when(service.getActiveProducts()).thenReturn(List.of());
        mvc.perform(get("/api/products")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test void managementListIncludesInactive() throws Exception {
        when(service.getProductsForManagement()).thenReturn(List.of(product(1L, true), product(2L, false)));
        mvc.perform(get("/api/products/management")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].active").value(false));
    }

    @Test void getMissingProductReturns404() throws Exception {
        when(service.getProductById(44L)).thenThrow(new ResourceNotFoundException("Product not found"));
        mvc.perform(get("/api/products/44")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test void createReturns201() throws Exception {
        when(service.createProduct(any())).thenReturn(product(3L, true));
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"CF-003\",\"name\":\"Latte\",\"price\":45000}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(3));
    }

    @Test void blankSkuReturns400WithoutCallingService() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\" \",\"name\":\"Latte\",\"price\":45000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.sku").value("SKU is required"));
        verify(service, never()).createProduct(any());
    }

    @Test void zeroPriceReturns400() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"CF-003\",\"name\":\"Latte\",\"price\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.price").value("Price must be greater than 0"));
    }

    @Test void duplicateSkuReturns409() throws Exception {
        when(service.createProduct(any())).thenThrow(new DuplicateSkuException("CF-001"));
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"CF-001\",\"name\":\"Espresso\",\"price\":30000}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
    }

    @Test void updateReturns200() throws Exception {
        when(service.updateProduct(eq(1L), any())).thenReturn(product(1L, true));
        mvc.perform(put("/api/products/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"CF-001\",\"name\":\"Espresso\",\"price\":32000}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
    }

    @Test void deactivateReturnsInactiveProduct() throws Exception {
        when(service.deactivateProduct(1L)).thenReturn(product(1L, false));
        mvc.perform(patch("/api/products/1/deactivate")).andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test void activateReturnsActiveProduct() throws Exception {
        when(service.activateProduct(1L)).thenReturn(product(1L, true));
        mvc.perform(patch("/api/products/1/activate")).andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    private ProductResponse product(Long id, boolean active) {
        return new ProductResponse(id, "CF-00" + id, "Espresso", "Coffee",
                new BigDecimal("30000.00"), active);
    }
}
