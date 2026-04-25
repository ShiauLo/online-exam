package com.maghert.examcommon.utils;

import com.maghert.examcommon.constants.AuthConstants;
import com.maghert.examcommon.exception.InvalidTokenException;
import com.maghert.examcommon.exception.TokenExpiredException;
import com.maghert.examcommon.properties.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilsTests {

    private static final String SECRET = "mySuperSecretKeyThatIsAtLeast32BytesLongForHS512";

    @Test
    void shouldGenerateAndParseTokensWithFrozenClaims() throws Exception {
        JwtProperties properties = createProperties(60_000L, 120_000L);
        JwtUtils jwtUtils = new JwtUtils(properties);

        String accessToken = jwtUtils.generateAccessToken("1001", Map.of(
                AuthConstants.USER_ID_CLAIM, 1001L,
                AuthConstants.ROLE_ID_CLAIM, 3));
        String refreshToken = jwtUtils.generateRefreshToken("1001", Map.of(
                AuthConstants.USER_ID_CLAIM, 1001L,
                AuthConstants.ROLE_ID_CLAIM, 3));

        assertEquals(1001L, jwtUtils.getUserIdFromToken(accessToken));
        assertEquals(3, jwtUtils.getRoleIdFromToken(accessToken));
        assertEquals(1001L, jwtUtils.getUserIdFromToken(refreshToken));
        assertEquals(3, jwtUtils.getRoleIdFromToken(refreshToken));
    }

    @Test
    void shouldThrowWhenTokenExpired() {
        JwtProperties properties = createProperties(60_000L, 120_000L);
        JwtUtils jwtUtils = new JwtUtils(properties);
        String expiredToken = Jwts.builder()
                .setSubject("1001")
                .setClaims(Map.of(AuthConstants.USER_ID_CLAIM, 1001L))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()), SignatureAlgorithm.HS256)
                .setIssuedAt(new Date(System.currentTimeMillis() - 5_000L))
                .setExpiration(new Date(System.currentTimeMillis() - 1_000L))
                .compact();

        assertThrows(TokenExpiredException.class, () -> jwtUtils.validateToken(expiredToken));
    }

    @Test
    void shouldThrowWhenTokenInvalid() {
        JwtProperties properties = createProperties(60_000L, 120_000L);
        JwtUtils jwtUtils = new JwtUtils(properties);

        assertThrows(InvalidTokenException.class, () -> jwtUtils.validateToken("invalid-token"));
    }

    @Test
    @SuppressWarnings("deprecation")
    void shouldKeepCompatibilityForLegacyPluralPropertyNames() {
        JwtProperties properties = new JwtProperties();
        properties.setAccessTokenExpirations(10L);
        properties.setRefreshTokenExpirations(20L);

        assertEquals(10L, properties.getAccessTokenExpiration());
        assertEquals(20L, properties.getRefreshTokenExpiration());
        assertEquals(10L, properties.getAccessTokenExpirations());
        assertEquals(20L, properties.getRefreshTokenExpirations());
    }

    @Test
    void shouldFailFastWhenSecretMissing() {
        JwtProperties properties = new JwtProperties();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new JwtUtils(properties));

        assertTrue(exception.getMessage().contains("exam.jwt.secret"));
    }

    private JwtProperties createProperties(long accessExpiration, long refreshExpiration) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setAccessTokenExpiration(accessExpiration);
        properties.setRefreshTokenExpiration(refreshExpiration);
        return properties;
    }
}
