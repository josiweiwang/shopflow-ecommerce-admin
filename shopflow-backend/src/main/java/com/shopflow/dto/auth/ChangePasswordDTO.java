package com.shopflow.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 修改密码请求参数。
 *
 * @author shopflow
 */
@Data
public class ChangePasswordDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "原密码")
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @Schema(description = "新密码，8-32 位且必须同时包含字母与数字")
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 32, message = "新密码长度需在 8-32 位之间")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "新密码必须同时包含字母和数字")
    private String newPassword;
}