package com.learning.souvenirstoreproject.security;

import com.learning.souvenirstoreproject.repository.InvalidatedTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlacklistTokenValidator implements OAuth2TokenValidator<Jwt> {

    private final InvalidatedTokenRepository invalidatedTokenRepository;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {

        String jti = jwt.getId();

        if (jti == null || jti.isBlank()) {
            log.warn("JWT missing jti");

            OAuth2Error error = new OAuth2Error(
                    "invalid_token",
                    "JWT is missing jti",
                    null);

            return OAuth2TokenValidatorResult.failure(error);
        }

        boolean exists = invalidatedTokenRepository.existsById(jti);

        if (exists) {
            OAuth2Error error = new OAuth2Error(
                    "invalid_token",
                    "Token has been invalidated",
                    null);

            return OAuth2TokenValidatorResult.failure(error);
        }

        return OAuth2TokenValidatorResult.success();
    }
}