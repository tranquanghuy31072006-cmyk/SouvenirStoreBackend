package com.learning.souvenirstorebackend.repository;

import com.learning.souvenirstorebackend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, String> {
}
