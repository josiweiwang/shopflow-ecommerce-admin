package com.shopflow.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录成功返回结果。
 *
 * <p>采用双 Token 机制：
 * <ul>
 *     <li>accessToken 有效期短（默认 30 分钟），用于访问业务接口；</li>
 *     <li>refreshToken 有效期长（默认 7 天），只用于换取新的 accessToken，且存储于 Redis 可随时失效。</li>
 * </ul>
 * 这样既避免频繁登录，又能在令牌泄露时把影响窗口限制在 30 分钟内。
 *
 * @author shopflow
 */
@Data
@Builder
public class LoginVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "令牌类型", example = "Bearer")
    private String tokenType;

    @Schema(description = "访问令牌")
    private String accessToken;

    @Schema(description = "刷新令牌")
    private String refreshToken;

    @Schema(description = "访问令牌有效期（秒）", example = "1800")
    private Long expiresIn;

    @Schema(description = "登录用户信息")
    private UserInfoVO userInfo;
}