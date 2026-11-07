package com.learning.souvenirstorebackend.service;

import com.learning.souvenirstorebackend.configuration.JwtProperties;
import com.learning.souvenirstorebackend.entity.RefreshToken;
import com.learning.souvenirstorebackend.entity.User;
import com.learning.souvenirstorebackend.exception.AppException;
import com.learning.souvenirstorebackend.exception.ErrorCode;
import com.learning.souvenirstorebackend.repository.RefreshTokenRepository;
import com.learning.souvenirstorebackend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RefreshTokenService {
    RefreshTokenRepository refreshTokenRepository;
    UserRepository userRepository;
    UserService userService;
    JwtProperties jwtProperties;

    SecureRandom secureRandom = new SecureRandom();

    public String createRefreshToken(UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow(()
                -> new AppException(ErrorCode.USER_NOT_FOUND));

        String rawToken = generateRandomToken();

        String tokenHash = hashToken(rawToken);

        Instant now = Instant.now();
        Instant expirationTime = now.plusSeconds(jwtProperties.getRefreshTokenExpiration());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expirationTime(expirationTime)
                .revoked(false)
                .createdAt(now)
                .build();
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Transactional
    public UserDetails rotateRefreshToken(String rawToken) {
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        if (refreshToken.isRevoked())
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        if (refreshToken.getExpirationTime().isBefore(Instant.now()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        UserDetails userDetails = userService.loadUserByUsername(refreshToken.getUser().getUsername());

        if (!userDetails.isEnabled())
            throw new AppException(ErrorCode.USER_BLOCKED);

        refreshToken.setRevoked(true);

        refreshTokenRepository.save(refreshToken);

        return userDetails;
    }

    @Transactional
    public void revokeRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank())
            return;

        String tokenHash = hashToken(rawToken);

        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(refreshToken -> {
                    if (!refreshToken.isRevoked()) {
                        refreshToken.setRevoked(true);

                        refreshTokenRepository.save(refreshToken);
                    }
                }
        );
    }

    private String generateRandomToken() {
        byte[] randomBytes = new byte[32];

        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Can not hash refresh token", e);
        }
    }
}
