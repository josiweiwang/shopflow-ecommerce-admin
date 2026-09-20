package com.shopflow.dto.log;

import com.shopflow.dto.common.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作日志分页查询条件。
 *
 * @author shopflow
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OperationLogQueryDTO extends PageQuery {

    @Schema(description = "关键词，匹配操作描述或用户名")
    private String keyword;

    @Schema(description = "业务模块")
    private String module;

    @Schema(description = "是否成功：0-失败 1-成功")
    private Integer success;
}