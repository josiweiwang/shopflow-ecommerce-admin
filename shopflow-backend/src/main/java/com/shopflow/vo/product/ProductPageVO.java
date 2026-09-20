package com.shopflow.vo.product;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.shopflow.common.enums.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品列表项（含分类名与库存，由 ProductMapper.xml 连表查询得到）。
 *
 * @author shopflow
 */
@Data
public class ProductPageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    private String name;

    private String sku;

    private String subtitle;

    private String mainImage;

    private BigDecimal price;

    private BigDecimal originalPrice;

    @Schema(description = "状态：0-下架 1-上架")
    private Integer status;

    private Integer sales;

    private Integer sort;

    @Schema(description = "可用库存")
    private Integer availableStock;

    @Schema(description = "锁定库存")
    private Integer lockedStock;

    @Schema(description = "总库存")
    private Integer totalStock;

    @Schema(description = "库存预警阈值")
    private Integer warnStock;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 状态描述，前端直接展示，避免各端各自维护映射 */
    public String getStatusDesc() {
        ProductStatus productStatus = ProductStatus.fromCode(this.status);
        return productStatus == null ? "" : productStatus.getDescription();
    }

    /** 是否低于预警阈值 */
    public boolean isLowStock() {
        return availableStock != null && warnStock != null && availableStock <= warnStock;
    }
}