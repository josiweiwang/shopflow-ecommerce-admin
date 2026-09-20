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
 * 库存流水表（inventory_log）。
 *
 * <p>只增不改：每次库存变更都留痕，支持「用流水推算库存」与库存表实际值对账，
 * 是排查线上超卖、少卖问题的关键手段。
 *
 * @author shopflow
 */
@Data
@TableName("inventory_log")
public class InventoryLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    /** 关联订单号，非订单操作可为空串 */
    private String orderNo;

    /** 业务类型：见 InventoryBizType */
    private Integer bizType;

    /** 变更数量，正数增加负数减少 */
    private Integer quantity;

    private Integer beforeAvailable;

    private Integer afterAvailable;

    /** 操作人 ID，0 表示系统 */
    private Long operatorId;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}