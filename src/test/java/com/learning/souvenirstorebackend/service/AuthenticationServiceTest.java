package com.learning.souvenirstorebackend.service;

import com.learning.souvenirstorebackend.configuration.JwtProperties;
import com.learning.souvenirstorebackend.dto.request.LoginRequest;
import com.learning.souvenirstorebackend.dto.request.RegisterRequest;
import com.learning.souvenirstorebackend.entity.InvalidatedToken;
import com.learning.souvenirstorebackend.entity.Role;
import com.learning.souvenirstorebackend.entity.User;
import com.learning.souvenirstorebackend.exception.AppException;
import com.learning.souvenirstorebackend.exception.ErrorCode;
import com.learning.souvenirstorebackend.mapper.UserMapper;
import com.learning.souvenirstorebackend.repository.CartRepository;
import com.learning.souvenirstorebackend.repository.InvalidatedTokenRepository;
import com.learning.souvenirstorebackend.repository.RoleRepository;
import com.learning.souvenirstorebackend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTest {

    private static final String TEST_USERNAME = "testUser";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PHONE = "0999999999";
    private static final long ACCESS_TOKEN_EXPIRATION_SECONDS = 900L;

    @Mock
    UserRepository userRepository;
    @Mock
    RoleRepository roleRepository;
    @Mock
    CartRepository cartRepository;
    @Mock
    InvalidatedTokenRepository invalidatedTokenRepository;
    @Mock
    UserMapper userMapper;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    AuthenticationManager authenticationManager;
    @Mock
    JwtService jwtService;
    @Mock
    RefreshTokenService refreshTokenService;
    @Mock
    JwtProperties jwtProperties;

    @InjectMocks
    AuthenticationService authenticationService;

    @Captor
    ArgumentCaptor<Authentication> authTokenCaptor;
    @Captor
    ArgumentCaptor<InvalidatedToken> invalidatedCaptor;

    private Role customerRole;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        customerRole = Role.builder().name("CUSTOMER").build();

        userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(TEST_USERNAME)
                .password("x")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private RegisterRequest newRegisterRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(TEST_USERNAME);
        req.setPassword(TEST_PASSWORD);
        req.setEmail(TEST_EMAIL);
        req.setPhone(TEST_PHONE);
        return req;
    }

    private User bareUser() {
        return User.builder()
                .username(TEST_USERNAME)
                .password(TEST_PASSWORD)
                .build();
    }

    @Test
    void registerUser_whenValid_hashesPasswordAndCreatesCart() {
        RegisterRequest req = newRegisterRequest();
        User bareUser = bareUser();

        when(userRepository.existsUserByUsername(TEST_USERNAME)).thenReturn(false);
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(userRepository.existsUserByPhone(TEST_PHONE)).thenReturn(false);
        when(roleRepository.findById("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(userMapper.toUser(req)).thenReturn(bareUser);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn("hashed");
        when(userRepository.save(bareUser)).thenReturn(bareUser);

        authenticationService.registerUser(req);

        assertThat(bareUser.getPassword()).isEqualTo("hashed");
        assertThat(bareUser.getRole()).isEqualTo(customerRole);
        verify(userRepository).save(bareUser);
        verify(cartRepository).save(argThat(c -> c.getUser() == bareUser));
    }

    @Test
    void registerUser_whenUsernameExists_throwsUserAlreadyExists() {
        RegisterRequest req = newRegisterRequest();
        when(userRepository.existsUserByUsername(TEST_USERNAME)).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.registerUser(req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_ALREADY_EXISTS);

        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any());
        verify(cartRepository, never()).save(any());
    }

    @Test
    void registerUser_whenEmailExists_throwsEmailAlreadyExists() {
        RegisterRequest req = newRegisterRequest();

        when(userRepository.existsUserByUsername(TEST_USERNAME)).thenReturn(false);

        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.registerUser(req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_ALREADY_EXISTS);

        verify(userRepository, never()).save(any());

        verify(cartRepository, never()).save(any());
    }

    @Test
    void registerUser_whenPhoneExists_throwsPhoneAlreadyExists() {
        RegisterRequest req = newRegisterRequest();

        when(userRepository.existsUserByUsername(TEST_USERNAME)).thenReturn(false);

        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);

        when(userRepository.existsUserByPhone(TEST_PHONE)).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.registerUser(req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PHONE_ALREADY_EXISTS);

        verify(userRepository, never()).save(any());

        verify(cartRepository, never()).save(any());
    }

    @Test
    void registerUser_whenRoleNotFound_throwsRoleNotFound() {
        RegisterRequest req = newRegisterRequest();

        when(userRepository.existsUserByUsername(TEST_USERNAME)).thenReturn(false);

        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);

        when(userRepository.existsUserByPhone(TEST_PHONE)).thenReturn(false);

        when(roleRepository.findById("CUSTOMER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.registerUser(req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROLE_NOT_FOUND);

        verify(userMapper, never()).toUser(any(RegisterRequest.class));

        verify(userRepository, never()).save(any());

        verify(cartRepository, never()).save(any());
    }

    @Test
    void registerUser_whenSaveThrowsDataIntegrityViolation_throwsUserAlreadyExists() {
        RegisterRequest req = newRegisterRequest();

        User bareUser = bareUser();

        when(userRepository.existsUserByUsername(TEST_USERNAME)).thenReturn(false);

        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);

        when(userRepository.existsUserByPhone(TEST_PHONE)).thenReturn(false);

        when(roleRepository.findById("CUSTOMER")).thenReturn(Optional.of(customerRole));

        when(userMapper.toUser(req)).thenReturn(bareUser);

        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn("hashed");

        when(userRepository.save(bareUser)).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> authenticationService.registerUser(req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_ALREADY_EXISTS);

        verify(userRepository, times(1)).save(bareUser);

        verify(cartRepository, never()).save(any());
    }

    @Test
    void login_whenValid_returnsTokenPair() {
        LoginRequest req = new LoginRequest();

        req.setUsername(TEST_USERNAME);

        req.setPassword(TEST_PASSWORD);

        Authentication authResult = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authResult);

        when(jwtService.generateAccessToken(userDetails)).thenReturn("access");

        when(refreshTokenService.createRefreshToken(userDetails)).thenReturn("refresh");

        when(jwtProperties.getAccessTokenExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION_SECONDS);

        var result = authenticationService.login(req);

        assertThat(result.accessToken()).isEqualTo("access");

        assertThat(result.refreshToken()).isEqualTo("refresh");

        assertThat(result.expiresIn()).isEqualTo(ACCESS_TOKEN_EXPIRATION_SECONDS);

        verify(authenticationManager).authenticate(authTokenCaptor.capture());

        assertThat(authTokenCaptor.getValue().getPrincipal()).isEqualTo(TEST_USERNAME);

        assertThat(authTokenCaptor.getValue().getCredentials()).isEqualTo(TEST_PASSWORD);
    }

    @Test
    void login_whenAuthenticationFails_doesNotIssueTokens() {
        LoginRequest req = new LoginRequest();

        req.setUsername(TEST_USERNAME);

        req.setPassword("wrong");

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authenticationService.login(req))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(jwtService);

        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void refresh_whenValid_issuesNewPairFromReturnedUserDetails() {
        when(refreshTokenService.rotateRefreshToken("raw")).thenReturn(userDetails);

        when(jwtService.generateAccessToken(userDetails)).thenReturn("new-access");

        when(refreshTokenService.createRefreshToken(userDetails)).thenReturn("new-refresh");

        when(jwtProperties.getAccessTokenExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION_SECONDS);

        var result = authenticationService.refresh("raw");

        assertThat(result.accessToken()).isEqualTo("new-access");

        assertThat(result.refreshToken()).isEqualTo("new-refresh");

        assertThat(result.expiresIn()).isEqualTo(ACCESS_TOKEN_EXPIRATION_SECONDS);

        verify(refreshTokenService).rotateRefreshToken("raw");
    }

    @Test
    void refresh_whenNull_throwsUnauthenticated() {
        assertThatThrownBy(() -> authenticationService.refresh(null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHENTICATED);

        verifyNoInteractions(refreshTokenService);

        verifyNoInteractions(jwtService);
    }

    @Test
    void refresh_whenBlank_throwsUnauthenticated() {
        assertThatThrownBy(() -> authenticationService.refresh("   "))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHENTICATED);

        verifyNoInteractions(refreshTokenService);

        verifyNoInteractions(jwtService);
    }

    @Test
    void refresh_whenRotateThrows_propagates() {
        when(refreshTokenService.rotateRefreshToken("raw"))
                .thenThrow(new AppException(ErrorCode.USER_BLOCKED));

        assertThatThrownBy(() -> authenticationService.refresh("raw"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_BLOCKED);

        verifyNoInteractions(jwtService);
    }

    @Test
    void logout_whenRefreshTokenPresent_revokesIt() {
        authenticationService.logout("raw");

        verify(refreshTokenService).revokeRefreshToken("raw");

        verifyNoInteractions(invalidatedTokenRepository);
    }

    @Test
    void logout_whenRefreshTokenNull_doesNotRevoke() {
        authenticationService.logout(null);

        verify(refreshTokenService, never()).revokeRefreshToken(any());

        verifyNoInteractions(invalidatedTokenRepository);
    }

    @Test
    void logout_whenRefreshTokenBlank_doesNotRevoke() {
        authenticationService.logout("   ");

        verify(refreshTokenService, never()).revokeRefreshToken(any());

        verifyNoInteractions(invalidatedTokenRepository);
    }

    @Test
    void logout_whenJwtAuthentication_blacklistsByJtiAndExp() {
        Jwt jwt = Jwt.withTokenValue("access")
                .header("alg", "HS256")
                .claim("sub", TEST_USERNAME)
                .claim("jti", "jti-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        authenticationService.logout("raw");

        verify(refreshTokenService).revokeRefreshToken("raw");

        verify(invalidatedTokenRepository).save(invalidatedCaptor.capture());

        InvalidatedToken saved = invalidatedCaptor.getValue();

        assertThat(saved.getId()).isEqualTo("jti-123");

        assertThat(saved.getExpirationTime()).isEqualTo(jwt.getExpiresAt());
    }

    @Test
    void logout_whenAuthenticationNotJwt_doesNotBlacklist() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()));

        authenticationService.logout("raw");

        verify(refreshTokenService).revokeRefreshToken("raw");

        verify(invalidatedTokenRepository, never()).save(any());
    }

    @Test
    void logout_whenJtiNull_doesNotBlacklist() {
        Jwt jwt = Jwt.withTokenValue("access")
                .header("alg", "HS256")
                .claim("sub", TEST_USERNAME)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        authenticationService.logout(null);

        verifyNoInteractions(invalidatedTokenRepository);
    }

    @Test
    void logout_whenJtiBlank_doesNotBlacklist() {
        Jwt jwt = Jwt.withTokenValue("access")
                .header("alg", "HS256")
                .claim("sub", TEST_USERNAME)
                .claim("jti", "   ")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        authenticationService.logout(null);

        verifyNoInteractions(invalidatedTokenRepository);
    }
}