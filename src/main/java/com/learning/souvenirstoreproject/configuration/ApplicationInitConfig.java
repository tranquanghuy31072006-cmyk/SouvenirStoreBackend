package com.learning.souvenirstoreproject.configuration;

import com.learning.souvenirstoreproject.entity.Role;
import com.learning.souvenirstoreproject.entity.User;
import com.learning.souvenirstoreproject.repository.RoleRepository;
import com.learning.souvenirstoreproject.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ApplicationInitConfig {

    UserRepository userRepository;
    RoleRepository roleRepository;
    PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initDefaultUsers() {
        return args -> {
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
        };
    }

    private void createDefaultUsers(String roleName, String userName, String password, String email,
                                    String fullName, String phone) {

        long count = userRepository.countByRole_Name(roleName);

        if (count > 1)
            throw new IllegalArgumentException("Database contains more than one " + roleName + " role");

        if (count == 1) {
            return;
        }

        Role role = roleRepository.findById(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role " + roleName + " not found"));

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