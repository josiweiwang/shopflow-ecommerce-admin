package com.shopflow.vo.inventory;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 库存信息。
 *
 * @author shopflow
 */
@Data
public class InventoryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long productId;

    private String productName;

    private String sku;

    @Schema(description = "总库存 = 可用 + 锁定")
    private Integer totalStock;

    private Integer availableStock;

    private Integer lockedStock;

    private Integer warnStock;

    @Schema(description = "乐观锁版本号")
    private Integer version;

    @Schema(description = "是否为 Redis 缓存中的库存值")
    private Boolean fromCache;

    private LocalDateTime updateTime;

    /** 是否低于预警阈值 */
    public boolean isLowStock() {
        return availableStock != null && warnStock != null && availableStock <= warnStock;
    }
}