package com.shopflow.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表（orders，表名避开 MySQL 关键字 order）。
 *
 * <p>收货人信息为下单时的快照，用户后续修改资料不影响历史订单。
 *
 * @author shopflow
 */
@Data
@TableName("orders")
public class Orders implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务订单号，对外暴露的唯一标识 */
    private String orderNo;

    private Long userId;

    /** 商品总额 */
    private BigDecimal totalAmount;

    /** 运费 */
    private BigDecimal freightAmount;

    /** 应付金额 = 商品总额 + 运费 */
    private BigDecimal payAmount;

    /** 状态：见 OrderStatus */
    private Integer status;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    private String remark;

    /** 客户端幂等号，配合 (userId, requestNo) 唯一索引防重复下单 */
    private String requestNo;

    /** 支付截止时间 */
    private LocalDateTime expireTime;

    private LocalDateTime payTime;

    private LocalDateTime deliverTime;

    private LocalDateTime finishTime;

    private LocalDateTime cancelTime;

    private String cancelReason;

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