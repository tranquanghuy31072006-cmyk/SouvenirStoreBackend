package com.learning.souvenirstorebackend.service;

import com.learning.souvenirstorebackend.configuration.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {

    @Mock
    JwtEncoder jwtEncoder;

    @Mock
    JwtProperties jwtProperties;

    @InjectMocks
    JwtService jwtService;

    @Captor
    ArgumentCaptor<JwtEncoderParameters> paramsCaptor;

    private static final String USERNAME = "testUser";
    private static final String ISSUER = "souvenir-store-test";
    private static final long ACCESS_TOKEN_EXPIRATION_SECONDS = 900L;

    @BeforeEach
    void setUp() {
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION_SECONDS);
    }


    private UserDetails userDetails(String role) {
        return User.builder()
                .username(USERNAME)
                .password("irrelevant")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + role)))
                .build();
    }

    private void stubEncoderReturns(String tokenValue) {
        Jwt stub = Jwt.withTokenValue(tokenValue)
                .header("alg", "HS256")
                .claim("sub", USERNAME)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(ACCESS_TOKEN_EXPIRATION_SECONDS))
                .build();
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(stub);
    }

    private void stubHappyPath(String tokenValue) {
        when(jwtProperties.getIssuer()).thenReturn(ISSUER);
        stubEncoderReturns(tokenValue);
    }

    private JwtClaimsSet captureClaims() {
        verify(jwtEncoder).encode(paramsCaptor.capture());
        return paramsCaptor.getValue().getClaims();
    }

    @Test
    void generateAccessToken_withAdmin_stripsRolePrefixInClaim() {
        stubHappyPath("token");

        jwtService.generateAccessToken(userDetails("ADMIN"));

        assertThat(captureClaims().getClaimAsString("role")).isEqualTo("ADMIN");
    }

    @Test
    void generateAccessToken_withStaff_setsStaffRole() {
        stubHappyPath("token");

        jwtService.generateAccessToken(userDetails("STAFF"));

        assertThat(captureClaims().getClaimAsString("role")).isEqualTo("STAFF");
    }

    @Test
    void generateAccessToken_withCustomer_setsCustomerRole() {
        stubHappyPath("token");

        jwtService.generateAccessToken(userDetails("CUSTOMER"));

        assertThat(captureClaims().getClaimAsString("role")).isEqualTo("CUSTOMER");
    }

    @Test
    void generateAccessToken_setsSubjectUsername() {
        stubHappyPath("token");

        jwtService.generateAccessToken(userDetails("CUSTOMER"));

        assertThat(captureClaims().getSubject()).isEqualTo(USERNAME);
    }

    @Test
    void generateAccessToken_setsIssuerFromJwtProperties() {
        stubHappyPath("token");

        jwtService.generateAccessToken(userDetails("CUSTOMER"));

        JwtClaimsSet claims = captureClaims();

        assertThat(claims.getClaimAsString("iss")).isEqualTo(ISSUER);

        verify(jwtProperties).getIssuer();
    }

    @Test
    void generateAccessToken_setsIssuedAtNearNow() {
        stubHappyPath("token");

        Instant before = Instant.now();

        jwtService.generateAccessToken(userDetails("CUSTOMER"));

        Instant after = Instant.now();

        assertThat(captureClaims().getIssuedAt()).isBetween(before, after);
    }

    @Test
    void generateAccessToken_expMinusIat_equalsConfiguredDuration() {
        stubHappyPath("token");

        jwtService.generateAccessToken(userDetails("CUSTOMER"));

        JwtClaimsSet claims = captureClaims();

        long deltaSeconds = claims.getExpiresAt().getEpochSecond() - claims.getIssuedAt().getEpochSecond();

        assertThat(deltaSeconds).isEqualTo(ACCESS_TOKEN_EXPIRATION_SECONDS);
    }

    @Test
    void generateAccessToken_setsJti() {
        stubHappyPath("token");

        jwtService.generateAccessToken(userDetails("CUSTOMER"));

        assertThat(captureClaims().getClaimAsString("jti")).isNotBlank();
    }

    @Test
    void generateAccessToken_twoCalls_produceDifferentJti() {
        stubHappyPath("token");

        jwtService.generateAccessToken(userDetails("CUSTOMER"));

        jwtService.generateAccessToken(userDetails("CUSTOMER"));

        verify(jwtEncoder, times(2)).encode(paramsCaptor.capture());

        List<JwtEncoderParameters> allParams = paramsCaptor.getAllValues();

        String jti1 = allParams.get(0).getClaims().getClaimAsString("jti");

        String jti2 = allParams.get(1).getClaims().getClaimAsString("jti");

        assertThat(jti1).isNotBlank();

        assertThat(jti2).isNotBlank();

        assertThat(jti1).isNotEqualTo(jti2);
    }

    @Test
    void generateAccessToken_headerAlgorithmIsHS256() {
        stubHappyPath("token");

        jwtService.generateAccessToken(userDetails("CUSTOMER"));

        verify(jwtEncoder).encode(paramsCaptor.capture());

        JwsHeader header = paramsCaptor.getValue().getJwsHeader();

        assertThat(header).isNotNull();

        assertThat(header.getHeaders().get("alg")).hasToString(MacAlgorithm.HS256.getName());
    }

    @Test
    void generateAccessToken_returnsTokenValueFromEncoder() {
        stubHappyPath("my-token");

        assertThat(jwtService.generateAccessToken(userDetails("CUSTOMER"))).isEqualTo("my-token");
    }

    @Test
    void generateAccessToken_whenNoRoleAuthority_throwsIllegalArgument() {
        UserDetails noRole = User.builder()
                .username(USERNAME)
                .password("x")
                .authorities(List.of(new SimpleGrantedAuthority("SOMETHING")))
                .build();

        assertThatThrownBy(() -> jwtService.generateAccessToken(noRole))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("role");

        verify(jwtEncoder, never()).encode(any());

        verify(jwtProperties, never()).getIssuer();
    }
}