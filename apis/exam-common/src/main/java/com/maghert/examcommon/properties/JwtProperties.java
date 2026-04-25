package com.maghert.examcommon.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "exam.jwt")
public class JwtProperties {

    private String secret;
    private Long accessTokenExpiration;
    private Long refreshTokenExpiration;

    @Deprecated(forRemoval = false)
    private Long accessTokenExpirations;

    @Deprecated(forRemoval = false)
    private Long refreshTokenExpirations;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Long getAccessTokenExpiration() {
        return accessTokenExpiration != null ? accessTokenExpiration : accessTokenExpirations;
    }

    public void setAccessTokenExpiration(Long accessTokenExpiration) {
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public Long getRefreshTokenExpiration() {
        return refreshTokenExpiration != null ? refreshTokenExpiration : refreshTokenExpirations;
    }

    public void setRefreshTokenExpiration(Long refreshTokenExpiration) {
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    @Deprecated(forRemoval = false)
    public Long getAccessTokenExpirations() {
        return getAccessTokenExpiration();
    }

    @Deprecated(forRemoval = false)
    public void setAccessTokenExpirations(Long accessTokenExpirations) {
        this.accessTokenExpirations = accessTokenExpirations;
        if (this.accessTokenExpiration == null) {
            this.accessTokenExpiration = accessTokenExpirations;
        }
    }

    @Deprecated(forRemoval = false)
    public Long getRefreshTokenExpirations() {
        return getRefreshTokenExpiration();
    }

    @Deprecated(forRemoval = false)
    public void setRefreshTokenExpirations(Long refreshTokenExpirations) {
        this.refreshTokenExpirations = refreshTokenExpirations;
        if (this.refreshTokenExpiration == null) {
            this.refreshTokenExpiration = refreshTokenExpirations;
        }
    }
}
