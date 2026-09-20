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
 * 权限表（sys_permission）。
 *
 * @author shopflow
 */
@Data
@TableName("sys_permission")
public class SysPermission implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 权限码，如 product:create */
    private String permissionCode;

    private String permissionName;

    /** 所属模块：user / product / order / inventory / dashboard / log */
    private String module;

    /** 类型：1-菜单 2-按钮 3-接口 */
    private Integer type;

    private Integer sort;

    private Integer status;

    private Long deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}