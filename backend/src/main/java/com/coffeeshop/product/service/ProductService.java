package com.coffeeshop.product.service;

import com.coffeeshop.common.exception.DuplicateSkuException;
import com.coffeeshop.common.exception.ResourceNotFoundException;
import com.coffeeshop.product.dto.ProductCreateRequest;
import com.coffeeshop.product.dto.ProductResponse;
import com.coffeeshop.product.dto.ProductUpdateRequest;
import com.coffeeshop.product.entity.Product;
import com.coffeeshop.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponse> getActiveProducts() {
        return productRepository.findByActiveTrueOrderByNameAsc()
                .stream().map(this::toResponse).toList();
    }

    public List<ProductResponse> getProductsForManagement() {
        return productRepository.findAllByOrderByNameAsc()
                .stream().map(this::toResponse).toList();
    }

    public ProductResponse getProductById(Long id) {
        return toResponse(findProductOrThrow(id));
    }

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        String sku = normalizeSku(request.sku());
        ensureSkuAvailable(sku, null);

        Product product = Product.builder()
                .sku(sku)
                .name(request.name().trim())
                .description(normalizeDescription(request.description()))
                .price(request.price())
                .active(true)
                .build();
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        Product product = findProductOrThrow(id);
        String sku = normalizeSku(request.sku());
        ensureSkuAvailable(sku, id);

        product.setSku(sku);
        product.setName(request.name().trim());
        product.setDescription(normalizeDescription(request.description()));
        product.setPrice(request.price());
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse activateProduct(Long id) {
        Product product = findProductOrThrow(id);
        if (!product.isActive()) {
            product.setActive(true);
            product = productRepository.save(product);
        }
        return toResponse(product);
    }

    @Transactional
    public ProductResponse deactivateProduct(Long id) {
        Product product = findProductOrThrow(id);
        if (product.isActive()) {
            product.setActive(false);
            product = productRepository.save(product);
        }
        return toResponse(product);
    }

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private void ensureSkuAvailable(String sku, Long currentId) {
        boolean exists = currentId == null
                ? productRepository.existsBySku(sku)
                : productRepository.existsBySkuAndIdNot(sku, currentId);
        if (exists) throw new DuplicateSkuException(sku);
    }

    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(product.getId(), product.getSku(), product.getName(),
                product.getDescription(), product.getPrice(), product.isActive());
    }
}
