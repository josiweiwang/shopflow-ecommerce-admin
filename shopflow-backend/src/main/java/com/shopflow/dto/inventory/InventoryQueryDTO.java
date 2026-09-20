package com.shopflow.dto.inventory;

import com.shopflow.dto.common.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库存分页查询条件。
 *
 * @author shopflow
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InventoryQueryDTO extends PageQuery {

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "仅查询低库存（可用库存 <= 预警阈值）")
    private Boolean lowStockOnly;
}