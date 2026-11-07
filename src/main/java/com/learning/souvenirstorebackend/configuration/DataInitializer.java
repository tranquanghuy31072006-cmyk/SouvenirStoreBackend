package com.learning.souvenirstorebackend.configuration;

import com.learning.souvenirstorebackend.entity.Role;
import com.learning.souvenirstorebackend.entity.User;
import com.learning.souvenirstorebackend.repository.RoleRepository;
import com.learning.souvenirstorebackend.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DataInitializer implements ApplicationRunner {

    RoleRepository roleRepository;
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        initRoles();
        initDefaultUsers();
    }

    private void initRoles() {
        createRoleIfNotExists("ADMIN", "Admin");
        createRoleIfNotExists("STAFF", "Staff");
        createRoleIfNotExists("CUSTOMER", "Customer");
    }

    private void createRoleIfNotExists(String name, String description) {
        if (roleRepository.findById(name).isEmpty()) {
            Role role = Role.builder()
                    .name(name)
                    .description(description)
                    .build();

            roleRepository.save(role);

            log.info("Role {} has been created", name);
        }
    }

    private void initDefaultUsers() {
        createDefaultUsers(
                "ADMIN",
                "admin",
                "admin@12345",
                "admin@souvenirstore.com",
                "System Administrator",
                "0988888888"
        );
        createDefaultUsers(
                "STAFF",
                "staff",
                "staff@12345",
                "staff@souvenirstore.com",
                "Store Staff",
                "0983868386"
        );
    }

    private void createDefaultUsers(String roleName, String userName, String password, String email,
                                    String fullName, String phone) {

        long count = userRepository.countByRole_Name(roleName);

        if (count > 1)
            throw new IllegalArgumentException("Database contains more than one " + roleName + " role");

        if (count == 1) {
            return;
        }

        Role role = roleRepository.findById(roleName).orElseThrow(()
                -> new IllegalArgumentException("Role " + roleName + " not found"));

        User user = User.builder()
                .role(role)
                .username(userName)
                .password(passwordEncoder.encode(password))
                .email(email)
                .fullName(fullName)
                .phone(phone)
                .build();

        userRepository.save(user);

        log.warn("User with role name: {} has been created with default password, Please change it", roleName);
    }
}
