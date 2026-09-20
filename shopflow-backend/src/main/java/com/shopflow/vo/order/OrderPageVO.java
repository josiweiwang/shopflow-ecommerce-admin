package com.shopflow.vo.order;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.shopflow.common.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单列表项。
 *
 * @author shopflow
 */
@Data
public class OrderPageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String orderNo;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @Schema(description = "下单用户名")
    private String username;

    private BigDecimal totalAmount;

    private BigDecimal freightAmount;

    private BigDecimal payAmount;

    @Schema(description = "状态：0-待支付 1-已支付 2-配送中 3-已完成 4-已取消")
    private Integer status;

    private String receiverName;

    private String receiverPhone;

    @Schema(description = "商品种类数")
    private Integer itemCount;

    @Schema(description = "商品总件数")
    private Integer totalQuantity;

    private LocalDateTime expireTime;

    private LocalDateTime payTime;

    private LocalDateTime createTime;

    public String getStatusDesc() {
        OrderStatus orderStatus = OrderStatus.fromCode(this.status);
        return orderStatus == null ? "" : orderStatus.getDescription();
    }
}