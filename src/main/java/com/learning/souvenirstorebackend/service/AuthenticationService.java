package com.learning.souvenirstorebackend.service;

import com.learning.souvenirstorebackend.configuration.JwtProperties;
import com.learning.souvenirstorebackend.dto.request.*;
import com.learning.souvenirstorebackend.entity.Cart;
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
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {

    AuthenticationManager authenticationManager;
    JwtService jwtService;
    RefreshTokenService refreshTokenService;
    InvalidatedTokenRepository invalidatedTokenRepository;
    JwtProperties jwtProperties;
    UserRepository userRepository;
    RoleRepository roleRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    CartRepository cartRepository;

    @Transactional
    public void registerUser(RegisterRequest registerRequest) {
        if (userRepository.existsUserByUsername(registerRequest.getUsername()))
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);

        if (userRepository.existsByEmail(registerRequest.getEmail()))
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);

        if (userRepository.existsUserByPhone(registerRequest.getPhone()))
            throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);

        Role role = roleRepository.findById("CUSTOMER")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        User user = userMapper.toUser(registerRequest);

        user.setRole(role);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        cartRepository.save(Cart.builder().user(user).build());
    }

    @Transactional
    public TokenPair login(LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(userDetails);

        return new TokenPair(accessToken, refreshToken, jwtProperties.getAccessTokenExpiration());
    }

    @Transactional
    public TokenPair refresh(String rawRefreshToken) {

        if (rawRefreshToken == null || rawRefreshToken.isBlank())
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        UserDetails userDetails = refreshTokenService.rotateRefreshToken(rawRefreshToken);

        String accessToken = jwtService.generateAccessToken(userDetails);
        String newRefreshToken = refreshTokenService.createRefreshToken(userDetails);

        return new TokenPair(accessToken, newRefreshToken, jwtProperties.getAccessTokenExpiration());
    }

    @Transactional
    public void logout(String refreshToken) {

        if (refreshToken != null && !refreshToken.isBlank())
            refreshTokenService.revokeRefreshToken(refreshToken);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {

            Jwt jwt = jwtAuthentication.getToken();

            if (jwt.getId() != null && !jwt.getId().isBlank()) {

                InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                        .id(jwt.getId())
                        .expirationTime(jwt.getExpiresAt())
                        .build();

                invalidatedTokenRepository.save(invalidatedToken);
            }
        }
    }

    public record TokenPair(String accessToken, String refreshToken, long expiresIn) {
    }
}