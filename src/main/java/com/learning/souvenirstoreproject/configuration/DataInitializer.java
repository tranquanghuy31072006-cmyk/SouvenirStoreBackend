package com.learning.souvenirstoreproject.configuration;

import com.learning.souvenirstoreproject.entity.Role;
import com.learning.souvenirstoreproject.repository.RoleRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Order(1)
public class DataInitializer implements ApplicationRunner {

    RoleRepository roleRepository;

    static final String ROLE_ADMIN = "ADMIN";
    static final String ROLE_STAFF = "STAFF";
    static final String ROLE_CUSTOMER = "CUSTOMER";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedRoles();
        log.info("Roles initialization completed");
    }

    private void seedRoles() {
        createRolesIfNotExist(ROLE_ADMIN, "Admin");
        createRolesIfNotExist(ROLE_STAFF, "Staff");
        createRolesIfNotExist(ROLE_CUSTOMER, "Customer");
    }

    private void createRolesIfNotExist(String name, String description) {
        if (roleRepository.findById(name).isEmpty()) {
            Role role = Role.builder()
                    .name(name)
                    .description(description)
                    .build();

            roleRepository.save(role);

            log.info("Seeded role: {}", name);
        }
    }
}