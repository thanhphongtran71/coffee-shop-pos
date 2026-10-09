package com.coffeeshop.product.service;

import com.coffeeshop.common.exception.DuplicateSkuException;
import com.coffeeshop.common.exception.ResourceNotFoundException;
import com.coffeeshop.product.dto.*;
import com.coffeeshop.product.entity.Product;
import com.coffeeshop.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock ProductRepository repository;
    @InjectMocks ProductService service;
    Product active;
    Product inactive;

    @BeforeEach
    void setup() {
        active = product(1L, "CF-001", "Espresso", true);
        inactive = product(2L, "CF-002", "Latte", false);
    }

    @Test void activeListMapsProducts() {
        when(repository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(active));
        assertEquals("CF-001", service.getActiveProducts().get(0).sku());
    }

    @Test void activeListCanBeEmpty() {
        when(repository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of());
        assertTrue(service.getActiveProducts().isEmpty());
    }

    @Test void managementListIncludesInactive() {
        when(repository.findAllByOrderByNameAsc()).thenReturn(List.of(active, inactive));
        assertEquals(2, service.getProductsForManagement().size());
        assertFalse(service.getProductsForManagement().get(1).active());
    }

    @Test void getByIdReturnsProduct() {
        when(repository.findById(1L)).thenReturn(Optional.of(active));
        assertEquals("Espresso", service.getProductById(1L).name());
    }

    @Test void getByIdThrows404ExceptionWhenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getProductById(99L));
    }

    @Test void createNormalizesSkuNameAndDescription() {
        when(repository.existsBySku("CF-003")).thenReturn(false);
        when(repository.save(any(Product.class))).thenAnswer(i -> {
            Product p = i.getArgument(0); p.setId(3L); return p;
        });
        ProductResponse result = service.createProduct(new ProductCreateRequest(
                " cf-003 ", " Cappuccino ", " Milk ", new BigDecimal("42000.00")));
        assertEquals("CF-003", result.sku());
        assertEquals("Cappuccino", result.name());
        assertEquals("Milk", result.description());
        assertTrue(result.active());
    }

    @Test void createNormalizesBlankDescriptionToNull() {
        when(repository.existsBySku("CF-004")).thenReturn(false);
        when(repository.save(any(Product.class))).thenAnswer(i -> {
            Product p = i.getArgument(0); p.setId(4L); return p;
        });
        assertNull(service.createProduct(new ProductCreateRequest(
                "CF-004", "Americano", " ", new BigDecimal("35000"))).description());
    }

    @Test void createRejectsDuplicateSkuWithoutSaving() {
        when(repository.existsBySku("CF-001")).thenReturn(true);
        assertThrows(DuplicateSkuException.class, () -> service.createProduct(
                new ProductCreateRequest("cf-001", "Dup", null, new BigDecimal("30000"))));
        verify(repository, never()).save(any());
    }

    @Test void updateCanKeepOwnSkuAndChangesFields() {
        when(repository.findById(1L)).thenReturn(Optional.of(active));
        when(repository.existsBySkuAndIdNot("CF-001", 1L)).thenReturn(false);
        when(repository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        ProductResponse result = service.updateProduct(1L, new ProductUpdateRequest(
                "cf-001", "Espresso Double", null, new BigDecimal("40000")));
        assertEquals("Espresso Double", result.name());
        assertNull(result.description());
        assertEquals(new BigDecimal("40000"), result.price());
    }

    @Test void updateRejectsSkuOfAnotherProduct() {
        when(repository.findById(1L)).thenReturn(Optional.of(active));
        when(repository.existsBySkuAndIdNot("CF-002", 1L)).thenReturn(true);
        assertThrows(DuplicateSkuException.class, () -> service.updateProduct(1L,
                new ProductUpdateRequest("CF-002", "Espresso", null, new BigDecimal("30000"))));
        verify(repository, never()).save(any());
    }

    @Test void updateMissingProductThrows() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.updateProduct(99L,
                new ProductUpdateRequest("CF-099", "Missing", null, new BigDecimal("10000"))));
    }

    @Test void activateInactiveProduct() {
        when(repository.findById(2L)).thenReturn(Optional.of(inactive));
        when(repository.save(inactive)).thenReturn(inactive);
        assertTrue(service.activateProduct(2L).active());
        verify(repository).save(inactive);
    }

    @Test void activateAlreadyActiveIsIdempotent() {
        when(repository.findById(1L)).thenReturn(Optional.of(active));
        assertTrue(service.activateProduct(1L).active());
        verify(repository, never()).save(any());
    }

    @Test void deactivateActiveProduct() {
        when(repository.findById(1L)).thenReturn(Optional.of(active));
        when(repository.save(active)).thenReturn(active);
        assertFalse(service.deactivateProduct(1L).active());
        verify(repository).save(active);
    }

    @Test void deactivateAlreadyInactiveIsIdempotent() {
        when(repository.findById(2L)).thenReturn(Optional.of(inactive));
        assertFalse(service.deactivateProduct(2L).active());
        verify(repository, never()).save(any());
    }

    @Test void lifecycleMissingProductThrows() {
        when(repository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.activateProduct(404L));
        assertThrows(ResourceNotFoundException.class, () -> service.deactivateProduct(404L));
    }

    private Product product(Long id, String sku, String name, boolean activeFlag) {
        Product p = new Product();
        p.setId(id); p.setSku(sku); p.setName(name); p.setDescription("Coffee");
        p.setPrice(new BigDecimal("30000")); p.setActive(activeFlag);
        return p;
    }
}
