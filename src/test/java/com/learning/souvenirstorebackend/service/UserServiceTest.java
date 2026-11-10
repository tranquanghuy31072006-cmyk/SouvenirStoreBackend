package com.learning.souvenirstorebackend.service;

import com.learning.souvenirstorebackend.dto.request.UserCreationRequest;
import com.learning.souvenirstorebackend.dto.request.UserPasswordUpdateRequest;
import com.learning.souvenirstorebackend.dto.request.UserUpdateRequest;
import com.learning.souvenirstorebackend.dto.response.UserResponse;
import com.learning.souvenirstorebackend.entity.RefreshToken;
import com.learning.souvenirstorebackend.entity.Role;
import com.learning.souvenirstorebackend.entity.User;
import com.learning.souvenirstorebackend.enums.UserStatus;
import com.learning.souvenirstorebackend.exception.AppException;
import com.learning.souvenirstorebackend.exception.ErrorCode;
import com.learning.souvenirstorebackend.mapper.UserMapper;
import com.learning.souvenirstorebackend.repository.RefreshTokenRepository;
import com.learning.souvenirstorebackend.repository.RoleRepository;
import com.learning.souvenirstorebackend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock UserMapper userMapper;

    @InjectMocks UserService userService;

    private Role customerRole;
    private User user;
    private UserResponse response;
    private UserCreationRequest createRequest;
    private UserUpdateRequest updateRequest;
    private UserPasswordUpdateRequest passwordRequest;

    @BeforeEach
    void setUp() {
        Role adminRole = Role.builder().name("ADMIN").build();
        customerRole = Role.builder().name("CUSTOMER").build();

        user = User.builder()
                .id(1L)
                .username("testUser")
                .password("encodedPassword")
                .email("test@example.com")
                .phone("0901234567")
                .status(UserStatus.ACTIVE)
                .role(adminRole)
                .build();

        response = UserResponse.builder()
                .id(1L)
                .username("testUser")
                .email("test@example.com")
                .build();

        createRequest = UserCreationRequest.builder()
                .username("newUser")
                .password("rawPassword")
                .email("new@example.com")
                .phone("0900000000")
                .build();

        updateRequest = new UserUpdateRequest();

        passwordRequest = new UserPasswordUpdateRequest();

        passwordRequest.setOldPassword("oldPassword");

        passwordRequest.setNewPassword("newPassword");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of()));
    }


    @Test
    void loadUserByUsername_whenExists_returnsUserDetails() {
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));

        UserDetails result = userService.loadUserByUsername("testUser");

        assertThat(result.getUsername()).isEqualTo("testUser");

        assertThat(result.getPassword()).isEqualTo("encodedPassword");

        assertThat(result.isEnabled()).isTrue();

        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void loadUserByUsername_whenBlocked_disablesAccount() {
        user.setStatus(UserStatus.BLOCKED);

        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));

        UserDetails result = userService.loadUserByUsername("testUser");

        assertThat(result.isEnabled()).isFalse();
    }

    @Test
    void loadUserByUsername_whenNotFound_throwsUsernameNotFound() {
        when(userRepository.findByUsername("ghostUser")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername("ghostUser"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void createUser_whenValid_savesAndReturnsResponse() {
        User mappedUser = User.builder().username("newUser").password("rawPassword").build();

        User encodedUser = User.builder().id(2L).username("newUser").password("encodedPassword").build();

        when(userRepository.existsUserByUsername("newUser")).thenReturn(false);

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        when(userRepository.existsUserByPhone("0900000000")).thenReturn(false);

        when(userMapper.toUser(any(UserCreationRequest.class))).thenReturn(mappedUser);

        when(roleRepository.findById("CUSTOMER")).thenReturn(Optional.of(customerRole));

        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");

        when(userRepository.save(mappedUser)).thenReturn(encodedUser);

        when(userMapper.toUserResponse(encodedUser)).thenReturn(response);

        var result = userService.createUser(createRequest);

        assertThat(result).isEqualTo(response);

        assertThat(mappedUser.getRole()).isEqualTo(customerRole);

        assertThat(mappedUser.getPassword()).isEqualTo("encodedPassword");

        verify(userRepository).save(mappedUser);
    }

    @Test
    void createUser_whenUsernameExists_throwsUserAlreadyExists() {
        when(userRepository.existsUserByUsername("newUser")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(createRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_ALREADY_EXISTS);

        verify(userRepository, never()).save(any());

        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    void createUser_whenEmailExists_throwsEmailAlreadyExists() {
        when(userRepository.existsUserByUsername("newUser")).thenReturn(false);

        when(userRepository.existsByEmail("new@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(createRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_ALREADY_EXISTS);

        verify(userRepository, never()).save(any());

        verify(userRepository, never()).existsUserByPhone(anyString());
    }

    @Test
    void createUser_whenPhoneExists_throwsPhoneAlreadyExists() {
        when(userRepository.existsUserByUsername("newUser")).thenReturn(false);

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        when(userRepository.existsUserByPhone("0900000000")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(createRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PHONE_ALREADY_EXISTS);

        verify(userRepository, never()).save(any());

        verify(userMapper, never()).toUser(any(UserCreationRequest.class));
    }

    @Test
    void createUser_whenRoleNotFound_throwsRoleNotFound() {
        User mappedUser = User.builder().username("newUser").password("rawPassword").build();

        when(userRepository.existsUserByUsername("newUser")).thenReturn(false);

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        when(userRepository.existsUserByPhone("0900000000")).thenReturn(false);

        when(userMapper.toUser(any(UserCreationRequest.class))).thenReturn(mappedUser);

        when(roleRepository.findById("CUSTOMER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.createUser(createRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROLE_NOT_FOUND);

        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_whenSaveThrowsDataIntegrityViolation_throwsUserAlreadyExists() {
        User mappedUser = User.builder().username("newUser").password("rawPassword").build();

        when(userRepository.existsUserByUsername("newUser")).thenReturn(false);

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        when(userRepository.existsUserByPhone("0900000000")).thenReturn(false);

        when(userMapper.toUser(any(UserCreationRequest.class))).thenReturn(mappedUser);

        when(roleRepository.findById("CUSTOMER")).thenReturn(Optional.of(customerRole));

        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");

        when(userRepository.save(mappedUser))
                .thenThrow(new DataIntegrityViolationException("duplicate constraint"));

        assertThatThrownBy(() -> userService.createUser(createRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_ALREADY_EXISTS);

        verify(userRepository, times(1)).save(mappedUser);
    }

    @Test
    void getUserById_whenExists_returnsResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(userMapper.toUserResponse(user)).thenReturn(response);

        var result = userService.getUserById(1L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getUserById_whenNotFound_throwsUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void updateUser_whenExists_callsMapperUpdateAndSaves() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(userRepository.save(user)).thenReturn(user);

        when(userMapper.toUserResponse(user)).thenReturn(response);

        var result = userService.updateUser(1L, updateRequest);

        assertThat(result).isEqualTo(response);

        verify(userMapper).updateUser(user, updateRequest);

        verify(userRepository).save(user);
    }

    @Test
    void updateUser_whenNotFound_throwsUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(999L, updateRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(userRepository, never()).save(any());
    }


    @Test
    void getAllUsers_returnsMappedList() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        when(userMapper.toUserResponse(user)).thenReturn(response);

        var result = userService.getAllUsers();

        assertThat(result).hasSize(1).containsExactly(response);

        verify(userMapper).toUserResponse(user);
    }

    @Test
    void getAllUsers_whenEmpty_returnsEmptyList() {
        when(userRepository.findAll()).thenReturn(List.of());

        var result = userService.getAllUsers();

        assertThat(result).isEmpty();

        verify(userMapper, never()).toUserResponse(any());
    }


    @Test
    void deleteUser_alwaysCallsDeleteById() {
        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);

        verify(userRepository, never()).findById(any());
    }


    @Test
    void getMyInfo_whenAuthenticated_returnsResponse() {
        loginAs("testUser");

        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));

        when(userMapper.toUserResponse(user)).thenReturn(response);

        var result = userService.getMyInfo();

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getMyInfo_whenUserNotFound_throwsUserNotFound() {
        loginAs("ghostUser");

        when(userRepository.findByUsername("ghostUser")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyInfo())
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void changePassword_whenOldPasswordCorrect_encodesAndSaves() {
        loginAs("testUser");

        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));

        when(passwordEncoder.matches("oldPassword", "encodedPassword"))
                .thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");

        userService.changePassword(passwordRequest);

        assertThat(user.getPassword()).isEqualTo("encodedNewPassword");

        verify(userRepository).save(user);
    }

    @Test
    void changePassword_whenUserNotFound_throwsUserNotFound() {
        loginAs("ghostUser");

        when(userRepository.findByUsername("ghostUser")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changePassword(passwordRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_whenOldPasswordWrong_throwsPasswordIncorrect() {
        loginAs("testUser");

        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));

        when(passwordEncoder.matches("oldPassword", "encodedPassword"))
                .thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(passwordRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PASSWORD_INCORRECT);

        verify(passwordEncoder, never()).encode(anyString());

        verify(userRepository, never()).save(any());
    }


    @Test
    void activateUserStatus_whenInactive_setsActiveAndSaves() {
        user.setStatus(UserStatus.BLOCKED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(userRepository.save(user)).thenReturn(user);

        when(userMapper.toUserResponse(user)).thenReturn(response);

        var result = userService.activateUserStatus(1L);

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);

        assertThat(result).isEqualTo(response);

        verify(userRepository).save(user);
    }

    @Test
    void activateUserStatus_whenAlreadyActive_throwsUserAlreadyActive() {
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.activateUserStatus(1L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_ALREADY_ACTIVE);

        verify(userRepository, never()).save(any());
    }

    @Test
    void activateUserStatus_whenNotFound_throwsUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.activateUserStatus(999L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }


    @Test
    void deactivateUserStatus_whenActive_setsBlockedAndRevokesTokens() {
        user.setStatus(UserStatus.ACTIVE);

        RefreshToken token1 = RefreshToken.builder().id(1L).revoked(false).build();

        RefreshToken token2 = RefreshToken.builder().id(2L).revoked(false).build();

        List<RefreshToken> tokens = List.of(token1, token2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(refreshTokenRepository.findAllByUser_Id(1L)).thenReturn(tokens);

        when(userMapper.toUserResponse(user)).thenReturn(response);

        var result = userService.deactivateUserStatus(1L);

        assertThat(user.getStatus()).isEqualTo(UserStatus.BLOCKED);

        assertThat(result).isEqualTo(response);

        assertThat(token1.isRevoked()).isTrue();

        assertThat(token2.isRevoked()).isTrue();

        verify(userRepository).save(user);

        verify(refreshTokenRepository).saveAll(tokens);
    }

    @Test
    void deactivateUserStatus_whenAlreadyBlocked_throwsUserAlreadyBlocked() {
        user.setStatus(UserStatus.BLOCKED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.deactivateUserStatus(1L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_ALREADY_BLOCKED);

        verify(userRepository, never()).save(any());

        verify(refreshTokenRepository, never()).saveAll(any());
    }

    @Test
    void deactivateUserStatus_whenNotFound_throwsUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deactivateUserStatus(999L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(refreshTokenRepository, never()).findAllByUser_Id(any());
    }
}