package com.learning.souvenirstorebackend.repository;

import com.learning.souvenirstorebackend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsByName(String name);

    boolean existsByCategoryId(Long categoryId);
}
