package com.learning.souvenirstorebackend.repository;

import com.learning.souvenirstorebackend.entity.ProductVariant;
import com.learning.souvenirstorebackend.enums.ProductVariantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductId(Long productId);

    boolean existsBySku(String sku);

    List<ProductVariant> findByProductIdAndStatus(Long productId, ProductVariantStatus status);
}
