package com.learning.souvenirstorebackend.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String issuer;

    private String secret;

    private long accessTokenExpiration = 900;

    private long refreshTokenExpiration = 604800;

    private boolean refreshCookieSecure = false;

    private String refreshCookiePath = "/souvenir/auth";

    private String refreshCookieSameSite = "Strict";
}