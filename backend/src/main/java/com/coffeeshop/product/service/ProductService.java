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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<ProductResponse> getActiveProducts() {
        return productRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ProductResponse> getProductsForManagement() {
        return productRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse getProductById(Long id) {
        return toResponse(findProductOrThrow(id));
    }

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        String sku = normalizeSku(request.sku());
        ensureSkuAvailable(sku, null);

        // A newly assigned Category must exist and be active.
        Category category = findActiveCategoryOrThrow(request.categoryId());

        Product product = Product.builder()
                .sku(sku)
                .name(request.name().trim())
                .description(normalizeDescription(request.description()))
                .price(request.price())
                .active(true)
                .category(category)
                .build();

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        Product product = findProductOrThrow(id);

        String sku = normalizeSku(request.sku());
        ensureSkuAvailable(sku, id);

        Category category = resolveCategoryForUpdate(
                product,
                request.categoryId()
        );

        product.setSku(sku);
        product.setName(request.name().trim());
        product.setDescription(normalizeDescription(request.description()));
        product.setPrice(request.price());
        product.setCategory(category);

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
        return productRepository.findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Product not found with id: " + id
                        )
                );
    }

    private Category findActiveCategoryOrThrow(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Category not found with id: " + categoryId
                        )
                );

        if (!category.isActive()) {
            throw new InactiveCategoryException(categoryId);
        }

        return category;
    }

    private Category resolveCategoryForUpdate(
            Product product,
            Long requestedCategoryId
    ) {
        Category currentCategory = product.getCategory();

        // Allow editing other Product fields while preserving its existing
        // Category, even if that Category has since been deactivated.
        if (currentCategory != null
                && Objects.equals(currentCategory.getId(), requestedCategoryId)) {
            return currentCategory;
        }

        // A new or changed assignment must point to an active Category.
        return findActiveCategoryOrThrow(requestedCategoryId);
    }

    private void ensureSkuAvailable(String sku, Long currentId) {
        boolean exists = currentId == null
                ? productRepository.existsBySku(sku)
                : productRepository.existsBySkuAndIdNot(sku, currentId);

        if (exists) {
            throw new DuplicateSkuException(sku);
        }
    }

    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank()
                ? null
                : description.trim();
    }

    private ProductResponse toResponse(Product product) {
        Category category = product.getCategory();

        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.isActive(),
                category.getId(),
                category.getName()
        );
    }
}