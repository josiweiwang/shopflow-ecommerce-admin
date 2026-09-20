package com.shopflow.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 修改分类请求参数。
 *
 * @author shopflow
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CategoryUpdateDTO extends CategorySaveDTO {

    @Schema(description = "分类ID")
    @NotNull(message = "分类ID不能为空")
    private Long id;
}