package com.shopflow.dto.product;

import com.shopflow.dto.common.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商品分页查询条件。
 *
 * @author shopflow
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductQueryDTO extends PageQuery {

    @Schema(description = "关键词，匹配商品名称或 SKU")
    private String keyword;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "状态：0-下架 1-上架")
    private Integer status;

    @Schema(description = "最低价")
    private BigDecimal minPrice;

    @Schema(description = "最高价")
    private BigDecimal maxPrice;
}