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
 * 后台操作日志表（sys_operation_log）。
 *
 * <p>由 AOP 切面在管理员关键操作后异步写入，用于审计与问题回溯。
 * 请求参数中的密码等敏感字段在写入前会被脱敏。
 *
 * @author shopflow
 */
@Data
@TableName("sys_operation_log")
public class SysOperationLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 操作人用户名（快照） */
    private String username;

    private String module;

    private String operation;

    private String requestUri;

    private String requestMethod;

    /** 请求参数（脱敏后） */
    private String requestParam;

    private String ip;

    private Long durationMs;

    /** 是否成功：0-失败 1-成功 */
    private Integer success;

    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}