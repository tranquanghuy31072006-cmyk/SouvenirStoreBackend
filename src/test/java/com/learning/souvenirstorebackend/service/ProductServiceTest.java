package com.learning.souvenirstorebackend.service;

import com.learning.souvenirstorebackend.dto.request.ProductImageRequest;
import com.learning.souvenirstorebackend.dto.request.ProductRequest;
import com.learning.souvenirstorebackend.dto.response.ProductResponse;
import com.learning.souvenirstorebackend.entity.Brand;
import com.learning.souvenirstorebackend.entity.Category;
import com.learning.souvenirstorebackend.entity.Product;
import com.learning.souvenirstorebackend.entity.ProductImage;
import com.learning.souvenirstorebackend.enums.ProductStatus;
import com.learning.souvenirstorebackend.exception.AppException;
import com.learning.souvenirstorebackend.exception.ErrorCode;
import com.learning.souvenirstorebackend.mapper.ProductMapper;
import com.learning.souvenirstorebackend.repository.BrandRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    ProductRepository productRepository;

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    BrandRepository brandRepository;

    @Mock
    ProductMapper productMapper;

    @InjectMocks
    ProductService productService;

    private Product product;

    private Category category;

    private Brand brand;

    private ProductRequest request;

    private ProductResponse response;

    @BeforeEach
    void setUp() {
        category = Category.builder().id(10L).name("Cat").build();

        brand = Brand.builder().id(20L).name("Br").build();

        product = Product.builder().id(1L).name("TestProduct")
                .category(category).brand(brand)
                .status(ProductStatus.ACTIVE).build();

        request = ProductRequest.builder()
                .name("TestProduct").categoryId(10L).brandId(20L)
                .build();

        response = ProductResponse.builder().id(1L).name("TestProduct").build();
    }

    @Test
    void createProduct_whenValid_savesAndReturnsResponse() {
        Product bareProduct = Product.builder().id(1L).name("TestProduct").build();

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        when(brandRepository.findById(20L)).thenReturn(Optional.of(brand));

        when(productRepository.existsByName("TestProduct")).thenReturn(false);

        when(productMapper.toProduct(request)).thenReturn(bareProduct);

        when(productRepository.save(bareProduct)).thenReturn(bareProduct);

        when(productMapper.toProductResponse(bareProduct)).thenReturn(response);

        var result = productService.createProduct(request);

        assertThat(result).isEqualTo(response);

        assertThat(bareProduct.getCategory()).isEqualTo(category);

        assertThat(bareProduct.getBrand()).isEqualTo(brand);

        verify(productRepository).save(bareProduct);

        verify(productMapper).toProduct(request);
    }

    @Test
    void createProduct_whenCategoryNotFound_throwsCategoryNotFound() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NOT_FOUND);

        verify(categoryRepository).findById(10L);

        verify(productRepository, never()).save(any());

        verify(brandRepository, never()).findById(anyLong());
    }

    @Test
    void createProduct_whenBrandNotFound_throwsBrandNotFound() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        when(brandRepository.findById(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BRAND_NOT_FOUND);

        verify(productRepository, never()).save(any());

        verify(productRepository, never()).existsByName(anyString());
    }

    @Test
    void createProduct_whenNameExists_throwsProductAlreadyExists() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        when(brandRepository.findById(20L)).thenReturn(Optional.of(brand));

        when(productRepository.existsByName("TestProduct")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_ALREADY_EXISTS);

        verify(productRepository, never()).save(any());

        verify(productMapper, never()).toProduct(any());
    }

    @Test
    void createProduct_whenSaveThrowsDataIntegrityViolation_throwsProductAlreadyExists() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        when(brandRepository.findById(20L)).thenReturn(Optional.of(brand));

        when(productRepository.existsByName(anyString())).thenReturn(false);

        when(productMapper.toProduct(request)).thenReturn(product);

        when(productRepository.save(any(Product.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_ALREADY_EXISTS);

        verify(productRepository, times(1)).save(any());
    }

    @Test
    void createProduct_whenProductImagesNullOrEmpty_doesNotCreateImages() {
        Product bareProduct = Product.builder().id(1L).name("TestProduct").build();

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        when(brandRepository.findById(20L)).thenReturn(Optional.of(brand));

        when(productRepository.existsByName(anyString())).thenReturn(false);

        when(productMapper.toProduct(request)).thenReturn(bareProduct);

        when(productRepository.save(any(Product.class))).thenReturn(bareProduct);

        when(productMapper.toProductResponse(bareProduct)).thenReturn(response);

        productService.createProduct(request);

        assertThat(bareProduct.getProductImages()).isNullOrEmpty();
    }

    @Test
    void createProduct_whenHasProductImages_createsImagesWithSortOrder() {
        ProductImageRequest img1 = ProductImageRequest.builder().imageUrl("url1").sortOrder(1).build();

        ProductImageRequest img2 = ProductImageRequest.builder().imageUrl("url2").sortOrder(2).build();

        ProductRequest requestWithImages = ProductRequest.builder()
                .name("TestProduct").categoryId(10L).brandId(20L)
                .productImages(List.of(img1, img2))
                .build();

        Product bareProduct = Product.builder().id(1L).name("TestProduct").build();

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        when(brandRepository.findById(20L)).thenReturn(Optional.of(brand));

        when(productRepository.existsByName(anyString())).thenReturn(false);

        when(productMapper.toProduct(requestWithImages)).thenReturn(bareProduct);

        when(productRepository.save(any(Product.class))).thenReturn(bareProduct);

        when(productMapper.toProductResponse(bareProduct)).thenReturn(response);

        productService.createProduct(requestWithImages);

        assertThat(bareProduct.getProductImages()).hasSize(2);

        assertThat(bareProduct.getProductImages())
                .extracting(ProductImage::getImageUrl)
                .containsExactly("url1", "url2");

        assertThat(bareProduct.getProductImages())
                .extracting(ProductImage::getSortOrder)
                .containsExactly(1, 2);

        assertThat(bareProduct.getProductImages())
                .allSatisfy(img -> assertThat(img.getProduct()).isEqualTo(bareProduct));
    }

    @Test
    void getProductById_whenExists_returnsResponse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        when(productMapper.toProductResponse(product)).thenReturn(response);

        var result = productService.getProductById(1L);

        assertThat(result).isEqualTo(response);

        verify(productMapper).toProductResponse(product);
    }

    @Test
    void getProductById_whenNotFound_throwsProductNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(999L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void getAllProducts_returnsMappedList() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        when(productMapper.toProductResponseList(List.of(product))).thenReturn(List.of(response));

        var result = productService.getAllProducts();

        assertThat(result).hasSize(1).containsExactly(response);

        verify(productMapper).toProductResponseList(List.of(product));
    }

    @Test
    void getAllProducts_whenEmpty_returnsEmptyList() {
        when(productRepository.findAll()).thenReturn(List.of());

        when(productMapper.toProductResponseList(List.of())).thenReturn(List.of());

        var result = productService.getAllProducts();

        assertThat(result).isEmpty();
    }

    @Test
    void updateProduct_whenExists_callsMapperUpdateAndSaves() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        when(productRepository.save(product)).thenReturn(product);

        when(productMapper.toProductResponse(product)).thenReturn(response);

        var result = productService.updateProduct(1L, request);

        assertThat(result).isEqualTo(response);

        verify(productMapper).updateProduct(product, request);

        verify(productRepository).save(product);
    }

    @Test
    void updateProduct_whenNotFound_throwsProductNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(999L, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);

        verify(productRepository, never()).save(any());

        verify(productMapper, never()).updateProduct(any(), any());
    }

    @Test
    void activateProductStatus_whenExists_setsActiveAndSaves() {
        product.setStatus(ProductStatus.INACTIVE);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        when(productRepository.save(product)).thenReturn(product);

        when(productMapper.toProductResponse(product)).thenReturn(response);

        var result = productService.activateProductStatus(1L);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(result).isEqualTo(response);

        verify(productRepository).save(product);
    }

    @Test
    void activateProductStatus_whenAlreadyActive_stillSetsAndSaves() {
        product.setStatus(ProductStatus.ACTIVE);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        when(productRepository.save(product)).thenReturn(product);

        when(productMapper.toProductResponse(product)).thenReturn(response);

        productService.activateProductStatus(1L);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);

        verify(productRepository).save(product);
    }

    @Test
    void activateProductStatus_whenNotFound_throwsProductNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.activateProductStatus(999L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);

        verify(productRepository, never()).save(any());
    }

    @Test
    void deactivateProductStatus_whenExists_setsInactiveAndSaves() {
        product.setStatus(ProductStatus.ACTIVE);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        when(productRepository.save(product)).thenReturn(product);

        when(productMapper.toProductResponse(product)).thenReturn(response);

        var result = productService.deactivateProductStatus(1L);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.INACTIVE);

        assertThat(result).isEqualTo(response);

        verify(productRepository).save(product);
    }

    @Test
    void deactivateProductStatus_whenAlreadyInactive_stillSetsAndSaves() {
        product.setStatus(ProductStatus.INACTIVE);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        when(productRepository.save(product)).thenReturn(product);

        when(productMapper.toProductResponse(product)).thenReturn(response);

        productService.deactivateProductStatus(1L);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.INACTIVE);

        verify(productRepository).save(product);
    }

    @Test
    void deactivateProductStatus_whenNotFound_throwsProductNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deactivateProductStatus(999L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);

        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteProductById_whenExists_callsDelete() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.deleteProductById(1L);

        verify(productRepository).delete(product);
    }

    @Test
    void deleteProductById_whenNotFound_throwsProductNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProductById(999L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);

        verify(productRepository, never()).delete(any());
    }
}