package com.learning.souvenirstorebackend.service;

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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Builder
public class ProductService {
    ProductRepository productRepository;
    ProductMapper productMapper;
    CategoryRepository categoryRepository;
    BrandRepository brandRepository;

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ProductResponse createProduct(ProductRequest productRequest) {
        Category category = categoryRepository.findById(productRequest.getCategoryId()).
                orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        Brand brand = brandRepository.findById(productRequest.getBrandId()).
                orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));

        if (productRepository.existsByName(productRequest.getName()))
            throw new AppException(ErrorCode.PRODUCT_ALREADY_EXISTS);

        Product product = productMapper.toProduct(productRequest);

        product.setCategory(category);

        product.setBrand(brand);

        product.setThumbnail(productRequest.getThumbnail());

        if (productRequest.getProductImages() != null && !productRequest.getProductImages().isEmpty()) {
            Product finalProduct = product;
            List<ProductImage> images = productRequest.getProductImages().stream()
                    .map(req -> ProductImage.builder()
                            .product(finalProduct)
                            .imageUrl(req.getImageUrl())
                            .sortOrder(req.getSortOrder())
                            .build())
                    .toList();
            product.setProductImages(images);
        }

        try {
            product = productRepository.save(product);
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.PRODUCT_ALREADY_EXISTS);
        }

        return productMapper.toProductResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id).orElseThrow(()
                -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        return productMapper.toProductResponse(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        List<Product> products = productRepository.findAll();

        return productMapper.toProductResponseList(products);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ProductResponse updateProduct(Long id, ProductRequest productRequest) {
        Product product = productRepository.findById(id).orElseThrow(()
                -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        productMapper.updateProduct(product, productRequest);

        return productMapper.toProductResponse(productRepository.save(product));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse activateProductStatus(Long id) {
        Product product = productRepository.findById(id).orElseThrow(()
                -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        product.setStatus(ProductStatus.ACTIVE);

        return productMapper.toProductResponse(productRepository.save(product));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse deactivateProductStatus(Long id) {
        Product product = productRepository.findById(id).orElseThrow(()
                -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        product.setStatus(ProductStatus.INACTIVE);

        return productMapper.toProductResponse(productRepository.save(product));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        productRepository.delete(product);
    }
}
