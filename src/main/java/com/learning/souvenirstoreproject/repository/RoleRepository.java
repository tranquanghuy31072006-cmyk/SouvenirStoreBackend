package com.learning.souvenirstoreproject.repository;

import com.learning.souvenirstoreproject.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, String> {
}
