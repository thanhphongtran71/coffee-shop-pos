
package com.coffeeshop.category.controller;

import com.coffeeshop.category.dto.CategoryCreateRequest;
import com.coffeeshop.category.dto.CategoryResponse;
import com.coffeeshop.category.dto.CategoryUpdateRequest;
import com.coffeeshop.category.service.CategoryService;
import com.coffeeshop.common.exception.DuplicateCategoryException;
import com.coffeeshop.common.exception.GlobalExceptionHandler;
import com.coffeeshop.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@Import(GlobalExceptionHandler.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CategoryService service;

    @Test
    void getActiveCategories_returnsCategories() throws Exception {
        when(service.getActiveCategories()).thenReturn(
                List.of(category(1L, "Coffee", true))
        );

        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Coffee"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void getActiveCategories_returnsEmptyList() throws Exception {
        when(service.getActiveCategories()).thenReturn(List.of());

        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getCategoriesForManagement_passesSearchToService() throws Exception {
        when(service.getCategories("coffee")).thenReturn(
                List.of(category(1L, "Coffee", true))
        );

        mvc.perform(get("/api/categories/management")
                        .param("search", "coffee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Coffee"));

        verify(service).getCategories("coffee");
    }

    @Test
    void getCategoryById_returnsCategory() throws Exception {
        when(service.getCategoryById(1L))
                .thenReturn(category(1L, "Coffee", true));

        mvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Coffee"));
    }

    @Test
    void getCategoryById_whenMissing_returnsNotFound() throws Exception {
        when(service.getCategoryById(99L))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        mvc.perform(get("/api/categories/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCategory_withValidRequest_returnsCreated() throws Exception {
        when(service.createCategory(any(CategoryCreateRequest.class)))
                .thenReturn(category(1L, "Coffee", true));

        mvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Coffee",
                                  "description": "Coffee drinks"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Coffee"))
                .andExpect(jsonPath("$.description").value("Coffee drinks"))
                .andExpect(jsonPath("$.active").value(true));

        verify(service).createCategory(any(CategoryCreateRequest.class));
    }

    @Test
    void createCategory_whenNameBlank_returnsBadRequest() throws Exception {
        mvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " ",
                                  "description": "Coffee drinks"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name")
                        .value("Category name is required"));

        verifyNoInteractions(service);
    }

    @Test
    void createCategory_whenNameTooLong_returnsBadRequest() throws Exception {
        String longName = "C".repeat(101);
        String requestBody = """
                {
                  "name": "%s",
                  "description": "Coffee drinks"
                }
                """.formatted(longName);

        mvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void createCategory_whenNameAlreadyExists_returnsConflict() throws Exception {
        when(service.createCategory(any(CategoryCreateRequest.class)))
                .thenThrow(new DuplicateCategoryException("Coffee"));

        mvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Coffee",
                                  "description": "Coffee drinks"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void updateCategory_withValidRequest_returnsUpdatedCategory() throws Exception {
        when(service.updateCategory(eq(1L), any(CategoryUpdateRequest.class)))
                .thenReturn(category(1L, "Tea", true));

        mvc.perform(put("/api/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Tea",
                                  "description": "Tea drinks"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Tea"));

        verify(service).updateCategory(
                eq(1L), any(CategoryUpdateRequest.class));
    }

    @Test
    void updateCategory_whenNameBlank_returnsBadRequest() throws Exception {
        mvc.perform(put("/api/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "description": "Tea drinks"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name")
                        .value("Category name is required"));

        verifyNoInteractions(service);
    }

    @Test
    void updateCategory_whenMissing_returnsNotFound() throws Exception {
        when(service.updateCategory(eq(99L), any(CategoryUpdateRequest.class)))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        mvc.perform(put("/api/categories/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Tea",
                                  "description": "Tea drinks"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void activateCategory_returnsUpdatedCategory() throws Exception {
        when(service.activateCategory(1L))
                .thenReturn(category(1L, "Coffee", true));

        mvc.perform(patch("/api/categories/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void deactivateCategory_returnsUpdatedCategory() throws Exception {
        when(service.deactivateCategory(1L))
                .thenReturn(category(1L, "Coffee", false));

        mvc.perform(patch("/api/categories/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void activateCategory_whenMissing_returnsNotFound() throws Exception {
        when(service.activateCategory(99L))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        mvc.perform(patch("/api/categories/99/activate"))
                .andExpect(status().isNotFound());
    }

    private CategoryResponse category(Long id, String name, boolean active) {
        return new CategoryResponse(
                id,
                name,
                name.equals("Coffee") ? "Coffee drinks" : "Tea drinks",
                active
        );
    }
}
