
package com.learning.souvenirstorebackend.controller;

import com.learning.souvenirstorebackend.dto.request.CategoryRequest;
import com.learning.souvenirstorebackend.dto.response.ApiResponse;
import com.learning.souvenirstorebackend.dto.response.CategoryResponse;
import com.learning.souvenirstorebackend.service.CategoryService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryController {

    CategoryService categoryService;

    @PostMapping
    public ApiResponse<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest categoryRequest) {

        return ApiResponse.<CategoryResponse>builder()
                .result(categoryService.createCategory(categoryRequest))
                .build();
    }

    @GetMapping("/{categoryId}")
    public ApiResponse<CategoryResponse> getCategoryById(@PathVariable Long categoryId) {

        return ApiResponse.<CategoryResponse>builder()
                .result(categoryService.getCategoryById(categoryId))
                .build();
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> getAllCategories() {
        return ApiResponse.<List<CategoryResponse>>builder()
                .result(categoryService.getAllCategories())
                .build();
    }

    @GetMapping("/active")
    public ApiResponse<List<CategoryResponse>> getActiveCategories() {

        return ApiResponse.<List<CategoryResponse>>builder()
                .result(categoryService.getAllActiveCategory())
                .build();
    }

    @PutMapping("/{categoryId}")
    public ApiResponse<CategoryResponse> updateCategory(@PathVariable Long categoryId,
                                                        @Valid @RequestBody CategoryRequest categoryRequest) {

        return ApiResponse.<CategoryResponse>builder()
                .result(categoryService.updateCategory(categoryId, categoryRequest))
                .build();
    }

    @PutMapping("/{categoryId}/activate")
    public ApiResponse<CategoryResponse> activateCategoryStatus(@PathVariable Long categoryId) {

        return ApiResponse.<CategoryResponse>builder()
                .result(categoryService.activateCategory(categoryId))
                .build();
    }

    @PutMapping("/{categoryId}/deactivate")
    public ApiResponse<CategoryResponse> deactivateCategoryStatus(@PathVariable Long categoryId) {

        return ApiResponse.<CategoryResponse>builder()
                .result(categoryService.deactivateCategory(categoryId))
                .build();
    }

    @DeleteMapping("/{categoryId}")
    public ApiResponse<String> deleteCategory(@PathVariable Long categoryId) {

        categoryService.deleteCategory(categoryId);

        return ApiResponse.<String>builder()
                .message("Category with id " + categoryId + " has been deleted successfully")
                .build();
    }
}