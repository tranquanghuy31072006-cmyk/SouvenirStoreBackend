package com.learning.souvenirstorebackend.controller;

import com.learning.souvenirstorebackend.configuration.JwtProperties;
import com.learning.souvenirstorebackend.dto.request.LoginRequest;
import com.learning.souvenirstorebackend.dto.request.RegisterRequest;
import com.learning.souvenirstorebackend.dto.response.ApiResponse;
import com.learning.souvenirstorebackend.dto.response.LoginResponse;
import com.learning.souvenirstorebackend.service.AuthenticationService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationService authenticationService;
    JwtProperties jwtProperties;

    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    @PostMapping("/register")
    public ApiResponse<String> register(@Valid @RequestBody RegisterRequest registerRequest) {
        authenticationService.registerUser(registerRequest);

        return ApiResponse.<String>builder()
                .message("User registered successfully!")
                .build();
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                            HttpServletResponse response) {

        AuthenticationService.TokenPair tokenPair = authenticationService.login(request);

        response.addHeader(HttpHeaders.SET_COOKIE,
                createRefreshCookie(tokenPair.refreshToken()).toString());

        return ApiResponse.<LoginResponse>builder()
                .result(LoginResponse.builder()
                        .accessToken(tokenPair.accessToken())
                        .expiresIn(tokenPair.expiresIn())
                        .build())
                .build();
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {

        AuthenticationService.TokenPair tokenPair = authenticationService.refresh(refreshToken);

        response.addHeader(HttpHeaders.SET_COOKIE,
                createRefreshCookie(tokenPair.refreshToken()).toString());

        return ApiResponse.<LoginResponse>builder()
                .result(LoginResponse.builder().
                        accessToken(tokenPair.accessToken())
                        .expiresIn(tokenPair.expiresIn())
                        .build())
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<String> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {

        authenticationService.logout(refreshToken);

        response.addHeader(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString());

        return ApiResponse.<String>builder()
                .message("Logout successful")
                .build();
    }

    private ResponseCookie createRefreshCookie(String refreshToken) {

        return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(jwtProperties.isRefreshCookieSecure())
                .sameSite(jwtProperties.getRefreshCookieSameSite())
                .path(jwtProperties.getRefreshCookiePath())
                .maxAge(Duration.ofSeconds(jwtProperties.getRefreshTokenExpiration()))
                .build();
    }

    private ResponseCookie clearRefreshCookie() {

        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(jwtProperties.isRefreshCookieSecure())
                .sameSite(jwtProperties.getRefreshCookieSameSite())
                .path(jwtProperties.getRefreshCookiePath())
                .maxAge(Duration.ZERO)
                .build();
    }
}
