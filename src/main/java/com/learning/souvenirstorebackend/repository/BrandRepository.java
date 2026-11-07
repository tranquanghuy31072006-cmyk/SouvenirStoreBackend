package com.learning.souvenirstorebackend.repository;

import com.learning.souvenirstorebackend.entity.Brand;
import com.learning.souvenirstorebackend.enums.BrandStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    boolean existsByName(String name);

    List<Brand> getBrandByStatus(BrandStatus status);
}
