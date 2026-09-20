package com.shopflow.vo.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 后台看板核心指标。
 *
 * @author shopflow
 */
@Data
@Builder
public class DashboardOverviewVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户总数")
    private Long userCount;

    @Schema(description = "商品总数")
    private Long productCount;

    @Schema(description = "有效订单总数（不含已取消）")
    private Long orderCount;

    @Schema(description = "待支付订单数")
    private Long pendingPaymentCount;

    @Schema(description = "今日订单数")
    private Long todayOrderCount;

    @Schema(description = "今日成交额")
    private BigDecimal todayAmount;

    @Schema(description = "累计成交额")
    private BigDecimal totalAmount;
}