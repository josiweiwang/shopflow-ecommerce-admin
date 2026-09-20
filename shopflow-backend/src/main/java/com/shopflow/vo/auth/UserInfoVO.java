package com.shopflow.vo.auth;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 当前登录用户信息。
 *
 * <p>用户 ID 用字符串返回：BIGINT 主键在 JS 中超过 2^53 会丢精度，
 * 统一序列化为字符串可以从根上避免前端拿到错误 ID。
 *
 * @author shopflow
 */
@Data
public class UserInfoVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String username;

    private String nickname;

    private String email;

    private String phone;

    private String avatar;

    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;

    @Schema(description = "角色编码集合")
    private Set<String> roleCodes;

    @Schema(description = "权限码集合")
    private Set<String> permissions;

    private LocalDateTime lastLoginAt;
}