package com.shopflow.vo.inventory;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.shopflow.common.enums.InventoryBizType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 库存流水记录。
 *
 * @author shopflow
 */
@Data
public class InventoryLogVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long productId;

    private String productName;

    private String orderNo;

    @Schema(description = "业务类型：1-入库 2-锁定 3-扣减 4-释放 5-盘点调整")
    private Integer bizType;

    private Integer quantity;

    private Integer beforeAvailable;

    private Integer afterAvailable;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long operatorId;

    private String remark;

    private LocalDateTime createTime;

    public String getBizTypeDesc() {
        InventoryBizType type = InventoryBizType.fromCode(this.bizType);
        return type == null ? "" : type.getDescription();
    }
}