package com.learning.souvenirstorebackend.service;

import com.learning.souvenirstorebackend.dto.request.UserCreationRequest;
import com.learning.souvenirstorebackend.entity.Role;
import com.learning.souvenirstorebackend.entity.User;
import com.learning.souvenirstorebackend.mapper.UserMapper;
import com.learning.souvenirstorebackend.repository.RefreshTokenRepository;
import com.learning.souvenirstorebackend.repository.RoleRepository;
import com.learning.souvenirstorebackend.repository.UserRepository;
import com.learning.souvenirstorebackend.support.SecurityTestHelper;
import com.learning.souvenirstorebackend.support.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    RoleRepository roleRepository;

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Mock
    UserMapper userMapper;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserService userService;

    private User user;
    private Role customerRole;

    @BeforeEach
    public void setUp() {
        customerRole = Role.builder().name("CUSTOMER").build();
        user = TestDataFactory.createUser("test user", "CUSTOMER");

        user.setId(1L);
        SecurityTestHelper.mockCurrentUser("test user", "CUSTOMER");
    }

    @AfterEach
    public void tearDown() {
        SecurityTestHelper.clearSecurityContext();
    }

    @Test
    void createUser_whenValid_hashPasswordAndSave(){
        UserCreationRequest request = new UserCreationRequest();

        request.setUsername("new_user");
        request.setPassword("password123");
        request.setEmail("new@gmail.com");
        request.setPhone("0999999999");

        when(roleRepository.findById("CUSTOMER")).thenReturn(Optional.of(customerRole));

        when(userRepository.existsUserByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsUserByPhone(anyString())).thenReturn(false);

        when(passwordEncoder.encode("password123")).thenReturn("hashed_password");

        when(userMapper.toUser(request)).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.createUser(request);

        verify(passwordEncoder, times(1)).encode("password123");

        assertThat(user.getPassword()).isEqualTo("hashed_password");
        assertThat(user.getRole().getName()).isEqualTo("CUSTOMER");
    }
}
