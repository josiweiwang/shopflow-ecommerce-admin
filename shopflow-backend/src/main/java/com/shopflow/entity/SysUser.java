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
 * 用户表（sys_user）。
 *
 * <p>逻辑删除约定：{@code deleted = 0} 表示未删除，删除时写入本行主键 ID。
 * 这样 (username, deleted) 唯一索引既能约束活跃数据，又能保留历史删除记录。
 *
 * @author shopflow
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录用户名 */
    private String username;

    /** BCrypt 密文，绝不返回给前端 */
    private String password;

    /** 昵称 */
    private String nickname;

    private String email;

    private String phone;

    private String avatar;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    private LocalDateTime lastLoginAt;

    private String lastLoginIp;

    /** 逻辑删除：0-未删除，非 0-写入主键 ID */
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