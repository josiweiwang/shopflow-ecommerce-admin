package com.shopflow.dto.inventory;

import com.shopflow.dto.common.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库存流水分页查询条件。
 *
 * @author shopflow
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InventoryLogQueryDTO extends PageQuery {

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "业务类型：1-入库 2-锁定 3-扣减 4-释放 5-盘点调整")
    private Integer bizType;

    @Schema(description = "订单号")
    private String orderNo;
}