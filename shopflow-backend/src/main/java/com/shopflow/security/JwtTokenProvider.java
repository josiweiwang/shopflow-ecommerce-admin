package com.shopflow.security;

import com.shopflow.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 令牌生成与解析。
 *
 * <p>设计要点：
 * <ul>
 *     <li>令牌分 access / refresh 两种类型，用 {@code typ} 声明区分；
 *         refresh 令牌不能直接访问业务接口，避免「长效令牌等于长期通行证」；</li>
 *     <li>每个令牌带唯一 {@code jti}，登出时只需把 jti 写入 Redis 黑名单，
 *         无需等到令牌自然过期，也无需维护服务端会话；</li>
 *     <li>载荷中只放 userId、username 这类非敏感信息，绝不放入密码或权限明细。</li>
 * </ul>
 *
 * @author shopflow
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    /** 令牌类型声明键 */
    public static final String CLAIM_TYPE = "typ";

    /** 访问令牌 */
    public static final String TYPE_ACCESS = "access";

    /** 刷新令牌 */
    public static final String TYPE_REFRESH = "refresh";

    /** 用户名声明键 */
    public static final String CLAIM_USERNAME = "username";

    /** HMAC-SHA256 要求密钥不短于 32 字节 */
    private static final int MIN_SECRET_BYTES = 32;

    private final JwtProperties jwtProperties;

    private SecretKey signingKey;

    @PostConstruct
    void initSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("shopflow.jwt.secret 长度不足 32 字节，存在被暴力破解的风险");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT 签名密钥初始化完成 | issuer={} | accessTokenExpireMinutes={}",
                jwtProperties.getIssuer(), jwtProperties.getAccessTokenExpireMinutes());
    }

    /**
     * 生成访问令牌。
     */
    public String createAccessToken(LoginUser loginUser) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(jwtProperties.getAccessTokenExpireSeconds());
        return Jwts.builder()
                .subject(String.valueOf(loginUser.getUserId()))
                .id(newJti())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .claim(CLAIM_USERNAME, loginUser.getUsername())
                .signWith(signingKey)
                .compact();
    }

    /**
     * 生成刷新令牌：只承载用户 ID 与 jti，权限变更后重新解析即可生效。
     */
    public String createRefreshToken(Long userId) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(jwtProperties.getRefreshTokenExpireSeconds());
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(newJti())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .signWith(signingKey)
                .compact();
    }

    /**
     * 解析并校验令牌（签名、签发者、过期时间）。
     *
     * @throws io.jsonwebtoken.ExpiredJwtException 令牌过期
     * @throws io.jsonwebtoken.JwtException        签名不合法或格式错误
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(jwtProperties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 取用户 ID */
    public Long getUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    /** 是否为访问令牌 */
    public boolean isAccessToken(Claims claims) {
        return TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class));
    }

    /** 是否为刷新令牌 */
    public boolean isRefreshToken(Claims claims) {
        return TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class));
    }

    /** 令牌剩余有效期（秒），用于设置黑名单 TTL */
    public long getRemainingSeconds(Claims claims) {
        Date expiration = claims.getExpiration();
        if (expiration == null) {
            return 0L;
        }
        long remaining = (expiration.getTime() - System.currentTimeMillis()) / 1000;
        return Math.max(remaining, 0L);
    }

    private String newJti() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}