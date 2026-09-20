package com.shopflow.dto.user;

import com.shopflow.dto.common.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户分页查询条件。
 *
 * @author shopflow
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserQueryDTO extends PageQuery {

    @Schema(description = "关键词，匹配用户名/昵称/手机号")
    private String keyword;

    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;

    @Schema(description = "角色ID")
    private Long roleId;
}