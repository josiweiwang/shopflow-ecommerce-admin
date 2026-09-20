package com.shopflow.vo.order;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单明细项（商品信息为下单时的快照）。
 *
 * @author shopflow
 */
@Data
public class OrderItemVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long productId;

    private String sku;

    @Schema(description = "商品名称（下单时快照）")
    private String productName;

    private String productImage;

    @Schema(description = "成交单价（下单时快照）")
    private BigDecimal price;

    private Integer quantity;

    private BigDecimal subtotal;
}