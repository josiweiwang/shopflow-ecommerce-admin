package com.shopflow.vo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品详情（在列表字段基础上补充大字段 detail）。
 *
 * <p>详情与列表拆成两个 VO，是为了让列表查询不携带 TEXT 大字段——
 * 商品详情页每次只查一个商品，而列表页一次要取 10~100 条，
 * 带上大字段会显著增加网络与内存开销。
 *
 * @author shopflow
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductDetailVO extends ProductPageVO {

    @Schema(description = "商品详情（富文本）")
    private String detail;
}