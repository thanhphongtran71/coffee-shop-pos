package com.coffeeshop.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductUpdateRequest(
        @NotBlank(message = "SKU is required")
        @Size(max = 50, message = "SKU must not exceed 50 characters")
        String sku,
        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,
        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,
        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        BigDecimal price
) {}
