package com.shopflow.dto.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 库存盘点调整请求参数。
 *
 * @author shopflow
 */
@Data
public class InventoryAdjustDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "调整数量，正数增加、负数减少", example = "-5")
    @NotNull(message = "调整数量不能为空")
    private Integer delta;

    @Schema(description = "调整原因")
    @Size(max = 255, message = "备注最长 255 位")
    private String remark;
}