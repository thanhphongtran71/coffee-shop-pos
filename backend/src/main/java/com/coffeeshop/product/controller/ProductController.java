package com.coffeeshop.product.controller;

import com.coffeeshop.product.dto.ProductCreateRequest;
import com.coffeeshop.product.dto.ProductResponse;
import com.coffeeshop.product.dto.ProductUpdateRequest;
import com.coffeeshop.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> getActiveProducts() {
        return productService.getActiveProducts();
    }

    @GetMapping("/management")
    public List<ProductResponse> getProductsForManagement() {
        return productService.getProductsForManagement();
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@Valid @RequestBody ProductCreateRequest request) {
        return productService.createProduct(request);
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request) {
        return productService.updateProduct(id, request);
    }

    @PatchMapping("/{id}/activate")
    public ProductResponse activateProduct(@PathVariable Long id) {
        return productService.activateProduct(id);
    }

    @PatchMapping("/{id}/deactivate")
    public ProductResponse deactivateProduct(@PathVariable Long id) {
        return productService.deactivateProduct(id);
    }
}
