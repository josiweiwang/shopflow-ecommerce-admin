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
 * 订单状态流转日志表（order_status_log）。
 *
 * <p>每次状态变更写入一条，{@code fromStatus -> toStatus} 可完整还原订单生命周期，
 * 便于排查「订单状态异常」类线上问题。
 *
 * @author shopflow
 */
@Data
@TableName("order_status_log")
public class OrderStatusLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String orderNo;

    /** 变更前状态，NULL 表示订单创建 */
    private Integer fromStatus;

    private Integer toStatus;

    private Long operatorId;

    /** 操作人类型：见 OrderOperatorType */
    private Integer operatorType;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}