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

import java.util.List;
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
    void getCategoryById_whenNotFound_throwsCategoryNotFound() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> categoryService.getCategoryById(1L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    void getAllActiveCategories_returnsOnlyActiveCategories() {
        when(categoryRepository.findAllByStatus(CategoryStatus.ACTIVE)).thenReturn(List.of(category));

        when(categoryMapper.toCategoryResponse(category)).thenReturn(categoryResponse);

        categoryService.getAllActiveCategory();

        verify(categoryRepository).findAllByStatus(CategoryStatus.ACTIVE);
    }

    @Test
    void getAllActiveCategories_whenEmpty_throwsEmptyList() {
        when(categoryRepository.findAllByStatus(CategoryStatus.ACTIVE)).thenReturn(List.of());
        categoryService.getAllActiveCategory();
        assertThat(categoryService.getAllActiveCategory()).isEmpty();
    }

    @Test
    void updateCategory_whenExists_callsMapperUpdateAndSaves() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        when(categoryMapper.toCategoryResponse(category)).thenReturn(categoryResponse);

        var result = categoryService.updateCategory(1L, categoryRequest);

        assertThat(result).isEqualTo(categoryResponse);

        verify(categoryMapper).updateCategory(category, categoryRequest);

        verify(categoryRepository).save(category);
    }

    @Test
    void updateCategory_whenNotFound_throwsCategoryNotFound() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> categoryService.updateCategory(1L, categoryRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    void activateCategory_whenInactive_setActiveAndSaves() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        when(categoryMapper.toCategoryResponse(category)).thenReturn(categoryResponse);

        category.setStatus(CategoryStatus.INACTIVE);

        var result = categoryService.activateCategory(1L);

        assertThat(result).isEqualTo(categoryResponse);

        assertThat(category.getStatus()).isEqualTo(CategoryStatus.ACTIVE);

        verify(categoryRepository).findById(1L);

        verify(categoryRepository).save(category);
    }

    @Test
    void activateCategory_whenAlreadyActive_throwsCategoryAlreadyActive() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> categoryService.activateCategory(1L)).isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_ALREADY_ACTIVE);

        verify(productRepository, never()).save(any());
    }

    @Test
    void activateCategory_whenNotFound_throwsCategoryNotFound() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.activateCategory(1L)).isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NOT_FOUND);

        verify(productRepository, never()).save(any());
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
    void deactivateCategory_whenActive_setsInactiveAndSaves() {
        category.setStatus(CategoryStatus.ACTIVE);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        when(categoryMapper.toCategoryResponse(category)).thenReturn(categoryResponse);

        var result = categoryService.deactivateCategory(1L);

        assertThat(result).isEqualTo(categoryResponse);

        assertThat(category.getStatus()).isEqualTo(CategoryStatus.INACTIVE);

        verify(categoryRepository).findById(1L);

        verify(categoryRepository).save(category);
    }

    @Test
    void deactivateCategory_whenAlreadyInactive_throwsCategoryAlreadyInactive() {
        category.setStatus(CategoryStatus.INACTIVE);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> categoryService.deactivateCategory(1L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_ALREADY_INACTIVE);

        verify(categoryRepository).findById(1L);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void deactivateCategory_whenNotFound_throwsCategoryNotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deactivateCategory(999L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NOT_FOUND);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void deleteCategory_whenNoProducts_deleteSuccessful() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        when(productRepository.existsByCategoryId(1L)).thenReturn(false);

        categoryService.deleteCategory(1L);

        verify(categoryRepository, times(1)).delete(category);
    }

    @Test
    void deleteCategory_whenNotFound_throwsCategoryNotFound() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NOT_FOUND);
        verify(categoryRepository, never()).delete(any());
    }
}