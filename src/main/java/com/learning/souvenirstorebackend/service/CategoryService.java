package com.learning.souvenirstorebackend.service;

import com.learning.souvenirstorebackend.dto.request.CategoryRequest;
import com.learning.souvenirstorebackend.dto.response.CategoryResponse;
import com.learning.souvenirstorebackend.entity.Category;
import com.learning.souvenirstorebackend.enums.CategoryStatus;
import com.learning.souvenirstorebackend.exception.AppException;
import com.learning.souvenirstorebackend.exception.ErrorCode;
import com.learning.souvenirstorebackend.mapper.CategoryMapper;
import com.learning.souvenirstorebackend.repository.CategoryRepository;
import com.learning.souvenirstorebackend.repository.ProductRepository;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Builder
public class CategoryService {
    CategoryRepository categoryRepository;
    ProductRepository productRepository;
    CategoryMapper categoryMapper;

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public CategoryResponse createCategory(CategoryRequest request) {

        if (categoryRepository.existsByName(request.getName()))
            throw new AppException(ErrorCode.CATEGORY_ALREADY_EXISTS);

        Category category = categoryMapper.toCategory(request);

        try {
            category = categoryRepository.save(category);
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }

        return categoryMapper.toCategoryResponse(category);
    }

    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id).orElseThrow(()
                -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        return categoryMapper.toCategoryResponse(category);
    }

    public List<CategoryResponse> getAllActiveCategory() {
        return categoryRepository.findAllByStatus(CategoryStatus.ACTIVE).stream()
                .map(categoryMapper::toCategoryResponse).toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public CategoryResponse updateCategory(Long id, CategoryRequest categoryRequest) {
        Category category = categoryRepository.findById(id).orElseThrow(()
                -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        categoryMapper.updateCategory(category, categoryRequest);

        return categoryMapper.toCategoryResponse(categoryRepository.save(category));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse activateCategory(Long id) {
        Category category = categoryRepository.findById(id).orElseThrow(()
                -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        if (category.getStatus() == CategoryStatus.ACTIVE)
            throw new AppException(ErrorCode.CATEGORY_ALREADY_ACTIVE);

        category.setStatus(CategoryStatus.ACTIVE);

        return categoryMapper.toCategoryResponse(categoryRepository.save(category));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse deactivateCategory(Long id) {
        Category category = categoryRepository.findById(id).orElseThrow(()
                -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        if (category.getStatus() == CategoryStatus.INACTIVE)
            throw new AppException(ErrorCode.CATEGORY_ALREADY_INACTIVE);

        category.setStatus(CategoryStatus.INACTIVE);

        return categoryMapper.toCategoryResponse(categoryRepository.save(category));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        if (productRepository.existsByCategoryId(categoryId))
            throw new AppException(ErrorCode.CATEGORY_HAS_PRODUCTS);

        categoryRepository.delete(category);
    }
}
