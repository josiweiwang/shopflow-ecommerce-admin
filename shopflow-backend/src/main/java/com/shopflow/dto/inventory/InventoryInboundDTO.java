package com.shopflow.dto.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 商品入库请求参数。
 *
 * @author shopflow
 */
@Data
public class InventoryInboundDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "入库数量", example = "100")
    @NotNull(message = "入库数量不能为空")
    @Min(value = 1, message = "入库数量必须大于 0")
    @Max(value = 1000000, message = "单次入库数量过大")
    private Integer quantity;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注最长 255 位")
    private String remark;
}