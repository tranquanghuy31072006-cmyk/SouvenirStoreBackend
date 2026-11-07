package com.learning.souvenirstorebackend.repository;

import com.learning.souvenirstorebackend.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    boolean existsByProductVariantId(Long productVariantId);
}
