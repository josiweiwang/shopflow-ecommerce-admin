package com.shopflow.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录请求参数。
 *
 * @author shopflow
 */
@Data
public class LoginDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户名", example = "admin")
    @NotBlank(message = "用户名不能为空")
    @Size(max = 32, message = "用户名长度不合法")
    private String username;

    @Schema(description = "密码", example = "Admin@123456")
    @NotBlank(message = "密码不能为空")
    @Size(max = 32, message = "密码长度不合法")
    private String password;
}