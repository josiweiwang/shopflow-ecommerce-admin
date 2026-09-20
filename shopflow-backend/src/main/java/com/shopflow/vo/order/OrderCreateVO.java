package com.shopflow.vo.order;

import com.shopflow.common.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建订单返回结果。
 *
 * <p>只返回前端下单成功后立刻要用的信息（订单号、应付金额、支付截止时间），
 * 不回传整单数据，减少一次无意义的查询与序列化。
 *
 * @author shopflow
 */
@Data
@Builder
public class OrderCreateVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String orderNo;

    private BigDecimal payAmount;

    private Integer status;

    @Schema(description = "支付截止时间，逾期自动关单")
    private LocalDateTime expireTime;

    public String getStatusDesc() {
        OrderStatus orderStatus = OrderStatus.fromCode(this.status);
        return orderStatus == null ? "" : orderStatus.getDescription();
    }
}