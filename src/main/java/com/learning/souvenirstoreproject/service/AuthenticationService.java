package com.learning.souvenirstoreproject.service;

import com.learning.souvenirstoreproject.configuration.JwtProperties;
import com.learning.souvenirstoreproject.dto.request.*;
import com.learning.souvenirstoreproject.entity.Cart;
import com.learning.souvenirstoreproject.entity.InvalidatedToken;
import com.learning.souvenirstoreproject.entity.Role;
import com.learning.souvenirstoreproject.entity.User;
import com.learning.souvenirstoreproject.exception.AppException;
import com.learning.souvenirstoreproject.exception.ErrorCode;
import com.learning.souvenirstoreproject.mapper.UserMapper;
import com.learning.souvenirstoreproject.repository.CartRepository;
import com.learning.souvenirstoreproject.repository.InvalidatedTokenRepository;
import com.learning.souvenirstoreproject.repository.RoleRepository;
import com.learning.souvenirstoreproject.repository.UserRepository;
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