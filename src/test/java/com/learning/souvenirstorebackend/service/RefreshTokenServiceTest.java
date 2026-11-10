package com.learning.souvenirstorebackend.service;

import com.learning.souvenirstorebackend.configuration.JwtProperties;
import com.learning.souvenirstorebackend.entity.RefreshToken;
import com.learning.souvenirstorebackend.entity.User;
import com.learning.souvenirstorebackend.enums.UserStatus;
import com.learning.souvenirstorebackend.exception.AppException;
import com.learning.souvenirstorebackend.exception.ErrorCode;
import com.learning.souvenirstorebackend.repository.RefreshTokenRepository;
import com.learning.souvenirstorebackend.repository.UserRepository;
import com.learning.souvenirstorebackend.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceTest {

    private static final String TEST_USERNAME = "testUser";
    private static final String TEST_ROLE = "CUSTOMER";
    private static final long USER_ID = 1L;
    private static final long REFRESH_TOKEN_EXPIRATION_SECONDS = 604800L;

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    UserService userService;

    @Mock
    JwtProperties jwtProperties;

    @InjectMocks
    RefreshTokenService refreshTokenService;

    @Captor
    ArgumentCaptor<RefreshToken> tokenCaptor;

    private User user;

    @BeforeEach
    void setUp() {
        user = TestDataFactory.createUser(TEST_USERNAME, TEST_ROLE);
        user.setId(USER_ID);
        user.setStatus(UserStatus.ACTIVE);
    }


    private String sha256Hex(String raw) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
    }

    private UserDetails userDetails(boolean enabled) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(TEST_USERNAME)
                .password("irrelevant")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + TEST_ROLE)))
                .disabled(!enabled)
                .build();
    }

    private RefreshToken buildStoredToken(String hash, boolean revoked, Instant expirationTime) {
        return RefreshToken.builder()
                .id(1L)
                .user(user)
                .tokenHash(hash)
                .revoked(revoked)
                .expirationTime(expirationTime)
                .build();
    }

    @Test
    void createRefreshToken_whenUserExists_savesHashedToken() throws Exception {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));

        when(jwtProperties.getRefreshTokenExpiration()).thenReturn(REFRESH_TOKEN_EXPIRATION_SECONDS);

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Instant before = Instant.now();

        String raw = refreshTokenService.createRefreshToken(userDetails(true));

        Instant after = Instant.now();

        verify(refreshTokenRepository).save(tokenCaptor.capture());

        RefreshToken saved = tokenCaptor.getValue();

        assertThat(saved.getUser()).isEqualTo(user);

        assertThat(saved.isRevoked()).isFalse();

        assertThat(saved.getCreatedAt()).isBetween(before, after);

        assertThat(saved.getExpirationTime()).isBetween(
                before.plusSeconds(REFRESH_TOKEN_EXPIRATION_SECONDS),
                after.plusSeconds(REFRESH_TOKEN_EXPIRATION_SECONDS));

        assertThat(raw).isNotEqualTo(saved.getTokenHash());

        assertThat(saved.getTokenHash()).isEqualTo(sha256Hex(raw));
    }

    @Test
    void createRefreshToken_rawTokenIsBase64UrlSafe43Chars() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));

        when(jwtProperties.getRefreshTokenExpiration()).thenReturn(REFRESH_TOKEN_EXPIRATION_SECONDS);

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        String raw = refreshTokenService.createRefreshToken(userDetails(true));

        assertThat(raw).hasSize(43);

        assertThat(raw).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void createRefreshToken_whenUserNotFound_throwsUserNotFound() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.createRefreshToken(userDetails(true)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void rotateRefreshToken_whenValid_returnsUserDetailsAndRevokes() throws Exception {
        String raw = "some-raw-token";

        String hash = sha256Hex(raw);

        RefreshToken stored = buildStoredToken(hash, false, Instant.now().plusSeconds(3600));

        UserDetails storedUser = userDetails(true);

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(stored));

        when(userService.loadUserByUsername(TEST_USERNAME)).thenReturn(storedUser);

        when(refreshTokenRepository.save(stored)).thenReturn(stored);

        UserDetails result = refreshTokenService.rotateRefreshToken(raw);

        assertThat(result).isSameAs(storedUser);

        assertThat(stored.isRevoked()).isTrue();

        verify(refreshTokenRepository).save(stored);

        verify(userService).loadUserByUsername(TEST_USERNAME);
    }

    @Test
    void rotateRefreshToken_whenHashNotFound_throwsUnauthenticated() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("raw"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHENTICATED);

        verify(refreshTokenRepository, never()).save(any());

        verifyNoInteractions(userService);
    }

    @Test
    void rotateRefreshToken_whenRevoked_throwsUnauthenticated() throws Exception {
        String raw = "raw";

        String hash = sha256Hex(raw);

        RefreshToken stored = buildStoredToken(hash, true, Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(raw))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHENTICATED);

        verify(refreshTokenRepository, never()).save(any());

        verifyNoInteractions(userService);
    }

    @Test
    void rotateRefreshToken_whenExpired_throwsUnauthenticated() throws Exception {
        String raw = "raw";

        String hash = sha256Hex(raw);

        RefreshToken stored = buildStoredToken(hash, false, Instant.now().minusSeconds(1));

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(raw))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHENTICATED);

        verify(refreshTokenRepository, never()).save(any());

        verifyNoInteractions(userService);
    }

    @Test
    void rotateRefreshToken_whenUserBlocked_throwsUserBlocked() throws Exception {
        String raw = "raw";

        String hash = sha256Hex(raw);

        RefreshToken stored = buildStoredToken(hash, false, Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(stored));

        when(userService.loadUserByUsername(TEST_USERNAME)).thenReturn(userDetails(false));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(raw))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_BLOCKED);

        assertThat(stored.isRevoked()).isFalse();

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void rotateRefreshToken_whenUserDeleted_propagatesUsernameNotFound() throws Exception {
        String raw = "raw";

        String hash = sha256Hex(raw);

        RefreshToken stored = buildStoredToken(hash, false, Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(stored));

        when(userService.loadUserByUsername(TEST_USERNAME))
                .thenThrow(new UsernameNotFoundException("User not found"));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(raw))
                .isInstanceOf(UsernameNotFoundException.class);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void revokeRefreshToken_whenNull_returnWithoutQuery() {
        refreshTokenService.revokeRefreshToken(null);

        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void revokeRefreshToken_whenBlank_returnWithoutQuery() {
        refreshTokenService.revokeRefreshToken("   ");

        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void revokeRefreshToken_whenNotFound_noException() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatCode(() -> refreshTokenService.revokeRefreshToken("abc"))
                .doesNotThrowAnyException();

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void revokeRefreshToken_whenNotRevoked_setsTrueAndSaves() throws Exception {
        String raw = "abc";

        String hash = sha256Hex(raw);

        RefreshToken stored = buildStoredToken(hash, false, Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(stored));

        when(refreshTokenRepository.save(stored)).thenReturn(stored);

        refreshTokenService.revokeRefreshToken(raw);

        assertThat(stored.isRevoked()).isTrue();

        verify(refreshTokenRepository).save(stored);
    }

    @Test
    void revokeRefreshToken_whenAlreadyRevoked_doesNotSaveAgain() throws Exception {
        String raw = "abc";

        String hash = sha256Hex(raw);

        RefreshToken stored = buildStoredToken(hash, true, Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(stored));

        refreshTokenService.revokeRefreshToken(raw);

        verify(refreshTokenRepository, never()).save(any());
    }
}