package com.shopflow.security;

import com.shopflow.common.enums.UserStatus;
import com.shopflow.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JWT 令牌工具测试。
 *
 * <p>覆盖最容易出错的三件事：载荷内容是否正确、令牌类型是否可区分、过期令牌是否被拒绝。
 *
 * @author shopflow
 */
class JwtTokenProviderTest {

    private static final String SECRET = "unit-test-secret-key-must-be-long-enough-0123456789";

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("shopflow-test");
        properties.setSecret(SECRET);
        properties.setAccessTokenExpireMinutes(30);
        properties.setRefreshTokenExpireDays(7);

        jwtTokenProvider = new JwtTokenProvider(properties);
        jwtTokenProvider.initSigningKey();
    }

    @Test
    @DisplayName("访问令牌：用户ID、用户名、jti 均可正确解析")
    void accessTokenShouldCarryUserInfo() {
        LoginUser loginUser = LoginUser.builder()
                .userId(1001L)
                .username("shopee")
                .nickname("测试用户")
                .status(UserStatus.ENABLED.getCode())
                .roleCodes(Set.of("USER"))
                .permissions(Set.of("product:read"))
                .build();

        String token = jwtTokenProvider.createAccessToken(loginUser);
        Claims claims = jwtTokenProvider.parse(token);

        assertThat(jwtTokenProvider.getUserId(claims)).isEqualTo(1001L);
        assertThat(claims.get("username", String.class)).isEqualTo("shopee");
        assertThat(claims.getId()).isNotBlank();
        assertThat(jwtTokenProvider.isAccessToken(claims)).isTrue();
        assertThat(jwtTokenProvider.isRefreshToken(claims)).isFalse();
        assertThat(jwtTokenProvider.getRemainingSeconds(claims)).isBetween(1700L, 1800L);
    }

    @Test
    @DisplayName("刷新令牌：类型声明与访问令牌不同，不会被当成访问令牌使用")
    void refreshTokenShouldHaveDifferentType() {
        String refreshToken = jwtTokenProvider.createRefreshToken(2002L);
        Claims claims = jwtTokenProvider.parse(refreshToken);

        assertThat(jwtTokenProvider.isRefreshToken(claims)).isTrue();
        assertThat(jwtTokenProvider.isAccessToken(claims)).isFalse();
        assertThat(jwtTokenProvider.getUserId(claims)).isEqualTo(2002L);
    }

    @Test
    @DisplayName("令牌必须一次一签：同一用户两次签发得到不同的 jti")
    void jtiShouldBeUniquePerIssue() {
        String first = jwtTokenProvider.createRefreshToken(3003L);
        String second = jwtTokenProvider.createRefreshToken(3003L);

        assertThat(jwtTokenProvider.parse(first).getId())
                .isNotEqualTo(jwtTokenProvider.parse(second).getId());
    }

    @Test
    @DisplayName("被篡改的令牌无法通过签名校验")
    void tamperedTokenShouldBeRejected() {
        String token = jwtTokenProvider.createAccessToken(LoginUser.builder()
                .userId(4004L).username("hacker").status(1).build());
        String tampered = token.substring(0, token.length() - 3) + "abc";

        assertThatThrownBy(() -> jwtTokenProvider.parse(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("过期令牌抛出 ExpiredJwtException，便于上层返回 40102")
    void expiredTokenShouldThrowExpiredException() {
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("shopflow-test");
        properties.setSecret(SECRET);
        // 手动构造一个已过期的令牌：有效期设为负数
        properties.setAccessTokenExpireMinutes(-1);
        JwtTokenProvider expiredProvider = new JwtTokenProvider(properties);
        expiredProvider.initSigningKey();

        String expiredToken = expiredProvider.createAccessToken(
                LoginUser.builder().userId(5005L).username("expired").status(1).build());

        assertThatThrownBy(() -> jwtTokenProvider.parse(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("密钥过短时启动即失败，避免线上使用弱密钥")
    void shortSecretShouldFailFast() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("too-short");
        JwtTokenProvider weakProvider = new JwtTokenProvider(properties);

        assertThatThrownBy(weakProvider::initSigningKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }
}