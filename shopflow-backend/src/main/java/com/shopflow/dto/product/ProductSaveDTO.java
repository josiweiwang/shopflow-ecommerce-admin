package com.shopflow.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 新增商品请求参数。
 *
 * @author shopflow
 */
@Data
public class ProductSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "分类ID", example = "4")
    @NotNull(message = "商品分类不能为空")
    private Long categoryId;

    @Schema(description = "商品名称", example = "ShopFlow 智能手机 Pro")
    @NotBlank(message = "商品名称不能为空")
    @Size(max = 128, message = "商品名称最长 128 位")
    private String name;

    @Schema(description = "商品编码，全局唯一", example = "SF-PHONE-0007")
    @NotBlank(message = "商品编码不能为空")
    @Size(max = 64, message = "商品编码最长 64 位")
    @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "商品编码只能包含字母、数字与短横线")
    private String sku;

    @Schema(description = "副标题/卖点")
    @Size(max = 255, message = "副标题最长 255 位")
    private String subtitle;

    @Schema(description = "主图地址")
    @Size(max = 255, message = "主图地址过长")
    private String mainImage;

    @Schema(description = "商品详情")
    private String detail;

    @Schema(description = "销售价", example = "3999.00")
    @NotNull(message = "销售价不能为空")
    @DecimalMin(value = "0.00", message = "销售价不能为负")
    private BigDecimal price;

    @Schema(description = "原价", example = "4299.00")
    @DecimalMin(value = "0.00", message = "原价不能为负")
    private BigDecimal originalPrice;

    @Schema(description = "状态：0-下架 1-上架", example = "1")
    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status = 0;

    @Schema(description = "排序值", example = "1")
    @Min(value = 0, message = "排序值不能为负")
    private Integer sort = 0;

    @Schema(description = "初始库存，新增商品时同时初始化库存记录", example = "100")
    @Min(value = 0, message = "初始库存不能为负")
    private Integer initStock = 0;

    @Schema(description = "库存预警阈值", example = "10")
    @Min(value = 0, message = "预警阈值不能为负")
    private Integer warnStock = 10;
}