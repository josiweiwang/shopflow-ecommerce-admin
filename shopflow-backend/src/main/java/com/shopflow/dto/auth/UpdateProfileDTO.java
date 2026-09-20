package com.shopflow.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 修改个人资料请求参数。
 *
 * @author shopflow
 */
@Data
public class UpdateProfileDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "昵称")
    @Size(max = 32, message = "昵称最长 32 位")
    private String nickname;

    @Schema(description = "邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(max = 64, message = "邮箱最长 64 位")
    private String email;

    @Schema(description = "手机号")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "头像地址")
    @Size(max = 255, message = "头像地址过长")
    private String avatar;
}