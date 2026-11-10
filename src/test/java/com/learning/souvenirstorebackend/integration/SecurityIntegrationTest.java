package com.learning.souvenirstorebackend.integration;

import com.learning.souvenirstorebackend.entity.Role;
import com.learning.souvenirstorebackend.entity.User;
import com.learning.souvenirstorebackend.enums.UserStatus;
import com.learning.souvenirstorebackend.repository.RoleRepository;
import com.learning.souvenirstorebackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class SecurityIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    private static final String RAW_PASSWORD = "password123";

    private String adminToken;
    private String staffToken;
    private String customerToken;

    @BeforeEach
    void setUp() throws Exception {
        Role adminRole = roleRepository.findById("ADMIN").orElseGet(() ->
                roleRepository.save(Role.builder().name("ADMIN").build()));
        Role staffRole = roleRepository.findById("STAFF").orElseGet(() ->
                roleRepository.save(Role.builder().name("STAFF").build()));
        Role customerRole = roleRepository.findById("CUSTOMER").orElseGet(() ->
                roleRepository.save(Role.builder().name("CUSTOMER").build()));

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        adminToken = seedAndLogin("si_admin_" + suffix, adminRole);
        staffToken = seedAndLogin("si_staff_" + suffix, staffRole);
        customerToken = seedAndLogin("si_custom_" + suffix, customerRole);
    }

    private String seedAndLogin(String username, Role role) throws Exception {
        userRepository.save(User.builder()
                .username(username)
                .password(passwordEncoder.encode(RAW_PASSWORD))
                .email(username + "@test.local")
                .phone("09" + Math.abs(username.hashCode()))
                .role(role).status(UserStatus.ACTIVE).build());

        MvcResult r = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn();

        return new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(r.getResponse().getContentAsString())
                .path("result").path("accessToken").asText();
    }

    private String bearer(String t) {
        return "Bearer " + t;
    }

    @Test
    void publicProductList_noToken_returns2xx() throws Exception {
        mockMvc.perform(get("/products")).andExpect(status().is2xxSuccessful());
    }

    @Test
    void authLogin_noToken_notBlockedBySecurity() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpoint_noToken_returns401() throws Exception {
        mockMvc.perform(get("/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminOnly_calledByAdmin_returns2xx() throws Exception {
        mockMvc.perform(get("/users").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void adminOnly_calledByCustomer_returns403() throws Exception {
        mockMvc.perform(get("/users").header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminOrStaff_calledByStaff_returns2xx() throws Exception {
        mockMvc.perform(get("/orders/manage").header(HttpHeaders.AUTHORIZATION, bearer(staffToken)))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void adminOrStaff_calledByCustomer_returns403() throws Exception {
        mockMvc.perform(get("/orders/manage").header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerRole_mapsToRoleCustomerAuthority() throws Exception {
        mockMvc.perform(get("/cart").header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void cors_fromAllowedOrigin_returnsCorsHeaders() throws Exception {
        mockMvc.perform(options("/products")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void cors_fromDisallowedOrigin_noAllowOriginHeader() throws Exception {
        mockMvc.perform(options("/products")
                        .header(HttpHeaders.ORIGIN, "https://evil.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}