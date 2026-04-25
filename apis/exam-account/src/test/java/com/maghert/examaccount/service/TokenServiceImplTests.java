package com.maghert.examaccount.service;

import com.maghert.examaccount.service.serviceImpl.TokenServiceImpl;
import com.maghert.examcommon.pojo.entity.SysUser;
import com.maghert.examcommon.properties.JwtProperties;
import com.maghert.examcommon.utils.JwtUtils;
import com.maghert.examcommon.web.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.maghert.examaccount.constants.AccountConstants.REDIS_ACCESS_TOKEN;
import static com.maghert.examaccount.constants.AccountConstants.REDIS_REFRESH_TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTests {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private JwtUtils jwtUtils;
    private JwtProperties jwtProperties;
    private TokenServiceImpl tokenService;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("mySuperSecretKeyThatIsAtLeast32BytesLongForHS512");
        jwtProperties.setAccessTokenExpiration(60_000L);
        jwtProperties.setRefreshTokenExpiration(120_000L);
        jwtUtils = new JwtUtils(jwtProperties);
        tokenService = new TokenServiceImpl(jwtUtils, redisTemplate, jwtProperties);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldKeepRoleIdWhenGeneratingAndRefreshingTokens() throws Exception {
        SysUser user = new SysUser().setId(1001L).setRoleId(3);

        ApiResponse<com.maghert.examcommon.pojo.vo.LoginVO> loginResponse = tokenService.generateAccessTokenAndRefreshToken(user);
        com.maghert.examcommon.pojo.vo.LoginVO loginData = Objects.requireNonNull(loginResponse.getData());
        String accessToken = Objects.requireNonNull(loginData.getAccessToken());
        String refreshToken = Objects.requireNonNull(loginData.getRefreshToken());
        TimeUnit milliseconds = Objects.requireNonNull(TimeUnit.MILLISECONDS);
        when(valueOperations.get(REDIS_REFRESH_TOKEN + 1001L)).thenReturn(refreshToken);

        ApiResponse<String> refreshed = tokenService.refreshToken(refreshToken);
        String refreshedAccessToken = Objects.requireNonNull(refreshed.getData());

        assertNotNull(accessToken);
        assertEquals(3, jwtUtils.getRoleIdFromToken(accessToken));
        assertEquals(3, jwtUtils.getRoleIdFromToken(refreshToken));
        assertEquals(3, jwtUtils.getRoleIdFromToken(refreshedAccessToken));
        verify(valueOperations, atLeastOnce())
                .set(REDIS_ACCESS_TOKEN + 1001L, refreshedAccessToken, 60_000L, milliseconds);
        verify(valueOperations, atLeastOnce())
                .set(REDIS_REFRESH_TOKEN + 1001L, refreshToken, 120_000L, milliseconds);
    }
}
