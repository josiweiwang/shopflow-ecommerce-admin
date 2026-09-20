package com.shopflow.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 分配角色请求参数。
 *
 * @author shopflow
 */
@Data
public class AssignRolesDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "角色ID列表", example = "[2]")
    @NotEmpty(message = "角色不能为空")
    private List<Long> roleIds;
}