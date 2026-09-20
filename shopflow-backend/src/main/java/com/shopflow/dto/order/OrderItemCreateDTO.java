package com.shopflow.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 下单商品明细参数。
 *
 * @author shopflow
 */
@Data
public class OrderItemCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "商品ID", example = "1")
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Schema(description = "购买数量", example = "2")
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于 0")
    @Max(value = 999, message = "单个商品购买数量不能超过 999")
    private Integer quantity;
}