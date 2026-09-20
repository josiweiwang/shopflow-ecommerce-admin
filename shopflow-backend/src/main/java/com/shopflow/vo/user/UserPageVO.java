package com.shopflow.vo.user;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户列表项。
 *
 * @author shopflow
 */
@Data
public class UserPageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String username;

    private String nickname;

    private String email;

    private String phone;

    private String avatar;

    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;

    @Schema(description = "角色名称，多个用逗号分隔")
    private String roleNames;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createTime;
}