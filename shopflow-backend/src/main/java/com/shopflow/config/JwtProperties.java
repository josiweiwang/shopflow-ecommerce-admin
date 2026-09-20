package com.shopflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置项（对应 application.yml 中的 shopflow.jwt）。
 *
 * @author shopflow
 */
@Data
@Component
@ConfigurationProperties(prefix = "shopflow.jwt")
public class JwtProperties {

    /** 签发者 */
    private String issuer = "shopflow";

    /** 签名密钥，生产环境必须通过环境变量覆盖 */
    private String secret;

    /** 访问令牌有效期（分钟） */
    private long accessTokenExpireMinutes = 30;

    /** 刷新令牌有效期（天） */
    private long refreshTokenExpireDays = 7;

    public long getAccessTokenExpireSeconds() {
        return accessTokenExpireMinutes * 60;
    }

    public long getRefreshTokenExpireSeconds() {
        return refreshTokenExpireDays * 24 * 60 * 60;
    }
}