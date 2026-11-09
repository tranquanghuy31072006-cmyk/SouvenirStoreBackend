package com.learning.souvenirstorebackend.service;

import com.learning.souvenirstorebackend.dto.request.ProductVariantCreationRequest;
import com.learning.souvenirstorebackend.dto.request.ProductVariantUpdateRequest;
import com.learning.souvenirstorebackend.dto.response.ProductVariantResponse;
import com.learning.souvenirstorebackend.entity.Product;
import com.learning.souvenirstorebackend.entity.ProductVariant;
import com.learning.souvenirstorebackend.enums.ProductVariantStatus;
import com.learning.souvenirstorebackend.exception.AppException;
import com.learning.souvenirstorebackend.exception.ErrorCode;
import com.learning.souvenirstorebackend.mapper.ProductVariantMapper;
import com.learning.souvenirstorebackend.repository.CartItemRepository;
import com.learning.souvenirstorebackend.repository.OrderItemRepository;
import com.learning.souvenirstorebackend.repository.ProductRepository;
import com.learning.souvenirstorebackend.repository.ProductVariantRepository;
import com.learning.souvenirstorebackend.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductVariantServiceTest {

    @Mock
    ProductVariantRepository productVariantRepository;

    @Mock
    ProductRepository productRepository;

    @Mock
    CartItemRepository cartItemRepository;

    @Mock
    OrderItemRepository orderItemRepository;

    @Mock
    ProductVariantMapper productVariantMapper;

    @InjectMocks
    ProductVariantService productVariantService;

    private Product product;

    private ProductVariant variant;

    private ProductVariantResponse response;

    private ProductVariantCreationRequest createRequest;

    private ProductVariantUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        product = TestDataFactory.createProduct(1L, "TestProduct");

        variant = TestDataFactory.createProductVariant(10L, product, 50);

        variant.setStatus(ProductVariantStatus.INACTIVE);

        response = ProductVariantResponse.builder().id(10L).sku("SKU-10").build();

        createRequest = new ProductVariantCreationRequest();
        createRequest.setSku("SKU-NEW");
        createRequest.setSize("M");
        createRequest.setColor("Red");
        createRequest.setStockQuantity(20);
        createRequest.setPrice(new BigDecimal("100000"));
        createRequest.setSalePrice(new BigDecimal("80000"));

        updateRequest = new ProductVariantUpdateRequest();
        updateRequest.setPrice(new BigDecimal("100000"));
        updateRequest.setSalePrice(new BigDecimal("80000"));
    }

    @Test
    void createProductVariant_whenValid_savesAndReturnsResponse() {
        ProductVariant bareVariant = ProductVariant.builder().id(10L).sku("SKU-NEW").build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        when(productVariantRepository.existsBySku("SKU-NEW")).thenReturn(false);

        when(productVariantMapper.toProductVariant(createRequest)).thenReturn(bareVariant);

        when(productVariantRepository.save(bareVariant)).thenReturn(bareVariant);

        when(productVariantMapper.toProductVariantResponse(bareVariant)).thenReturn(response);

        var result = productVariantService.createProductVariant(1L, createRequest);

        assertThat(result).isEqualTo(response);

        assertThat(bareVariant.getProduct()).isEqualTo(product);

        verify(productVariantRepository).save(bareVariant);

        verify(productVariantMapper).toProductVariant(createRequest);
    }

    @Test
    void createProductVariant_whenProductNotFound_throwsProductNotFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productVariantService.createProductVariant(1L, createRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);

        verify(productVariantRepository, never()).save(any());

        verify(productVariantRepository, never()).existsBySku(anyString());
    }

    @Test
    void createProductVariant_whenSkuExists_throwsSkuAlreadyExists() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        when(productVariantRepository.existsBySku("SKU-NEW")).thenReturn(true);

        assertThatThrownBy(() -> productVariantService.createProductVariant(1L, createRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_VARIANT_SKU_ALREADY_EXISTS);

        verify(productVariantRepository, never()).save(any());

        verify(productVariantMapper, never()).toProductVariant(any());
    }

    @Test
    void createProductVariant_whenSalePriceEqualsPrice_throwsInvalidSalePrice() {
        createRequest.setSalePrice(new BigDecimal("100000"));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        when(productVariantRepository.existsBySku("SKU-NEW")).thenReturn(false);

        assertThatThrownBy(() -> productVariantService.createProductVariant(1L, createRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SALE_PRICE);

        verify(productVariantRepository, never()).save(any());

        verify(productVariantMapper, never()).toProductVariant(any());
    }

    @Test
    void createProductVariant_whenSalePriceGreaterThanPrice_throwsInvalidSalePrice() {
        createRequest.setSalePrice(new BigDecimal("150000"));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        when(productVariantRepository.existsBySku("SKU-NEW")).thenReturn(false);

        assertThatThrownBy(() -> productVariantService.createProductVariant(1L, createRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SALE_PRICE);

        verify(productVariantRepository, never()).save(any());
    }

    @Test
    void createProductVariant_whenSalePriceNull_isAccepted() {
        createRequest.setSalePrice(null);

        ProductVariant bareVariant = ProductVariant.builder().id(10L).sku("SKU-NEW").build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        when(productVariantRepository.existsBySku("SKU-NEW")).thenReturn(false);

        when(productVariantMapper.toProductVariant(createRequest)).thenReturn(bareVariant);

        when(productVariantRepository.save(bareVariant)).thenReturn(bareVariant);

        when(productVariantMapper.toProductVariantResponse(bareVariant)).thenReturn(response);

        var result = productVariantService.createProductVariant(1L, createRequest);

        assertThat(result).isEqualTo(response);

        verify(productVariantRepository).save(bareVariant);
    }

    @Test
    void getProductVariantsByProductId_whenProductExists_returnsList() {
        when(productRepository.existsById(1L)).thenReturn(true);

        when(productVariantRepository.findByProductId(1L)).thenReturn(List.of(variant));

        when(productVariantMapper.toProductVariantResponse(variant)).thenReturn(response);

        var result = productVariantService.getProductVariantsByProductId(1L);

        assertThat(result).hasSize(1).containsExactly(response);

        verify(productVariantRepository).findByProductId(1L);
    }

    @Test
    void getProductVariantsByProductId_whenProductNotFound_throwsProductNotFound() {
        when(productRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> productVariantService.getProductVariantsByProductId(999L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);

        verify(productVariantRepository, never()).findByProductId(any());
    }

    @Test
    void getProductVariantsByProductId_whenEmpty_returnsEmptyList() {
        when(productRepository.existsById(1L)).thenReturn(true);

        when(productVariantRepository.findByProductId(1L)).thenReturn(List.of());

        var result = productVariantService.getProductVariantsByProductId(1L);

        assertThat(result).isEmpty();

        verify(productVariantMapper, never()).toProductVariantResponse(any());
    }

    @Test
    void getActiveVariantsByProductId_queriesWithActiveStatus() {
        when(productRepository.existsById(1L)).thenReturn(true);

        when(productVariantRepository.findByProductIdAndStatus(1L, ProductVariantStatus.ACTIVE))
                .thenReturn(List.of(variant));

        when(productVariantMapper.toProductVariantResponse(variant)).thenReturn(response);

        var result = productVariantService.getActiveVariantsByProductId(1L);

        assertThat(result).hasSize(1).containsExactly(response);

        verify(productVariantRepository).findByProductIdAndStatus(1L, ProductVariantStatus.ACTIVE);
    }

    @Test
    void getActiveVariantsByProductId_whenProductNotFound_throwsProductNotFound() {
        when(productRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> productVariantService.getActiveVariantsByProductId(999L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);

        verify(productVariantRepository, never()).findByProductIdAndStatus(any(), any());
    }

    @Test
    void getActiveVariantsByProductId_whenEmpty_returnsEmptyList() {
        when(productRepository.existsById(1L)).thenReturn(true);

        when(productVariantRepository.findByProductIdAndStatus(1L, ProductVariantStatus.ACTIVE))
                .thenReturn(List.of());

        var result = productVariantService.getActiveVariantsByProductId(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void getProductVariantById_whenExists_returnsResponse() {
        when(productVariantRepository.findById(10L)).thenReturn(Optional.of(variant));

        when(productVariantMapper.toProductVariantResponse(variant)).thenReturn(response);

        var result = productVariantService.getProductVariantById(10L);

        assertThat(result).isEqualTo(response);

        verify(productVariantMapper).toProductVariantResponse(variant);
    }

    @Test
    void getProductVariantById_whenNotFound_throwsVariantNotFound() {
        when(productVariantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productVariantService.getProductVariantById(999L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_VARIANT_NOT_FOUND);

        verify(productVariantMapper, never()).toProductVariantResponse(any());
    }

    @Test
    void updateProductVariant_whenValid_callsMapperUpdateAndSaves() {
        when(productVariantRepository.findById(10L)).thenReturn(Optional.of(variant));

        when(productVariantRepository.save(variant)).thenReturn(variant);

        when(productVariantMapper.toProductVariantResponse(variant)).thenReturn(response);

        var result = productVariantService.updateProductVariant(10L, updateRequest);

        assertThat(result).isEqualTo(response);

        verify(productVariantMapper).updateProductVariant(variant, updateRequest);

        verify(productVariantRepository).save(variant);
    }

    @Test
    void updateProductVariant_whenNotFound_throwsVariantNotFound() {
        when(productVariantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productVariantService.updateProductVariant(999L, updateRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_VARIANT_NOT_FOUND);

        verify(productVariantMapper, never()).updateProductVariant(any(), any());

        verify(productVariantRepository, never()).save(any());
    }

    @Test
    void updateProductVariant_whenSalePriceInvalid_throwsInvalidSalePrice() {
        updateRequest.setPrice(new BigDecimal("100000"));

        updateRequest.setSalePrice(new BigDecimal("100000"));

        when(productVariantRepository.findById(10L)).thenReturn(Optional.of(variant));

        assertThatThrownBy(() -> productVariantService.updateProductVariant(10L, updateRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SALE_PRICE);

        verify(productVariantMapper, never()).updateProductVariant(any(), any());
        verify(productVariantRepository, never()).save(any());
    }

    @Test
    void activateProductVariant_whenInactive_setsActiveAndSaves() {
        variant.setStatus(ProductVariantStatus.INACTIVE);
        when(productVariantRepository.findById(10L)).thenReturn(Optional.of(variant));

        productVariantService.activateProductVariant(10L);

        assertThat(variant.getStatus()).isEqualTo(ProductVariantStatus.ACTIVE);
        verify(productVariantRepository).save(variant);
    }

    @Test
    void activateProductVariant_whenAlreadyActive_throwsAlreadyActive() {
        variant.setStatus(ProductVariantStatus.ACTIVE);

        when(productVariantRepository.findById(10L)).thenReturn(Optional.of(variant));

        assertThatThrownBy(() -> productVariantService.activateProductVariant(10L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_VARIANT_ALREADY_ACTIVE);

        verify(productVariantRepository, never()).save(any());
    }

    @Test
    void activateProductVariant_whenNotFound_throwsNotFound() {
        when(productVariantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productVariantService.activateProductVariant(999L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_VARIANT_NOT_FOUND);

        verify(productVariantRepository, never()).save(any());
    }

    @Test
    void deactivateProductVariant_whenActive_setsInactiveAndSaves() {
        variant.setStatus(ProductVariantStatus.ACTIVE);

        when(productVariantRepository.findById(10L)).thenReturn(Optional.of(variant));

        productVariantService.deactivateProductVariant(10L);

        assertThat(variant.getStatus()).isEqualTo(ProductVariantStatus.INACTIVE);

        verify(productVariantRepository).save(variant);
    }

    @Test
    void deactivateProductVariant_whenAlreadyInactive_throwsAlreadyInactive() {
        variant.setStatus(ProductVariantStatus.INACTIVE);

        when(productVariantRepository.findById(10L)).thenReturn(Optional.of(variant));

        assertThatThrownBy(() -> productVariantService.deactivateProductVariant(10L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_VARIANT_ALREADY_INACTIVE);

        verify(productVariantRepository, never()).save(any());
    }

    @Test
    void deactivateProductVariant_whenNotFound_throwsNotFound() {
        when(productVariantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productVariantService.deactivateProductVariant(999L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_VARIANT_NOT_FOUND);

        verify(productVariantRepository, never()).save(any());
    }

    @Test
    void deleteProductVariant_whenNotInUse_deletes() {
        when(productVariantRepository.findById(10L)).thenReturn(Optional.of(variant));

        when(cartItemRepository.existsByProductVariantId(10L)).thenReturn(false);

        when(orderItemRepository.existsByProductVariantId(10L)).thenReturn(false);

        productVariantService.deleteProductVariant(10L);

        verify(productVariantRepository).delete(variant);
    }

    @Test
    void deleteProductVariant_whenUsedInCart_throwsVariantInUse() {
        when(productVariantRepository.findById(10L)).thenReturn(Optional.of(variant));

        when(cartItemRepository.existsByProductVariantId(10L)).thenReturn(true);

        when(orderItemRepository.existsByProductVariantId(10L)).thenReturn(false);

        assertThatThrownBy(() -> productVariantService.deleteProductVariant(10L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_VARIANT_IN_USE);

        verify(productVariantRepository, never()).delete(any());
    }

    @Test
    void deleteProductVariant_whenUsedInOrder_throwsVariantInUse() {
        when(productVariantRepository.findById(10L)).thenReturn(Optional.of(variant));

        when(cartItemRepository.existsByProductVariantId(10L)).thenReturn(false);

        when(orderItemRepository.existsByProductVariantId(10L)).thenReturn(true);

        assertThatThrownBy(() -> productVariantService.deleteProductVariant(10L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_VARIANT_IN_USE);

        verify(productVariantRepository, never()).delete(any());
    }

    @Test
    void deleteProductVariant_whenUsedInBoth_throwsVariantInUse() {
        when(productVariantRepository.findById(10L)).thenReturn(Optional.of(variant));

        when(cartItemRepository.existsByProductVariantId(10L)).thenReturn(true);

        when(orderItemRepository.existsByProductVariantId(10L)).thenReturn(true);

        assertThatThrownBy(() -> productVariantService.deleteProductVariant(10L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_VARIANT_IN_USE);

        verify(productVariantRepository, never()).delete(any());
    }

    @Test
    void deleteProductVariant_whenNotFound_throwsNotFound() {
        when(productVariantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productVariantService.deleteProductVariant(999L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_VARIANT_NOT_FOUND);

        verify(cartItemRepository, never()).existsByProductVariantId(any());

        verify(orderItemRepository, never()).existsByProductVariantId(any());

        verify(productVariantRepository, never()).delete(any());
    }
}