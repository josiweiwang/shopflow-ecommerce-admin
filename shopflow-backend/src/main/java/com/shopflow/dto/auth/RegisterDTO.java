package com.shopflow.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 注册请求参数。
 *
 * <p>密码强度在参数层做基础约束，真正的加密与唯一性校验在 Service 层完成。
 *
 * @author shopflow
 */
@Data
public class RegisterDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户名，4-32 位字母数字下划线", example = "shopee2026")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 32, message = "用户名长度需在 4-32 位之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字与下划线")
    private String username;

    @Schema(description = "密码，8-32 位且必须同时包含字母与数字", example = "User@123456")
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度需在 8-32 位之间")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码必须同时包含字母和数字")
    private String password;

    @Schema(description = "昵称", example = "小蒋")
    @Size(max = 32, message = "昵称最长 32 位")
    private String nickname;

    @Schema(description = "邮箱", example = "user@example.com")
    @Email(message = "邮箱格式不正确")
    @Size(max = 64, message = "邮箱最长 64 位")
    private String email;

    @Schema(description = "手机号", example = "13800000000")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}