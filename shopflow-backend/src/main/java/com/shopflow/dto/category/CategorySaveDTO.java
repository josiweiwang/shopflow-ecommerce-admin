package com.shopflow.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 新增/修改分类请求参数。
 *
 * @author shopflow
 */
@Data
public class CategorySaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "父分类ID，0 表示一级分类", example = "0")
    @NotNull(message = "父分类不能为空")
    @Min(value = 0, message = "父分类ID不合法")
    private Long parentId;

    @Schema(description = "分类名称", example = "手机通讯")
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 32, message = "分类名称最长 32 位")
    private String name;

    @Schema(description = "排序值，越小越靠前", example = "1")
    @Min(value = 0, message = "排序值不能为负")
    private Integer sort = 0;

    @Schema(description = "分类图标")
    @Size(max = 255, message = "图标地址过长")
    private String icon;

    @Schema(description = "状态：0-停用 1-启用", example = "1")
    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status = 1;
}