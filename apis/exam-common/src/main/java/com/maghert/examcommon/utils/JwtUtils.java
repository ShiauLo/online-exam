package com.maghert.examcommon.utils;

import com.maghert.examcommon.constants.AuthConstants;
import com.maghert.examcommon.exception.InvalidTokenException;
import com.maghert.examcommon.exception.TokenExpiredException;
import com.maghert.examcommon.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtils {

    private final SecretKey secretKey;
    private final JwtProperties jwtProperties;

    public JwtUtils(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        Assert.hasText(jwtProperties.getSecret(), "配置项 exam.jwt.secret 不能为空");
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String subject) {
        return generateAccessToken(subject, new HashMap<>());
    }

    public String generateAccessToken(String subject, Map<String, Object> claims) {
        return generateToken(subject, claims, jwtProperties.getAccessTokenExpiration());
    }

    public String generateRefreshToken(String subject) {
        return generateRefreshToken(subject, new HashMap<>());
    }

    public String generateRefreshToken(String subject, Map<String, Object> claims) {
        return generateToken(subject, claims, jwtProperties.getRefreshTokenExpiration());
    }

    public Long getUserIdFromToken(String token) throws InvalidTokenException, TokenExpiredException {
        Object claimValue = getClaimsFromToken(token).get(AuthConstants.USER_ID_CLAIM);
        if (claimValue instanceof Number number) {
            return number.longValue();
        }
        if (claimValue instanceof String text && !text.isBlank()) {
            return Long.valueOf(text);
        }
        return Long.valueOf(getClaimFromToken(token, Claims::getSubject));
    }

    public Integer getRoleIdFromToken(String token) throws InvalidTokenException, TokenExpiredException {
        Object claimValue = getClaimsFromToken(token).get(AuthConstants.ROLE_ID_CLAIM);
        if (claimValue == null) {
            return null;
        }
        if (claimValue instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(claimValue));
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver)
            throws InvalidTokenException, TokenExpiredException {
        return claimsResolver.apply(getClaimsFromToken(token));
    }

    public Claims getClaimsFromToken(String token) throws InvalidTokenException, TokenExpiredException {
        return parseToken(token);
    }

    public void validateToken(String token) throws InvalidTokenException, TokenExpiredException {
        parseToken(token);
    }

    private String generateToken(String subject, Map<String, Object> claims, long expiration) {
        Assert.hasText(subject, "JWT subject must not be empty");
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        Map<String, Object> tokenClaims = new HashMap<>(claims);
        if (!tokenClaims.containsKey(AuthConstants.USER_ID_CLAIM)) {
            tokenClaims.put(AuthConstants.USER_ID_CLAIM, subject);
        }

        return Jwts.builder()
                .setClaims(tokenClaims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims parseToken(String token) throws TokenExpiredException, InvalidTokenException {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException();
        } catch (Exception e) {
            throw new InvalidTokenException();
        }
    }
}
