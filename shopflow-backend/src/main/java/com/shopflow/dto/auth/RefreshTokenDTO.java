package com.shopflow.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 刷新令牌请求参数。
 *
 * @author shopflow
 */
@Data
public class RefreshTokenDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "登录时返回的 refreshToken")
    @NotBlank(message = "刷新令牌不能为空")
    private String refreshToken;
}