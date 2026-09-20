package com.shopflow.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品分类表（category）。
 *
 * <p>两级树结构：{@code parentId = 0} 表示一级分类，其余指向一级分类 ID。
 *
 * @author shopflow
 */
@Data
@TableName("category")
public class Category implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父分类 ID，0 表示一级分类 */
    private Long parentId;

    private String name;

    /** 层级：1-一级 2-二级 */
    private Integer level;

    private Integer sort;

    private String icon;

    /** 状态：0-停用 1-启用 */
    private Integer status;

    private Long deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}