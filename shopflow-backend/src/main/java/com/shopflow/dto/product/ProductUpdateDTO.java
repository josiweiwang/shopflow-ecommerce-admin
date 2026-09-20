package com.shopflow.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 修改商品请求参数。
 *
 * @author shopflow
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductUpdateDTO extends ProductSaveDTO {

    @Schema(description = "商品ID")
    @NotNull(message = "商品ID不能为空")
    private Long id;
}