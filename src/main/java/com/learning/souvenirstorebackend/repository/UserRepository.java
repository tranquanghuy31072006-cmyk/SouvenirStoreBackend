package com.learning.souvenirstorebackend.repository;

import com.learning.souvenirstorebackend.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    boolean existsUserByUsername(String username);

    Optional<User> findByUsername(String username);

    boolean existsUserByPhone(@NotBlank @Size(max = 10) String phone);

    long countByRole_Name(String roleName);
}
