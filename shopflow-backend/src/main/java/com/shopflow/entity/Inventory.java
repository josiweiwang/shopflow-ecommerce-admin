package com.shopflow.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 库存表（inventory），与商品一对一。
 *
 * <p>库存拆成独立表的三个原因：
 * <ol>
 *     <li>库存是高频更新的热点行，商品信息以读为主，拆开后锁竞争范围更小；</li>
 *     <li>商品查询不需要带库存字段，避免「读商品被写库存阻塞」；</li>
 *     <li>库存可独立做 Redis 缓存、流水审计与对账。</li>
 * </ol>
 *
 * <p>库存恒等式：{@code totalStock = availableStock + lockedStock}。
 * 下单锁定可用库存，支付时扣减锁定与总库存，取消时把锁定退回可用库存。
 *
 * @author shopflow
 */
@Data
@TableName("inventory")
public class Inventory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    /** 总库存 = 可用库存 + 锁定库存 */
    private Integer totalStock;

    /** 可用库存 */
    private Integer availableStock;

    /** 锁定库存（待支付订单占用） */
    private Integer lockedStock;

    /** 库存预警阈值 */
    private Integer warnStock;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}