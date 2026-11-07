package com.learning.souvenirstorebackend.repository;

import com.learning.souvenirstorebackend.entity.Category;
import com.learning.souvenirstorebackend.enums.CategoryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByStatus(CategoryStatus status);

    boolean existsByName(@NotBlank @Size(max = 100) String name);
}
