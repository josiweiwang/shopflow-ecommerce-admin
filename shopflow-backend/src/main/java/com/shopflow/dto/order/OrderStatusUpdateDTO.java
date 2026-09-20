package com.shopflow.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 修改订单状态请求参数（后台发货、确认收货等）。
 *
 * @author shopflow
 */
@Data
public class OrderStatusUpdateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "目标状态：1-已支付 2-配送中 3-已完成 4-已取消", example = "2")
    @NotNull(message = "目标状态不能为空")
    @Min(value = 0, message = "订单状态不合法")
    @Max(value = 4, message = "订单状态不合法")
    private Integer targetStatus;

    @Schema(description = "变更说明")
    @Size(max = 255, message = "备注最长 255 位")
    private String remark;
}