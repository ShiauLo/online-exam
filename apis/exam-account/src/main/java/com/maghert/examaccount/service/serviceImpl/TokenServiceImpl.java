package com.maghert.examaccount.service.serviceImpl;

import com.maghert.examaccount.service.TokenService;
import com.maghert.examcommon.constants.AuthConstants;
import com.maghert.examcommon.pojo.entity.SysUser;
import com.maghert.examcommon.pojo.vo.LoginVO;
import com.maghert.examcommon.properties.JwtProperties;
import com.maghert.examcommon.utils.JwtUtils;
import com.maghert.examcommon.web.ApiResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.maghert.examaccount.constants.AccountConstants.REDIS_ACCESS_TOKEN;
import static com.maghert.examaccount.constants.AccountConstants.REDIS_REFRESH_TOKEN;

@Service
public class TokenServiceImpl implements TokenService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtUtils jwtUtils;
    private final JwtProperties jwtProperties;

    public TokenServiceImpl(
            JwtUtils jwtUtils,
            RedisTemplate<String, Object> redisTemplate,
            JwtProperties jwtProperties) {
        this.jwtUtils = jwtUtils;
        this.redisTemplate = redisTemplate;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public ApiResponse<LoginVO> generateAccessTokenAndRefreshToken(SysUser user) {
        Map<String, Object> claims = buildUserClaims(user.getId(), user.getRoleId());
        String accessToken = jwtUtils.generateAccessToken(user.getId().toString(), claims);
        String refreshToken = jwtUtils.generateRefreshToken(user.getId().toString(), claims);

        redisTemplate.opsForValue().set(
                REDIS_ACCESS_TOKEN + user.getId(),
                Objects.requireNonNull(accessToken),
                jwtProperties.getAccessTokenExpiration(),
                TimeUnit.MILLISECONDS);
        redisTemplate.opsForValue().set(
                REDIS_REFRESH_TOKEN + user.getId(),
                Objects.requireNonNull(refreshToken),
                jwtProperties.getRefreshTokenExpiration(),
                TimeUnit.MILLISECONDS);

        return ApiResponse.ok(LoginVO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build());
    }

    @Override
    public ApiResponse<String> refreshToken(String refreshToken) {
        try {
            jwtUtils.validateToken(refreshToken);
            Long userId = jwtUtils.getUserIdFromToken(refreshToken);
            Integer roleId = jwtUtils.getRoleIdFromToken(refreshToken);
            Object cachedRefreshToken = redisTemplate.opsForValue().get(REDIS_REFRESH_TOKEN + userId);
            if (cachedRefreshToken == null || !refreshToken.equals(cachedRefreshToken.toString())) {
                return ApiResponse.fail(401, "refresh token invalid");
            }

            String accessToken = jwtUtils.generateAccessToken(userId.toString(), buildUserClaims(userId, roleId));
            redisTemplate.opsForValue().set(
                    REDIS_ACCESS_TOKEN + userId,
                    Objects.requireNonNull(accessToken),
                    jwtProperties.getAccessTokenExpiration(),
                    TimeUnit.MILLISECONDS);
            return ApiResponse.ok(accessToken);
        } catch (Exception e) {
            return ApiResponse.fail(401, "refresh token invalid");
        }
    }

    private Map<String, Object> buildUserClaims(Long userId, Integer roleId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(AuthConstants.USER_ID_CLAIM, userId);
        if (roleId != null) {
            claims.put(AuthConstants.ROLE_ID_CLAIM, roleId);
        }
        return claims;
    }
}
