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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;

    private CategoryRequest categoryRequest;

    private CategoryResponse categoryResponse;

    @BeforeEach
    public void setUp() {
        category = Category.builder().id(1L).name("TestCategory").status(CategoryStatus.ACTIVE).build();

        categoryRequest = CategoryRequest.builder().name("TestCategory").build();

        categoryResponse = CategoryResponse.builder().id(1L).name("TestCategory").build();
    }

    @Test
    void createCategory_whenNameNotExists_savesAndReturnsResponse() {
        when(categoryRepository.existsByName(anyString())).thenReturn(false);

        categoryService.createCategory(categoryRequest);

        assertThat(category.getName()).isEqualTo(categoryRequest.getName());

        verify(categoryRepository, times(1)).save(any());
    }

    @Test
    void createCategory_whenNameExists_throwsCategoryAlreadyExists() {
        when(categoryRepository.existsByName(anyString())).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(categoryRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_ALREADY_EXISTS);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void createCategory_whenSaveThrowsDataIntegrityViolation_throwsCategoryAlreadyExists() {
        when(categoryRepository.existsByName(anyString())).thenReturn(false);

        when(categoryMapper.toCategory(categoryRequest)).thenReturn(category);

        when(categoryRepository.save(any(Category.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        assertThatThrownBy(() -> categoryService.createCategory(categoryRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_ALREADY_EXISTS);

        verify(categoryRepository, times(1)).save(any());
    }

    @Test
    void getCategoryById_whenExists_returnsResponse() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        when(categoryMapper.toCategoryResponse(category)).thenReturn(categoryResponse);

        var result = categoryService.getCategoryById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void deleteCategory_whenHasProducts_throwException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_HAS_PRODUCTS);

        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void deleteCategory_whenNoProducts_deleteSuccessful() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.existsByCategoryId(1L)).thenReturn(false);

        categoryService.deleteCategory(1L);

        verify(categoryRepository, times(1)).delete(category);
    }
}