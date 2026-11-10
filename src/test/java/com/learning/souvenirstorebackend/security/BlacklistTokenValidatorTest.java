package com.learning.souvenirstorebackend.security;

import com.learning.souvenirstorebackend.repository.InvalidatedTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BlacklistTokenValidatorTest {

    @Mock
    InvalidatedTokenRepository invalidatedTokenRepository;

    @InjectMocks
    BlacklistTokenValidator blacklistTokenValidator;

    private static Jwt jwtWithJti(String jti) {
        return Jwt.withTokenValue("t")
                .header("alg", "HS256")
                .subject("user")
                .claim("jti", jti)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    void validate_whenJtiMissing_failsWithInvalidToken() {
        Jwt jwt = Jwt.withTokenValue("t")
                .header("alg", "HS256")
                .subject("user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        OAuth2TokenValidatorResult result = blacklistTokenValidator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
                .hasSize(1)
                .extracting(OAuth2Error::getErrorCode)
                .containsExactly("invalid_token");
        assertThat(result.getErrors())
                .extracting(OAuth2Error::getDescription)
                .containsExactly("JWT is missing jti");

        verify(invalidatedTokenRepository, never()).existsById(anyString());
    }

    @Test
    void validate_whenJtiBlank_failsWithoutQueryingDb() {
        Jwt jwt = jwtWithJti("");

        OAuth2TokenValidatorResult result = blacklistTokenValidator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
                .extracting(OAuth2Error::getErrorCode)
                .containsExactly("invalid_token");

        verify(invalidatedTokenRepository, never()).existsById(anyString());
    }

    @Test
    void validate_whenJtiBlacklisted_failsWithInvalidatedMessage() {
        Jwt jwt = jwtWithJti("revoked-jti");
        when(invalidatedTokenRepository.existsById("revoked-jti")).thenReturn(true);

        OAuth2TokenValidatorResult result = blacklistTokenValidator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
                .hasSize(1)
                .extracting(OAuth2Error::getErrorCode)
                .containsExactly("invalid_token");
        assertThat(result.getErrors())
                .extracting(OAuth2Error::getDescription)
                .containsExactly("Token has been invalidated");

        verify(invalidatedTokenRepository).existsById("revoked-jti");
    }

    @Test
    void validate_whenJtiNotBlacklisted_succeeds() {
        Jwt jwt = jwtWithJti("valid-jti");
        when(invalidatedTokenRepository.existsById("valid-jti")).thenReturn(false);

        OAuth2TokenValidatorResult result = blacklistTokenValidator.validate(jwt);

        assertThat(result.hasErrors()).isFalse();
        assertThat(result.getErrors()).isEmpty();

        verify(invalidatedTokenRepository).existsById("valid-jti");
    }
}