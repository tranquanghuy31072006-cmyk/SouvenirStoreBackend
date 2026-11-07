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
import com.learning.souvenirstorebackend.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductVariantService {

    ProductVariantRepository productVariantRepository;
    ProductRepository productRepository;
    CartItemRepository cartItemRepository;
    OrderItemRepository orderItemRepository;
    ProductVariantMapper productVariantMapper;

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ProductVariantResponse createProductVariant(Long productId, ProductVariantCreationRequest request) {
        Product product = productRepository.findById(productId).orElseThrow(()
                -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (productVariantRepository.existsBySku(request.getSku()))
            throw new AppException(ErrorCode.PRODUCT_VARIANT_SKU_ALREADY_EXISTS);

        if (request.getSalePrice() != null && request.getSalePrice().compareTo(request.getPrice()) >= 0)
            throw new AppException(ErrorCode.INVALID_SALE_PRICE);

        ProductVariant productVariant = productVariantMapper.toProductVariant(request);

        productVariant.setProduct(product);

        return productVariantMapper.toProductVariantResponse(productVariantRepository.save(productVariant));
    }

    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getProductVariantsByProductId(Long productId) {
        if (!productRepository.existsById(productId))
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);

        return productVariantRepository.findByProductId(productId).stream()
                .map(productVariantMapper::toProductVariantResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getActiveVariantsByProductId(Long productId) {
        if (!productRepository.existsById(productId))
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);

        return productVariantRepository.findByProductIdAndStatus(productId, ProductVariantStatus.ACTIVE).stream()
                .map(productVariantMapper::toProductVariantResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductVariantResponse getProductVariantById(Long productVariantId) {
        ProductVariant productVariant = productVariantRepository.findById(productVariantId).orElseThrow(()
                -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        return productVariantMapper.toProductVariantResponse(productVariant);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ProductVariantResponse updateProductVariant(Long productVariantId, ProductVariantUpdateRequest request) {
        ProductVariant productVariant = productVariantRepository.findById(productVariantId).orElseThrow(()
                -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        if (request.getSalePrice() != null && request.getSalePrice().compareTo(request.getPrice()) >= 0)
            throw new AppException(ErrorCode.INVALID_SALE_PRICE);

        productVariantMapper.updateProductVariant(productVariant, request);

        return productVariantMapper.toProductVariantResponse(productVariantRepository.save(productVariant));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void activateProductVariant(Long productVariantId) {
        ProductVariant productVariant = productVariantRepository.findById(productVariantId).orElseThrow(()
                -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        if (productVariant.getStatus().equals(ProductVariantStatus.ACTIVE))
            throw new AppException(ErrorCode.PRODUCT_VARIANT_ALREADY_ACTIVE);

        productVariant.setStatus(ProductVariantStatus.ACTIVE);

        productVariantRepository.save(productVariant);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deactivateProductVariant(Long productVariantId) {
        ProductVariant productVariant = productVariantRepository.findById(productVariantId).orElseThrow(()
                -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        if (productVariant.getStatus().equals(ProductVariantStatus.INACTIVE))
            throw new AppException(ErrorCode.PRODUCT_VARIANT_ALREADY_INACTIVE);

        productVariant.setStatus(ProductVariantStatus.INACTIVE);

        productVariantRepository.save(productVariant);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteProductVariant(Long productVariantId) {
        ProductVariant productVariant = productVariantRepository.findById(productVariantId).orElseThrow(()
                -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        boolean inCart = cartItemRepository.existsByProductVariantId(productVariantId);

        boolean inOrder = orderItemRepository.existsByProductVariantId(productVariantId);

        if (inCart || inOrder)
            throw new AppException(ErrorCode.PRODUCT_VARIANT_IN_USE);

        productVariantRepository.delete(productVariant);
    }
}
