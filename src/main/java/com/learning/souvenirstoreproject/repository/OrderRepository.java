package com.learning.souvenirstoreproject.repository;

import com.learning.souvenirstoreproject.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long customerId);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    List<Order> findAllByOrderByCreatedAtDesc();

    @Query("SELECT COUNT(o) > 0 FROM Order o JOIN o.orderItems oi JOIN oi.productVariant pv " +
            "WHERE o.user.id = :userId AND o.orderStatus = 'COMPLETED' AND pv.product.id = :productId")
    boolean hasUserPurchasedProduct(@Param("userId") Long userId, @Param("productId") Long productId);
}
