package com.shopflow.vo.category;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 分类树节点。
 *
 * <p>树结构在服务层一次性组装完成（按 parentId 分组后递归挂载），
 * 前端拿到即可直接渲染，不需要多次请求拼树。
 *
 * @author shopflow
 */
@Data
public class CategoryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    private String name;

    @Schema(description = "层级：1-一级 2-二级")
    private Integer level;

    private Integer sort;

    private String icon;

    @Schema(description = "状态：0-停用 1-启用")
    private Integer status;

    private LocalDateTime createTime;

    @Schema(description = "子分类")
    private List<CategoryVO> children = new ArrayList<>();
}