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
 * 订单明细表（order_item）。
 *
 * <p>商品名称、价格、SKU、主图都是「下单时刻的快照」，不是外键引用。
 * 因此商品改价、改名、下架甚至删除，都不会影响历史订单的展示与金额——
 * 这是电商领域中很容易被忽略、但必须做对的设计。
 *
 * <p>明细只增不改，所以没有 update_time。
 *
 * @author shopflow
 */
@Data
@TableName("order_item")
public class OrderItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String orderNo;

    private Long productId;

    /** 商品编码（快照） */
    private String sku;

    /** 商品名称（快照） */
    private String productName;

    /** 商品主图（快照） */
    private String productImage;

    /** 成交单价（快照） */
    private BigDecimal price;

    private Integer quantity;

    /** 小计 = 单价 * 数量 */
    private BigDecimal subtotal;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}